package com.thali.menu;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thali.booking.BookingRepository;
import com.thali.booking.BookingStatus;
import com.thali.common.exception.ApiException;
import com.thali.menu.dto.CopyWeekRequest;
import com.thali.menu.dto.CopyWeekResponse;
import com.thali.menu.dto.DailyMenuResponse;
import com.thali.menu.dto.SetMenuRequest;
import com.thali.user.UserRepository;

@Service
public class MenuService {

    private static final int MAX_RANGE_DAYS = 62;

    private final DailyMenuRepository menus;
    private final MenuItemRepository items;
    private final BookingRepository bookings;
    private final UserRepository users;
    private final MealSlotService slots;
    private final Clock clock;

    public MenuService(DailyMenuRepository menus, MenuItemRepository items, BookingRepository bookings,
                       UserRepository users, MealSlotService slots, Clock clock) {
        this.menus = menus;
        this.items = items;
        this.bookings = bookings;
        this.users = users;
        this.slots = slots;
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /** Students only ever see published menus; staff/admin also see drafts. */
    @Transactional(readOnly = true)
    public List<DailyMenuResponse> range(LocalDate from, LocalDate to, boolean includeDrafts) {
        if (to.isBefore(from)) throw ApiException.badRequest("INVALID_RANGE", "`to` must not be before `from`.");
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw ApiException.badRequest("RANGE_TOO_LARGE", "Ask for at most " + MAX_RANGE_DAYS + " days at a time.");
        }
        Map<MealType, MealSlotConfig> cfg = slots.all();
        Instant now = clock.instant();
        return menus.findRange(from, to, includeDrafts).stream()
                .sorted(java.util.Comparator.comparing(DailyMenu::getMenuDate).thenComparing(DailyMenu::getMealType))
                .map(m -> toResponse(m, cfg, now)).toList();
    }

    @Transactional
    public DailyMenuResponse setItems(LocalDate date, MealType type, SetMenuRequest req, Long actorId) {
        if (date.isBefore(today())) {
            throw ApiException.unprocessable("MENU_IN_PAST", "Menus for past dates can't be changed.");
        }
        List<MenuItem> found = items.findAllById(new LinkedHashSet<>(req.itemIds()));
        if (found.size() != new LinkedHashSet<>(req.itemIds()).size()) {
            throw ApiException.badRequest("DISH_NOT_FOUND", "One or more dishes don't exist.");
        }
        if (found.stream().anyMatch(i -> !i.isActive())) {
            throw ApiException.unprocessable("DISH_INACTIVE", "Inactive dishes can't be added to a menu.");
        }
        slots.get(type);   // 404 for unknown meal
        DailyMenu menu = menus.findByDateAndType(date, type).orElseGet(() -> {
            DailyMenu m = new DailyMenu();
            m.setMenuDate(date);
            m.setMealType(type);
            m.setCreatedBy(users.getReferenceById(actorId));
            return m;
        });
        menu.getItems().clear();
        menu.getItems().addAll(found);
        if (req.publish() != null) applyPublish(menu, req.publish());
        return toResponse(menus.save(menu), slots.all(), clock.instant());
    }

    @Transactional
    public DailyMenuResponse publish(Long id, boolean published) {
        DailyMenu menu = menus.findWithItems(id).orElseThrow(() -> ApiException.notFound("MENU_NOT_FOUND", "Menu not found."));
        applyPublish(menu, published);
        return toResponse(menu, slots.all(), clock.instant());
    }

    @Transactional
    public CopyWeekResponse copyWeek(CopyWeekRequest req, Long actorId) {
        boolean overwrite = Boolean.TRUE.equals(req.overwrite());
        if (req.toWeekStart().isBefore(today())) {
            throw ApiException.unprocessable("MENU_IN_PAST", "Can't copy onto past dates.");
        }
        int copied = 0, skipped = 0;
        for (DailyMenu src : menus.findRange(req.fromWeekStart(), req.fromWeekStart().plusDays(6), true)) {
            LocalDate target = req.toWeekStart().plusDays(ChronoUnit.DAYS.between(req.fromWeekStart(), src.getMenuDate()));
            if (target.isBefore(today())) { skipped++; continue; }
            DailyMenu dest = menus.findByDateAndType(target, src.getMealType()).orElse(null);
            if (dest != null && !overwrite) { skipped++; continue; }
            if (dest == null) {
                dest = new DailyMenu();
                dest.setMenuDate(target);
                dest.setMealType(src.getMealType());
                dest.setCreatedBy(users.getReferenceById(actorId));
            }
            dest.getItems().clear();
            dest.getItems().addAll(src.getItems());
            menus.save(dest);
            copied++;
        }
        return new CopyWeekResponse(copied, skipped);
    }

    /**
     * Looks up a menu or fails with 404; used by booking code. noRollbackFor matters: callers such as bulk booking catch this
     * ApiException and carry on, and without it the exception passing through this proxy would mark their transaction rollback-only.
     */
    @Transactional(readOnly = true, noRollbackFor = ApiException.class)
    public DailyMenu require(LocalDate date, MealType type) {
        return menus.findByDateAndType(date, type)
                .orElseThrow(() -> ApiException.notFound("MENU_NOT_FOUND", "No menu has been set for that meal."));
    }

    private void applyPublish(DailyMenu menu, boolean published) {
        if (published && menu.getItems().isEmpty()) {
            throw ApiException.unprocessable("MENU_EMPTY", "Add at least one dish before publishing.");
        }
        if (!published && menu.isPublished() && menu.getId() != null
                && bookings.countByDailyMenuIdAndStatus(menu.getId(), BookingStatus.BOOKED) > 0) {
            throw ApiException.unprocessable("MENU_HAS_BOOKINGS", "Students have already booked this meal, so it can't be unpublished.");
        }
        menu.setPublished(published);
    }

    private DailyMenuResponse toResponse(DailyMenu m, Map<MealType, MealSlotConfig> cfg, Instant now) {
        MealSlotConfig slot = cfg.get(m.getMealType());
        return DailyMenuResponse.of(m, slot, slots.bookingClosesAt(slot, m.getMenuDate()), now);
    }
}
