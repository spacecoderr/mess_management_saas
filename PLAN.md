# Thali — Mess Management System for IIIT Bhubaneswar · Master Plan

> **Phases 7-9: ✅ done** — staff: serve counter with camera QR scanner / pasted pass code / manual search, shared menu grid + editor, complaints queue. Admin: dashboard charts (recharts), students & approvals, staff accounts, dishes, meal settings, billing (generate, cash, CSV), menu builder (+copy week), attendance + CSV, feedback analytics, announcements. Every sidebar item is now a real screen. 162 frontend tests. **Remaining: deployment only** (Neon + Render + Vercel + Brevo; needs your accounts).
> **Phase 6 status: ✅ done** — student screens: menu with day tabs + book/cancel/"book all open meals", My bookings with QR pass, Bills (breakdown + mock UPI/card payment), Feedback (star ratings), Complaints (raise + timeline + comment), Profile (details + password). 149 frontend tests.
> **Phase 5 status: ✅ done** — design tokens + light/dark themes, API client with single-flight token refresh, stores, UI primitives, app shell (sidebar / bottom nav / drawer), route guards, login (password + email code), 3-step registration with OTP, forgot password, pending-approval, and live role homes for student / staff / admin. 135 frontend tests. Real screens for the other nav items arrive in Phases 6-8.
> **Local run: ✅ verified** (API :8081 + web :5173 + Postgres; `./scripts/check-local.sh` = 7/7; VS Code tasks fixed with ready-matchers).
> **Phase 4 status: ✅ done** — 197 backend tests (72 unit, 125 integration on Testcontainers PostgreSQL) + 17 frontend tests, all passing, 0 skipped; see TEST-SUMMARY.md. Found and fixed a real bulk-booking transaction bug. Backend `./mvnw verify` needs Docker running.
> **Phase 3 status: ✅ done** — bills (generate/list/detail with per-meal lines), mock payments (checkout→confirm, admin cash), analytics (overview, bookings trend, meal split, wastage, ratings trend, top/worst dishes, revenue), CSV reports, announcements, scheduled jobs (no-show every 5 min, overdue daily 00:30, auto-billing on the 1st at 02:00) and a one-time demo-data seeder (dev/demo profiles). Bill = consumed + no-show meals + ₹300 fixed charge (`app.billing.fixed-monthly-charge`), due 15 days after month end.
> **Phase 2 status: ✅ done** — users/student approval/profile, meal slots, dish library, daily menus (+copy-week, publish), bookings (cutoff rules, re-book, bulk, QR), attendance (scan/manual/live), feedback (+pending, summary), complaints (assign, timeline). Deviations: concrete service classes (no interface/impl pairs) and hand-written DTO mappers (no MapStruct). Booking/cancel closes at *meal start − cutoff*; attendance window is enforced in prod (start−30 min … end+30 min) but disabled in `dev`.
> **Phase 1 status: ✅ done** — full 15-table Flyway schema + entities/repos, JWT (Nimbus, HS256; access 15 min, rotating opaque refresh tokens stored as SHA-256), IIIT-BH email OTP (register / login / reset), global problem+json errors, Swagger. Students must be admin-approved before login is allowed (403 `ACCOUNT_PENDING_APPROVAL`); the approval endpoint arrives in Phase 2. Dev admin is seeded from config (see README).
> **Phase 0 status: ✅ done.** Deviations: API port **8081** (8080 is taken locally); Node 18 → Vite 5 + Tailwind 3 + React 18; local Postgres 14 db `thali` (docker-compose uses host port 5433).

> Build in this exact phase order. Each phase ends with a runnable checkpoint, so you can stop at any point and still have something that works.
> Goal: it runs on localhost, then goes live on free hosting with a public link. It is not built to scale.

---

## 1. Requirements analysis

