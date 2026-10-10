package com.healthcare.emr.service;

import com.healthcare.emr.dto.PrescriptionDto;
import com.healthcare.emr.entity.*;
import com.healthcare.emr.exception.ResourceNotFoundException;
import com.healthcare.emr.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                               PatientRepository patientRepository,
                               DoctorRepository doctorRepository,
                               MedicalRecordRepository medicalRecordRepository,
                               NotificationService notificationService,
                               AuditLogService auditLogService) {
        this.prescriptionRepository = prescriptionRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public PrescriptionDto createPrescription(PrescriptionDto dto, Long doctorId, String doctorEmail) {
        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + dto.getPatientId()));

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));

        MedicalRecord record = null;
        if (dto.getMedicalRecordId() != null) {
            record = medicalRecordRepository.findById(dto.getMedicalRecordId()).orElse(null);
        }

        Prescription rx = new Prescription();
        rx.setPatient(patient);
        rx.setDoctor(doctor);
        rx.setMedicalRecord(record);
        rx.setMedication(dto.getMedication());
        rx.setDosage(dto.getDosage());
        rx.setDuration(dto.getDuration());
        rx.setInstructions(dto.getInstructions());
        rx.setPrescribedDate(dto.getPrescribedDate() != null ? dto.getPrescribedDate() : LocalDate.now());

        Prescription saved = prescriptionRepository.save(rx);

        notificationService.sendNotification(
                patient.getUser(),
                "New Prescription Issued",
                "Dr. " + doctor.getUser().getName() + " prescribed: " + rx.getMedication() + " (" + rx.getDosage() + ").",
                "PRESCRIPTION"
        );

        auditLogService.log(doctorEmail, "ROLE_DOCTOR", "CREATE_PRESCRIPTION", "Prescription", saved.getPrescriptionId(), null,
                "Prescribed " + rx.getMedication() + " to patient " + patient.getUser().getName());

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionDto> getPrescriptionsForPatient(Long patientId) {
        return prescriptionRepository.findByPatient_PatientIdOrderByPrescribedDateDesc(patientId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PrescriptionDto> getPrescriptionsByDoctor(Long doctorId) {
        return prescriptionRepository.findByDoctor_DoctorIdOrderByPrescribedDateDesc(doctorId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PrescriptionDto getPrescriptionById(Long id) {
        Prescription rx = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + id));
        return mapToDto(rx);
    }

    public PrescriptionDto mapToDto(Prescription rx) {
        PrescriptionDto dto = new PrescriptionDto();
        dto.setPrescriptionId(rx.getPrescriptionId());
        dto.setPatientId(rx.getPatient().getPatientId());
        dto.setPatientName(rx.getPatient().getUser().getName());
        dto.setDoctorId(rx.getDoctor().getDoctorId());
        dto.setDoctorName(rx.getDoctor().getUser().getName());
        dto.setDoctorSpecialization(rx.getDoctor().getSpecialization());
        dto.setMedicalRecordId(rx.getMedicalRecord() != null ? rx.getMedicalRecord().getRecordId() : null);
        dto.setMedication(rx.getMedication());
        dto.setDosage(rx.getDosage());
        dto.setDuration(rx.getDuration());
        dto.setInstructions(rx.getInstructions());
        dto.setPrescribedDate(rx.getPrescribedDate());
        dto.setCreatedAt(rx.getCreatedAt());
        return dto;
    }
}
