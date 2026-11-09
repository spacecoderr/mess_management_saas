package com.thali.user.dto;

import java.time.Instant;

import com.thali.user.ApprovalStatus;
import com.thali.user.DietPref;
import com.thali.user.StudentProfile;

public record StudentResponse(Long userId, String name, String email, String phone, String rollNo, Short batchYear,
                              String hostelBlock, String roomNo, DietPref dietPref, ApprovalStatus approvalStatus,
                              boolean active, Instant registeredAt) {
    public static StudentResponse of(StudentProfile p) {
        var u = p.getUser();
        return new StudentResponse(u.getId(), u.getName(), u.getEmail(), u.getPhone(), p.getRollNo(), p.getBatchYear(),
                p.getHostelBlock(), p.getRoomNo(), p.getDietPref(), p.getApprovalStatus(), u.isActive(), u.getCreatedAt());
    }
}