| Role | Can do |
|---|---|
| **Student** | Register (waits for admin approval), log in, view weekly menu, book or cancel meals before the cutoff, show a QR pass at the counter, rate meals, raise and track complaints, view and pay monthly bills, see personal stats |
| **Staff** | Scan QR or mark attendance by hand, see live headcount per meal, manage today's menu, handle the complaints assigned to them |
| **Admin** | Everything staff can do, plus: approve students, manage users and staff, build menus, set meal prices and cutoffs, generate bills, record payments, view analytics, post announcements |

### Identity & verification (IIIT Bhubaneswar)
- Students can only sign up with a college email matching `^[a-z]\d{6}@iiit-bh\.ac\.in$` (e.g. `b423046@iiit-bh.ac.in`). The check runs on both the frontend (Zod) and the backend (`@Pattern` + a domain allow-list in config: `app.auth.allowed-domain=iiit-bh.ac.in`).
- **Roll number** is taken from the email's local part (`b423046`), so the student doesn't type it. The batch year is guessed from digits 2–3 (`23` → 2023). This is an assumption; the student can edit it on the profile.
- **OTP flow:** a 6-digit code goes to the college email. It is valid for 5 minutes, allows at most 5 attempts, and can be resent once every 60 s (no more than 5 per hour per email). Only a BCrypt hash of the code is stored.
  - **Registration:** enter email → OTP → verify → fill profile (name, phone, block, room, diet) + set a password → account goes to admin approval.
  - **Login:** email + password, **or** "Login with OTP" (passwordless, college email only).
  - **Forgot password:** OTP to email → reset.
- **Why email, not SMS:** OTP by email is free, and owning the college email is what proves the person is a real student. SMS to the phone needs a paid provider (MSG91/Twilio) plus DLT registration in India. It is kept behind the same `OtpSender` interface (`EmailOtpSender` now, `SmsOtpSender` later), so it can be added without touching anything else.
- **Email provider:** **Brevo HTTP API** (free, 300 emails/day, verify one sender address). It uses HTTPS, not SMTP, because Render's free tier blocks outbound SMTP ports. On the `dev` profile, a `ConsoleOtpSender` prints the OTP to the backend logs, so local testing needs no email setup.
- **Staff and admin** don't need college emails. They are created by an admin (or seeded) and log in with email + password.
- **Demo mode** (`demo` profile on the live link): seeded demo students keep password login, so reviewers can try the app without an IIIT-BH inbox. Real `@iiit-bh.ac.in` sign-ups still go through OTP.

**Business rules (defaults, change in §9 if needed)**
- Meals: BREAKFAST, LUNCH, SNACKS, DINNER, each with its own price, serving window and booking cutoff (default: 4h before the meal starts).
- Bill = sum of meals CONSUMED + NO_SHOW (booked but not eaten is still charged) + a fixed monthly charge.
- Each booking gets a single-use QR token. When staff scan it, the booking is marked CONSUMED. At the end of the serving window, unscanned bookings become NO_SHOW (scheduled job).
- A student can rate only meals they consumed, once per meal.
- Payments: a **mock gateway** (UPI/Card simulation) plus "mark paid in cash" for admin. Razorpay test mode is an optional add-on.

**Non-functional:** JWT auth, role-based access, input validation, consistent error format, Swagger docs, tests, responsive layout, and dark mode.

---

## 2. System architecture

```
┌──────────────────────┐   HTTPS/JSON   ┌───────────────────────────┐   JDBC   ┌──────────────┐
│ React SPA (Vite)     │ ─────────────▶ │ Spring Boot API           │ ───────▶ │ PostgreSQL   │
│ Vercel (static)      │ ◀───────────── │ Render (Docker, free)     │          │ Neon (free)  │
└──────────────────────┘   JWT Bearer   └───────────────────────────┘          └──────────────┘
```

