package com.healthcare.emr.controller;

import com.healthcare.emr.dto.*;
import com.healthcare.emr.entity.User;
import com.healthcare.emr.repository.UserRepository;
import com.healthcare.emr.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.registerPatient(request);
        return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody PasswordRecoveryRequest request) {
        authService.initiatePasswordRecovery(request);
        return ResponseEntity.ok(ApiResponse.success("If the account exists, a recovery code has been dispatched. (Check Dev Notifications tab/log).", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody PasswordRecoveryRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully. Please login with your new password.", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Not authenticated"));
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("name", user.getName());
        data.put("email", user.getEmail());
        data.put("phone", user.getPhone());
        data.put("role", user.getRole().name());
        data.put("roleEntityId", authService.resolveRoleEntityId(user));
        data.put("redirectPath", authService.resolveRedirectPath(user.getRole()));

        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
