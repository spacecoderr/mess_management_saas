package com.thali.auth.dto;

import com.thali.user.ApprovalStatus;
import com.thali.user.DietPref;
import com.thali.user.Role;
import com.thali.user.StudentProfile;
import com.thali.user.User;

/** Public view of a user. Student-only fields are null for staff/admin. */
public record UserResponse(Long id, String name, String email, Role role, String phone, boolean emailVerified,
                           ApprovalStatus approvalStatus, String rollNo, Short batchYear,
                           String hostelBlock, String roomNo, DietPref dietPref) {

    public static UserResponse of(User u, StudentProfile p) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getPhone(), u.isEmailVerified(),
                p == null ? null : p.getApprovalStatus(), p == null ? null : p.getRollNo(),
                p == null ? null : p.getBatchYear(), p == null ? null : p.getHostelBlock(),
                p == null ? null : p.getRoomNo(), p == null ? null : p.getDietPref());
    }
}
