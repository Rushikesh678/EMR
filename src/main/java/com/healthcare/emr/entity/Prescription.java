package com.healthcare.emr.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "prescriptions")
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prescription_id")
    private Long prescriptionId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_record_id")
    private MedicalRecord medicalRecord;

    @Column(nullable = false, length = 255)
    private String medication;

    @Column(nullable = false, length = 100)
    private String dosage;

    @Column(nullable = false, length = 100)
    private String duration;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "prescribed_date", nullable = false)
    private LocalDate prescribedDate = LocalDate.now();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Prescription() {}

    public Prescription(Patient patient, Doctor doctor, MedicalRecord medicalRecord, String medication, String dosage, String duration, String instructions, LocalDate prescribedDate) {
        this.patient = patient;
        this.doctor = doctor;
        this.medicalRecord = medicalRecord;
        this.medication = medication;
        this.dosage = dosage;
        this.duration = duration;
        this.instructions = instructions;
        this.prescribedDate = prescribedDate != null ? prescribedDate : LocalDate.now();
        this.createdAt = LocalDateTime.now();
    }

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public MedicalRecord getMedicalRecord() {
        return medicalRecord;
    }

    public void setMedicalRecord(MedicalRecord medicalRecord) {
        this.medicalRecord = medicalRecord;
    }

    public String getMedication() {
        return medication;
    }

    public void setMedication(String medication) {
        this.medication = medication;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public LocalDate getPrescribedDate() {
        return prescribedDate;
    }

    public void setPrescribedDate(LocalDate prescribedDate) {
        this.prescribedDate = prescribedDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
