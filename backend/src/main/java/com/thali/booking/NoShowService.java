package com.thali.booking;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thali.config.AppProperties;
import com.thali.menu.MealSlotConfig;
import com.thali.menu.MealSlotService;
import com.thali.menu.MealType;

import java.util.Map;

/** Booked-but-never-scanned meals become NO_SHOW (and are still billed) once the serving window plus grace has ended. */
@Service
public class NoShowService {

    private static final Logger log = LoggerFactory.getLogger(NoShowService.class);

    private final BookingRepository bookings;
    private final MealSlotService slots;
    private final AppProperties.Attendance cfg;
    private final Clock clock;

    public NoShowService(BookingRepository bookings, MealSlotService slots, AppProperties props, Clock clock) {
        this.bookings = bookings;
        this.slots = slots;
        this.cfg = props.attendance();
        this.clock = clock;
    }

    @Transactional
    public int markNoShows() {
        Instant now = clock.instant();
        Map<MealType, MealSlotConfig> cfgBySlot = slots.all();
        int marked = 0;
        for (Booking b : bookings.findOpenUpTo(LocalDate.now(clock))) {
            MealSlotConfig slot = cfgBySlot.get(b.getDailyMenu().getMealType());
            Instant expiresAt = slots.endsAt(slot, b.getDailyMenu().getMenuDate()).plusSeconds(cfg.lateMinutes() * 60L);
            if (now.isAfter(expiresAt)) {
                b.setStatus(BookingStatus.NO_SHOW);
                marked++;
            }
        }
        if (marked > 0) log.info("Marked {} bookings as NO_SHOW", marked);
        return marked;
    }
}
