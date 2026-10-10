package com.healthcare.emr.controller;

import com.healthcare.emr.dto.ApiResponse;
import com.healthcare.emr.entity.Notification;
import com.healthcare.emr.entity.User;
import com.healthcare.emr.exception.UnauthorizedException;
import com.healthcare.emr.repository.UserRepository;
import com.healthcare.emr.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping("/my-notifications")
    public ResponseEntity<ApiResponse<List<Notification>>> getMyNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        return ResponseEntity.ok(ApiResponse.success(notificationService.getUserNotifications(user.getUserId())));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(ApiResponse.success("Marked as read", null));
    }

    @GetMapping("/dev-previews")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getDevEmailPreviews() {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getDevEmailPreviews()));
    }
}
