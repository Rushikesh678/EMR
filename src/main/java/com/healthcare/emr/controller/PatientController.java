package com.healthcare.emr.controller;

import com.healthcare.emr.dto.ApiResponse;
import com.healthcare.emr.dto.AuthResponse;
import com.healthcare.emr.dto.PatientDto;
import com.healthcare.emr.dto.RegisterRequest;
import com.healthcare.emr.entity.Patient;
import com.healthcare.emr.entity.User;
import com.healthcare.emr.exception.UnauthorizedException;
import com.healthcare.emr.repository.PatientRepository;
import com.healthcare.emr.repository.UserRepository;
import com.healthcare.emr.service.AuthService;
import com.healthcare.emr.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;

    public PatientController(PatientService patientService,
                             AuthService authService,
                             UserRepository userRepository,
                             PatientRepository patientRepository) {
        this.patientService = patientService;
        this.authService = authService;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR', 'ROLE_RECEPTIONIST')")
    public ResponseEntity<ApiResponse<List<PatientDto>>> getAllOrSearchPatients(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(ApiResponse.success(patientService.searchPatients(query)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ResponseEntity<ApiResponse<PatientDto>> getCurrentPatientProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(ApiResponse.success(patientService.getPatientByUserId(user.getUserId())));
    }

    @GetMapping("/{patientId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR', 'ROLE_RECEPTIONIST', 'ROLE_PATIENT')")
    public ResponseEntity<ApiResponse<PatientDto>> getPatientById(@PathVariable Long patientId,
                                                                  @AuthenticationPrincipal UserDetails userDetails) {
        enforcePatientAccess(patientId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(patientService.getPatientById(patientId)));
    }

    @PutMapping("/{patientId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_RECEPTIONIST', 'ROLE_PATIENT')")
    public ResponseEntity<ApiResponse<PatientDto>> updatePatient(@PathVariable Long patientId,
                                                                 @RequestBody PatientDto dto,
                                                                 @AuthenticationPrincipal UserDetails userDetails) {
        enforcePatientAccess(patientId, userDetails);
        PatientDto updated = patientService.updatePatient(patientId, dto, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Patient updated successfully", updated));
    }

    @PostMapping("/register-by-staff")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_RECEPTIONIST')")
    public ResponseEntity<ApiResponse<PatientDto>> registerPatientByStaff(@Valid @RequestBody RegisterRequest request) {
        AuthResponse res = authService.registerPatient(request);
        PatientDto patient = patientService.getPatientById(res.getRoleEntityId());
        return ResponseEntity.ok(ApiResponse.success("Patient registered successfully by staff", patient));
    }

    private void enforcePatientAccess(Long requestedPatientId, UserDetails userDetails) {
        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (currentUser.getRole() == com.healthcare.emr.entity.Role.ROLE_PATIENT) {
            Patient currentPatient = patientRepository.findByUser(currentUser)
                    .orElseThrow(() -> new UnauthorizedException("Patient record not found"));
            if (!currentPatient.getPatientId().equals(requestedPatientId)) {
                throw new UnauthorizedException("Access Denied: You cannot view or modify another patient's data.");
            }
        }
    }
}