**Backend:** Java 21 · Spring Boot 4.1 (Initializr no longer offers 3.x) · Spring Security + JWT (jjwt) · Spring Data JPA · Flyway · Bean Validation · MapStruct · Lombok · springdoc-openapi · JUnit 5 + Mockito + Testcontainers.
- Code is organised **by feature**, with these layers inside each feature: `controller → service (interface + impl) → repository → entity`; DTOs are Java `record`s.
- `GlobalExceptionHandler` (`@RestControllerAdvice`) returns RFC 7807 `ProblemDetail` responses for validation, not-found, conflict, forbidden and business-rule errors.
- Security is stateless. A 15 min access token comes back in the JSON response; a 7 day refresh token is stored in the DB. Method-level checks use `@PreAuthorize("hasRole('ADMIN')")`.
- `@Scheduled` jobs mark NO_SHOWs and flag overdue bills.
- A `DataSeeder` runs on the `dev` and `demo` profiles and creates demo users, 4 weeks of menus, bookings, feedback, complaints and bills, so the dashboards show data immediately.

**Frontend:** React 18 · TypeScript · Vite · Tailwind CSS · React Router 6 · TanStack Query · Zustand (auth store) · React Hook Form + Zod · Axios (interceptor handles refresh) · Framer Motion · Recharts · lucide-react · sonner (toasts) · html5-qrcode (scanner) · qrcode.react.

