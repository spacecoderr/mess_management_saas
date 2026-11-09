package com.thali.billing;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.stereotype.Component;

/** Pretend gateway for demos: every payment succeeds. The UPI payload is a real-looking deep link (no money moves). */
@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public Order createOrder(BigDecimal amount, PaymentMethod method, String description) {
        String ref = "MOCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String payload = method == PaymentMethod.UPI
                ? "upi://pay?pa=thali.mess@mockbank&pn=Thali%20Mess&cu=INR&am=" + amount.toPlainString()
                        + "&tn=" + URLEncoder.encode(description, StandardCharsets.UTF_8) + "&tr=" + ref
                : "mock-card-checkout:" + ref;
        return new Order(ref, payload);
    }

    @Override
    public boolean confirm(String txnRef) {
        return txnRef != null && txnRef.startsWith("MOCK-");
    }
}
