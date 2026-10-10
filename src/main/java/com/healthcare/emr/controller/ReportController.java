package com.healthcare.emr.controller;

import com.healthcare.emr.dto.ApiResponse;
import com.healthcare.emr.dto.DashboardSummaryDto;
import com.healthcare.emr.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR', 'ROLE_RECEPTIONIST')")
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> getSystemAnalytics() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getSystemAnalytics()));
    }

    @GetMapping("/export-pdf")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR')")
    public ResponseEntity<byte[]> exportAnalyticsPdf() {
        byte[] pdfBytes = reportService.generateSystemReportPdf();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Healthcare_Operations_Report_" + LocalDate.now() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
