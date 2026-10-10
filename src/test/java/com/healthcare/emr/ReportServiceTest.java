package com.healthcare.emr;

import com.healthcare.emr.dto.DashboardSummaryDto;
import com.healthcare.emr.entity.Prescription;
import com.healthcare.emr.repository.PrescriptionRepository;
import com.healthcare.emr.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ReportServiceTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Test
    @DisplayName("Should compute aggregated system analytics")
    void testGetSystemAnalytics() {
        DashboardSummaryDto analytics = reportService.getSystemAnalytics();
        assertNotNull(analytics);
        assertTrue(analytics.getTotalPatients() >= 0);
        assertTrue(analytics.getTotalDoctors() >= 0);
        assertTrue(analytics.getTotalAppointments() >= 0);
        assertNotNull(analytics.getTotalPaidRevenue());
        assertNotNull(analytics.getTotalPendingRevenue());
        assertNotNull(analytics.getAppointmentsByStatus());
        assertNotNull(analytics.getAppointmentsBySpecialization());
    }

    @Test
    @DisplayName("Should generate valid system report PDF byte array")
    void testGenerateSystemReportPdf() {
        byte[] pdfBytes = reportService.generateSystemReportPdf();
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 100);

        // Verify PDF signature (%PDF-)
        String header = new String(pdfBytes, 0, Math.min(pdfBytes.length, 5));
        assertEquals("%PDF-", header);
    }

    @Test
    @DisplayName("Should generate valid prescription PDF byte array")
    void testGeneratePrescriptionPdf() {
        Prescription rx = prescriptionRepository.findAll().get(0);
        byte[] pdfBytes = reportService.generatePrescriptionPdf(rx.getPrescriptionId());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 100);

        String header = new String(pdfBytes, 0, Math.min(pdfBytes.length, 5));
        assertEquals("%PDF-", header);
    }
}
