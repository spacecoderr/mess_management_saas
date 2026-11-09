package com.thali.billing.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.thali.billing.Payment;
import com.thali.billing.PaymentMethod;
import com.thali.billing.PaymentStatus;

/** gatewayPayload is only returned by checkout (UPI deep link / mock card reference for the UI). */
public record PaymentResponse(Long id, Long billId, String billMonth, String studentName, String rollNo, BigDecimal amount,
                              PaymentMethod method, String txnRef, PaymentStatus status, Instant paidAt, Instant createdAt,
                              String recordedBy, String gatewayPayload) {

    public static PaymentResponse of(Payment p, String rollNo, String gatewayPayload) {
        return new PaymentResponse(p.getId(), p.getBill().getId(), p.getBill().getBillMonth().toString().substring(0, 7),
                p.getBill().getStudent().getName(), rollNo, p.getAmount(), p.getMethod(), p.getTxnRef(), p.getStatus(),
                p.getPaidAt(), p.getCreatedAt(), p.getRecordedBy() == null ? null : p.getRecordedBy().getName(), gatewayPayload);
    }
}
