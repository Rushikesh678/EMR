package com.healthcare.emr.service;

import com.healthcare.emr.dto.DoctorDto;
import com.healthcare.emr.entity.Appointment;
import com.healthcare.emr.entity.Doctor;
import com.healthcare.emr.exception.ResourceNotFoundException;
import com.healthcare.emr.repository.AppointmentRepository;
import com.healthcare.emr.repository.DoctorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    public DoctorService(DoctorRepository doctorRepository, AppointmentRepository appointmentRepository) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public List<DoctorDto> getAllDoctors() {
        return doctorRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DoctorDto> searchDoctors(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllDoctors();
        }
        return doctorRepository.searchDoctors(query.trim()).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DoctorDto getDoctorById(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));
        return mapToDto(doctor);
    }

    @Transactional(readOnly = true)
    public DoctorDto getDoctorByUserId(Long userId) {
        Doctor doctor = doctorRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor record not found for user: " + userId));
        return mapToDto(doctor);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAvailableSlots(Long doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));

        Map<String, Object> result = new HashMap<>();
        result.put("doctorId", doctorId);
        result.put("doctorName", doctor.getUser().getName());
        result.put("date", date.toString());

        DayOfWeek dow = date.getDayOfWeek();
        String dayCode = dow.name().substring(0, 3); // MON, TUE, WED, etc.
        String availabilityDays = doctor.getAvailabilityDays() != null ? doctor.getAvailabilityDays().toUpperCase() : "MON,TUE,WED,THU,FRI";

        if (!availabilityDays.contains(dayCode)) {
            result.put("isWorkingDay", false);
            result.put("message", "Doctor is not scheduled to work on " + dow.name());
            result.put("slots", Collections.emptyList());
            return result;
        }

        result.put("isWorkingDay", true);

        // Fetch existing active appointments for doctor on this date
        List<Appointment> bookedAppts = appointmentRepository.findByDoctor_DoctorIdAndAppointmentDateOrderByAppointmentTimeAsc(doctorId, date);
        Set<String> bookedTimes = bookedAppts.stream()
                .filter(a -> a.getStatus() != com.healthcare.emr.entity.AppointmentStatus.CANCELLED)
                .map(Appointment::getAppointmentTime)
                .collect(Collectors.toSet());

        // Parse shift hours
        LocalTime start = LocalTime.parse(doctor.getShiftStartTime() != null ? doctor.getShiftStartTime() : "09:00");
        LocalTime end = LocalTime.parse(doctor.getShiftEndTime() != null ? doctor.getShiftEndTime() : "17:00");

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        List<Map<String, Object>> slots = new ArrayList<>();

        LocalTime current = start;
        while (current.isBefore(end)) {
            String slotTime = current.format(timeFmt);
            boolean available = !bookedTimes.contains(slotTime);

            Map<String, Object> slotObj = new HashMap<>();
            slotObj.put("time", slotTime);
            slotObj.put("available", available);
            slots.add(slotObj);

            current = current.plusMinutes(30);
        }

        result.put("slots", slots);
        return result;
    }

    @Transactional
    public DoctorDto updateSchedule(Long doctorId, String days, String start, String end) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));

        if (days != null) doctor.setAvailabilityDays(days);
        if (start != null) doctor.setShiftStartTime(start);
        if (end != null) doctor.setShiftEndTime(end);

        Doctor saved = doctorRepository.save(doctor);
        return mapToDto(saved);
    }

    public DoctorDto mapToDto(Doctor doctor) {
        DoctorDto dto = new DoctorDto();
        dto.setDoctorId(doctor.getDoctorId());
        dto.setUserId(doctor.getUser().getUserId());
        dto.setName(doctor.getUser().getName());
        dto.setEmail(doctor.getUser().getEmail());
        dto.setPhone(doctor.getUser().getPhone());
        dto.setSpecialization(doctor.getSpecialization());
        dto.setQualification(doctor.getQualification());
        dto.setExperienceYears(doctor.getExperienceYears());
        dto.setDepartment(doctor.getDepartment());
        dto.setConsultationFee(doctor.getConsultationFee());
        dto.setAvailabilityDays(doctor.getAvailabilityDays());
        dto.setShiftStartTime(doctor.getShiftStartTime());
        dto.setShiftEndTime(doctor.getShiftEndTime());
        dto.setActive(doctor.getUser().isActive());
        return dto;
    }
}
