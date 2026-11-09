package com.thali.user;

import java.util.EnumSet;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thali.auth.EmailPolicy;
import com.thali.auth.RefreshTokenRepository;
import com.thali.auth.dto.UserResponse;
import com.thali.common.PageResponse;
import com.thali.common.exception.ApiException;
import com.thali.user.dto.ChangePasswordRequest;
import com.thali.user.dto.CreateUserRequest;
import com.thali.user.dto.StudentResponse;
import com.thali.user.dto.UpdateProfileRequest;
import com.thali.user.dto.UpdateUserRequest;
import com.thali.user.dto.UserAdminResponse;

@Service
public class UserService {

    private final UserRepository users;
    private final StudentProfileRepository profiles;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final EmailPolicy emailPolicy;

    public UserService(UserRepository users, StudentProfileRepository profiles, RefreshTokenRepository refreshTokens,
                       PasswordEncoder encoder, EmailPolicy emailPolicy) {
        this.users = users;
        this.profiles = profiles;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.emailPolicy = emailPolicy;
    }

    // ------------------------------------------------------------- admin: users

    @Transactional(readOnly = true)
    public PageResponse<UserAdminResponse> list(Role role, String q, int page, int size) {
        var roles = role == null ? EnumSet.allOf(Role.class) : EnumSet.of(role);
        var result = users.search(roles, like(q), PageRequest.of(page, clamp(size), Sort.by("name")));
        return PageResponse.of(result, UserAdminResponse::of);
    }

    @Transactional
    public UserAdminResponse create(CreateUserRequest req) {
        if (req.role() == Role.STUDENT) {
            throw ApiException.badRequest("STUDENTS_SELF_REGISTER", "Students register themselves with their college email.");
        }
        String email = emailPolicy.normalize(req.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("EMAIL_ALREADY_REGISTERED", "A user with this email already exists.");
        }
        User u = new User();
        u.setName(req.name().trim());
        u.setEmail(email);
        u.setPhone(req.phone());
        u.setRole(req.role());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setEmailVerified(true);
        return UserAdminResponse.of(users.save(u));
    }

    @Transactional
    public UserAdminResponse update(Long id, UpdateUserRequest req) {
        User u = get(id);
        u.setName(req.name().trim());
        u.setPhone(req.phone());
        return UserAdminResponse.of(u);
    }

    @Transactional
    public UserAdminResponse setActive(Long id, boolean active, Long actingAdminId) {
        User u = get(id);
        if (u.getId().equals(actingAdminId) && !active) {
            throw ApiException.unprocessable("CANNOT_DISABLE_SELF", "You can't disable your own account.");
        }
        u.setActive(active);
        if (!active) refreshTokens.revokeAllForUser(u.getId());
        return UserAdminResponse.of(u);
    }

    // --------------------------------------------------------- admin: students

    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> students(ApprovalStatus status, String q, int page, int size) {
        var statuses = status == null ? EnumSet.allOf(ApprovalStatus.class) : EnumSet.of(status);
        var result = profiles.search(statuses, like(q), PageRequest.of(page, clamp(size), Sort.by("rollNo")));
        return PageResponse.of(result, StudentResponse::of);
    }

    @Transactional
    public StudentResponse decide(Long userId, ApprovalStatus status) {
        if (status == ApprovalStatus.PENDING) {
            throw ApiException.badRequest("INVALID_STATUS", "Choose APPROVED or REJECTED.");
        }
        StudentProfile p = profiles.findById(userId)
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND", "Student not found."));
        p.setApprovalStatus(status);
        if (status == ApprovalStatus.REJECTED) refreshTokens.revokeAllForUser(userId);
        return StudentResponse.of(p);
    }

    // ------------------------------------------------------------------ self

    @Transactional(readOnly = true)
    public UserResponse profile(Long userId) {
        return UserResponse.of(get(userId), profiles.findById(userId).orElse(null));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User u = get(userId);
        u.setName(req.name().trim());
        if (req.phone() != null) u.setPhone(req.phone());
        StudentProfile p = u.getRole() == Role.STUDENT ? profiles.findById(userId).orElse(null) : null;
        if (p != null) {
            if (req.hostelBlock() != null) p.setHostelBlock(req.hostelBlock().trim());
            if (req.roomNo() != null) p.setRoomNo(req.roomNo().trim());
            if (req.dietPref() != null) p.setDietPref(req.dietPref());
            if (req.batchYear() != null) p.setBatchYear(req.batchYear().shortValue());
        }
        return UserResponse.of(u, p);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest req) {
        User u = get(userId);
        if (!encoder.matches(req.currentPassword(), u.getPasswordHash())) {
            throw ApiException.badRequest("WRONG_PASSWORD", "Your current password is incorrect.");
        }
        u.setPasswordHash(encoder.encode(req.newPassword()));
        refreshTokens.revokeAllForUser(userId);
    }

    // --------------------------------------------------------------- helpers

    private User get(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found."));
    }

    static String like(String q) {
        return q == null || q.isBlank() ? "%" : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
    }

    static int clamp(int size) {
        return Math.max(1, Math.min(size, 100));
    }

}
