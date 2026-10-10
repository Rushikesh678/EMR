package com.healthcare.emr;

import com.healthcare.emr.dto.PatientDto;
import com.healthcare.emr.entity.Patient;
import com.healthcare.emr.exception.ResourceNotFoundException;
import com.healthcare.emr.repository.PatientRepository;
import com.healthcare.emr.service.PatientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PatientServiceTest {

    @Autowired
    private PatientService patientService;

    @Autowired
    private PatientRepository patientRepository;

    @Test
    @DisplayName("Should fetch all patients and search patients by name")
    void testGetAllAndSearchPatients() {
        List<PatientDto> patients = patientService.getAllPatients();
        assertFalse(patients.isEmpty(), "Patients should be seeded");

        Patient first = patientRepository.findAll().get(0);
        List<PatientDto> searchResult = patientService.searchPatients(first.getUser().getName().substring(0, 3));
        assertFalse(searchResult.isEmpty());

        List<PatientDto> emptySearch = patientService.searchPatients("");
        assertEquals(patients.size(), emptySearch.size());
    }

    @Test
    @DisplayName("Should fetch patient by ID and user ID")
    void testGetPatientByIdAndUserId() {
        Patient first = patientRepository.findAll().get(0);

        PatientDto byId = patientService.getPatientById(first.getPatientId());
        assertNotNull(byId);
        assertEquals(first.getPatientId(), byId.getPatientId());
        assertEquals(first.getUser().getEmail(), byId.getEmail());

        PatientDto byUserId = patientService.getPatientByUserId(first.getUser().getUserId());
        assertNotNull(byUserId);
        assertEquals(first.getPatientId(), byUserId.getPatientId());

        assertThrows(ResourceNotFoundException.class, () -> patientService.getPatientById(999999L));
        assertThrows(ResourceNotFoundException.class, () -> patientService.getPatientByUserId(999999L));
    }

    @Test
    @DisplayName("Should update patient profile demographics and medical history")
    void testUpdatePatient() {
        Patient first = patientRepository.findAll().get(0);

        PatientDto updateDto = new PatientDto();
        updateDto.setName("Updated " + first.getUser().getName());
        updateDto.setPhone("+1-555-7788");
        updateDto.setBloodGroup("AB+");
        updateDto.setMedicalHistory("Updated asthma history notes");
        updateDto.setEmergencyContactName("Mary Contact");
        updateDto.setEmergencyContactPhone("+1-555-0011");

        PatientDto updated = patientService.updatePatient(first.getPatientId(), updateDto, "admin@healthcare.local");

        assertEquals(updateDto.getName(), updated.getName());
        assertEquals(updateDto.getPhone(), updated.getPhone());
        assertEquals(updateDto.getBloodGroup(), updated.getBloodGroup());
        assertEquals(updateDto.getMedicalHistory(), updated.getMedicalHistory());
        assertEquals(updateDto.getEmergencyContactName(), updated.getEmergencyContactName());
    }
}
