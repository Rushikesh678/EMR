package com.healthcare.emr;

import com.healthcare.emr.dto.AuthRequest;
import com.healthcare.emr.dto.AuthResponse;
import com.healthcare.emr.dto.PasswordRecoveryRequest;
import com.healthcare.emr.dto.RegisterRequest;
import com.healthcare.emr.exception.BadRequestException;
import com.healthcare.emr.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Test
    @DisplayName("Should successfully authenticate seeded admin account")
    void testAdminLogin() {
        AuthRequest request = new AuthRequest("admin@healthcare.local", "Admin@123");
        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("ROLE_ADMIN", response.getRole());
        assertEquals("#admin-portal", response.getRedirectPath());
    }

    @Test
    @DisplayName("Should successfully authenticate seeded doctor account")
    void testDoctorLogin() {
        AuthRequest request = new AuthRequest("dr.sarah@healthcare.local", "Doctor@123");
        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("ROLE_DOCTOR", response.getRole());
        assertEquals("#doctor-portal", response.getRedirectPath());
    }

    @Test
    @DisplayName("Should reject invalid password")
    void testInvalidPassword() {
        AuthRequest request = new AuthRequest("admin@healthcare.local", "WrongPassword");
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Should register a new patient and return valid token")
    void testPatientRegistration() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Alice Wonderland");
        request.setEmail("alice.unique." + System.currentTimeMillis() + "@patient.local");
        request.setPassword("Secret@123");
        request.setPhone("+1-555-9988");
        request.setGender("Female");
        request.setBloodGroup("B+");
        request.setDateOfBirth(LocalDate.of(1992, 4, 12));

        AuthResponse res = authService.registerPatient(request);
        assertNotNull(res);
        assertNotNull(res.getToken());
        assertEquals("ROLE_PATIENT", res.getRole());
        assertNotNull(res.getRoleEntityId());
    }

    @Test
    @DisplayName("Should reject registration with already registered email")
    void testDuplicateRegistration() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Duplicate Admin");
        request.setEmail("admin@healthcare.local");
        request.setPassword("Secret@123");

        assertThrows(BadRequestException.class, () -> authService.registerPatient(request));
    }

    @Test
    @DisplayName("Should initiate password recovery and reset password")
    void testPasswordRecoveryAndReset() {
        PasswordRecoveryRequest recoveryRequest = new PasswordRecoveryRequest();
        recoveryRequest.setEmail("john.doe@patient.local");

        assertDoesNotThrow(() -> authService.initiatePasswordRecovery(recoveryRequest));

        // Reset password
        recoveryRequest.setNewPassword("NewPatientPass@123");
        assertDoesNotThrow(() -> authService.resetPassword(recoveryRequest));

        // Authenticate with new password
        AuthRequest newLogin = new AuthRequest("john.doe@patient.local", "NewPatientPass@123");
        AuthResponse res = authService.login(newLogin);
        assertNotNull(res);
        assertNotNull(res.getToken());
    }
}
