package com.thali.seed;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.thali.announcement.Announcement;
import com.thali.announcement.AnnouncementRepository;
import com.thali.attendance.Attendance;
import com.thali.attendance.AttendanceMethod;
import com.thali.attendance.AttendanceRepository;
import com.thali.billing.Bill;
import com.thali.billing.BillRepository;
import com.thali.billing.BillStatus;
import com.thali.billing.BillingService;
import com.thali.billing.Payment;
import com.thali.billing.PaymentMethod;
import com.thali.billing.PaymentRepository;
import com.thali.billing.PaymentStatus;
import com.thali.booking.Booking;
import com.thali.booking.BookingRepository;
import com.thali.booking.BookingStatus;
import com.thali.complaint.Complaint;
import com.thali.complaint.ComplaintCategory;
import com.thali.complaint.ComplaintPriority;
import com.thali.complaint.ComplaintRepository;
import com.thali.complaint.ComplaintStatus;
import com.thali.complaint.ComplaintUpdate;
import com.thali.complaint.ComplaintUpdateRepository;
import com.thali.config.AppProperties;
import com.thali.feedback.Feedback;
import com.thali.feedback.FeedbackRepository;
import com.thali.menu.DailyMenu;
import com.thali.menu.DailyMenuRepository;
import com.thali.menu.MealCategory;
import com.thali.menu.MealSlotConfig;
import com.thali.menu.MealSlotService;
import com.thali.menu.MealType;
import com.thali.menu.MenuItem;
import com.thali.menu.MenuItemRepository;
import com.thali.user.ApprovalStatus;
import com.thali.user.DietPref;
import com.thali.user.Role;
import com.thali.user.StudentProfile;
import com.thali.user.StudentProfileRepository;
import com.thali.user.User;
import com.thali.user.UserRepository;

/**
 * Loads ~6 weeks of believable demo data once (dev/demo profiles, app.seed.enabled=true) so every dashboard has numbers.
 * Demo students use roll numbers Z9000xx so they can never collide with real IIIT-BH students.
 * Safe to re-run: it does nothing when the marker student already exists, and reuses any dishes/menus already present.
 */
