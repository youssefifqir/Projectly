package com.example.Projectly.service.facade.email;

public interface PasswordResetService {

    void initiateForgotPassword(String email);

    void resetPassword(String token, String newPassword, String confirmNewPassword);

}
