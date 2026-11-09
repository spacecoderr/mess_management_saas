package com.thali.menu;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Clock;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thali.common.exception.ApiException;
import com.thali.menu.dto.MealSlotRequest;
import com.thali.menu.dto.MealSlotResponse;

@Service
public class MealSlotService {

    private final MealSlotConfigRepository repo;
    private final Clock clock;

    public MealSlotService(MealSlotConfigRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MealSlotResponse> list() {
        return repo.findAll().stream().sorted((a, b) -> a.getMealType().compareTo(b.getMealType())).map(MealSlotResponse::of).toList();
    }

    @Transactional(readOnly = true, noRollbackFor = ApiException.class)
    public MealSlotConfig get(MealType type) {
        return repo.findById(type).orElseThrow(() -> ApiException.notFound("MEAL_SLOT_NOT_FOUND", "Unknown meal type."));
    }

    @Transactional(readOnly = true)
    public Map<MealType, MealSlotConfig> all() {
        Map<MealType, MealSlotConfig> map = new EnumMap<>(MealType.class);
        repo.findAll().forEach(s -> map.put(s.getMealType(), s));
        return map;
    }

    @Transactional
    public MealSlotResponse update(MealType type, MealSlotRequest req) {
        if (!req.endTime().isAfter(req.startTime())) {
            throw ApiException.unprocessable("INVALID_TIME_RANGE", "End time must be after start time.");
        }
        MealSlotConfig c = get(type);
        c.setStartTime(req.startTime());
        c.setEndTime(req.endTime());
        c.setCutoffMinutes(req.cutoffMinutes());
        c.setPrice(req.price());
        c.setActive(req.active());
        return MealSlotResponse.of(c);
    }

    public ZoneId zone() {
        return clock.getZone();
    }

    /** The instant a meal starts on the given date, in campus time. */
    public Instant startsAt(MealSlotConfig slot, LocalDate date) {
        return date.atTime(slot.getStartTime()).atZone(clock.getZone()).toInstant();
    }

    public Instant endsAt(MealSlotConfig slot, LocalDate date) {
        return date.atTime(slot.getEndTime()).atZone(clock.getZone()).toInstant();
    }

    /** Booking (and cancelling) is allowed strictly before this instant. */
    public Instant bookingClosesAt(MealSlotConfig slot, LocalDate date) {
        return startsAt(slot, date).minusSeconds(slot.getCutoffMinutes() * 60L);
    }
}
