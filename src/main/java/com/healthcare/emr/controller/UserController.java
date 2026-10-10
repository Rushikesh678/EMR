package com.healthcare.emr.controller;

import com.healthcare.emr.dto.ApiResponse;
import com.healthcare.emr.dto.UserDto;
import com.healthcare.emr.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers()));
    }

    @PatchMapping("/{userId}/toggle-status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> toggleUserStatus(@PathVariable Long userId,
                                                                 @AuthenticationPrincipal UserDetails userDetails) {
        UserDto updated = userService.toggleUserStatus(userId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("User status changed successfully", updated));
    }

    @PostMapping("/staff")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> createStaffMember(@RequestBody Map<String, Object> payload,
                                                                  @AuthenticationPrincipal UserDetails userDetails) {
        UserDto created = userService.createStaffMember(payload, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Staff account created successfully", created));
    }
}
