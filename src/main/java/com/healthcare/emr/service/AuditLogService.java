package com.healthcare.emr.service;

import com.healthcare.emr.entity.AuditLog;
import com.healthcare.emr.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(String email, String role, String action, String entityName, Long entityId, HttpServletRequest request, String details) {
        String ipAddress = request != null ? request.getRemoteAddr() : "127.0.0.1";
        AuditLog log = new AuditLog(email, role, action, entityName, entityId, ipAddress, details);
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop100ByOrderByTimestampDesc();
    }
}
