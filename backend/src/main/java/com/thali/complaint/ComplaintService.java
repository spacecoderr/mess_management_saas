package com.thali.complaint;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thali.common.PageResponse;
import com.thali.common.exception.ApiException;
import com.thali.complaint.dto.ComplaintResponse;
import com.thali.complaint.dto.ComplaintUpdateRequest;
import com.thali.complaint.dto.CreateComplaintRequest;
import com.thali.complaint.dto.UpdateEntryResponse;
import com.thali.user.Role;
import com.thali.user.StudentProfile;
import com.thali.user.StudentProfileRepository;
import com.thali.user.User;
import com.thali.user.UserRepository;

@Service
public class ComplaintService {

    private final ComplaintRepository complaints;
    private final ComplaintUpdateRepository updates;
    private final UserRepository users;
    private final StudentProfileRepository profiles;

    public ComplaintService(ComplaintRepository complaints, ComplaintUpdateRepository updates, UserRepository users,
                            StudentProfileRepository profiles) {
        this.complaints = complaints;
        this.updates = updates;
        this.users = users;
        this.profiles = profiles;
    }

    @Transactional
    public ComplaintResponse create(Long studentId, CreateComplaintRequest req) {
        Complaint c = new Complaint();
        c.setStudent(users.getReferenceById(studentId));
        c.setCategory(req.category());
        c.setTitle(req.title().trim());
        c.setDescription(req.description().trim());
        if (req.priority() != null) c.setPriority(req.priority());
        complaints.save(c);
        return get(c.getId(), studentId, Role.STUDENT);
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> mine(Long studentId) {
        List<Complaint> list = complaints.findMine(studentId);
        Map<Long, String> rolls = rollNos(list);
        return list.stream().map(c -> ComplaintResponse.of(c, rolls.get(c.getStudent().getId()), null)).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ComplaintResponse> search(ComplaintStatus status, ComplaintCategory category, Long assignee, int page, int size) {
        var statuses = status == null ? EnumSet.allOf(ComplaintStatus.class) : EnumSet.of(status);
        var categories = category == null ? EnumSet.allOf(ComplaintCategory.class) : EnumSet.of(category);
        var result = complaints.search(statuses, categories, assignee == null ? 0L : assignee,
                PageRequest.of(page, Math.max(1, Math.min(size, 100)), Sort.by("createdAt").descending()));
        Map<Long, String> rolls = rollNos(result.getContent());
        return PageResponse.of(result, c -> ComplaintResponse.of(c, rolls.get(c.getStudent().getId()), null));
    }

    @Transactional(readOnly = true)
    public ComplaintResponse get(Long id, Long userId, Role role) {
        Complaint c = visible(id, userId, role);
        String roll = profiles.findById(c.getStudent().getId()).map(StudentProfile::getRollNo).orElse(null);
        return ComplaintResponse.of(c, roll, timeline(id));
    }

    @Transactional
    public ComplaintResponse assign(Long id, Long staffId, Long actorId) {
        Complaint c = complaints.findDetailed(id).orElseThrow(this::notFound);
        User staff = users.findById(staffId).orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "Staff member not found."));
        if (staff.getRole() == Role.STUDENT || !staff.isActive()) {
            throw ApiException.unprocessable("INVALID_ASSIGNEE", "Complaints can only be assigned to active staff or admins.");
        }
        c.setAssignedTo(staff);
        ComplaintStatus next = c.getStatus() == ComplaintStatus.OPEN ? ComplaintStatus.IN_PROGRESS : null;
        if (next != null) c.setStatus(next);
        addEntry(c, actorId, "Assigned to " + staff.getName(), next);
        return get(id, actorId, Role.ADMIN);
    }

    @Transactional
    public ComplaintResponse addUpdate(Long id, Long userId, Role role, ComplaintUpdateRequest req) {
        Complaint c = visible(id, userId, role);
        if (role == Role.STUDENT) {
            if (req.newStatus() != null) throw ApiException.forbidden("STATUS_NOT_ALLOWED", "Only staff can change a complaint's status.");
            if (c.getStatus() == ComplaintStatus.RESOLVED || c.getStatus() == ComplaintStatus.REJECTED) {
                throw ApiException.unprocessable("COMPLAINT_CLOSED", "This complaint is closed.");
            }
        } else if (req.newStatus() != null) {
            c.setStatus(req.newStatus());
        }
        addEntry(c, userId, req.message().trim(), role == Role.STUDENT ? null : req.newStatus());
        return get(id, userId, role);
    }

    // ------------------------------------------------------------------ internals

    /** Students only see their own complaints (404 otherwise); staff and admins see all. */
    private Complaint visible(Long id, Long userId, Role role) {
        Complaint c = complaints.findDetailed(id).orElseThrow(this::notFound);
        if (role == Role.STUDENT && !c.getStudent().getId().equals(userId)) throw notFound();
        return c;
    }

    private void addEntry(Complaint c, Long authorId, String message, ComplaintStatus newStatus) {
        ComplaintUpdate u = new ComplaintUpdate();
        u.setComplaint(c);
        u.setAuthor(users.getReferenceById(authorId));
        u.setMessage(message);
        u.setNewStatus(newStatus);
        updates.save(u);
    }

    private List<UpdateEntryResponse> timeline(Long id) {
        return updates.findByComplaintIdOrderByCreatedAtAsc(id).stream().map(UpdateEntryResponse::of).toList();
    }

    private Map<Long, String> rollNos(List<Complaint> list) {
        return profiles.findAllById(list.stream().map(c -> c.getStudent().getId()).distinct().toList()).stream()
                .collect(Collectors.toMap(StudentProfile::getUserId, StudentProfile::getRollNo, (a, b) -> a, java.util.HashMap::new));
    }

    private ApiException notFound() {
        return ApiException.notFound("COMPLAINT_NOT_FOUND", "Complaint not found.");
    }

}
