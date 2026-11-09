package com.thali.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.http.HttpMethod;

import com.thali.user.Role;
import com.thali.user.User;

/**
 * Every endpoint is called anonymously and as each role. Anonymous must get 401; a role that is not allowed must get 403; an
 * allowed role must get anything else (200/201/404/422... are all fine here, we only test the gate, not the feature).
 */
class SecurityMatrixIT extends AbstractIntegrationTest {

    record Endpoint(HttpMethod method, String url, String body, Set<Role> allowed) {
        @Override public String toString() { return method + " " + url; }
    }

    static final Set<Role> ADMIN = EnumSet.of(Role.ADMIN);
    static final Set<Role> STUDENT = EnumSet.of(Role.STUDENT);
    static final Set<Role> STAFF_ADMIN = EnumSet.of(Role.STAFF, Role.ADMIN);
    static final Set<Role> ALL = EnumSet.allOf(Role.class);

    static Endpoint get(String url, Set<Role> allowed) { return new Endpoint(HttpMethod.GET, url, null, allowed); }
    static Endpoint post(String url, String body, Set<Role> allowed) { return new Endpoint(HttpMethod.POST, url, body, allowed); }

    static final Stream<Endpoint> ENDPOINTS = Stream.of(
            // users & students
            get("/api/v1/users", ADMIN), get("/api/v1/students", ADMIN), get("/api/v1/students/pending", ADMIN),
            new Endpoint(HttpMethod.PATCH, "/api/v1/users/999/status", "{\"active\":true}", ADMIN),
            new Endpoint(HttpMethod.PATCH, "/api/v1/students/999/approval", "{\"status\":\"APPROVED\"}", ADMIN),
            get("/api/v1/me/profile", ALL),
            // meals, dishes, menus
            get("/api/v1/meal-slots", ALL),
            new Endpoint(HttpMethod.PUT, "/api/v1/meal-slots/LUNCH", "{\"startTime\":\"12:30\",\"endTime\":\"14:30\",\"cutoffMinutes\":240,\"price\":60,\"active\":true}", ADMIN),
            get("/api/v1/menu-items", STAFF_ADMIN), post("/api/v1/menu-items", "{\"name\":\"Matrix Dish\",\"category\":\"MAIN\",\"veg\":true}", ADMIN),
            get("/api/v1/menus?from=2026-11-09&to=2026-11-10", ALL), get("/api/v1/menus/today", ALL),
            new Endpoint(HttpMethod.PUT, "/api/v1/menus/2026-11-10/LUNCH", "{\"itemIds\":[]}", STAFF_ADMIN),
            post("/api/v1/menus/copy-week", "{\"fromWeekStart\":\"2026-11-09\",\"toWeekStart\":\"2026-11-16\"}", STAFF_ADMIN),
            new Endpoint(HttpMethod.PATCH, "/api/v1/menus/999/publish", "{\"published\":true}", STAFF_ADMIN),
            // bookings & attendance
            get("/api/v1/bookings/me", STUDENT), post("/api/v1/bookings", "{\"date\":\"2026-11-10\",\"mealType\":\"LUNCH\"}", STUDENT),
            post("/api/v1/bookings/bulk", "{\"items\":[{\"date\":\"2026-11-10\",\"mealType\":\"LUNCH\"}]}", STUDENT),
            new Endpoint(HttpMethod.DELETE, "/api/v1/bookings/999", null, STUDENT), get("/api/v1/bookings/999/qr", STUDENT),
            get("/api/v1/bookings?date=2026-11-09", STAFF_ADMIN),
            post("/api/v1/attendance/scan", "{\"qrToken\":\"11111111-1111-1111-1111-111111111111\"}", STAFF_ADMIN),
            post("/api/v1/attendance/manual", "{\"bookingId\":999}", STAFF_ADMIN), get("/api/v1/attendance/live", STAFF_ADMIN),
            // feedback & complaints
            post("/api/v1/feedback", "{\"dailyMenuId\":1,\"rating\":5}", STUDENT), get("/api/v1/feedback/me", STUDENT),
            get("/api/v1/feedback/me/pending", STUDENT), get("/api/v1/feedback", STAFF_ADMIN), get("/api/v1/feedback/summary", STAFF_ADMIN),
            post("/api/v1/complaints", "{\"category\":\"OTHER\",\"title\":\"t\",\"description\":\"d\"}", STUDENT),
            get("/api/v1/complaints/me", STUDENT), get("/api/v1/complaints", STAFF_ADMIN), get("/api/v1/complaints/999", ALL),
            new Endpoint(HttpMethod.PATCH, "/api/v1/complaints/999/assign", "{\"staffId\":1}", ADMIN),
            post("/api/v1/complaints/999/updates", "{\"message\":\"m\"}", ALL),
            // billing & payments
            get("/api/v1/bills", ADMIN), post("/api/v1/bills/generate?month=2026-10", null, ADMIN), get("/api/v1/bills/me", STUDENT), get("/api/v1/bills/999", ALL),
            post("/api/v1/payments/checkout", "{\"billId\":999,\"method\":\"UPI\"}", STUDENT), post("/api/v1/payments/999/confirm", null, STUDENT),
            get("/api/v1/payments/me", STUDENT), post("/api/v1/payments/cash", "{\"billId\":999,\"amount\":10}", ADMIN), get("/api/v1/payments", ADMIN),
            // analytics & reports
            get("/api/v1/analytics/overview", ADMIN), get("/api/v1/analytics/revenue", ADMIN),
            get("/api/v1/analytics/bookings-trend", STAFF_ADMIN), get("/api/v1/analytics/meal-distribution", STAFF_ADMIN),
            get("/api/v1/analytics/ratings-trend", STAFF_ADMIN), get("/api/v1/analytics/top-dishes", STAFF_ADMIN), get("/api/v1/analytics/wastage", STAFF_ADMIN),
            get("/api/v1/reports/bills.csv?month=2026-10", ADMIN), get("/api/v1/reports/attendance.csv?from=2026-11-01&to=2026-11-09", STAFF_ADMIN),
            // announcements
            get("/api/v1/announcements", ALL), post("/api/v1/announcements", "{\"title\":\"t\",\"body\":\"b\"}", ADMIN),
            new Endpoint(HttpMethod.DELETE, "/api/v1/announcements/999", null, ADMIN));

    @TestFactory
    Stream<DynamicTest> everyEndpointEnforcesItsRoles() {
        // @BeforeEach runs once per factory, not once per dynamic test, so the three users are created exactly once here.
        User admin = admin(), staff = staff(), student = student("B423001");
        return ENDPOINTS.map(e -> DynamicTest.dynamicTest(e.toString(), () -> check(e, admin, staff, student)));
    }

    private void check(Endpoint e, User admin, User staff, User student) throws Exception {
        assertThat(status(e, null)).as("anonymous").isEqualTo(401);
        for (Role role : Role.values()) {
            User as = switch (role) { case ADMIN -> admin; case STAFF -> staff; case STUDENT -> student; };
            int code = status(e, as);
            if (e.allowed().contains(role)) assertThat(code).as("%s should be allowed", role).isNotIn(401, 403);
            else assertThat(code).as("%s should be forbidden", role).isEqualTo(403);
        }
    }

    private int status(Endpoint e, User as) throws Exception {
        return call(e.method(), e.url(), as, e.body()).andReturn().getResponse().getStatus();
    }
}
