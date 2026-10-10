package com.healthcare.emr.dto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class DashboardSummaryDto {
    private long totalPatients;
    private long totalDoctors;
    private long totalAppointments;
    private long appointmentsToday;
    private long pendingAppointments;
    private long completedAppointments;
    private long totalPrescriptions;
    private long totalMedicalRecords;
    private BigDecimal totalPaidRevenue;
    private BigDecimal totalPendingRevenue;
    private Map<String, Long> appointmentsByStatus = new HashMap<>();
    private Map<String, Long> appointmentsBySpecialization = new HashMap<>();

    public DashboardSummaryDto() {}

    public long getTotalPatients() {
        return totalPatients;
    }

    public void setTotalPatients(long totalPatients) {
        this.totalPatients = totalPatients;
    }

    public long getTotalDoctors() {
        return totalDoctors;
    }

    public void setTotalDoctors(long totalDoctors) {
        this.totalDoctors = totalDoctors;
    }

    public long getTotalAppointments() {
        return totalAppointments;
    }

    public void setTotalAppointments(long totalAppointments) {
        this.totalAppointments = totalAppointments;
    }

    public long getAppointmentsToday() {
        return appointmentsToday;
    }

    public void setAppointmentsToday(long appointmentsToday) {
        this.appointmentsToday = appointmentsToday;
    }

    public long getPendingAppointments() {
        return pendingAppointments;
    }

    public void setPendingAppointments(long pendingAppointments) {
        this.pendingAppointments = pendingAppointments;
    }

    public long getCompletedAppointments() {
        return completedAppointments;
    }

    public void setCompletedAppointments(long completedAppointments) {
        this.completedAppointments = completedAppointments;
    }

    public long getTotalPrescriptions() {
        return totalPrescriptions;
    }

    public void setTotalPrescriptions(long totalPrescriptions) {
        this.totalPrescriptions = totalPrescriptions;
    }

    public long getTotalMedicalRecords() {
        return totalMedicalRecords;
    }

    public void setTotalMedicalRecords(long totalMedicalRecords) {
        this.totalMedicalRecords = totalMedicalRecords;
    }

    public BigDecimal getTotalPaidRevenue() {
        return totalPaidRevenue;
    }

    public void setTotalPaidRevenue(BigDecimal totalPaidRevenue) {
        this.totalPaidRevenue = totalPaidRevenue;
    }

    public BigDecimal getTotalPendingRevenue() {
        return totalPendingRevenue;
    }

    public void setTotalPendingRevenue(BigDecimal totalPendingRevenue) {
        this.totalPendingRevenue = totalPendingRevenue;
    }

    public Map<String, Long> getAppointmentsByStatus() {
        return appointmentsByStatus;
    }

    public void setAppointmentsByStatus(Map<String, Long> appointmentsByStatus) {
        this.appointmentsByStatus = appointmentsByStatus;
    }

    public Map<String, Long> getAppointmentsBySpecialization() {
        return appointmentsBySpecialization;
    }

    public void setAppointmentsBySpecialization(Map<String, Long> appointmentsBySpecialization) {
        this.appointmentsBySpecialization = appointmentsBySpecialization;
    }
}
