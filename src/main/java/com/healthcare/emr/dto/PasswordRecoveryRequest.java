package com.healthcare.emr.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class PasswordRecoveryRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    private String email;

    private String newPassword;
    private String resetToken;

    public PasswordRecoveryRequest() {}

    public PasswordRecoveryRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }
}
