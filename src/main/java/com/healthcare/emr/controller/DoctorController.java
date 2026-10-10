package com.healthcare.emr.controller;

import com.healthcare.emr.dto.ApiResponse;
import com.healthcare.emr.dto.DoctorDto;
import com.healthcare.emr.entity.Doctor;
import com.healthcare.emr.entity.User;
import com.healthcare.emr.repository.DoctorRepository;
import com.healthcare.emr.repository.UserRepository;
import com.healthcare.emr.service.DoctorService;
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
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;

    public DoctorController(DoctorService doctorService,
                            UserRepository userRepository,
                            DoctorRepository doctorRepository) {
        this.doctorService = doctorService;
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DoctorDto>>> getAllDoctors(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(ApiResponse.success(doctorService.searchDoctors(query)));
    }

    @GetMapping("/{doctorId}")
    public ResponseEntity<ApiResponse<DoctorDto>> getDoctorById(@PathVariable Long doctorId) {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getDoctorById(doctorId)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    public ResponseEntity<ApiResponse<DoctorDto>> getCurrentDoctorProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(ApiResponse.success(doctorService.getDoctorByUserId(user.getUserId())));
    }

    @GetMapping("/{doctorId}/available-slots")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAvailableSlots(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getAvailableSlots(doctorId, date)));
    }

    @PutMapping("/{doctorId}/schedule")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DOCTOR')")
    public ResponseEntity<ApiResponse<DoctorDto>> updateDoctorSchedule(
            @PathVariable Long doctorId,
            @RequestBody Map<String, String> schedule) {
        DoctorDto updated = doctorService.updateSchedule(
                doctorId,
                schedule.get("availabilityDays"),
                schedule.get("shiftStartTime"),
                schedule.get("shiftEndTime")
        );
        return ResponseEntity.ok(ApiResponse.success("Schedule updated successfully", updated));
    }
}
