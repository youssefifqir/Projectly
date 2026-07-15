package com.example.Projectly.service.facade.email;

public interface EmailService {

    void sendPasswordResetEmail(String toEmail, String firstName, String resetToken);

}
