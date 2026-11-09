package com.thali.billing.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.thali.billing.Bill;
import com.thali.billing.BillStatus;

/** `lines` (per-meal breakdown) and `paidAt` are only filled on the detail endpoint. */
public record BillResponse(Long id, Long studentId, String studentName, String rollNo, String month, int mealCount,
                           BigDecimal mealCharges, BigDecimal fixedCharges, BigDecimal total, BillStatus status,
                           LocalDate dueDate, Instant generatedAt, Instant paidAt, List<BillLine> lines) {

    public static BillResponse of(Bill b, String rollNo, Instant paidAt, List<BillLine> lines) {
        return new BillResponse(b.getId(), b.getStudent().getId(), b.getStudent().getName(), rollNo,
                b.getBillMonth().toString().substring(0, 7), b.getMealCount(), b.getMealCharges(), b.getFixedCharges(),
                b.getTotal(), b.getStatus(), b.getDueDate(), b.getGeneratedAt(), paidAt, lines);
    }
}
