package com.healthcare.emr.service;

import com.healthcare.emr.entity.*;
import com.healthcare.emr.exception.BadRequestException;
import com.healthcare.emr.exception.ResourceNotFoundException;
import com.healthcare.emr.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

@Service
public class LaboratoryReportService {

    private final LaboratoryReportRepository laboratoryReportRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final Path uploadPath;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg", "txt", "docx");

    public LaboratoryReportService(
            LaboratoryReportRepository laboratoryReportRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository,
            MedicalRecordRepository medicalRecordRepository,
            NotificationService notificationService,
            AuditLogService auditLogService,
            @Value("${app.upload.dir:./uploads/reports/}") String uploadDir) throws IOException {
        this.laboratoryReportRepository = laboratoryReportRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;

        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadPath);
    }

    @Transactional
    public LaboratoryReport uploadReport(Long patientId, Long doctorId, Long medicalRecordId, String testName, String resultSummary, MultipartFile file, String uploaderEmail) throws IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BadRequestException("File size exceeds 10MB limit");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BadRequestException("File must have a valid extension");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("File type not allowed. Supported formats: PDF, PNG, JPG, JPEG, TXT, DOCX");
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));

        Doctor doctor = doctorId != null ? doctorRepository.findById(doctorId).orElse(null) : null;
        MedicalRecord record = medicalRecordId != null ? medicalRecordRepository.findById(medicalRecordId).orElse(null) : null;

        // Securely sanitize stored file name using UUID
        String safeStoredFileName = UUID.randomUUID() + "." + extension;
        Path targetLocation = this.uploadPath.resolve(safeStoredFileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        LaboratoryReport report = new LaboratoryReport();
        report.setPatient(patient);
        report.setDoctor(doctor);
        report.setMedicalRecord(record);
        report.setTestName(testName != null ? testName : "Clinical Diagnostic Report");
        report.setTestDate(LocalDate.now());
        report.setFileName(originalFilename);
        report.setFilePath(safeStoredFileName);
        report.setFileType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        report.setFileSize(file.getSize());
        report.setResultSummary(resultSummary);

        LaboratoryReport saved = laboratoryReportRepository.save(report);

        notificationService.sendNotification(
                patient.getUser(),
                "New Diagnostic Report Available",
                "Laboratory report for '" + report.getTestName() + "' is now available.",
                "LAB_REPORT"
        );

        auditLogService.log(uploaderEmail, "LAB_REPORT", "UPLOAD", "LaboratoryReport", saved.getReportId(), null,
                "Uploaded lab document " + originalFilename + " for patient " + patient.getUser().getName());

        return saved;
    }

    @Transactional(readOnly = true)
    public List<LaboratoryReport> getReportsForPatient(Long patientId) {
        return laboratoryReportRepository.findByPatient_PatientIdOrderByTestDateDesc(patientId);
    }

    @Transactional(readOnly = true)
    public List<LaboratoryReport> getReportsForDoctor(Long doctorId) {
        return laboratoryReportRepository.findByDoctor_DoctorIdOrderByTestDateDesc(doctorId);
    }

    @Transactional(readOnly = true)
    public LaboratoryReport getReportById(Long reportId) {
        return laboratoryReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratory report not found: " + reportId));
    }

    public Resource loadFileAsResource(String storedFileName) {
        try {
            Path filePath = this.uploadPath.resolve(storedFileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found on server storage: " + storedFileName);
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File not found on server storage: " + storedFileName);
        }
    }
}
