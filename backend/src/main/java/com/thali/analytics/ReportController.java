package com.thali.analytics;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Reports", description = "CSV exports")
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private static final MediaType CSV = new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8);

    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/bills.csv")
    public ResponseEntity<byte[]> bills(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return csv(reports.billsCsv(month), "bills-" + month + ".csv");
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/attendance.csv")
    public ResponseEntity<byte[]> attendance(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return csv(reports.attendanceCsv(from, to), "attendance-" + from + "_" + to + ".csv");
    }

    private ResponseEntity<byte[]> csv(byte[] body, String filename) {
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(body);
    }
}
