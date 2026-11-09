package com.thali.booking;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.thali.booking.dto.BookingRequest;
import com.thali.booking.dto.BookingResponse;
import com.thali.booking.dto.BulkBookingRequest;
import com.thali.booking.dto.BulkBookingResponse;
import com.thali.booking.dto.QrResponse;
import com.thali.menu.MealType;
import com.thali.security.CurrentUser;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Bookings", description = "Daily meal booking with cutoff rules and QR passes")
@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService service;
    private final Clock clock;

    public BookingController(BookingService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/me")
    public List<BookingResponse> mine(@AuthenticationPrincipal Jwt jwt,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now(clock);
        return service.mine(CurrentUser.id(jwt), start, to != null ? to : start.plusDays(13));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse book(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody BookingRequest req) {
        return service.book(CurrentUser.id(jwt), req);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/bulk")
    public BulkBookingResponse bulk(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody BulkBookingRequest req) {
        return service.bookMany(CurrentUser.id(jwt), req.items());
    }

    @PreAuthorize("hasRole('STUDENT')")
    @DeleteMapping("/{id}")
    public BookingResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.cancel(CurrentUser.id(jwt), id);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/{id}/qr")
    public QrResponse qr(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.qr(CurrentUser.id(jwt), id);
    }

    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @GetMapping
    public List<BookingResponse> forStaff(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                          @RequestParam(required = false) MealType mealType,
                                          @RequestParam(required = false) BookingStatus status,
                                          @RequestParam(required = false) String q) {
        return service.forStaff(date, mealType, status, q);
    }
}
