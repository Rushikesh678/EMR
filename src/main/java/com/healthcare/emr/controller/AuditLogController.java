package com.healthcare.emr.controller;

import com.healthcare.emr.dto.ApiResponse;
import com.healthcare.emr.entity.AuditLog;
import com.healthcare.emr.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getRecentAuditLogs() {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getRecentLogs()));
    }
}
