package com.healthcare.emr;

import com.healthcare.emr.entity.Doctor;
import com.healthcare.emr.entity.LaboratoryReport;
import com.healthcare.emr.entity.Patient;
import com.healthcare.emr.exception.BadRequestException;
import com.healthcare.emr.exception.ResourceNotFoundException;
import com.healthcare.emr.repository.DoctorRepository;
import com.healthcare.emr.repository.LaboratoryReportRepository;
import com.healthcare.emr.repository.PatientRepository;
import com.healthcare.emr.service.LaboratoryReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class LaboratoryReportServiceTest {

    @Autowired
    private LaboratoryReportService laboratoryReportService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private LaboratoryReportRepository laboratoryReportRepository;

    @Test
    @DisplayName("Should upload valid lab report file and retrieve report")
    void testUploadAndRetrieveReport() throws IOException {
        Patient patient = patientRepository.findAll().get(0);
        Doctor doctor = doctorRepository.findAll().get(0);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-blood-work.pdf",
                "application/pdf",
                "%PDF-1.4 Mock Lab Report Content".getBytes()
        );

        LaboratoryReport report = laboratoryReportService.uploadReport(
                patient.getPatientId(),
                doctor.getDoctorId(),
                null,
                "Complete Blood Count (CBC)",
                "Hemoglobin normal at 14.5 g/dL. Platelets 250k.",
                file,
                "lab@healthcare.local"
        );

        assertNotNull(report);
        assertNotNull(report.getReportId());
        assertEquals("Complete Blood Count (CBC)", report.getTestName());

        // Get by ID
        LaboratoryReport fetched = laboratoryReportService.getReportById(report.getReportId());
        assertEquals(report.getReportId(), fetched.getReportId());

        // Get Patient reports
        List<LaboratoryReport> patientReports = laboratoryReportService.getReportsForPatient(patient.getPatientId());
        assertFalse(patientReports.isEmpty());

        // Get Doctor reports
        List<LaboratoryReport> doctorReports = laboratoryReportService.getReportsForDoctor(doctor.getDoctorId());
        assertFalse(doctorReports.isEmpty());

        // Load Resource file
        Resource fileResource = laboratoryReportService.loadFileAsResource(report.getFilePath());
        assertNotNull(fileResource);
        assertTrue(fileResource.exists());
    }

    @Test
    @DisplayName("Should reject empty file or invalid file extension")
    void testUploadValidationFailures() {
        Patient patient = patientRepository.findAll().get(0);

        // Empty file
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
        assertThrows(BadRequestException.class, () -> {
            laboratoryReportService.uploadReport(patient.getPatientId(), null, null, "Test", "Summary", emptyFile, "lab@local");
        });

        // Invalid extension (.exe)
        MockMultipartFile badExtFile = new MockMultipartFile("file", "malicious.exe", "application/octet-stream", "dummy data".getBytes());
        assertThrows(BadRequestException.class, () -> {
            laboratoryReportService.uploadReport(patient.getPatientId(), null, null, "Test", "Summary", badExtFile, "lab@local");
        });

        // Non-existent report
        assertThrows(ResourceNotFoundException.class, () -> laboratoryReportService.getReportById(999999L));
    }
}
