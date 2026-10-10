package com.healthcare.emr.service;

import com.healthcare.emr.dto.BillingDto;
import com.healthcare.emr.dto.PaymentRequest;
import com.healthcare.emr.entity.*;
import com.healthcare.emr.exception.BadRequestException;
import com.healthcare.emr.exception.ResourceNotFoundException;
import com.healthcare.emr.repository.BillingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BillingService {

    private final BillingRepository billingRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public BillingService(BillingRepository billingRepository,
                          NotificationService notificationService,
                          AuditLogService auditLogService) {
        this.billingRepository = billingRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<BillingDto> getBillsForPatient(Long patientId) {
        return billingRepository.findByPatient_PatientIdOrderByCreatedAtDesc(patientId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BillingDto> getAllBills() {
        return billingRepository.findAll().stream()
                .map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BillingDto getBillById(Long billingId) {
        Billing bill = billingRepository.findById(billingId)
                .orElseThrow(() -> new ResourceNotFoundException("Billing record not found: " + billingId));
        return mapToDto(bill);
    }

    @Transactional
    public BillingDto processSimulatedPayment(PaymentRequest request, String payerEmail) {
        Billing bill = billingRepository.findById(request.getBillingId())
                .orElseThrow(() -> new ResourceNotFoundException("Billing record not found: " + request.getBillingId()));

        if (bill.getStatus() == BillingStatus.PAID) {
            throw new BadRequestException("This invoice has already been paid.");
        }

        // Validate amount matches
        if (request.getAmount() == null || bill.getAmount().compareTo(request.getAmount()) != 0) {
            throw new BadRequestException("Payment amount (" + request.getAmount() + ") does not match outstanding bill (" + bill.getAmount() + ")");
        }

        // Demo Payment Processing Simulation
        // Security requirement: Never store actual card numbers, CVVs, or cardholder PANs
        String demoTxnRef = "TXN-DEMO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String method = request.getPaymentMethod() != null ? request.getPaymentMethod() : "Credit Card (Simulated Demo)";

        bill.setStatus(BillingStatus.PAID);
        bill.setPaymentMethod(method);
        bill.setPaymentDate(LocalDateTime.now());
        bill.setTransactionRef(demoTxnRef);

        Billing updated = billingRepository.save(bill);

        notificationService.sendNotification(
                bill.getPatient().getUser(),
                "Payment Received: " + bill.getInvoiceNumber(),
                "Payment of $" + bill.getAmount() + " was processed successfully via " + method + ". Reference: " + demoTxnRef,
                "PAYMENT"
        );

        auditLogService.log(payerEmail, "BILLING", "SIMULATED_PAYMENT", "Billing", bill.getBillingId(), null,
                "Simulated payment $" + bill.getAmount() + " completed. TxnRef: " + demoTxnRef);

        return mapToDto(updated);
    }

    public BillingDto mapToDto(Billing b) {
        BillingDto dto = new BillingDto();
        dto.setBillingId(b.getBillingId());
        dto.setPatientId(b.getPatient().getPatientId());
        dto.setPatientName(b.getPatient().getUser().getName());
        dto.setAppointmentId(b.getAppointment() != null ? b.getAppointment().getAppointmentId() : null);
        dto.setDoctorName(b.getAppointment() != null ? b.getAppointment().getDoctor().getUser().getName() : "General Clinic");
        dto.setAmount(b.getAmount());
        dto.setStatus(b.getStatus());
        dto.setPaymentMethod(b.getPaymentMethod());
        dto.setPaymentDate(b.getPaymentDate());
        dto.setTransactionRef(b.getTransactionRef());
        dto.setInvoiceNumber(b.getInvoiceNumber());
        dto.setCreatedAt(b.getCreatedAt());
        return dto;
    }
}
