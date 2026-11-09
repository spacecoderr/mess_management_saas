package com.thali.billing;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.thali.billing.dto.BillResponse;
import com.thali.billing.dto.CashPaymentRequest;
import com.thali.billing.dto.CheckoutRequest;
import com.thali.billing.dto.GenerateBillsResponse;
import com.thali.billing.dto.PaymentResponse;
import com.thali.common.PageResponse;
import com.thali.security.CurrentUser;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Billing & Payments", description = "Monthly bills, mock online payments and cash recording")
@RestController
@RequestMapping("/api/v1")
public class BillingController {

    private final BillingService billing;
    private final PaymentService payments;
    private final Clock clock;

    public BillingController(BillingService billing, PaymentService payments, Clock clock) {
        this.billing = billing;
        this.payments = payments;
        this.clock = clock;
    }

    // ---- bills
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/bills/generate")
    public GenerateBillsResponse generate(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return billing.generate(month);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/bills")
    public PageResponse<BillResponse> bills(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                                            @RequestParam(required = false) BillStatus status,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return billing.search(month, status, page, size);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/bills/me")
    public List<BillResponse> myBills(@AuthenticationPrincipal Jwt jwt) {
        return billing.mine(CurrentUser.id(jwt));
    }

    @GetMapping("/bills/{id}")
    public BillResponse bill(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return billing.get(id, CurrentUser.id(jwt), CurrentUser.role(jwt));
    }

    // ---- payments
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/payments/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse checkout(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CheckoutRequest req) {
        return payments.checkout(CurrentUser.id(jwt), req.billId(), req.method());
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/payments/{id}/confirm")
    public PaymentResponse confirm(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return payments.confirm(CurrentUser.id(jwt), id);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/payments/me")
    public List<PaymentResponse> myPayments(@AuthenticationPrincipal Jwt jwt) {
        return payments.mine(CurrentUser.id(jwt));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/payments/cash")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse cash(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CashPaymentRequest req) {
        return payments.recordCash(CurrentUser.id(jwt), req.billId(), req.amount());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/payments")
    public PageResponse<PaymentResponse> allPayments(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                     @RequestParam(required = false) PaymentStatus status,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        LocalDate end = to != null ? to : LocalDate.now(clock);
        return payments.search(from != null ? from : end.minusDays(30), end, status, page, size);
    }
}
