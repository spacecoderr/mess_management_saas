package com.thali.user;

import com.thali.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "student_profiles")
public class StudentProfile extends BaseEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "roll_no", nullable = false, unique = true, length = 20)
    private String rollNo;

    @Column(name = "batch_year")
    private Short batchYear;

    @Column(name = "hostel_block", length = 30)
    private String hostelBlock;

    @Column(name = "room_no", length = 20)
    private String roomNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "diet_pref", nullable = false, length = 20)
    private DietPref dietPref = DietPref.VEG;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;
}
