package com.example.Projectly.config;

import com.example.Projectly.bean.core.role.Role;
import com.example.Projectly.bean.core.user.User;
import com.example.Projectly.dao.facade.security.RoleDao;
import com.example.Projectly.dao.facade.security.UserDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Seeds an initial admin user on first startup if no admin exists.
 * Runs after RoleInitializer (Order 2) to ensure roles are available.
 *
 * <p>Configure via application.yml:</p>
 * <pre>
 * app:
 *   admin:
 *     email: admin@yourapp.com
 *     password: YourSecurePassword1!
 *     first-name: Admin
 *     last-name: User
 * </pre>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class DataInitializer implements CommandLineRunner {

    private final UserDao userDao;
    private final RoleDao roleDao;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@projectly.com}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.first-name:Admin}")
    private String adminFirstName;

    @Value("${app.admin.last-name:User}")
    private String adminLastName;

    @Override
    @Transactional
    public void run(String... args) {
        initializeAdminUser();
    }

    private void initializeAdminUser() {
        if (userDao.existsByEmail(adminEmail)) {
            log.debug("Admin user already exists: {}", adminEmail);
            return;
        }

        Role adminRole = roleDao.findByName("ROLE_ADMIN")
                .orElse(null);

        if (adminRole == null) {
            log.warn("ROLE_ADMIN not found. Skipping admin user creation.");
            return;
        }

        List<Role> roles = new ArrayList<>();
        roles.add(adminRole);
        roleDao.findByName("ROLE_USER").ifPresent(roles::add);

        boolean usingDefaultPassword = (adminPassword == null || adminPassword.isBlank());
        String password;

        if (usingDefaultPassword) {
            byte[] bytes = new byte[16];
            new SecureRandom().nextBytes(bytes);
            password = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } else {
            password = adminPassword;
        }

        User admin = User.builder()
                .firstName(adminFirstName)
                .lastName(adminLastName)
                .email(adminEmail)
                .password(passwordEncoder.encode(password))
                .enabled(true)
                .locked(false)
                .credentialsExpired(false)
                .emailVerified(true)
                .roles(roles)
                .build();

        userDao.save(admin);

        log.info("============================================");
        log.info("  Initial admin user created successfully");
        log.info("  Email: {}", adminEmail);
        if (usingDefaultPassword) {
            log.info("  Password: {} (auto-generated — set ADMIN_PASSWORD env var)", password);
            log.info("  >> CHANGE YOUR PASSWORD AFTER FIRST LOGIN <<");
        } else {
            log.info("  Password: (configured via ADMIN_PASSWORD)");
        }
        log.info("============================================");
    }
}
