package com.example.Projectly.config;

import com.example.Projectly.bean.core.role.Role;
import com.example.Projectly.dao.facade.security.RoleDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Initializes roles in the database if they don't exist.
 * This ensures roles are available even if Flyway migrations haven't run yet.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class RoleInitializer implements CommandLineRunner {

    private final RoleDao roleDao;

    @Override
    public void run(String... args) {
        log.info("Checking and initializing roles...");
        
        // Initialize roles from configuration
        initializeRole("ROLE_ADMIN");
        initializeRole("ROLE_MANAGER");
        initializeRole("ROLE_MEMBER");
        
        log.info("Role initialization completed. Total roles: {}", roleDao.count());
    }

    private void initializeRole(String roleName) {
        if (!roleDao.findByName(roleName).isPresent()) {
            Role role = Role.builder()
                    .name(roleName)
                    .createdDate(LocalDateTime.now())
                    .createdBy("system")
                    .build();
            roleDao.save(role);
            log.info("Created role: {}", roleName);
        } else {
            log.debug("Role already exists: {}", roleName);
        }
    }
}
