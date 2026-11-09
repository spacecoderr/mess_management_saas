package com.thali.announcement;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.thali.announcement.dto.AnnouncementRequest;
import com.thali.announcement.dto.AnnouncementResponse;
import com.thali.common.exception.ApiException;
import com.thali.security.CurrentUser;
import com.thali.user.UserRepository;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Announcements", description = "Notices from the mess office; everyone reads, admins write")
@RestController
@RequestMapping("/api/v1/announcements")
public class AnnouncementController {

    private final AnnouncementRepository repo;
    private final UserRepository users;

    public AnnouncementController(AnnouncementRepository repo, UserRepository users) {
        this.repo = repo;
        this.users = users;
    }

    @Transactional(readOnly = true)
    @GetMapping
    public List<AnnouncementResponse> list() {
        return repo.findTop50ByOrderByPinnedDescCreatedAtDesc().stream().map(AnnouncementResponse::of).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnnouncementResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AnnouncementRequest req) {
        Announcement a = new Announcement();
        a.setTitle(req.title().trim());
        a.setBody(req.body().trim());
        a.setPinned(Boolean.TRUE.equals(req.pinned()));
        a.setAuthor(users.getReferenceById(CurrentUser.id(jwt)));
        repo.save(a);
        return AnnouncementResponse.of(a);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) throw ApiException.notFound("ANNOUNCEMENT_NOT_FOUND", "Announcement not found.");
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
