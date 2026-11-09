package com.thali.complaint;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.thali.common.PageResponse;
import com.thali.complaint.dto.AssignRequest;
import com.thali.complaint.dto.ComplaintResponse;
import com.thali.complaint.dto.ComplaintUpdateRequest;
import com.thali.complaint.dto.CreateComplaintRequest;
import com.thali.security.CurrentUser;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Complaints", description = "Raise, assign, update and resolve complaints")
@RestController
@RequestMapping("/api/v1/complaints")
public class ComplaintController {

    private final ComplaintService service;

    public ComplaintController(ComplaintService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateComplaintRequest req) {
        return service.create(CurrentUser.id(jwt), req);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/me")
    public List<ComplaintResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.mine(CurrentUser.id(jwt));
    }

    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @GetMapping
    public PageResponse<ComplaintResponse> search(@AuthenticationPrincipal Jwt jwt,
                                                  @RequestParam(required = false) ComplaintStatus status,
                                                  @RequestParam(required = false) ComplaintCategory category,
                                                  @RequestParam(defaultValue = "false") boolean assignedToMe,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return service.search(status, category, assignedToMe ? CurrentUser.id(jwt) : null, page, size);
    }

    @GetMapping("/{id}")
    public ComplaintResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.get(id, CurrentUser.id(jwt), CurrentUser.role(jwt));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/assign")
    public ComplaintResponse assign(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody AssignRequest req) {
        return service.assign(id, req.staffId(), CurrentUser.id(jwt));
    }

    @PostMapping("/{id}/updates")
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintResponse addUpdate(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                       @Valid @RequestBody ComplaintUpdateRequest req) {
        return service.addUpdate(id, CurrentUser.id(jwt), CurrentUser.role(jwt), req);
    }
}
