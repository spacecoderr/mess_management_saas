package com.thali.billing;

import java.math.BigDecimal;

/** Seam for a real provider (e.g. Razorpay test mode). Only MockPaymentGateway exists for now. */
public interface PaymentGateway {

    record Order(String txnRef, String payload) {}

    Order createOrder(BigDecimal amount, PaymentMethod method, String description);

    /** Returns true when the provider reports the payment as successful. */
    boolean confirm(String txnRef);
}
