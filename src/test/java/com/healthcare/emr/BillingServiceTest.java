package com.healthcare.emr;

import com.healthcare.emr.dto.BillingDto;
import com.healthcare.emr.dto.PaymentRequest;
import com.healthcare.emr.entity.Billing;
import com.healthcare.emr.entity.BillingStatus;
import com.healthcare.emr.exception.BadRequestException;
import com.healthcare.emr.exception.ResourceNotFoundException;
import com.healthcare.emr.repository.BillingRepository;
import com.healthcare.emr.service.BillingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class BillingServiceTest {

    @Autowired
    private BillingService billingService;

    @Autowired
    private BillingRepository billingRepository;

    @Test
    @DisplayName("Should query bills, retrieve bill by id, and process simulated payment")
    void testBillingAndPayment() {
        List<BillingDto> allBills = billingService.getAllBills();
        assertFalse(allBills.isEmpty(), "Seeded bills should exist");

        Billing firstBill = billingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BillingStatus.PENDING)
                .findFirst()
                .orElse(null);
        assertNotNull(firstBill, "At least one pending bill should be seeded");

        // Query by ID
        BillingDto byId = billingService.getBillById(firstBill.getBillingId());
        assertEquals(firstBill.getBillingId(), byId.getBillingId());

        // Query by Patient
        List<BillingDto> patientBills = billingService.getBillsForPatient(firstBill.getPatient().getPatientId());
        assertFalse(patientBills.isEmpty());

        // Process simulated payment with mismatched amount throws BadRequestException
        PaymentRequest badAmountReq = new PaymentRequest();
        badAmountReq.setBillingId(firstBill.getBillingId());
        badAmountReq.setAmount(firstBill.getAmount().add(BigDecimal.TEN));
        badAmountReq.setPaymentMethod("Credit Card (Demo)");

        assertThrows(BadRequestException.class, () -> {
            billingService.processSimulatedPayment(badAmountReq, "payer@test.local");
        });

        // Valid Payment
        PaymentRequest validReq = new PaymentRequest();
        validReq.setBillingId(firstBill.getBillingId());
        validReq.setAmount(firstBill.getAmount());
        validReq.setPaymentMethod("Debit Card (Demo)");

        BillingDto paidDto = billingService.processSimulatedPayment(validReq, "payer@test.local");
        assertEquals(BillingStatus.PAID, paidDto.getStatus());
        assertNotNull(paidDto.getTransactionRef());

        // Attempting to pay an already paid invoice throws BadRequestException
        assertThrows(BadRequestException.class, () -> {
            billingService.processSimulatedPayment(validReq, "payer@test.local");
        });

        // Non-existent bill
        assertThrows(ResourceNotFoundException.class, () -> billingService.getBillById(999999L));
    }
}
