package com.thali.support;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.test.util.ReflectionTestUtils;

import com.thali.booking.Booking;
import com.thali.booking.BookingStatus;
import com.thali.menu.DailyMenu;
import com.thali.menu.MealSlotConfig;
import com.thali.menu.MealType;
import com.thali.user.ApprovalStatus;
import com.thali.user.Role;
import com.thali.user.StudentProfile;
import com.thali.user.User;

/** Tiny builders for plain (non-persisted) entities used by unit tests. */
public final class Fixtures {

    private Fixtures() {}

    public static User user(long id, Role role, String name) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        u.setName(name);
        u.setEmail(name.toLowerCase().replace(' ', '.') + "@test.local");
        return u;
    }

    public static StudentProfile profile(User u, String roll) {
        StudentProfile p = new StudentProfile();
        p.setUser(u);
        p.setUserId(u.getId());
        p.setRollNo(roll);
        p.setApprovalStatus(ApprovalStatus.APPROVED);
        return p;
    }

    public static MealSlotConfig slot(MealType t, String start, String end, int cutoffMinutes, String price) {
        MealSlotConfig c = new MealSlotConfig();
        c.setMealType(t);
        c.setStartTime(LocalTime.parse(start));
        c.setEndTime(LocalTime.parse(end));
        c.setCutoffMinutes(cutoffMinutes);
        c.setPrice(new BigDecimal(price));
        c.setActive(true);
        return c;
    }

    /** Same defaults as the V3 migration. */
    public static List<MealSlotConfig> defaultSlots() {
        return List.of(slot(MealType.BREAKFAST, "07:30", "09:00", 240, "35.00"), slot(MealType.LUNCH, "12:30", "14:30", 240, "60.00"),
                slot(MealType.SNACKS, "17:00", "18:00", 120, "20.00"), slot(MealType.DINNER, "20:00", "21:30", 240, "60.00"));
    }

    public static DailyMenu menu(long id, LocalDate date, MealType type, boolean published) {
        DailyMenu m = new DailyMenu();
        m.setId(id);
        m.setMenuDate(date);
        m.setMealType(type);
        m.setPublished(published);
        return m;
    }

    public static Booking booking(long id, User student, DailyMenu menu, BookingStatus status, String price) {
        Booking b = new Booking();
        b.setId(id);
        b.setStudent(student);
        b.setDailyMenu(menu);
        b.setStatus(status);
        b.setPriceAtBooking(new BigDecimal(price));
        return b;
    }

    /** created_at is normally set by @PrePersist; unit tests set it directly. */
    public static void createdAt(Object entity, Instant when) {
        ReflectionTestUtils.setField(entity, "createdAt", when);
    }
}
