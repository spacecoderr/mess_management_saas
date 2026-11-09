package com.thali.menu;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thali.common.exception.ApiException;
import com.thali.menu.dto.MenuItemRequest;
import com.thali.menu.dto.MenuItemResponse;

@Service
public class MenuItemService {

    private final MenuItemRepository repo;

    public MenuItemService(MenuItemRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> list(boolean activeOnly) {
        return repo.findAllByOrderByNameAsc().stream().filter(i -> !activeOnly || i.isActive()).map(MenuItemResponse::of).toList();
    }

    @Transactional
    public MenuItemResponse create(MenuItemRequest req) {
        if (repo.existsByNameIgnoreCase(req.name().trim())) {
            throw ApiException.conflict("DISH_EXISTS", "A dish with this name already exists.");
        }
        MenuItem m = new MenuItem();
        apply(m, req);
        return MenuItemResponse.of(repo.save(m));
    }

    @Transactional
    public MenuItemResponse update(Long id, MenuItemRequest req) {
        MenuItem m = get(id);
        if (repo.existsByNameIgnoreCaseAndIdNot(req.name().trim(), id)) {
            throw ApiException.conflict("DISH_EXISTS", "A dish with this name already exists.");
        }
        apply(m, req);
        return MenuItemResponse.of(m);
    }

    /** Soft delete: dishes already used in past menus must keep existing for history and ratings. */
    @Transactional
    public void deactivate(Long id) {
        get(id).setActive(false);
    }

    private MenuItem get(Long id) {
        return repo.findById(id).orElseThrow(() -> ApiException.notFound("DISH_NOT_FOUND", "Dish not found."));
    }

    private void apply(MenuItem m, MenuItemRequest r) {
        m.setName(r.name().trim());
        m.setDescription(r.description());
        m.setCategory(r.category());
        m.setVeg(r.veg());
        m.setImageUrl(r.imageUrl());
        m.setActive(r.active() == null || r.active());
    }
}
