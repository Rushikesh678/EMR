package com.healthcare.emr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class PaymentRequest {

    @NotNull(message = "Billing ID is required")
    private Long billingId;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod; // e.g. "Credit Card (Demo)", "Debit Card (Demo)", "UPI / Net Banking (Demo)"

    // Optional demo payment holder name / simulation note
    private String cardHolderName;

    public PaymentRequest() {}

    public Long getBillingId() {
        return billingId;
    }

    public void setBillingId(Long billingId) {
        this.billingId = billingId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }
}
