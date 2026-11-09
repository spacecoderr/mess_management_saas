package com.thali.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.thali.common.BaseEntity;
import com.thali.menu.DailyMenu;
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
@Table(name = "bookings", uniqueConstraints = @UniqueConstraint(columnNames = { "student_id", "daily_menu_id" }))
public class Booking extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_menu_id")
    private DailyMenu dailyMenu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.BOOKED;

    @Column(name = "price_at_booking", nullable = false, precision = 8, scale = 2)
    private BigDecimal priceAtBooking;

    @Column(name = "qr_token", nullable = false, unique = true)
    private UUID qrToken = UUID.randomUUID();

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Version
    private Long version;
}
