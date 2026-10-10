package com.healthcare.emr.controller;

import com.healthcare.emr.dto.ApiResponse;
import com.healthcare.emr.dto.MedicalRecordDto;
import com.healthcare.emr.entity.*;
import com.healthcare.emr.exception.UnauthorizedException;
import com.healthcare.emr.repository.*;
import com.healthcare.emr.service.MedicalRecordService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medical-records")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    public MedicalRecordController(MedicalRecordService medicalRecordService,
                                   UserRepository userRepository,
                                   DoctorRepository doctorRepository,
                                   PatientRepository patientRepository) {
        this.medicalRecordService = medicalRecordService;
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public ResponseEntity<ApiResponse<MedicalRecordDto>> createRecord(
            @Valid @RequestBody MedicalRecordDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        Doctor doctor = doctorRepository.findByUser(user)
                .orElseThrow(() -> new UnauthorizedException("Doctor profile missing"));

        MedicalRecordDto created = medicalRecordService.createRecord(dto, doctor.getDoctorId(), user.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Medical record saved successfully", created));
    }

    @PutMapping("/{recordId}")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public ResponseEntity<ApiResponse<MedicalRecordDto>> updateRecord(
            @PathVariable Long recordId,
            @RequestBody MedicalRecordDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        MedicalRecordDto updated = medicalRecordService.updateRecord(recordId, dto, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Medical record updated", updated));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR', 'ROLE_PATIENT')")
    public ResponseEntity<ApiResponse<List<MedicalRecordDto>>> getPatientRecords(
            @PathVariable Long patientId,
            @AuthenticationPrincipal UserDetails userDetails) {
        enforcePatientPrivacy(patientId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(medicalRecordService.getRecordsForPatient(patientId)));
    }

    @GetMapping("/my-records")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ResponseEntity<ApiResponse<List<MedicalRecordDto>>> getMyRecords(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        Patient patient = patientRepository.findByUser(user)
                .orElseThrow(() -> new UnauthorizedException("Patient record missing"));

        return ResponseEntity.ok(ApiResponse.success(medicalRecordService.getRecordsForPatient(patient.getPatientId())));
    }

    @GetMapping("/{recordId}")
    public ResponseEntity<ApiResponse<MedicalRecordDto>> getRecordById(@PathVariable Long recordId) {
        return ResponseEntity.ok(ApiResponse.success(medicalRecordService.getRecordById(recordId)));
    }

    private void enforcePatientPrivacy(Long requestedPatientId, UserDetails userDetails) {
        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (currentUser.getRole() == Role.ROLE_PATIENT) {
            Patient currentPatient = patientRepository.findByUser(currentUser)
                    .orElseThrow(() -> new UnauthorizedException("Patient record not found"));
            if (!currentPatient.getPatientId().equals(requestedPatientId)) {
                throw new UnauthorizedException("Access Denied: You cannot view clinical records belonging to another patient.");
            }
        }
    }
}
