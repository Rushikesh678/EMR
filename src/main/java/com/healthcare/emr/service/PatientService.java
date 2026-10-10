package com.healthcare.emr.service;

import com.healthcare.emr.dto.PatientDto;
import com.healthcare.emr.entity.Patient;
import com.healthcare.emr.entity.User;
import com.healthcare.emr.exception.ResourceNotFoundException;
import com.healthcare.emr.repository.PatientRepository;
import com.healthcare.emr.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public PatientService(PatientRepository patientRepository, UserRepository userRepository, AuditLogService auditLogService) {
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<PatientDto> getAllPatients() {
        return patientRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PatientDto> searchPatients(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllPatients();
        }
        return patientRepository.searchPatients(query.trim()).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PatientDto getPatientById(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));
        return mapToDto(patient);
    }

    @Transactional(readOnly = true)
    public PatientDto getPatientByUserId(Long userId) {
        Patient patient = patientRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient record not found for user: " + userId));
        return mapToDto(patient);
    }

    @Transactional
    public PatientDto updatePatient(Long patientId, PatientDto dto, String updatedByEmail) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));

        if (dto.getName() != null) {
            patient.getUser().setName(dto.getName());
        }
        if (dto.getPhone() != null) {
            patient.getUser().setPhone(dto.getPhone());
        }
        if (dto.getDateOfBirth() != null) {
            patient.setDateOfBirth(dto.getDateOfBirth());
        }
        if (dto.getGender() != null) {
            patient.setGender(dto.getGender());
        }
        if (dto.getBloodGroup() != null) {
            patient.setBloodGroup(dto.getBloodGroup());
        }
        if (dto.getAddress() != null) {
            patient.setAddress(dto.getAddress());
        }
        if (dto.getMedicalHistory() != null) {
            patient.setMedicalHistory(dto.getMedicalHistory());
        }
        if (dto.getEmergencyContactName() != null) {
            patient.setEmergencyContactName(dto.getEmergencyContactName());
        }
        if (dto.getEmergencyContactPhone() != null) {
            patient.setEmergencyContactPhone(dto.getEmergencyContactPhone());
        }

        Patient updated = patientRepository.save(patient);
        auditLogService.log(updatedByEmail, "PATIENT_UPDATE", "UPDATE", "Patient", patientId, null, "Updated patient demographics");

        return mapToDto(updated);
    }

    public PatientDto mapToDto(Patient patient) {
        PatientDto dto = new PatientDto();
        dto.setPatientId(patient.getPatientId());
        dto.setUserId(patient.getUser().getUserId());
        dto.setName(patient.getUser().getName());
        dto.setEmail(patient.getUser().getEmail());
        dto.setPhone(patient.getUser().getPhone());
        dto.setActive(patient.getUser().isActive());
        dto.setDateOfBirth(patient.getDateOfBirth());
        dto.setGender(patient.getGender());
        dto.setBloodGroup(patient.getBloodGroup());
        dto.setAddress(patient.getAddress());
        dto.setMedicalHistory(patient.getMedicalHistory());
        dto.setEmergencyContactName(patient.getEmergencyContactName());
        dto.setEmergencyContactPhone(patient.getEmergencyContactPhone());
        dto.setCreatedAt(patient.getCreatedAt());
        return dto;
    }
}