**Deployment (free tier, easiest):**
- DB → **Neon** Postgres (free, never expires; Render's free DB is deleted after 30 days).
- API → **Render** Web Service using the backend `Dockerfile`. Env vars: `DB_URL`, `DB_USER`, `DB_PASS`, `JWT_SECRET`, `CORS_ORIGINS`, `SPRING_PROFILES_ACTIVE=demo`.
- Web → **Vercel**, root `frontend/`, env var `VITE_API_URL`. `vercel.json` adds an SPA rewrite.
- Note: Render's free tier sleeps when idle, so the first request takes about 30–50 s. The login page shows a "waking server…" state for this.

**Local:** `docker compose up` starts Postgres, the API and the web app, or run Postgres alone in Docker and use `./mvnw spring-boot:run` + `npm run dev`.

---

## 3. Database schema

```
users 1─1 student_profiles
users 1─* bookings *─1 (date, meal_type) ── daily_menus *─* menu_items (via daily_menu_items)
bookings 1─0..1 attendance
users 1─* feedback *─1 daily_menus
users 1─* complaints 1─* complaint_updates
users 1─* bills 1─* payments
meal_slot_config (one row per meal_type)
users 1─* refresh_tokens
users 1─* announcements (author)
```

| Table | Key columns | Constraints |
|---|---|---|
| `users` | id BIGSERIAL, name, email, password_hash, role (ADMIN/STAFF/STUDENT), phone, email_verified, active, created_at | email UNIQUE |
| `otp_codes` | id, email, purpose (REGISTER/LOGIN/RESET_PASSWORD), code_hash, expires_at, attempts, consumed, created_at | idx(email, purpose, created_at) |
| `student_profiles` | user_id PK/FK, roll_no, hostel_block, room_no, diet_pref (VEG/NON_VEG/JAIN), approval_status (PENDING/APPROVED/REJECTED) | roll_no UNIQUE |
| `meal_slot_config` | meal_type PK, start_time, end_time, cutoff_minutes, price NUMERIC(8,2), active | |
| `menu_items` | id, name, description, category (MAIN/SIDE/BREAD/RICE/DESSERT/BEVERAGE), is_veg, image_url, active | name UNIQUE |
| `daily_menus` | id, menu_date, meal_type, published, created_by | UNIQUE(menu_date, meal_type) |
| `daily_menu_items` | daily_menu_id FK, menu_item_id FK | PK(both) |
| `bookings` | id, student_id FK, daily_menu_id FK, status (BOOKED/CANCELLED/CONSUMED/NO_SHOW), price_at_booking, qr_token UUID, created_at, cancelled_at | UNIQUE(student_id, daily_menu_id), qr_token UNIQUE, idx(daily_menu_id,status) |
| `attendance` | id, booking_id FK, marked_by FK users, marked_at, method (QR/MANUAL) | booking_id UNIQUE |
| `feedback` | id, student_id, daily_menu_id, rating SMALLINT 1–5, comment, created_at | UNIQUE(student_id, daily_menu_id), CHECK rating |
| `complaints` | id, student_id, assigned_to, category (FOOD_QUALITY/HYGIENE/SERVICE/BILLING/OTHER), title, description, priority (LOW/MED/HIGH), status (OPEN/IN_PROGRESS/RESOLVED/REJECTED), created_at, updated_at | idx(status) |
| `complaint_updates` | id, complaint_id, author_id, message, new_status, created_at | |
| `bills` | id, student_id, bill_month (DATE, 1st of month), meal_count, meal_charges, fixed_charges, total, status (UNPAID/PAID/OVERDUE), due_date, generated_at | UNIQUE(student_id, bill_month) |
| `payments` | id, bill_id, amount, method (UPI/CARD/CASH), txn_ref, status (SUCCESS/FAILED), paid_at, recorded_by | txn_ref UNIQUE |
| `announcements` | id, title, body, author_id, pinned, created_at | |
| `refresh_tokens` | id, user_id, token, expires_at, revoked | token UNIQUE |

All tables also get `created_at` and `updated_at` through a JPA `@MappedSuperclass BaseEntity`. Optimistic locking (`@Version`) goes on `bills` and `bookings`.

---

## 4. API endpoints (`/api/v1`, Swagger at `/swagger-ui.html`)

**Auth** (public)
- `POST /auth/otp/request` {email, purpose} → sends OTP (rate-limited; only `@iiit-bh.ac.in` for REGISTER/LOGIN)
- `POST /auth/otp/verify` {email, purpose, code} → for REGISTER returns a short-lived `registrationToken`; for LOGIN returns access + refresh tokens
- `POST /auth/register` {registrationToken, name, phone, hostelBlock, roomNo, dietPref, password} (student → PENDING approval)
- `POST /auth/login` {email, password} · `POST /auth/password/reset` {email, code, newPassword}
- `POST /auth/refresh` · `POST /auth/logout` · `GET /auth/me`

**Users / Students**
- `GET /users?role=&q=&page=` [ADMIN] · `POST /users` (create staff) [ADMIN] · `PATCH /users/{id}` · `PATCH /users/{id}/status` [ADMIN]
- `GET /students/pending` · `PATCH /students/{id}/approval` [ADMIN]
- `GET /me/profile` · `PUT /me/profile` · `PUT /me/password` [ANY]

**Meal config & menu**
- `GET /meal-slots` [ANY] · `PUT /meal-slots/{mealType}` [ADMIN]
- `GET/POST /menu-items` · `PUT/DELETE /menu-items/{id}` [ADMIN, STAFF for GET]
- `GET /menus?from=&to=` [ANY] (weekly view) · `GET /menus/today` [ANY]
- `PUT /menus/{date}/{mealType}` (set items) · `POST /menus/copy-week` · `PATCH /menus/{id}/publish` [ADMIN, STAFF]

**Bookings**
- `GET /bookings/me?from=&to=` · `POST /bookings` {date, mealType} · `POST /bookings/bulk` (whole week) · `DELETE /bookings/{id}` [STUDENT]
- `GET /bookings/{id}/qr` [STUDENT, owner]
- `GET /bookings?date=&mealType=&status=` [STAFF, ADMIN]

**Attendance**
- `POST /attendance/scan` {qrToken} · `POST /attendance/manual` {bookingId} [STAFF, ADMIN]
- `GET /attendance/live?date=&mealType=` → booked / served / remaining [STAFF, ADMIN]

**Feedback**
- `POST /feedback` · `GET /feedback/me` [STUDENT]
- `GET /feedback?from=&to=&mealType=` · `GET /feedback/summary` [STAFF, ADMIN]

**Complaints**
- `POST /complaints` · `GET /complaints/me` [STUDENT]
- `GET /complaints?status=&category=` [STAFF, ADMIN] · `GET /complaints/{id}` [owner/STAFF/ADMIN]
- `PATCH /complaints/{id}/assign` [ADMIN] · `POST /complaints/{id}/updates` {message, newStatus} [STAFF, ADMIN]

**Billing & payments**
- `POST /bills/generate?month=YYYY-MM` [ADMIN] · `GET /bills?month=&status=` [ADMIN]
- `GET /bills/me` · `GET /bills/{id}` [owner/ADMIN]
- `POST /payments/checkout` {billId, method} → mock txn · `POST /payments/{id}/confirm` [STUDENT]
- `POST /payments/cash` {billId, amount} [ADMIN] · `GET /payments?from=&to=` [ADMIN]

**Analytics** [ADMIN, some STAFF]
- `GET /analytics/overview` (KPIs: students, today's bookings, revenue for the month, average rating, open complaints)
- `GET /analytics/bookings-trend?days=30` · `GET /analytics/meal-distribution` · `GET /analytics/ratings-trend` · `GET /analytics/top-dishes` · `GET /analytics/revenue?months=6` · `GET /analytics/wastage` (booked vs consumed)
- `GET /reports/bills.csv?month=` · `GET /reports/attendance.csv?from=&to=`

**Announcements**
- `GET /announcements` [ANY] · `POST/DELETE /announcements/{id}` [ADMIN]

**Error format:** `{type, title, status, detail, instance, errors:[{field,message}], timestamp}`

---

## 5. Frontend page hierarchy

```
/                      Landing (hero, features, "Live today's menu" glass card, CTA)
/login                 Password tab | "Login with OTP" tab (split glass panel, animated food blobs)
/register              3-step stepper: college email → 6-box OTP input (auto-advance, paste, resend timer) → profile + password
/forgot-password       Email → OTP → new password
/pending-approval      Shown after register until admin approves
/app                   Role-aware shell: glass sidebar (desktop) / bottom nav (mobile), topbar, theme toggle
 ├─ Student
 │   /app/home          Greeting, today's meals with Book toggles, next-meal QR, announcements, this month's bill
 │   /app/menu          Weekly menu grid (day tabs), veg badges, book/cancel inline, "Book whole week"
 │   /app/bookings      Calendar + list, statuses, QR modal
 │   /app/feedback      Rate consumed meals (star + emoji), history
 │   /app/complaints    List + create drawer + timeline detail
 │   /app/bills         Bill cards, breakdown, Pay → mock checkout modal → success confetti
 │   /app/profile
 ├─ Staff
 │   /app/serve         Live counter: meal switcher, big headcount ring, QR scanner, manual search
 │   /app/menu-today    Edit today's/tomorrow's menu
 │   /app/complaints    Assigned queue (kanban: Open → In progress → Resolved)
 └─ Admin
     /app/dashboard     KPI glass tiles, charts (trend, meal split, ratings, revenue), recent complaints
     /app/students      Table + approval queue
     /app/staff         Manage staff
     /app/menu-builder  Week planner: drag dishes into day×meal grid, copy week, publish
     /app/dishes        Dish library CRUD
     /app/meal-settings Prices, timings, cutoffs
     /app/attendance    Per-meal reports
     /app/feedback      Ratings analytics + comments
     /app/complaints    All complaints, assign
     /app/billing       Generate month, bills table, record cash, CSV export
     /app/announcements
/403  /404
```

**Reusable components:** `GlassCard`, `GlassPanel`, `Button` (variants), `Input/Select/Textarea` (RHF-bound), `Modal`, `Drawer`, `DataTable` (sort/paginate/search), `StatTile`, `ChartCard`, `Badge/StatusPill`, `MealChip`, `StarRating`, `EmptyState`, `Skeleton`, `PageHeader`, `ConfirmDialog`, `RoleGuard`, `QRDisplay`, `QRScanner`, `Timeline`, `AnimatedNumber`.

### Design system: "Ivory & Forest Glass" (no violet, no black)
> Updated in Phase 0: palette is warm ivory `#FBF7EE`, forest green `#1F4D3A`, sage `#A8B9A0`, terracotta `#C8643B`. Tokens live in `frontend/tailwind.config.ts`; the older saffron/teal values below are superseded.
- **Background:** a warm, slowly moving mesh gradient of cream `#FFF7EC` → peach `#FFD9B8` → mint `#CFF5E7` → sky `#D6ECFF`, with 3 blurred floating blobs.
- **Glass:** `bg-white/40 backdrop-blur-xl border border-white/60 shadow-[0_8px_32px_rgba(15,118,110,0.12)] rounded-3xl`, plus a subtle noise texture overlay.
- **Accents:** Saffron `#F59E0B` (primary actions), Deep Teal `#0F766E` (nav, headings), Coral `#F97362` (alerts), Leaf `#22C55E` (veg/success). Text uses ink-teal `#13302E`.
- **Dark mode = "Midnight Lagoon":** deep teal-navy `#0B1F24` → `#0E2E33` gradient, glass `bg-white/5 border-white/10`, saffron glow on accents.
- **Type:** *Plus Jakarta Sans* (UI) + *Fraunces* (display numbers and headings).
- **Motion:** page fade/slide (Framer `AnimatePresence`), staggered card entry, springy toggles, animated counters, confetti on payment and booking the whole week, and a progress ring for the live headcount.
- **Signature touches:** meal cards tinted by time of day (breakfast = sunrise peach, lunch = saffron, snacks = mint, dinner = lagoon), and an animated thali illustration on the landing page.

---

## 6. Folder structure

```
mess_management/
├─ docker-compose.yml          # postgres + backend + frontend
├─ render.yaml                 # Render blueprint (backend)
├─ README.md                   # run locally, deploy, demo accounts
├─ backend/
│  ├─ Dockerfile  pom.xml  mvnw
│  └─ src/
│     ├─ main/java/com/thali/
│     │  ├─ ThaliApplication.java
│     │  ├─ common/       BaseEntity, ApiError, PageResponse, exceptions/, GlobalExceptionHandler
│     │  ├─ config/       SecurityConfig, CorsConfig, OpenApiConfig, SchedulingConfig
│     │  ├─ security/     JwtService, JwtAuthFilter, UserPrincipal, CustomUserDetailsService
│     │  ├─ auth/         controller, service, dto, otp/ (OtpService, OtpSender iface, ConsoleOtpSender, BrevoEmailOtpSender, RateLimiter)
│     │  ├─ user/         entity(User, StudentProfile), repository, service, controller, dto, mapper
│     │  ├─ menu/         MealSlotConfig, MenuItem, DailyMenu …
│     │  ├─ booking/
│     │  ├─ attendance/
│     │  ├─ feedback/
│     │  ├─ complaint/
│     │  ├─ billing/      Bill, Payment, BillingService, PaymentGateway (interface) + MockPaymentGateway
│     │  ├─ analytics/
│     │  ├─ announcement/
│     │  └─ seed/         DataSeeder (@Profile dev|demo)
│     ├─ main/resources/  application.yml, application-dev.yml, application-demo.yml, db/migration/V1__init.sql …
│     └─ test/java/com/thali/  unit (services), integration (controllers + Testcontainers)
└─ frontend/
   ├─ vercel.json  vite.config.ts  tailwind.config.ts  .env.example
   └─ src/
      ├─ main.tsx  App.tsx  routes.tsx
      ├─ api/          client.ts (axios+refresh), endpoints per feature (auth.ts, menu.ts …)
      ├─ hooks/        useAuth, query hooks per feature
      ├─ store/        authStore.ts, themeStore.ts
      ├─ components/   ui/ (primitives), layout/ (Shell, Sidebar, BottomNav, Topbar), charts/, feedback/
      ├─ features/     student/, staff/, admin/, auth/, landing/  (page + local components)
      ├─ lib/          utils, formatters, zod schemas, constants
      ├─ types/        api.ts (mirrors backend DTOs)
      └─ styles/       globals.css (mesh bg, glass utilities)
```

---

## 7. Implementation phases (one Sonnet session per phase)

| # | Phase | Deliverable / checkpoint |
|---|---|---|
| **0** | Scaffold | Spring Boot project from the starter deps, Vite React TS + Tailwind, docker-compose with Postgres, `application-dev.yml`, health endpoint. ✅ Both apps start; the API connects to the DB. |
| **1** | Core backend | BaseEntity, exceptions + handler, Flyway V1 (all tables), entities and repositories, Security + JWT + refresh, auth endpoints including the IIIT-BH email check + OTP (console sender on dev, Brevo on demo), Swagger. ✅ Request an OTP (it appears in the logs), verify it, register, log in and call `/auth/me` from Swagger. |
| **2** | Domain APIs | Users/approval, meal slots, dishes, menus, bookings (cutoff rules, QR), attendance, feedback, complaints. ✅ All endpoints work in Swagger with role checks. |
| **3** | Billing + analytics + jobs | Bill generation, mock payments, analytics queries, CSV reports, NO_SHOW and overdue schedulers, DataSeeder. ✅ The seeded demo shows realistic numbers. |
| **4** | Backend tests | Service unit tests (booking cutoff, billing math, attendance double-scan), controller integration tests with Testcontainers and security checks. ✅ `./mvnw verify` passes. |
| **5** | Frontend foundation | Design tokens, glass primitives, layout shell, routing + RoleGuard, axios with refresh, auth pages, landing page. ✅ Each role logs in and lands on its own home page. |
| **6** | Student UI | Home, menu, bookings + QR, feedback, complaints, bills + mock pay. |
| **7** | Staff UI | Live serve counter with QR scanner, today's menu, complaint kanban. |
| **8** | Admin UI | Dashboard charts, students, staff, menu builder, dishes, meal settings, attendance, feedback, complaints, billing, announcements. |
| **9** | Polish | Animations, skeletons, empty states, dark mode, mobile pass, a11y, error toasts, cold-start banner. A few Vitest + RTL tests for key components. |
| **10** | Run locally end to end | `docker compose up --build` → full demo walkthrough per role. |
| **11** | Deploy | Neon DB → Brevo account (verify sender, get API key → `BREVO_API_KEY`, `MAIL_FROM`) → Render API (Docker) → Vercel web → set CORS and env vars → smoke test on the live link, including a real OTP to an `@iiit-bh.ac.in` inbox. README gets deploy steps and demo credentials. |

**Demo accounts:** created by `DataSeeder` and listed in `README.md` (one admin, two staff, about 25 students).

---

## 8. Prompt to give Sonnet per phase
> "Read `PLAN.md`. Implement **Phase N** exactly as specified. Follow the folder structure, conventions and design system. When done, run it, fix errors, and tell me what to verify."

---

## 9. Open decisions (defaults in **bold**)
1. App/brand name: **Thali · IIIT Bhubaneswar** ✅ college set; is "Thali" OK as the app name?
1b. OTP channel: **college email** ✅ confirmed (SMS stays a later, optional add-on). Confirm the email format is always one letter + 6 digits.
2. Billing: **per meal consumed + fixed monthly charge**, or a flat monthly fee?
3. Payments: **mock gateway**, or Razorpay test mode?
4. Meal slots: **4 (B/L/S/D)** or 3?
5. Student signup: **needs admin approval**, or instant?
6. Currency: **₹ INR**?
7. Hosting: **Vercel + Render + Neon** (all free, needs GitHub + 3 accounts)?
8. Do you have Java 21, Maven, Node 20 and Docker installed locally?
