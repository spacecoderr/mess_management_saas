package com.thali.feedback;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.thali.common.PageResponse;
import com.thali.feedback.dto.FeedbackRequest;
import com.thali.feedback.dto.FeedbackResponse;
import com.thali.feedback.dto.FeedbackSummaryResponse;
import com.thali.feedback.dto.PendingFeedbackResponse;
import com.thali.menu.MealType;
import com.thali.security.CurrentUser;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Feedback", description = "Meal ratings (students) and rating analytics (staff/admin)")
@RestController
@RequestMapping("/api/v1/feedback")
public class FeedbackController {

    private final FeedbackService service;
    private final Clock clock;

    public FeedbackController(FeedbackService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackResponse submit(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody FeedbackRequest req) {
        return service.submit(CurrentUser.id(jwt), req);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/me")
    public List<FeedbackResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.mine(CurrentUser.id(jwt));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/me/pending")
    public List<PendingFeedbackResponse> pending(@AuthenticationPrincipal Jwt jwt) {
        return service.pending(CurrentUser.id(jwt));
    }

    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @GetMapping
    public PageResponse<FeedbackResponse> search(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                 @RequestParam(required = false) MealType mealType,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        LocalDate end = to != null ? to : LocalDate.now(clock);
        return service.search(from != null ? from : end.minusDays(30), end, mealType, page, size);
    }

    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @GetMapping("/summary")
    public FeedbackSummaryResponse summary(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now(clock);
        return service.summary(from != null ? from : end.minusDays(30), end);
    }
}
