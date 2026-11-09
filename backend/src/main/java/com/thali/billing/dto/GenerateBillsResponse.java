package com.thali.billing.dto;

/** created = new bills, updated = unpaid bills recalculated, skippedPaid = already paid so left untouched. */
public record GenerateBillsResponse(String month, int created, int updated, int skippedPaid) {}
