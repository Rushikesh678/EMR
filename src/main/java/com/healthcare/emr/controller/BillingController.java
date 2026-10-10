package com.healthcare.emr.controller;

import com.healthcare.emr.dto.ApiResponse;
import com.healthcare.emr.dto.BillingDto;
import com.healthcare.emr.dto.PaymentRequest;
import com.healthcare.emr.entity.Patient;
import com.healthcare.emr.entity.User;
import com.healthcare.emr.exception.UnauthorizedException;
import com.healthcare.emr.repository.PatientRepository;
import com.healthcare.emr.repository.UserRepository;
import com.healthcare.emr.service.BillingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;

    public BillingController(BillingService billingService,
                             UserRepository userRepository,
                             PatientRepository patientRepository) {
        this.billingService = billingService;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
    }

    @GetMapping("/my-bills")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ResponseEntity<ApiResponse<List<BillingDto>>> getMyBills(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        Patient patient = patientRepository.findByUser(user)
                .orElseThrow(() -> new UnauthorizedException("Patient profile missing"));

        return ResponseEntity.ok(ApiResponse.success(billingService.getBillsForPatient(patient.getPatientId())));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_RECEPTIONIST', 'ROLE_PATIENT')")
    public ResponseEntity<ApiResponse<List<BillingDto>>> getBillsByPatientId(@PathVariable Long patientId) {
        return ResponseEntity.ok(ApiResponse.success(billingService.getBillsForPatient(patientId)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<BillingDto>>> getAllBills() {
        return ResponseEntity.ok(ApiResponse.success(billingService.getAllBills()));
    }

    @PostMapping("/simulate-payment")
    public ResponseEntity<ApiResponse<BillingDto>> simulatePayment(
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        BillingDto paid = billingService.processSimulatedPayment(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Payment simulated & confirmed successfully", paid));
    }
}
