package com.healthcare.emr;

import com.healthcare.emr.dto.MedicalRecordDto;
import com.healthcare.emr.entity.Doctor;
import com.healthcare.emr.entity.Patient;
import com.healthcare.emr.exception.ResourceNotFoundException;
import com.healthcare.emr.repository.DoctorRepository;
import com.healthcare.emr.repository.PatientRepository;
import com.healthcare.emr.service.MedicalRecordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MedicalRecordServiceTest {

    @Autowired
    private MedicalRecordService medicalRecordService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Test
    @DisplayName("Should create, update, and retrieve medical records")
    void testMedicalRecordLifecycle() {
        Patient patient = patientRepository.findAll().get(0);
        Doctor doctor = doctorRepository.findAll().get(0);

        MedicalRecordDto newRecord = new MedicalRecordDto();
        newRecord.setPatientId(patient.getPatientId());
        newRecord.setRecordDate(LocalDate.now());
        newRecord.setDiagnosis("Hypertension Stage 1");
        newRecord.setTreatment("Lifestyle modifications and dietary adjustments");
        newRecord.setClinicalNotes("BP measured 142/90 mmHg. Follow-up in 4 weeks.");
        newRecord.setMedicalHistoryNotes("No previous cardiac incidents.");

        // Create
        MedicalRecordDto created = medicalRecordService.createRecord(newRecord, doctor.getDoctorId(), doctor.getUser().getEmail());
        assertNotNull(created);
        assertNotNull(created.getRecordId());
        assertEquals("Hypertension Stage 1", created.getDiagnosis());

        // Update
        MedicalRecordDto updateDto = new MedicalRecordDto();
        updateDto.setDiagnosis("Hypertension Controlled");
        updateDto.setTreatment("Continue dietary modifications");
        MedicalRecordDto updated = medicalRecordService.updateRecord(created.getRecordId(), updateDto, doctor.getUser().getEmail());
        assertEquals("Hypertension Controlled", updated.getDiagnosis());

        // Get by ID
        MedicalRecordDto fetched = medicalRecordService.getRecordById(created.getRecordId());
        assertEquals(created.getRecordId(), fetched.getRecordId());

        // Get patient records
        List<MedicalRecordDto> ptRecords = medicalRecordService.getRecordsForPatient(patient.getPatientId());
        assertFalse(ptRecords.isEmpty());

        // Get doctor records
        List<MedicalRecordDto> docRecords = medicalRecordService.getRecordsByDoctor(doctor.getDoctorId());
        assertFalse(docRecords.isEmpty());

        // Resource not found
        assertThrows(ResourceNotFoundException.class, () -> medicalRecordService.getRecordById(999999L));
    }
}
