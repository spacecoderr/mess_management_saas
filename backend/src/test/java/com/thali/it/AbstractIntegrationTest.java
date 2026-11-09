package com.thali.it;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.jayway.jsonpath.JsonPath;
import com.thali.billing.BillRepository;
import com.thali.booking.Booking;
import com.thali.booking.BookingRepository;
import com.thali.booking.BookingStatus;
import com.thali.menu.DailyMenu;
import com.thali.menu.DailyMenuRepository;
import com.thali.menu.MealCategory;
import com.thali.menu.MealType;
import com.thali.menu.MenuItem;
import com.thali.menu.MenuItemRepository;
import com.thali.security.JwtService;
import com.thali.support.MutableClock;
import com.thali.user.ApprovalStatus;
import com.thali.user.Role;
import com.thali.user.StudentProfile;
import com.thali.user.StudentProfileRepository;
import com.thali.user.User;
import com.thali.user.UserRepository;

/**
 * Boots the full app against a throw-away PostgreSQL 16 (Testcontainers). One container and one Spring context are shared by
 * every IT class; each test starts from empty tables and a clock fixed at Mon 9 Nov 2026 10:30 IST.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestConfig.class)
public abstract class AbstractIntegrationTest {

    protected static final String PASSWORD = "Passw0rd1";
    protected static final LocalDate TODAY = LocalDate.of(2026, 11, 9), TOMORROW = TODAY.plusDays(1);

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();   // stopped automatically by Testcontainers' Ryuk when the JVM exits
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected MutableClock clock;
    @Autowired protected CapturingOtpSender otps;
    @Autowired protected JwtService jwt;
    @Autowired protected PasswordEncoder encoder;
    @Autowired protected UserRepository users;
    @Autowired protected StudentProfileRepository profiles;
    @Autowired protected MenuItemRepository dishes;
    @Autowired protected DailyMenuRepository menus;
    @Autowired protected BookingRepository bookings;
    @Autowired protected BillRepository bills;

    @BeforeEach
    void resetState() {
        jdbc.execute("TRUNCATE users, student_profiles, otp_codes, refresh_tokens, menu_items, daily_menus, daily_menu_items, "
                + "bookings, attendance, feedback, complaints, complaint_updates, bills, payments, announcements RESTART IDENTITY CASCADE");
        jdbc.update("UPDATE meal_slot_config SET start_time='07:30', end_time='09:00', cutoff_minutes=240, price=35, active=true WHERE meal_type='BREAKFAST'");
        jdbc.update("UPDATE meal_slot_config SET start_time='12:30', end_time='14:30', cutoff_minutes=240, price=60, active=true WHERE meal_type='LUNCH'");
        jdbc.update("UPDATE meal_slot_config SET start_time='17:00', end_time='18:00', cutoff_minutes=120, price=20, active=true WHERE meal_type='SNACKS'");
        jdbc.update("UPDATE meal_slot_config SET start_time='20:00', end_time='21:30', cutoff_minutes=240, price=60, active=true WHERE meal_type='DINNER'");
        clock.setLocal("2026-11-09T10:30:00");
        otps.clear();
    }

    // ----------------------------------------------------------------------------------------- http helpers

    protected ResultActions call(HttpMethod method, String url, User as, String json) throws Exception {
        MockHttpServletRequestBuilder b = MockMvcRequestBuilders.request(method, url).contentType(APPLICATION_JSON);
        if (as != null) b.header(AUTHORIZATION, "Bearer " + jwt.issueAccessToken(as));
        if (json != null) b.content(json);
        return mvc.perform(b);
    }

    protected ResultActions get(String url, User as) throws Exception { return call(HttpMethod.GET, url, as, null); }
    protected ResultActions post(String url, User as, String json) throws Exception { return call(HttpMethod.POST, url, as, json); }
    protected ResultActions put(String url, User as, String json) throws Exception { return call(HttpMethod.PUT, url, as, json); }
    protected ResultActions patch(String url, User as, String json) throws Exception { return call(HttpMethod.PATCH, url, as, json); }
    protected ResultActions delete(String url, User as) throws Exception { return call(HttpMethod.DELETE, url, as, null); }

    /** Reads a JSON path from a response, e.g. read(post(...), "$.id"). */
    @SuppressWarnings("unchecked")
    protected <T> T read(ResultActions result, String jsonPath) throws Exception {
        return (T) JsonPath.read(result.andReturn().getResponse().getContentAsString(), jsonPath);
    }

    protected long readId(ResultActions result) throws Exception {
        return ((Number) read(result, "$.id")).longValue();
    }

    // ----------------------------------------------------------------------------------------- data helpers

    protected User user(String email, String name, Role role) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setRole(role);
        u.setPasswordHash(encoder.encode(PASSWORD));
        u.setEmailVerified(true);
        return users.save(u);
    }

    protected User admin() { return users.findByEmailIgnoreCase("admin@test.local").orElseGet(() -> user("admin@test.local", "Test Admin", Role.ADMIN)); }
    protected User staff() { return users.findByEmailIgnoreCase("staff@test.local").orElseGet(() -> user("staff@test.local", "Test Staff", Role.STAFF)); }

    /** Approved student, roll like "B423001", email b423001@iiit-bh.ac.in. */
    protected User student(String roll) { return student(roll, ApprovalStatus.APPROVED); }

    protected User student(String roll, ApprovalStatus status) {
        User u = user(roll.toLowerCase() + "@iiit-bh.ac.in", "Student " + roll, Role.STUDENT);
        StudentProfile p = new StudentProfile();
        p.setUser(u);
        p.setRollNo(roll);
        p.setBatchYear((short) 2023);
        p.setHostelBlock("A");
        p.setRoomNo("101");
        p.setApprovalStatus(status);
        profiles.save(p);
        return u;
    }

    protected MenuItem dish(String name) {
        return dishes.findAll().stream().filter(d -> d.getName().equals(name)).findFirst().orElseGet(() -> {
            MenuItem m = new MenuItem();
            m.setName(name);
            m.setCategory(MealCategory.MAIN);
            return dishes.save(m);
        });
    }

    protected DailyMenu menu(LocalDate date, MealType type, boolean published, String... dishNames) {
        DailyMenu m = new DailyMenu();
        m.setMenuDate(date);
        m.setMealType(type);
        m.setPublished(published);
        for (String n : dishNames) m.getItems().add(dish(n));
        return menus.save(m);
    }

    /** Inserts a booking directly (bypassing cutoff rules) so tests can set up history. */
    protected Booking booking(User student, DailyMenu menu, BookingStatus status) {
        Booking b = new Booking();
        b.setStudent(student);
        b.setDailyMenu(menu);
        b.setStatus(status);
        b.setPriceAtBooking(jdbc.queryForObject("SELECT price FROM meal_slot_config WHERE meal_type = ?", java.math.BigDecimal.class, menu.getMealType().name()));
        return bookings.save(b);
    }
}
