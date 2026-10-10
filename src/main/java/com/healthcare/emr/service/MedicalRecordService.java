package com.healthcare.emr.service;

import com.healthcare.emr.dto.MedicalRecordDto;
import com.healthcare.emr.entity.*;
import com.healthcare.emr.exception.ResourceNotFoundException;
import com.healthcare.emr.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final AuditLogService auditLogService;

    public MedicalRecordService(MedicalRecordRepository medicalRecordRepository,
                                PatientRepository patientRepository,
                                DoctorRepository doctorRepository,
                                AppointmentRepository appointmentRepository,
                                AuditLogService auditLogService) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public MedicalRecordDto createRecord(MedicalRecordDto dto, Long doctorId, String doctorEmail) {
        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + dto.getPatientId()));

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));

        Appointment appt = null;
        if (dto.getAppointmentId() != null) {
            appt = appointmentRepository.findById(dto.getAppointmentId()).orElse(null);
        }

        MedicalRecord record = new MedicalRecord();
        record.setPatient(patient);
        record.setDoctor(doctor);
        record.setAppointment(appt);
        record.setRecordDate(dto.getRecordDate() != null ? dto.getRecordDate() : LocalDate.now());
        record.setDiagnosis(dto.getDiagnosis());
        record.setTreatment(dto.getTreatment());
        record.setMedicalHistoryNotes(dto.getMedicalHistoryNotes());
        record.setClinicalNotes(dto.getClinicalNotes());

        MedicalRecord saved = medicalRecordRepository.save(record);

        auditLogService.log(doctorEmail, "ROLE_DOCTOR", "CREATE_EMR", "MedicalRecord", saved.getRecordId(), null,
                "Created medical record for patient: " + patient.getUser().getName());

        return mapToDto(saved);
    }

    @Transactional
    public MedicalRecordDto updateRecord(Long recordId, MedicalRecordDto dto, String doctorEmail) {
        MedicalRecord record = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record not found: " + recordId));

        if (dto.getDiagnosis() != null) record.setDiagnosis(dto.getDiagnosis());
        if (dto.getTreatment() != null) record.setTreatment(dto.getTreatment());
        if (dto.getClinicalNotes() != null) record.setClinicalNotes(dto.getClinicalNotes());
        if (dto.getMedicalHistoryNotes() != null) record.setMedicalHistoryNotes(dto.getMedicalHistoryNotes());

        MedicalRecord updated = medicalRecordRepository.save(record);

        auditLogService.log(doctorEmail, "ROLE_DOCTOR", "UPDATE_EMR", "MedicalRecord", recordId, null,
                "Updated medical record ID: " + recordId);

        return mapToDto(updated);
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordDto> getRecordsForPatient(Long patientId) {
        return medicalRecordRepository.findByPatient_PatientIdOrderByRecordDateDesc(patientId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordDto> getRecordsByDoctor(Long doctorId) {
        return medicalRecordRepository.findByDoctor_DoctorIdOrderByRecordDateDesc(doctorId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MedicalRecordDto getRecordById(Long recordId) {
        MedicalRecord record = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record not found: " + recordId));
        return mapToDto(record);
    }

    public MedicalRecordDto mapToDto(MedicalRecord r) {
        MedicalRecordDto dto = new MedicalRecordDto();
        dto.setRecordId(r.getRecordId());
        dto.setPatientId(r.getPatient().getPatientId());
        dto.setPatientName(r.getPatient().getUser().getName());
        dto.setDoctorId(r.getDoctor().getDoctorId());
        dto.setDoctorName(r.getDoctor().getUser().getName());
        dto.setAppointmentId(r.getAppointment() != null ? r.getAppointment().getAppointmentId() : null);
        dto.setRecordDate(r.getRecordDate());
        dto.setDiagnosis(r.getDiagnosis());
        dto.setTreatment(r.getTreatment());
        dto.setMedicalHistoryNotes(r.getMedicalHistoryNotes());
        dto.setClinicalNotes(r.getClinicalNotes());
        dto.setCreatedAt(r.getCreatedAt());
        return dto;
    }
}
