package com.healthcare.emr;

import com.healthcare.emr.dto.PrescriptionDto;
import com.healthcare.emr.entity.Doctor;
import com.healthcare.emr.entity.Patient;
import com.healthcare.emr.exception.ResourceNotFoundException;
import com.healthcare.emr.repository.DoctorRepository;
import com.healthcare.emr.repository.PatientRepository;
import com.healthcare.emr.service.PrescriptionService;
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
class PrescriptionServiceTest {

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Test
    @DisplayName("Should issue, retrieve, and filter prescriptions")
    void testPrescriptionLifecycle() {
        Patient patient = patientRepository.findAll().get(0);
        Doctor doctor = doctorRepository.findAll().get(0);

        PrescriptionDto dto = new PrescriptionDto();
        dto.setPatientId(patient.getPatientId());
        dto.setMedication("Amoxicillin");
        dto.setDosage("500mg");
        dto.setDuration("7 days");
        dto.setInstructions("Take one capsule three times daily after meals");
        dto.setPrescribedDate(LocalDate.now());

        PrescriptionDto created = prescriptionService.createPrescription(dto, doctor.getDoctorId(), doctor.getUser().getEmail());
        assertNotNull(created);
        assertNotNull(created.getPrescriptionId());
        assertEquals("Amoxicillin", created.getMedication());

        // Get by ID
        PrescriptionDto fetched = prescriptionService.getPrescriptionById(created.getPrescriptionId());
        assertEquals(created.getPrescriptionId(), fetched.getPrescriptionId());
        assertEquals("500mg", fetched.getDosage());

        // Get for Patient
        List<PrescriptionDto> patientRxs = prescriptionService.getPrescriptionsForPatient(patient.getPatientId());
        assertFalse(patientRxs.isEmpty());

        // Get by Doctor
        List<PrescriptionDto> docRxs = prescriptionService.getPrescriptionsByDoctor(doctor.getDoctorId());
        assertFalse(docRxs.isEmpty());

        assertThrows(ResourceNotFoundException.class, () -> prescriptionService.getPrescriptionById(999999L));
    }
}
