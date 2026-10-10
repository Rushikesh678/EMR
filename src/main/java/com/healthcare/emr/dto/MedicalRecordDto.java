package com.healthcare.emr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class MedicalRecordDto {
    private Long recordId;

    @NotNull(message = "Patient ID is required")
    private Long patientId;
    private String patientName;

    private Long doctorId;
    private String doctorName;

    private Long appointmentId;

    private LocalDate recordDate;

    @NotBlank(message = "Diagnosis is required")
    private String diagnosis;

    @NotBlank(message = "Treatment plan is required")
    private String treatment;

    private String medicalHistoryNotes;
    private String clinicalNotes;
    private LocalDateTime createdAt;

    public MedicalRecordDto() {}

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(LocalDate recordDate) {
        this.recordDate = recordDate;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getTreatment() {
        return treatment;
    }

    public void setTreatment(String treatment) {
        this.treatment = treatment;
    }

    public String getMedicalHistoryNotes() {
        return medicalHistoryNotes;
    }

    public void setMedicalHistoryNotes(String medicalHistoryNotes) {
        this.medicalHistoryNotes = medicalHistoryNotes;
    }

    public String getClinicalNotes() {
        return clinicalNotes;
    }

    public void setClinicalNotes(String clinicalNotes) {
        this.clinicalNotes = clinicalNotes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
