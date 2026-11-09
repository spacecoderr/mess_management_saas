package com.thali.user;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.thali.auth.dto.MessageResponse;
import com.thali.auth.dto.UserResponse;
import com.thali.common.PageResponse;
import com.thali.security.CurrentUser;
import com.thali.user.dto.ApprovalRequest;
import com.thali.user.dto.ChangePasswordRequest;
import com.thali.user.dto.CreateUserRequest;
import com.thali.user.dto.StudentResponse;
import com.thali.user.dto.UpdateProfileRequest;
import com.thali.user.dto.UpdateUserRequest;
import com.thali.user.dto.UserAdminResponse;
import com.thali.user.dto.UserStatusRequest;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Users", description = "Admin user management, student approval and own profile")
@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    // ---- admin: users
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public PageResponse<UserAdminResponse> users(@RequestParam(required = false) Role role,
                                                 @RequestParam(required = false) String q,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return service.list(role, q, page, size);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserAdminResponse createStaff(@Valid @RequestBody CreateUserRequest req) {
        return service.create(req);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{id}")
    public UserAdminResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) {
        return service.update(id, req);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{id}/status")
    public UserAdminResponse setStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest req,
                                       @AuthenticationPrincipal Jwt jwt) {
        return service.setActive(id, req.active(), CurrentUser.id(jwt));
    }

    // ---- admin: students
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/students")
    public PageResponse<StudentResponse> students(@RequestParam(required = false) ApprovalStatus status,
                                                  @RequestParam(required = false) String q,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return service.students(status, q, page, size);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/students/pending")
    public PageResponse<StudentResponse> pending(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "50") int size) {
        return service.students(ApprovalStatus.PENDING, null, page, size);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/students/{userId}/approval")
    public StudentResponse decide(@PathVariable Long userId, @Valid @RequestBody ApprovalRequest req) {
        return service.decide(userId, req.status());
    }

    // ---- self
    @GetMapping("/me/profile")
    public UserResponse profile(@AuthenticationPrincipal Jwt jwt) {
        return service.profile(CurrentUser.id(jwt));
    }

    @PutMapping("/me/profile")
    public UserResponse updateProfile(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest req) {
        return service.updateProfile(CurrentUser.id(jwt), req);
    }

    @PutMapping("/me/password")
    public MessageResponse changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest req) {
        service.changePassword(CurrentUser.id(jwt), req);
        return new MessageResponse("Password changed. Please log in again on your other devices.");
    }
}
