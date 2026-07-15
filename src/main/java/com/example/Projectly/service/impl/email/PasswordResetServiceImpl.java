package com.example.Projectly.service.impl.email;

import com.example.Projectly.bean.core.user.PasswordResetToken;
import com.example.Projectly.bean.core.user.User;
import com.example.Projectly.config.email.EmailProperties;
import com.example.Projectly.dao.facade.email.PasswordResetTokenDao;
import com.example.Projectly.dao.facade.security.UserDao;
import com.example.Projectly.exception.BusinessException;
import com.example.Projectly.service.facade.email.EmailService;
import com.example.Projectly.service.facade.email.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import static com.example.Projectly.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserDao userDao;
    private final PasswordResetTokenDao passwordResetTokenDao;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final EmailProperties emailProperties;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    @Transactional(timeout = 30)
    public void initiateForgotPassword(final String email) {
        final User user = this.userDao.findByEmail(email).orElse(null);
        // Always respond with 202 even if email not found — prevents user enumeration
        if (user == null) {
            log.debug("Password reset requested for unknown email: {}", email);
            return;
        }

        this.passwordResetTokenDao.deleteAllByUserId(user.getId());

        final String rawToken = generateToken();
        final PasswordResetToken token = PasswordResetToken.builder()
                .token(rawToken)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(this.emailProperties.getResetTokenExpiryMinutes()))
                .used(false)
                .build();

        this.passwordResetTokenDao.save(token);
        this.emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), rawToken);
        log.debug("Password reset token issued for user {}", user.getId());
    }

    @Override
    @Transactional(timeout = 30)
    public void resetPassword(final String rawToken, final String newPassword, final String confirmNewPassword) {
        if (!newPassword.equals(confirmNewPassword)) {
            throw new BusinessException(CHANGE_PASSWORD_MISMATCH);
        }

        final PasswordResetToken token = this.passwordResetTokenDao.findByToken(rawToken)
                .orElseThrow(() -> new BusinessException(TOKEN_INVALID));

        if (token.isUsed()) {
            throw new BusinessException(TOKEN_ALREADY_USED);
        }
        if (token.isExpired()) {
            throw new BusinessException(TOKEN_EXPIRED);
        }

        final User user = token.getUser();
        user.setPassword(this.passwordEncoder.encode(newPassword));
        this.userDao.save(user);

        token.setUsed(true);
        this.passwordResetTokenDao.save(token);
        log.debug("Password reset completed for user {}", user.getId());
    }

    private String generateToken() {
        final byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
