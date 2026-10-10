package com.healthcare.emr;

import com.healthcare.emr.entity.AuditLog;
import com.healthcare.emr.service.AuditLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuditLogServiceTest {

    @Autowired
    private AuditLogService auditLogService;

    @Test
    @DisplayName("Should write audit logs and retrieve recent audit trails")
    void testAuditLogging() {
        auditLogService.log(
                "tester@healthcare.local",
                "ROLE_ADMIN",
                "TEST_ACTION",
                "Patient",
                101L,
                null,
                "Executed automated audit log test verification"
        );

        List<AuditLog> recentLogs = auditLogService.getRecentLogs();
        assertFalse(recentLogs.isEmpty(), "Recent logs should contain entries");

        AuditLog latest = recentLogs.get(0);
        assertNotNull(latest.getTimestamp());
        assertNotNull(latest.getAction());
    }
}