@Order(2)
@Component
@Profile({ "dev", "demo" })
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String MARKER_EMAIL = "z900001@iiit-bh.ac.in";
    private static final int DAYS_BACK = 45;
    private static final int DAYS_AHEAD = 7;
    private static final int STUDENT_COUNT = 25;

    private final UserRepository users;
    private final StudentProfileRepository profiles;
    private final MenuItemRepository dishes;
    private final DailyMenuRepository menus;
    private final BookingRepository bookings;
    private final AttendanceRepository attendance;
    private final FeedbackRepository feedback;
    private final ComplaintRepository complaints;
    private final ComplaintUpdateRepository complaintUpdates;
    private final AnnouncementRepository announcements;
    private final BillRepository bills;
    private final PaymentRepository payments;
    private final BillingService billing;
    private final MealSlotService slots;
    private final PasswordEncoder encoder;
    private final PlatformTransactionManager tx;
    private final AppProperties props;
    private final Clock clock;

    public DataSeeder(UserRepository users, StudentProfileRepository profiles, MenuItemRepository dishes, DailyMenuRepository menus,
                      BookingRepository bookings, AttendanceRepository attendance, FeedbackRepository feedback,
                      ComplaintRepository complaints, ComplaintUpdateRepository complaintUpdates, AnnouncementRepository announcements,
                      BillRepository bills, PaymentRepository payments, BillingService billing, MealSlotService slots,
                      PasswordEncoder encoder, PlatformTransactionManager tx, AppProperties props, Clock clock) {
        this.users = users;
        this.profiles = profiles;
        this.dishes = dishes;
        this.menus = menus;
        this.bookings = bookings;
        this.attendance = attendance;
        this.feedback = feedback;
        this.complaints = complaints;
        this.complaintUpdates = complaintUpdates;
        this.announcements = announcements;
        this.bills = bills;
        this.payments = payments;
        this.billing = billing;
        this.slots = slots;
        this.encoder = encoder;
        this.tx = tx;
        this.props = props;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.existsByEmailIgnoreCase(MARKER_EMAIL)) {
            log.info("Demo data already present - skipping seed");
            return;
        }
        long t0 = System.currentTimeMillis();
        new TransactionTemplate(tx).executeWithoutResult(s -> seed());
        log.info("Demo data seeded in {} ms. Demo logins: staff1@thali.local / z900001@iiit-bh.ac.in (password from app.seed.demo-password)",
                System.currentTimeMillis() - t0);
    }

    // =====================================================================================================

    private void seed() {
        Random rnd = new Random(42);
        String hash = encoder.encode(props.seed().demoPassword());
        LocalDate today = LocalDate.now(clock);
        Instant now = clock.instant();

        User admin = users.findAll().stream().filter(u -> u.getRole() == Role.ADMIN).findFirst().orElseGet(() -> newUser("Mess Admin", "admin@thali.local", Role.ADMIN, hash));
        List<User> staff = List.of(findOrCreateStaff("Ramesh Behera", "staff1@thali.local", hash), findOrCreateStaff("Sunita Mohanty", "staff2@thali.local", hash));
        List<User> students = createStudents(hash, rnd);
        List<User> approved = students.stream().filter(u -> profiles.findById(u.getId()).map(p -> p.getApprovalStatus() == ApprovalStatus.APPROVED).orElse(false)).toList();

        Map<String, MenuItem> catalogue = createDishes();
        Map<MealType, MealSlotConfig> cfg = slots.all();

        List<Booking> newBookings = new ArrayList<>();
        List<DailyMenu> allMenus = new ArrayList<>();
        for (LocalDate d = today.minusDays(DAYS_BACK); !d.isAfter(today.plusDays(DAYS_AHEAD)); d = d.plusDays(1)) {
            for (MealType type : MealType.values()) {
                DailyMenu m = menus.findByDateAndType(d, type).orElse(null);
                if (m == null) {
                    m = new DailyMenu();
                    m.setMenuDate(d);
                    m.setMealType(type);
                    m.setCreatedBy(staff.get(0));
                    pickItems(d, type, catalogue).forEach(m.getItems()::add);
                    m.setPublished(!d.isAfter(today.plusDays(DAYS_AHEAD - 1)));   // the furthest day stays a draft
                    m = menus.save(m);
                }
                allMenus.add(m);
            }
        }

        Map<Long, Booking> consumedByMenuStudent = new HashMap<>();
        double[] pBook = { 0.55, 0.85, 0.50, 0.80 };   // BREAKFAST, LUNCH, SNACKS, DINNER
        for (DailyMenu m : allMenus) {
            if (!m.isPublished() || m.getMenuDate().isAfter(today.plusDays(1))) continue;   // bookings only up to tomorrow
            MealSlotConfig slot = cfg.get(m.getMealType());
            boolean over = now.isAfter(slots.endsAt(slot, m.getMenuDate()).plusSeconds(props.attendance().lateMinutes() * 60L));
            for (User s : approved) {
                if (rnd.nextDouble() > pBook[m.getMealType().ordinal()]) continue;
                if (bookings.findByStudentIdAndDailyMenuId(s.getId(), m.getId()).isPresent()) continue;
                Booking b = new Booking();
                b.setStudent(s);
                b.setDailyMenu(m);
                b.setPriceAtBooking(slot.getPrice());
                double r = rnd.nextDouble();
                if (over) {
                    b.setStatus(r < 0.89 ? BookingStatus.CONSUMED : r < 0.96 ? BookingStatus.NO_SHOW : BookingStatus.CANCELLED);
                } else {
                    b.setStatus(r < 0.96 ? BookingStatus.BOOKED : BookingStatus.CANCELLED);
                }
                if (b.getStatus() == BookingStatus.CANCELLED) b.setCancelledAt(slots.startsAt(slot, m.getMenuDate()).minus(Duration.ofHours(6)));
                newBookings.add(b);
            }
        }
        bookings.saveAll(newBookings);

        List<Attendance> marks = new ArrayList<>();
        List<Feedback> ratings = new ArrayList<>();
        for (Booking b : newBookings) {
            if (b.getStatus() != BookingStatus.CONSUMED) continue;
            DailyMenu m = b.getDailyMenu();
            MealSlotConfig slot = cfg.get(m.getMealType());
            Instant start = slots.startsAt(slot, m.getMenuDate());
            long windowMin = Duration.between(start, slots.endsAt(slot, m.getMenuDate())).toMinutes();
            Attendance a = new Attendance();
            a.setBooking(b);
            a.setMarkedBy(staff.get(rnd.nextInt(staff.size())));
            a.setMethod(rnd.nextDouble() < 0.88 ? AttendanceMethod.QR : AttendanceMethod.MANUAL);
            a.setMarkedAt(start.plus(Duration.ofMinutes(rnd.nextInt((int) windowMin))));
            marks.add(a);
            if (rnd.nextDouble() < 0.50) {
                Feedback f = new Feedback();
                f.setStudent(b.getStudent());
                f.setDailyMenu(m);
                f.setRating((short) ratingFor(m, rnd));
                if (rnd.nextDouble() < 0.25) f.setComment(COMMENTS[rnd.nextInt(COMMENTS.length)]);
                ratings.add(f);
            }
        }
        attendance.saveAll(marks);
        feedback.saveAll(ratings);

        seedComplaints(approved, staff, admin, rnd);
        seedAnnouncements(admin);
        seedBills(today, admin, rnd);

        log.info("Seed summary: {} students, {} dishes, {} menus, {} bookings, {} attendance, {} ratings",
                students.size(), catalogue.size(), allMenus.size(), newBookings.size(), marks.size(), ratings.size());
    }

    // ---------------------------------------------------------------------------------------- people

    private User newUser(String name, String email, Role role, String hash) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setRole(role);
        u.setPasswordHash(hash);
        u.setEmailVerified(true);
        u.setPhone("90000" + String.format("%05d", Math.abs(email.hashCode()) % 100000));
        return users.save(u);
    }

    private User findOrCreateStaff(String name, String email, String hash) {
        return users.findByEmailIgnoreCase(email).orElseGet(() -> newUser(name, email, Role.STAFF, hash));
    }

    private static final String[] FIRST = { "Aarav", "Ananya", "Aditya", "Ishita", "Rohan", "Sneha", "Kabir", "Priyanka", "Arjun", "Meera",
            "Siddharth", "Debasmita", "Soumya", "Tanmay", "Lipsa", "Biswajit", "Ritika", "Harsh", "Smruti", "Nikhil", "Pooja", "Abhinav", "Swati", "Manas", "Jyoti" };
    private static final String[] LAST = { "Mishra", "Das", "Patnaik", "Sahoo", "Nayak", "Behera", "Panda", "Rath", "Mohanty", "Swain", "Jena", "Pradhan" };
    private static final String[] BLOCKS = { "Boys Hostel A", "Boys Hostel B", "Girls Hostel A", "Girls Hostel B" };

    private List<User> createStudents(String hash, Random rnd) {
        List<User> out = new ArrayList<>();
        for (int i = 1; i <= STUDENT_COUNT; i++) {
            String roll = "Z9000" + String.format("%02d", i);
            User u = newUser(FIRST[i - 1] + " " + LAST[rnd.nextInt(LAST.length)], roll.toLowerCase() + "@iiit-bh.ac.in", Role.STUDENT, hash);
            StudentProfile p = new StudentProfile();
            p.setUser(u);
            p.setRollNo(roll);
            p.setBatchYear((short) (2022 + rnd.nextInt(4)));
            p.setHostelBlock(BLOCKS[rnd.nextInt(BLOCKS.length)]);
            p.setRoomNo(String.valueOf(100 + rnd.nextInt(300)));
            p.setDietPref(rnd.nextDouble() < 0.7 ? DietPref.VEG : DietPref.NON_VEG);
            p.setApprovalStatus(i >= STUDENT_COUNT - 1 ? ApprovalStatus.PENDING : ApprovalStatus.APPROVED);   // last two await approval
            profiles.save(p);
            out.add(u);
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------ menus

    private record Dish(String name, MealCategory cat, boolean veg, double quality) {}

    private static final Map<MealType, List<Dish>> POOL = new LinkedHashMap<>();
    static {
        POOL.put(MealType.BREAKFAST, List.of(
                new Dish("Idli", MealCategory.MAIN, true, 4.0), new Dish("Medu Vada", MealCategory.MAIN, true, 4.1),
                new Dish("Masala Dosa", MealCategory.MAIN, true, 4.5), new Dish("Poha", MealCategory.MAIN, true, 3.5),
                new Dish("Aloo Paratha", MealCategory.MAIN, true, 4.2), new Dish("Upma", MealCategory.MAIN, true, 3.1),
                new Dish("Bread Omelette", MealCategory.MAIN, false, 3.9), new Dish("Sambar", MealCategory.SIDE, true, 3.9),
                new Dish("Coconut Chutney", MealCategory.SIDE, true, 4.0), new Dish("Boiled Eggs", MealCategory.SIDE, false, 3.8),
                new Dish("Tea", MealCategory.BEVERAGE, true, 3.7), new Dish("Milk", MealCategory.BEVERAGE, true, 3.6)));
        POOL.put(MealType.LUNCH, List.of(
                new Dish("Steamed Rice", MealCategory.RICE, true, 3.9), new Dish("Jeera Rice", MealCategory.RICE, true, 4.1),
                new Dish("Dal Tadka", MealCategory.MAIN, true, 4.0), new Dish("Rajma Masala", MealCategory.MAIN, true, 4.3),
                new Dish("Paneer Butter Masala", MealCategory.MAIN, true, 4.6), new Dish("Chole", MealCategory.MAIN, true, 4.2),
                new Dish("Chicken Curry", MealCategory.MAIN, false, 4.4), new Dish("Egg Curry", MealCategory.MAIN, false, 3.8),
                new Dish("Mixed Veg Curry", MealCategory.SIDE, true, 3.0), new Dish("Aloo Gobi", MealCategory.SIDE, true, 3.3),
                new Dish("Chapati", MealCategory.BREAD, true, 3.8), new Dish("Curd", MealCategory.SIDE, true, 4.0),
                new Dish("Papad", MealCategory.SIDE, true, 3.9), new Dish("Green Salad", MealCategory.SIDE, true, 3.4),
                new Dish("Gulab Jamun", MealCategory.DESSERT, true, 4.7)));
        POOL.put(MealType.SNACKS, List.of(
                new Dish("Samosa", MealCategory.SIDE, true, 4.3), new Dish("Bread Pakora", MealCategory.SIDE, true, 3.9),
                new Dish("Veg Cutlet", MealCategory.SIDE, true, 3.6), new Dish("Pav Bhaji", MealCategory.MAIN, true, 4.4),
                new Dish("Masala Chai", MealCategory.BEVERAGE, true, 4.1), new Dish("Biscuits", MealCategory.SIDE, true, 3.5)));
        POOL.put(MealType.DINNER, List.of(
                new Dish("Chapati", MealCategory.BREAD, true, 3.8), new Dish("Steamed Rice", MealCategory.RICE, true, 3.9),
                new Dish("Paneer Tikka Masala", MealCategory.MAIN, true, 4.5), new Dish("Dal Fry", MealCategory.MAIN, true, 3.6),
                new Dish("Fish Curry", MealCategory.MAIN, false, 3.4), new Dish("Veg Biryani", MealCategory.RICE, true, 4.2),
                new Dish("Chicken Biryani", MealCategory.RICE, false, 4.8), new Dish("Aloo Matar", MealCategory.SIDE, true, 3.5),
                new Dish("Bhindi Fry", MealCategory.SIDE, true, 2.9), new Dish("Raita", MealCategory.SIDE, true, 3.9),
                new Dish("Kheer", MealCategory.DESSERT, true, 4.6), new Dish("Ice Cream", MealCategory.DESSERT, true, 4.7)));
    }

    private static final Map<String, Double> QUALITY = new HashMap<>();
    static { POOL.values().forEach(l -> l.forEach(d -> QUALITY.put(d.name(), d.quality()))); }

    private Map<String, MenuItem> createDishes() {
        Map<String, MenuItem> out = new LinkedHashMap<>();
        for (List<Dish> list : POOL.values()) {
            for (Dish d : list) {
                out.computeIfAbsent(d.name(), n -> dishes.findAll().stream().filter(x -> x.getName().equalsIgnoreCase(n)).findFirst().orElseGet(() -> {
                    MenuItem m = new MenuItem();
                    m.setName(d.name());
                    m.setCategory(d.cat());
                    m.setVeg(d.veg());
                    return dishes.save(m);
                }));
            }
        }
        return out;
    }

    /** Deterministic rotation: the same date + meal always yields the same dishes. */
    private List<MenuItem> pickItems(LocalDate date, MealType type, Map<String, MenuItem> cat) {
        Random r = new Random(date.toEpochDay() * 31 + type.ordinal());
        List<Dish> pool = POOL.get(type);
        List<MenuItem> out = new ArrayList<>();
        int mains = type == MealType.SNACKS ? 1 : 2;
        add(out, pool, cat, MealCategory.MAIN, mains, r);
        add(out, pool, cat, MealCategory.RICE, type == MealType.BREAKFAST || type == MealType.SNACKS ? 0 : 1, r);
        add(out, pool, cat, MealCategory.BREAD, type == MealType.LUNCH || type == MealType.DINNER ? 1 : 0, r);
        add(out, pool, cat, MealCategory.SIDE, type == MealType.SNACKS ? 1 : 2, r);
        add(out, pool, cat, MealCategory.BEVERAGE, type == MealType.BREAKFAST || type == MealType.SNACKS ? 1 : 0, r);
        if ((type == MealType.DINNER && date.getDayOfWeek().getValue() % 3 == 0) || (type == MealType.LUNCH && date.getDayOfWeek().getValue() == 7)) {
            add(out, pool, cat, MealCategory.DESSERT, 1, r);
        }
        return out;
    }

    private void add(List<MenuItem> out, List<Dish> pool, Map<String, MenuItem> cat, MealCategory c, int n, Random r) {
        List<Dish> options = new ArrayList<>(pool.stream().filter(d -> d.cat() == c).toList());
        for (int i = 0; i < n && !options.isEmpty(); i++) out.add(cat.get(options.remove(r.nextInt(options.size())).name()));
    }

    // --------------------------------------------------------------------------------------- feedback

    private static final String[] COMMENTS = { "Really tasty today!", "Portion was too small.", "Food was cold when I got it.", "Loved the variety.",
            "Please add more salad.", "Too oily.", "Best meal this week.", "Rice was a bit undercooked.", "Great service at the counter.", "Needs more salt." };

    private int ratingFor(DailyMenu m, Random rnd) {
        double avg = m.getItems().stream().mapToDouble(i -> QUALITY.getOrDefault(i.getName(), 3.8)).average().orElse(3.8);
        double v = avg + rnd.nextGaussian() * 0.8;
        return (int) Math.max(1, Math.min(5, Math.round(v)));
    }

    // ------------------------------------------------------------------ complaints / notices / bills

    private record C(ComplaintCategory cat, ComplaintPriority pri, ComplaintStatus status, String title, String body) {}

    private void seedComplaints(List<User> students, List<User> staff, User admin, Random rnd) {
        List<C> list = List.of(
                new C(ComplaintCategory.FOOD_QUALITY, ComplaintPriority.HIGH, ComplaintStatus.RESOLVED, "Rice undercooked at lunch", "The rice was hard and cold on Tuesday."),
                new C(ComplaintCategory.HYGIENE, ComplaintPriority.HIGH, ComplaintStatus.IN_PROGRESS, "Dirty plates at counter 2", "Several plates had food residue."),
                new C(ComplaintCategory.SERVICE, ComplaintPriority.MEDIUM, ComplaintStatus.OPEN, "Very long dinner queue", "Only one counter open at 8:30 pm."),
                new C(ComplaintCategory.BILLING, ComplaintPriority.MEDIUM, ComplaintStatus.RESOLVED, "Charged for a meal I cancelled", "Please re-check last month's bill."),
                new C(ComplaintCategory.FOOD_QUALITY, ComplaintPriority.LOW, ComplaintStatus.OPEN, "Need more variety in breakfast", "Idli almost every day."),
                new C(ComplaintCategory.OTHER, ComplaintPriority.LOW, ComplaintStatus.REJECTED, "Request for a mini-fridge in the mess", "It would help keep milk cold."),
                new C(ComplaintCategory.HYGIENE, ComplaintPriority.MEDIUM, ComplaintStatus.OPEN, "Water dispenser not working", "No drinking water near the entrance."),
                new C(ComplaintCategory.SERVICE, ComplaintPriority.HIGH, ComplaintStatus.IN_PROGRESS, "QR scanner failing at the counter", "Had to wait while staff typed my roll number."),
                new C(ComplaintCategory.FOOD_QUALITY, ComplaintPriority.MEDIUM, ComplaintStatus.RESOLVED, "Chicken curry too spicy", "Nearly inedible on Friday."),
                new C(ComplaintCategory.OTHER, ComplaintPriority.LOW, ComplaintStatus.OPEN, "Mess timings on Sunday", "Can breakfast start 30 minutes later on Sundays?"));
        for (C c : list) {
            Complaint x = new Complaint();
            x.setStudent(students.get(rnd.nextInt(students.size())));
            x.setCategory(c.cat());
            x.setPriority(c.pri());
            x.setTitle(c.title());
            x.setDescription(c.body());
            x.setStatus(c.status());
            if (c.status() != ComplaintStatus.OPEN) x.setAssignedTo(staff.get(rnd.nextInt(staff.size())));
            complaints.save(x);
            if (x.getAssignedTo() != null) {
                update(x, admin, "Assigned to " + x.getAssignedTo().getName(), ComplaintStatus.IN_PROGRESS);
                if (c.status() == ComplaintStatus.RESOLVED) update(x, x.getAssignedTo(), "Looked into it and fixed the issue. Thanks for flagging.", ComplaintStatus.RESOLVED);
                if (c.status() == ComplaintStatus.REJECTED) update(x, x.getAssignedTo(), "Sorry, this isn't possible right now.", ComplaintStatus.REJECTED);
            }
        }
    }

    private void update(Complaint c, User author, String msg, ComplaintStatus st) {
        ComplaintUpdate u = new ComplaintUpdate();
        u.setComplaint(c);
        u.setAuthor(author);
        u.setMessage(msg);
        u.setNewStatus(st);
        complaintUpdates.save(u);
    }

    private void seedAnnouncements(User admin) {
        record A(String t, String b, boolean pin) {}
        for (A a : List.of(
                new A("Welcome to Thali!", "Book your meals before the cutoff, show your QR at the counter, and rate what you eat.", true),
                new A("Special dinner this Saturday", "Chicken biryani and kheer for everyone who has booked dinner.", false),
                new A("Menu changes next week", "We've added Pav Bhaji to the snacks rotation after your feedback.", false))) {
            Announcement n = new Announcement();
            n.setTitle(a.t());
            n.setBody(a.b());
            n.setPinned(a.pin());
            n.setAuthor(admin);
            announcements.save(n);
        }
    }

    /** Bills for the last two months: most paid (mixed methods), a few unpaid, a few overdue. */
    private void seedBills(LocalDate today, User admin, Random rnd) {
        for (int back = 2; back >= 1; back--) {
            YearMonth ym = YearMonth.from(today).minusMonths(back - 1L).minusMonths(1);
            billing.generate(ym);
            for (Bill b : bills.findForMonth(ym.atDay(1))) {
                if (b.getStudent().getEmail().startsWith("z9") == false) continue;
                double r = rnd.nextDouble();
                if (r < 0.72) {
                    Payment p = new Payment();
                    p.setBill(b);
                    p.setAmount(b.getTotal());
                    PaymentMethod method = r < 0.35 ? PaymentMethod.UPI : r < 0.55 ? PaymentMethod.CARD : PaymentMethod.CASH;
                    p.setMethod(method);
                    p.setTxnRef((method == PaymentMethod.CASH ? "CASH-" : "MOCK-") + Long.toHexString(rnd.nextLong() & 0xFFFFFFFFFFFFL).toUpperCase());
                    p.setStatus(PaymentStatus.SUCCESS);
                    p.setPaidAt(ym.plusMonths(1).atDay(1 + rnd.nextInt(10)).atTime(11, rnd.nextInt(60)).atZone(clock.getZone()).toInstant());
                    if (method == PaymentMethod.CASH) p.setRecordedBy(admin);
                    payments.save(p);
                    b.setStatus(BillStatus.PAID);
                } else if (r < 0.88 && back == 1) {
                    b.setStatus(BillStatus.UNPAID);
                } else {
                    b.setStatus(BillStatus.OVERDUE);
                    b.setDueDate(today.minusDays(3 + rnd.nextInt(10)));
                }
                bills.save(b);
            }
        }
    }
}
