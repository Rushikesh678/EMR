package com.healthcare.emr.controller;

import com.healthcare.emr.dto.ApiResponse;
import com.healthcare.emr.dto.AppointmentBookingRequest;
import com.healthcare.emr.dto.AppointmentDto;
import com.healthcare.emr.entity.*;
import com.healthcare.emr.exception.UnauthorizedException;
import com.healthcare.emr.repository.DoctorRepository;
import com.healthcare.emr.repository.PatientRepository;
import com.healthcare.emr.repository.UserRepository;
import com.healthcare.emr.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AppointmentController(AppointmentService appointmentService,
                                 UserRepository userRepository,
                                 PatientRepository patientRepository,
                                 DoctorRepository doctorRepository) {
        this.appointmentService = appointmentService;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentDto>> bookAppointment(
            @Valid @RequestBody AppointmentBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        Long patientId = null;
        if (user.getRole() == Role.ROLE_PATIENT) {
            Patient patient = patientRepository.findByUser(user)
                    .orElseThrow(() -> new UnauthorizedException("Patient profile missing"));
            patientId = patient.getPatientId();
        } else {
            patientId = request.getPatientId();
        }

        AppointmentDto booked = appointmentService.bookAppointment(request, patientId, user.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Appointment booked successfully", booked));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> getAppointments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long doctorId) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (user.getRole() == Role.ROLE_PATIENT) {
            Patient patient = patientRepository.findByUser(user)
                    .orElseThrow(() -> new UnauthorizedException("Patient profile missing"));
            return ResponseEntity.ok(ApiResponse.success(appointmentService.getAppointmentsForPatient(patient.getPatientId())));
        } else if (user.getRole() == Role.ROLE_DOCTOR) {
            Doctor doctor = doctorRepository.findByUser(user)
                    .orElseThrow(() -> new UnauthorizedException("Doctor profile missing"));
            return ResponseEntity.ok(ApiResponse.success(appointmentService.getAppointmentsForDoctor(doctor.getDoctorId())));
        } else {
            // Reception / Admin
            if (patientId != null) {
                return ResponseEntity.ok(ApiResponse.success(appointmentService.getAppointmentsForPatient(patientId)));
            }
            if (doctorId != null) {
                return ResponseEntity.ok(ApiResponse.success(appointmentService.getAppointmentsForDoctor(doctorId)));
            }
            return ResponseEntity.ok(ApiResponse.success(appointmentService.getAllAppointments()));
        }
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR', 'ROLE_RECEPTIONIST')")
    public ResponseEntity<ApiResponse<List<AppointmentDto>>> getTodayAppointments() {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getTodayAppointments()));
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<ApiResponse<AppointmentDto>> getAppointmentById(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getAppointmentById(appointmentId)));
    }

    @PutMapping("/{appointmentId}/reschedule")
    public ResponseEntity<ApiResponse<AppointmentDto>> rescheduleAppointment(
            @PathVariable Long appointmentId,
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserDetails userDetails) {
        LocalDate newDate = LocalDate.parse(payload.get("newDate"));
        String newTime = payload.get("newTime");

        AppointmentDto updated = appointmentService.rescheduleAppointment(appointmentId, newDate, newTime, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Appointment rescheduled successfully", updated));
    }

    @PatchMapping("/{appointmentId}/status")
    public ResponseEntity<ApiResponse<AppointmentDto>> updateAppointmentStatus(
            @PathVariable Long appointmentId,
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserDetails userDetails) {
        AppointmentStatus status = AppointmentStatus.valueOf(payload.get("status"));
        AppointmentDto updated = appointmentService.updateStatus(appointmentId, status, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Appointment status changed to " + status.name(), updated));
    }
}
