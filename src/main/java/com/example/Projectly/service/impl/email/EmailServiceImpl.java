package com.example.Projectly.service.impl.email;

import com.example.Projectly.config.email.EmailProperties;
import com.example.Projectly.service.facade.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    @Override
    @Async
    public void sendPasswordResetEmail(final String toEmail, final String firstName, final String resetToken) {
        final String subject = "Reset your password";
        final String body = buildPasswordResetBody(firstName, resetToken);
        send(toEmail, subject, body);
    }

    private void send(final String to, final String subject, final String htmlBody) {
        try {
            final MimeMessage message = this.mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(this.emailProperties.getFromAddress(), this.emailProperties.getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            this.mailSender.send(message);
            log.debug("Email sent to {}: {}", to, subject);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String buildPasswordResetBody(final String firstName, final String token) {
        return """
                <html><body style="font-family:sans-serif;color:#222">
                <h2>Hi %s,</h2>
                <p>We received a request to reset your password.</p>
                <p>Use the token below in the reset password form (valid for %d minutes):</p>
                <p style="font-size:1.4em;font-weight:bold;letter-spacing:2px">%s</p>
                <p>If you did not request a password reset, ignore this email.</p>
                </body></html>
                """.formatted(firstName, this.emailProperties.getResetTokenExpiryMinutes(), token);
    }

}
