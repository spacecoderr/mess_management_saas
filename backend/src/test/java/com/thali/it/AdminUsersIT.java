package com.thali.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.thali.user.ApprovalStatus;
import com.thali.user.Role;
import com.thali.user.User;

class AdminUsersIT extends AbstractIntegrationTest {

    User admin, asha;

    @BeforeEach
    void people() {
        admin = admin();
        asha = student("B423001", ApprovalStatus.PENDING);
    }

    private ResultActionsHolder login(String email, String pw) throws Exception {
        return new ResultActionsHolder(post("/api/v1/auth/login", null, "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, pw)).andReturn().getResponse().getStatus());
    }

    record ResultActionsHolder(int status) {}

    @Test
    void approvalDecisionsChangeWhoCanLogIn() throws Exception {
        assertThat(login(asha.getEmail(), PASSWORD).status()).isEqualTo(403);
        patch("/api/v1/students/%d/approval".formatted(asha.getId()), admin, "{\"status\":\"PENDING\"}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_STATUS"));
        patch("/api/v1/students/%d/approval".formatted(asha.getId()), admin, "{\"status\":\"APPROVED\"}").andExpect(status().isOk()).andExpect(jsonPath("$.approvalStatus").value("APPROVED"));
        assertThat(login(asha.getEmail(), PASSWORD).status()).isEqualTo(200);
        patch("/api/v1/students/%d/approval".formatted(asha.getId()), admin, "{\"status\":\"REJECTED\"}").andExpect(status().isOk());
        assertThat(login(asha.getEmail(), PASSWORD).status()).isEqualTo(403);
        patch("/api/v1/students/99999/approval", admin, "{\"status\":\"APPROVED\"}").andExpect(status().isNotFound());
    }

    @Test
    void studentListSupportsStatusAndSearchFilters() throws Exception {
        student("B423002");
        get("/api/v1/students?status=PENDING", admin).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].rollNo").value("B423001"));
        get("/api/v1/students?q=b423002", admin).andExpect(jsonPath("$.totalElements").value(1));
        get("/api/v1/students", admin).andExpect(jsonPath("$.totalElements").value(2));
        get("/api/v1/students/pending", admin).andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void disablingAUserBlocksLoginAndRevokesTheirSessions() throws Exception {
        User ben = student("B423002");
        String refresh = read(post("/api/v1/auth/login", null, "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(ben.getEmail(), PASSWORD)).andExpect(status().isOk()), "$.refreshToken");
        patch("/api/v1/users/%d/status".formatted(ben.getId()), admin, "{\"active\":false}").andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        assertThat(login(ben.getEmail(), PASSWORD).status()).isEqualTo(403);
        post("/api/v1/auth/refresh", null, "{\"refreshToken\":\"%s\"}".formatted(refresh)).andExpect(status().isUnauthorized());
        patch("/api/v1/users/%d/status".formatted(ben.getId()), admin, "{\"active\":true}").andExpect(status().isOk());
        assertThat(login(ben.getEmail(), PASSWORD).status()).isEqualTo(200);
    }

    @Test
    void anAdminCannotDisableThemselves() throws Exception {
        patch("/api/v1/users/%d/status".formatted(admin.getId()), admin, "{\"active\":false}").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("CANNOT_DISABLE_SELF"));
    }

    @Test
    void userSearchFiltersByRoleAndText() throws Exception {
        staff();
        get("/api/v1/users?role=STAFF", admin).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].role").value("STAFF"));
        get("/api/v1/users?q=student", admin).andExpect(jsonPath("$.totalElements").value(1));
        get("/api/v1/users?size=1", admin).andExpect(jsonPath("$.content", hasSize(1))).andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void duplicateStaffEmailsAreRejected() throws Exception {
        String body = "{\"name\":\"Ramesh\",\"email\":\"ramesh@thali.local\",\"role\":\"STAFF\",\"password\":\"Staff12345\"}";
        post("/api/v1/users", admin, body).andExpect(status().isCreated());
        post("/api/v1/users", admin, body).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void profileAndPasswordSelfServiceWorks() throws Exception {
        patch("/api/v1/students/%d/approval".formatted(asha.getId()), admin, "{\"status\":\"APPROVED\"}");
        put("/api/v1/me/profile", asha, "{\"name\":\"Asha R\",\"phone\":\"9876543210\",\"hostelBlock\":\"B2\",\"roomNo\":\"305\",\"dietPref\":\"NON_VEG\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.hostelBlock").value("B2")).andExpect(jsonPath("$.dietPref").value("NON_VEG")).andExpect(jsonPath("$.rollNo").value("B423001"));

        put("/api/v1/me/password", asha, "{\"currentPassword\":\"wrong\",\"newPassword\":\"Another123\"}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("WRONG_PASSWORD"));
        put("/api/v1/me/password", asha, "{\"currentPassword\":\"%s\",\"newPassword\":\"Another123\"}".formatted(PASSWORD)).andExpect(status().isOk());
        assertThat(login(asha.getEmail(), PASSWORD).status()).isEqualTo(401);
        assertThat(login(asha.getEmail(), "Another123").status()).isEqualTo(200);
        assertThat(users.findById(asha.getId()).orElseThrow().getRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    void announcementsAreReadByEveryoneAndPinnedOnesComeFirst() throws Exception {
        post("/api/v1/announcements", admin, "{\"title\":\"Old\",\"body\":\"b\"}").andExpect(status().isCreated());
        long pinned = readId(post("/api/v1/announcements", admin, "{\"title\":\"Pinned\",\"body\":\"b\",\"pinned\":true}").andExpect(status().isCreated()));
        post("/api/v1/announcements", admin, "{\"title\":\"New\",\"body\":\"b\"}");
        get("/api/v1/announcements", asha).andExpect(jsonPath("$", hasSize(3))).andExpect(jsonPath("$[0].title").value("Pinned")).andExpect(jsonPath("$[1].title").value("New"));
        post("/api/v1/announcements", asha, "{\"title\":\"x\",\"body\":\"y\"}").andExpect(status().isForbidden());
        delete("/api/v1/announcements/" + pinned, admin).andExpect(status().isNoContent());
        delete("/api/v1/announcements/" + pinned, admin).andExpect(status().isNotFound());
    }
}
