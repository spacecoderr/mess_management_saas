package com.thali.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.thali.common.BaseEntity;
import com.thali.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "bills", uniqueConstraints = @UniqueConstraint(columnNames = { "student_id", "bill_month" }))
public class Bill extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @Column(name = "bill_month", nullable = false)
    private LocalDate billMonth;

    @Column(name = "meal_count", nullable = false)
    private Integer mealCount = 0;

    @Column(name = "meal_charges", nullable = false, precision = 10, scale = 2)
    private BigDecimal mealCharges = BigDecimal.ZERO;

    @Column(name = "fixed_charges", nullable = false, precision = 10, scale = 2)
    private BigDecimal fixedCharges = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BillStatus status = BillStatus.UNPAID;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt = Instant.now();

    @Version
    private Long version;
}
