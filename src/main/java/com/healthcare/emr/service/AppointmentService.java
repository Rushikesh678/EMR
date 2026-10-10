package com.healthcare.emr.service;

import com.healthcare.emr.dto.AppointmentBookingRequest;
import com.healthcare.emr.dto.AppointmentDto;
import com.healthcare.emr.entity.*;
import com.healthcare.emr.exception.AppointmentConflictException;
import com.healthcare.emr.exception.BadRequestException;
import com.healthcare.emr.exception.ResourceNotFoundException;
import com.healthcare.emr.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final BillingRepository billingRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository,
                              BillingRepository billingRepository,
                              NotificationService notificationService,
                              AuditLogService auditLogService) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.billingRepository = billingRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AppointmentDto bookAppointment(AppointmentBookingRequest request, Long patientIdOverride, String bookedByEmail) {
        Long targetPatientId = patientIdOverride != null ? patientIdOverride : request.getPatientId();
        if (targetPatientId == null) {
            throw new BadRequestException("Patient ID is required for booking");
        }

        Patient patient = patientRepository.findById(targetPatientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + targetPatientId));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + request.getDoctorId()));

        // Conflict check
        if (appointmentRepository.existsConflictingAppointment(doctor.getDoctorId(), request.getAppointmentDate(), request.getAppointmentTime())) {
            throw new AppointmentConflictException("Doctor " + doctor.getUser().getName() + " is already booked for "
                    + request.getAppointmentDate() + " at " + request.getAppointmentTime() + ". Please select another slot.");
        }

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setAppointmentTime(request.getAppointmentTime());
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setReason(request.getReason());
        appointment.setNotes(request.getNotes());

        Appointment saved = appointmentRepository.save(appointment);

        // Automatically generate linked pending billing record
        String invoiceNum = "INV-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Billing bill = new Billing(patient, saved, doctor.getConsultationFee(), BillingStatus.PENDING, invoiceNum);
        billingRepository.save(bill);

        // Notifications
        notificationService.sendNotification(
                patient.getUser(),
                "Appointment Confirmed",
                "Your appointment with " + doctor.getUser().getName() + " on " + saved.getAppointmentDate() + " at " + saved.getAppointmentTime() + " is confirmed.",
                "APPOINTMENT"
        );
        notificationService.sendNotification(
                doctor.getUser(),
                "New Appointment Scheduled",
                "Patient " + patient.getUser().getName() + " booked an appointment for " + saved.getAppointmentDate() + " at " + saved.getAppointmentTime() + ".",
                "APPOINTMENT"
        );

        auditLogService.log(bookedByEmail, "APPOINTMENT", "BOOK", "Appointment", saved.getAppointmentId(), null,
                "Booked appointment for Patient ID " + patient.getPatientId() + " with Doctor ID " + doctor.getDoctorId());

        return mapToDto(saved);
    }

    @Transactional
    public AppointmentDto rescheduleAppointment(Long appointmentId, LocalDate newDate, String newTime, String userEmail) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED || appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Cannot reschedule appointment with status: " + appointment.getStatus());
        }

        if (appointmentRepository.existsConflictingAppointment(appointment.getDoctor().getDoctorId(), newDate, newTime)) {
            throw new AppointmentConflictException("The selected time slot " + newTime + " on " + newDate + " is already occupied.");
        }

        appointment.setAppointmentDate(newDate);
        appointment.setAppointmentTime(newTime);
        appointment.setStatus(AppointmentStatus.RESCHEDULED);
        Appointment saved = appointmentRepository.save(appointment);

        notificationService.sendNotification(
                appointment.getPatient().getUser(),
                "Appointment Rescheduled",
                "Your appointment with " + appointment.getDoctor().getUser().getName() + " has been rescheduled to " + newDate + " at " + newTime + ".",
                "APPOINTMENT"
        );

        auditLogService.log(userEmail, "APPOINTMENT", "RESCHEDULE", "Appointment", appointmentId, null,
                "Rescheduled to " + newDate + " at " + newTime);

        return mapToDto(saved);
    }

    @Transactional
    public AppointmentDto updateStatus(Long appointmentId, AppointmentStatus newStatus, String userEmail) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));

        appointment.setStatus(newStatus);
        Appointment saved = appointmentRepository.save(appointment);

        notificationService.sendNotification(
                appointment.getPatient().getUser(),
                "Appointment Status Update",
                "Your appointment status is now: " + newStatus.name(),
                "APPOINTMENT"
        );

        auditLogService.log(userEmail, "APPOINTMENT", "STATUS_CHANGE", "Appointment", appointmentId, null,
                "Updated status to " + newStatus.name());

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<AppointmentDto> getAppointmentsForPatient(Long patientId) {
        return appointmentRepository.findByPatient_PatientIdOrderByAppointmentDateDescAppointmentTimeDesc(patientId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentDto> getAppointmentsForDoctor(Long doctorId) {
        return appointmentRepository.findByDoctor_DoctorIdOrderByAppointmentDateDescAppointmentTimeDesc(doctorId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentDto> getAllAppointments() {
        return appointmentRepository.findAll().stream()
                .map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentDto> getTodayAppointments() {
        return appointmentRepository.findByAppointmentDateOrderByAppointmentTimeAsc(LocalDate.now())
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AppointmentDto getAppointmentById(Long appointmentId) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));
        return mapToDto(appt);
    }

    public AppointmentDto mapToDto(Appointment a) {
        AppointmentDto dto = new AppointmentDto();
        dto.setAppointmentId(a.getAppointmentId());
        dto.setPatientId(a.getPatient().getPatientId());
        dto.setPatientName(a.getPatient().getUser().getName());
        dto.setPatientEmail(a.getPatient().getUser().getEmail());
        dto.setPatientPhone(a.getPatient().getUser().getPhone());
        dto.setDoctorId(a.getDoctor().getDoctorId());
        dto.setDoctorName(a.getDoctor().getUser().getName());
        dto.setDoctorSpecialization(a.getDoctor().getSpecialization());
        dto.setDoctorConsultationFee(a.getDoctor().getConsultationFee());
        dto.setAppointmentDate(a.getAppointmentDate());
        dto.setAppointmentTime(a.getAppointmentTime());
        dto.setStatus(a.getStatus());
        dto.setReason(a.getReason());
        dto.setNotes(a.getNotes());
        dto.setCreatedAt(a.getCreatedAt());
        return dto;
    }
}
