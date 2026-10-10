package com.healthcare.emr.controller;

import com.healthcare.emr.dto.ApiResponse;
import com.healthcare.emr.dto.PrescriptionDto;
import com.healthcare.emr.entity.*;
import com.healthcare.emr.exception.UnauthorizedException;
import com.healthcare.emr.repository.*;
import com.healthcare.emr.service.PrescriptionService;
import com.healthcare.emr.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final ReportService reportService;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    public PrescriptionController(PrescriptionService prescriptionService,
                                  ReportService reportService,
                                  UserRepository userRepository,
                                  DoctorRepository doctorRepository,
                                  PatientRepository patientRepository) {
        this.prescriptionService = prescriptionService;
        this.reportService = reportService;
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public ResponseEntity<ApiResponse<PrescriptionDto>> createPrescription(
            @Valid @RequestBody PrescriptionDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        Doctor doctor = doctorRepository.findByUser(user)
                .orElseThrow(() -> new UnauthorizedException("Doctor profile missing"));

        PrescriptionDto created = prescriptionService.createPrescription(dto, doctor.getDoctorId(), user.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Prescription issued successfully", created));
    }

    @GetMapping("/my-prescriptions")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ResponseEntity<ApiResponse<List<PrescriptionDto>>> getMyPrescriptions(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        Patient patient = patientRepository.findByUser(user)
                .orElseThrow(() -> new UnauthorizedException("Patient profile missing"));

        return ResponseEntity.ok(ApiResponse.success(prescriptionService.getPrescriptionsForPatient(patient.getPatientId())));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR', 'ROLE_PATIENT')")
    public ResponseEntity<ApiResponse<List<PrescriptionDto>>> getPatientPrescriptions(
            @PathVariable Long patientId,
            @AuthenticationPrincipal UserDetails userDetails) {
        enforcePatientPrivacy(patientId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(prescriptionService.getPrescriptionsForPatient(patientId)));
    }

    @GetMapping("/{prescriptionId}/pdf")
    public ResponseEntity<byte[]> downloadPrescriptionPdf(@PathVariable Long prescriptionId) {
        byte[] pdfBytes = reportService.generatePrescriptionPdf(prescriptionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Prescription-RX-" + prescriptionId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    private void enforcePatientPrivacy(Long requestedPatientId, UserDetails userDetails) {
        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (currentUser.getRole() == Role.ROLE_PATIENT) {
            Patient currentPatient = patientRepository.findByUser(currentUser)
                    .orElseThrow(() -> new UnauthorizedException("Patient record not found"));
            if (!currentPatient.getPatientId().equals(requestedPatientId)) {
                throw new UnauthorizedException("Access Denied: You cannot view prescriptions belonging to another patient.");
            }
        }
    }
}
