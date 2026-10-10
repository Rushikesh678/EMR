package com.healthcare.emr.controller;

import com.healthcare.emr.dto.ApiResponse;
import com.healthcare.emr.entity.*;
import com.healthcare.emr.exception.UnauthorizedException;
import com.healthcare.emr.repository.*;
import com.healthcare.emr.service.LaboratoryReportService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/laboratory-reports")
public class LaboratoryReportController {

    private final LaboratoryReportService laboratoryReportService;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public LaboratoryReportController(LaboratoryReportService laboratoryReportService,
                                      UserRepository userRepository,
                                      PatientRepository patientRepository,
                                      DoctorRepository doctorRepository) {
        this.laboratoryReportService = laboratoryReportService;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR', 'ROLE_RECEPTIONIST')")
    public ResponseEntity<ApiResponse<LaboratoryReport>> uploadReport(
            @RequestParam("patientId") Long patientId,
            @RequestParam(value = "doctorId", required = false) Long doctorId,
            @RequestParam(value = "medicalRecordId", required = false) Long medicalRecordId,
            @RequestParam("testName") String testName,
            @RequestParam(value = "resultSummary", required = false) String resultSummary,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        LaboratoryReport report = laboratoryReportService.uploadReport(
                patientId, doctorId, medicalRecordId, testName, resultSummary, file, userDetails.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.success("Laboratory report uploaded successfully", report));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR', 'ROLE_PATIENT')")
    public ResponseEntity<ApiResponse<List<LaboratoryReport>>> getPatientReports(
            @PathVariable Long patientId,
            @AuthenticationPrincipal UserDetails userDetails) {
        enforcePatientPrivacy(patientId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(laboratoryReportService.getReportsForPatient(patientId)));
    }

    @GetMapping("/my-reports")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ResponseEntity<ApiResponse<List<LaboratoryReport>>> getMyReports(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        Patient patient = patientRepository.findByUser(user)
                .orElseThrow(() -> new UnauthorizedException("Patient profile missing"));

        return ResponseEntity.ok(ApiResponse.success(laboratoryReportService.getReportsForPatient(patient.getPatientId())));
    }

    @GetMapping("/download/{reportId}")
    public ResponseEntity<Resource> downloadReportFile(
            @PathVariable Long reportId,
            @AuthenticationPrincipal UserDetails userDetails) {
        LaboratoryReport report = laboratoryReportService.getReportById(reportId);
        enforcePatientPrivacy(report.getPatient().getPatientId(), userDetails);

        Resource fileResource = laboratoryReportService.loadFileAsResource(report.getFilePath());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(report.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + report.getFileName() + "\"")
                .body(fileResource);
    }

    private void enforcePatientPrivacy(Long requestedPatientId, UserDetails userDetails) {
        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (currentUser.getRole() == Role.ROLE_PATIENT) {
            Patient currentPatient = patientRepository.findByUser(currentUser)
                    .orElseThrow(() -> new UnauthorizedException("Patient record not found"));
            if (!currentPatient.getPatientId().equals(requestedPatientId)) {
                throw new UnauthorizedException("Access Denied: You cannot view documents belonging to another patient.");
            }
        }
    }
}
