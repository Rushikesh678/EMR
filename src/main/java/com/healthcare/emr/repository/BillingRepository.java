package com.healthcare.emr.repository;

import com.healthcare.emr.entity.Billing;
import com.healthcare.emr.entity.BillingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillingRepository extends JpaRepository<Billing, Long> {

    List<Billing> findByPatient_PatientIdOrderByCreatedAtDesc(Long patientId);

    Optional<Billing> findByAppointment_AppointmentId(Long appointmentId);

    List<Billing> findByStatusOrderByCreatedAtDesc(BillingStatus status);

    @Query("SELECT COALESCE(SUM(b.amount), 0) FROM Billing b WHERE b.status = 'PAID'")
    BigDecimal sumTotalPaidRevenue();

    @Query("SELECT COALESCE(SUM(b.amount), 0) FROM Billing b WHERE b.status = 'PENDING'")
    BigDecimal sumTotalPendingRevenue();
}
