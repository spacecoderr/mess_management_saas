package com.thali.it;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.thali.user.Role;
import com.thali.user.User;

class ComplaintFlowIT extends AbstractIntegrationTest {

    User admin, staff, asha, ben;

    @BeforeEach
    void people() {
        admin = admin();
        staff = staff();
        asha = student("B423001");
        ben = student("B423002");
    }

    private long raise(User who, String title) throws Exception {
        return readId(post("/api/v1/complaints", who, "{\"category\":\"FOOD_QUALITY\",\"title\":\"%s\",\"description\":\"Details\",\"priority\":\"HIGH\"}".formatted(title))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("OPEN")));
    }

    @Test
    void lifecycle_raiseAssignResolveAndFollowTheTimeline() throws Exception {
        long id = raise(asha, "Cold rice");

        patch("/api/v1/complaints/%d/assign".formatted(id), admin, "{\"staffId\":%d}".formatted(staff.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS")).andExpect(jsonPath("$.assignedToName").value("Test Staff"));
        post("/api/v1/complaints/%d/updates".formatted(id), staff, "{\"message\":\"Talked to the cook\",\"newStatus\":\"RESOLVED\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("RESOLVED"));

        get("/api/v1/complaints/" + id, asha).andExpect(status().isOk()).andExpect(jsonPath("$.updates", hasSize(2)))
                .andExpect(jsonPath("$.updates[0].authorRole").value("ADMIN")).andExpect(jsonPath("$.updates[1].message").value("Talked to the cook"))
                .andExpect(jsonPath("$.updates[1].newStatus").value("RESOLVED"));
        get("/api/v1/complaints/me", asha).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].status").value("RESOLVED"));
    }

    @Test
    void studentsCannotReadOrCommentOnOthersComplaints() throws Exception {
        long id = raise(asha, "Cold rice");
        get("/api/v1/complaints/" + id, ben).andExpect(status().isNotFound());
        post("/api/v1/complaints/%d/updates".formatted(id), ben, "{\"message\":\"me too\"}").andExpect(status().isNotFound());
        get("/api/v1/complaints/me", ben).andExpect(jsonPath("$", hasSize(0)));
        get("/api/v1/complaints/" + id, staff).andExpect(status().isOk());
    }

    @Test
    void studentCanCommentWhileOpenButCannotChangeStatusOrReopenAClosedComplaint() throws Exception {
        long id = raise(asha, "Cold rice");
        post("/api/v1/complaints/%d/updates".formatted(id), asha, "{\"message\":\"Still cold today\"}").andExpect(status().isCreated()).andExpect(jsonPath("$.updates", hasSize(1)));
        post("/api/v1/complaints/%d/updates".formatted(id), asha, "{\"message\":\"close it\",\"newStatus\":\"RESOLVED\"}").andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("STATUS_NOT_ALLOWED"));

        post("/api/v1/complaints/%d/updates".formatted(id), staff, "{\"message\":\"Won't fix\",\"newStatus\":\"REJECTED\"}").andExpect(status().isCreated());
        post("/api/v1/complaints/%d/updates".formatted(id), asha, "{\"message\":\"please reconsider\"}").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("COMPLAINT_CLOSED"));
    }

    @Test
    void onlyAdminsAssignAndOnlyToStaffOrAdmins() throws Exception {
        long id = raise(asha, "Cold rice");
        patch("/api/v1/complaints/%d/assign".formatted(id), staff, "{\"staffId\":%d}".formatted(staff.getId())).andExpect(status().isForbidden());
        patch("/api/v1/complaints/%d/assign".formatted(id), admin, "{\"staffId\":%d}".formatted(ben.getId())).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INVALID_ASSIGNEE"));
        patch("/api/v1/complaints/%d/assign".formatted(id), admin, "{\"staffId\":99999}").andExpect(status().isNotFound());
        User off = user("off@test.local", "Off Duty", Role.STAFF);
        off.setActive(false);
        users.save(off);
        patch("/api/v1/complaints/%d/assign".formatted(id), admin, "{\"staffId\":%d}".formatted(off.getId())).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void staffQueueFiltersByStatusCategoryAndAssignee() throws Exception {
        long one = raise(asha, "One");
        raise(ben, "Two");
        patch("/api/v1/complaints/%d/assign".formatted(one), admin, "{\"staffId\":%d}".formatted(staff.getId()));

        get("/api/v1/complaints", staff).andExpect(jsonPath("$.totalElements").value(2));
        get("/api/v1/complaints?status=OPEN", staff).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].title").value("Two"));
        get("/api/v1/complaints?status=IN_PROGRESS", staff).andExpect(jsonPath("$.content[0].rollNo").value("B423001"));
        get("/api/v1/complaints?assignedToMe=true", staff).andExpect(jsonPath("$.totalElements").value(1));
        get("/api/v1/complaints?category=HYGIENE", staff).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void invalidComplaintsAreRejectedWithFieldErrors() throws Exception {
        post("/api/v1/complaints", asha, "{\"category\":\"FOOD_QUALITY\",\"title\":\"\",\"description\":\"\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED")).andExpect(jsonPath("$.errors", hasSize(2)));
        post("/api/v1/complaints", asha, "{\"category\":\"NOPE\",\"title\":\"x\",\"description\":\"y\"}").andExpect(status().isBadRequest());
        get("/api/v1/complaints/99999", admin).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("COMPLAINT_NOT_FOUND"));
    }
}
