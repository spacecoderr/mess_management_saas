package com.thali.billing.dto;

import com.thali.billing.PaymentMethod;

import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(@NotNull Long billId, @NotNull PaymentMethod method) {}
