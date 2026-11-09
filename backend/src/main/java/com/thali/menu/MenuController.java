package com.thali.menu;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.thali.menu.dto.CopyWeekRequest;
import com.thali.menu.dto.CopyWeekResponse;
import com.thali.menu.dto.DailyMenuResponse;
import com.thali.menu.dto.MealSlotRequest;
import com.thali.menu.dto.MealSlotResponse;
import com.thali.menu.dto.MenuItemRequest;
import com.thali.menu.dto.MenuItemResponse;
import com.thali.menu.dto.PublishRequest;
import com.thali.menu.dto.SetMenuRequest;
import com.thali.security.CurrentUser;
import com.thali.user.Role;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Menu", description = "Meal slots, dish library and daily menus")
@RestController
@RequestMapping("/api/v1")
public class MenuController {

    private final MealSlotService slots;
    private final MenuItemService dishes;
    private final MenuService menus;

    public MenuController(MealSlotService slots, MenuItemService dishes, MenuService menus) {
        this.slots = slots;
        this.dishes = dishes;
        this.menus = menus;
    }

    // ---- meal slots
    @GetMapping("/meal-slots")
    public List<MealSlotResponse> mealSlots() {
        return slots.list();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/meal-slots/{mealType}")
    public MealSlotResponse updateSlot(@PathVariable MealType mealType, @Valid @RequestBody MealSlotRequest req) {
        return slots.update(mealType, req);
    }

    // ---- dish library
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @GetMapping("/menu-items")
    public List<MenuItemResponse> dishes(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return dishes.list(activeOnly);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/menu-items")
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemResponse createDish(@Valid @RequestBody MenuItemRequest req) {
        return dishes.create(req);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/menu-items/{id}")
    public MenuItemResponse updateDish(@PathVariable Long id, @Valid @RequestBody MenuItemRequest req) {
        return dishes.update(id, req);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/menu-items/{id}")
    public ResponseEntity<Void> deleteDish(@PathVariable Long id) {
        dishes.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    // ---- daily menus
    @GetMapping("/menus")
    public List<DailyMenuResponse> range(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @AuthenticationPrincipal Jwt jwt) {
        return menus.range(from, to, CurrentUser.role(jwt) != Role.STUDENT);
    }

    @GetMapping("/menus/today")
    public List<DailyMenuResponse> today(@AuthenticationPrincipal Jwt jwt) {
        LocalDate today = menus.today();
        return menus.range(today, today, CurrentUser.role(jwt) != Role.STUDENT);
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PutMapping("/menus/{date}/{mealType}")
    public DailyMenuResponse setMenu(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                     @PathVariable MealType mealType, @Valid @RequestBody SetMenuRequest req,
                                     @AuthenticationPrincipal Jwt jwt) {
        return menus.setItems(date, mealType, req, CurrentUser.id(jwt));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PostMapping("/menus/copy-week")
    public CopyWeekResponse copyWeek(@Valid @RequestBody CopyWeekRequest req, @AuthenticationPrincipal Jwt jwt) {
        return menus.copyWeek(req, CurrentUser.id(jwt));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PatchMapping("/menus/{id}/publish")
    public DailyMenuResponse publish(@PathVariable Long id, @Valid @RequestBody PublishRequest req) {
        return menus.publish(id, req.published());
    }
}
