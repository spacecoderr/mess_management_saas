package com.thali.attendance;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thali.attendance.dto.AttendanceResponse;
import com.thali.attendance.dto.LiveCountsResponse;
import com.thali.attendance.dto.ManualRequest;
import com.thali.attendance.dto.ScanRequest;
import com.thali.menu.MealType;
import com.thali.security.CurrentUser;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Attendance", description = "Mark meals as served by QR scan or manually, and see live counts")
@PreAuthorize("hasAnyRole('STAFF','ADMIN')")
@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    private final AttendanceService service;
    private final Clock clock;

    public AttendanceController(AttendanceService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @PostMapping("/scan")
    public AttendanceResponse scan(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ScanRequest req) {
        return service.scan(CurrentUser.id(jwt), req.qrToken());
    }

    @PostMapping("/manual")
    public AttendanceResponse manual(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ManualRequest req) {
        return service.manual(CurrentUser.id(jwt), req.bookingId());
    }

    @GetMapping("/live")
    public List<LiveCountsResponse> live(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                         @RequestParam(required = false) MealType mealType) {
        return service.live(date != null ? date : LocalDate.now(clock), mealType);
    }
}
