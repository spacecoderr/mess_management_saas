# Thali — IIIT Bhubaneswar Mess Management

Spring Boot + PostgreSQL backend, React + TypeScript + Tailwind frontend. See [PLAN.md](PLAN.md) for the full roadmap.

## Quick start — run everything locally (VS Code)
**One-time setup**
1. Open this folder in VS Code and accept the *recommended extensions* prompt (Java, Spring Boot, ESLint/Prettier, Tailwind). Optional: `Cmd+Shift+P` → *Shell Command: Install 'code' command in PATH*.
2. Make sure PostgreSQL is running on `localhost:5432` with database `thali` (user/password `thali`). The setup commands are under "1. Database" below. Docker is only needed for the integration tests.
3. `cd frontend && npm install` (first time only). Java 21 and Node 18+ are required.

**Every time**
- **Easiest:** `Terminal → Run Task… → full stack: run both` (starts the API and the web app in two dedicated terminal panels).
- **With debuggers:** *Run and Debug* → **Full stack (API + Web)** (needs the Java and Chrome debug support that VS Code bundles/recommends).
- **Plain terminals:** `cd backend && ./mvnw spring-boot:run` and, in a second terminal, `cd frontend && npm run dev`.

Then open **http://localhost:5173** (the app) and **http://localhost:8081/swagger-ui.html** (API docs). On the very first start the backend loads demo data (see "Phase 3"), which takes a few seconds.

**Is everything healthy?** `./scripts/check-local.sh` checks Postgres, the API, the web server, the proxy and three real logins.

**Try it now** — the web app currently has the landing page only (the dashboards are built in Phases 5-8), so explore through Swagger: click *Authorize*, log in via `POST /api/v1/auth/login` (admin `admin@thali.local` / `Admin@12345`, staff `staff1@thali.local` / `Demo@1234`, student `z900002@iiit-bh.ac.in` / `Demo@1234`), paste the `accessToken`, and call any endpoint. In dev, OTP codes are printed in the backend terminal.

**Troubleshooting**
| Symptom | Fix |
|---|---|
| `Port 8081 already in use` | Another app holds it; stop it or run with `PORT=8082` and set `VITE_PROXY_TARGET=http://localhost:8082` for the frontend. (8080 is deliberately not used.) |
| Backend fails with `password authentication failed for user "thali"` | Create the role and database (README step 1) or point `DB_URL`/`DB_USER`/`DB_PASS` at yours. |
| Landing page says **API offline** | The backend isn't running or still starting; run `./scripts/check-local.sh`. |
| `Flyway ... checksum mismatch` or odd data | Reset the dev database: `psql -d postgres -c "DROP DATABASE thali" -c "CREATE DATABASE thali OWNER thali"` and restart the backend (demo data reloads). |
| Java debug config can't find project `thali` | Wait for the Java extension to finish importing the Maven project (status bar), or run *Java: Clean Java Language Server Workspace*. |

## Prerequisites
Java 21, Maven (or the bundled `./mvnw`), Node 18+, PostgreSQL 14+ **or** Docker.

## 1. Database (pick one)
**A. Your local Postgres** (what Phase 0 was verified with):
```bash
psql -d postgres -c "CREATE ROLE thali LOGIN PASSWORD 'thali'"
psql -d postgres -c "CREATE DATABASE thali OWNER thali"
```
**B. Docker** (exposed on port 5433):
```bash
docker compose up -d db
export DB_URL=jdbc:postgresql://localhost:5433/thali
```

## 2. Backend  → http://localhost:8081
```bash
cd backend
./mvnw spring-boot:run
```
Health check: `curl http://localhost:8081/api/v1/health` → `{"status":"UP","database":"UP",...}`
Run tests: `./mvnw test`. Port 8081 is used because 8080 is commonly taken; override with `PORT=...`.

## 3. Frontend  → http://localhost:5173
```bash
cd frontend
npm install
npm run dev
```
Vite proxies `/api` to the backend, so no CORS setup is needed in dev.

## VS Code
Open this folder, accept the recommended extensions, then use **Run and Debug → "Full stack (API + Web)"**, or **Terminal → Run Task** (`backend: run`, `frontend: dev`, `backend: test`).

## Configuration (env vars)
| Var | Default | Purpose |
|---|---|---|
| `DB_URL` / `DB_USER` / `DB_PASS` | `jdbc:postgresql://localhost:5432/thali` / `thali` / `thali` | Database |
| `PORT` | `8081` | API port |
| `CORS_ORIGINS` | `http://localhost:5173` | Allowed web origins |
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` or `demo` |
| `VITE_API_URL` | *(empty)* | API base URL for production frontend builds |

## Auth & API docs (Phase 1)
- **Swagger UI:** http://localhost:8081/swagger-ui.html (use *Authorize* and paste an access token).
- **OTP in dev:** codes are printed in the backend console as `==== OTP [REGISTER] for b423046@iiit-bh.ac.in -> 123456 ====`. On the `demo` profile they are emailed via Brevo (`BREVO_API_KEY`, `MAIL_FROM`).
- **Dev admin** (created on first start, `dev` profile only): `admin@thali.local` / `Admin@12345`.
- **Approving a student before Phase 2 exists:**
  `psql -h localhost -U thali -d thali -c "update student_profiles set approval_status='APPROVED' where roll_no='B423046'"`
- Extra env vars: `JWT_SECRET` (**required in production**, 32+ chars), `BREVO_API_KEY`, `MAIL_FROM`, `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD`.

## Phase 2 API summary (all under `/api/v1`, see Swagger for schemas)
| Area | Endpoints | Who |
|---|---|---|
| Users | `GET/POST /users`, `PATCH /users/{id}`, `PATCH /users/{id}/status` | Admin |
| Students | `GET /students?status=&q=`, `GET /students/pending`, `PATCH /students/{id}/approval` | Admin |
| Me | `GET/PUT /me/profile`, `PUT /me/password` | Any |
| Meals | `GET /meal-slots` (any), `PUT /meal-slots/{type}` | Admin |
| Dishes | `GET /menu-items` (staff/admin), `POST/PUT/DELETE /menu-items` | Admin |
| Menus | `GET /menus?from=&to=`, `GET /menus/today` (students see published only), `PUT /menus/{date}/{type}`, `POST /menus/copy-week`, `PATCH /menus/{id}/publish` | Any / Staff+Admin |
| Bookings | `POST /bookings`, `POST /bookings/bulk`, `DELETE /bookings/{id}`, `GET /bookings/me`, `GET /bookings/{id}/qr` | Student |
| | `GET /bookings?date=&mealType=&status=&q=` | Staff+Admin |
| Attendance | `POST /attendance/scan`, `POST /attendance/manual`, `GET /attendance/live` | Staff+Admin |
| Feedback | `POST /feedback`, `GET /feedback/me`, `GET /feedback/me/pending` | Student |
| | `GET /feedback`, `GET /feedback/summary` | Staff+Admin |
| Complaints | `POST /complaints`, `GET /complaints/me` | Student |
| | `GET /complaints`, `PATCH /complaints/{id}/assign` (admin), `GET /complaints/{id}`, `POST /complaints/{id}/updates` | Staff/Admin (+owner for detail/updates) |

Rules worth knowing: booking and cancelling close at *meal start − cutoff minutes*; a cancelled meal can be re-booked (new QR); students can rate only meals they were *served*; a published menu with live bookings can't be unpublished.

**Local test data:** my verification runs left sample data in your local `thali` database (staff `ramesh@thali.local` / `Staff12345`, students `b423046@…` / `NewPass456b` and `b424001@…` / `Priya12345`, 10 dishes, a few menus and bookings). Phase 3 adds a proper seeder; to start clean: `psql -d postgres -c "DROP DATABASE thali" -c "CREATE DATABASE thali OWNER thali"`.

## Phase 3: billing, analytics, jobs, demo data
**Demo data** is loaded automatically on first start (`dev` profile, or `demo` with `SEED_DEMO_DATA=true`): 25 students (`z900001`-`z900025@iiit-bh.ac.in`, two left pending), 2 staff, 43 dishes, 6 weeks of menus, ~2,900 bookings with attendance and ~1,270 ratings, 10 complaints, 3 announcements and bills for the last two months.

| Role | Login | Password |
|---|---|---|
| Admin | `admin@thali.local` | `Admin@12345` (dev) |
| Staff | `staff1@thali.local`, `staff2@thali.local` | `Demo@1234` |
| Student | `z900002@iiit-bh.ac.in` ... `z900023@iiit-bh.ac.in` | `Demo@1234` |

Demo accounts use roll numbers `Z9000xx` so they can't clash with real students. Change the password with `DEMO_PASSWORD`. To reload demo data from scratch: drop and recreate the `thali` database (see Phase 2 notes) and restart the backend.

| Area | Endpoints | Who |
|---|---|---|
| Bills | `POST /bills/generate?month=YYYY-MM`, `GET /bills?month=&status=` | Admin |
| | `GET /bills/me` | Student |
| | `GET /bills/{id}` (with per-meal lines) | Owner / Admin |
| Payments | `POST /payments/checkout`, `POST /payments/{id}/confirm`, `GET /payments/me` | Student |
| | `POST /payments/cash`, `GET /payments?from=&to=&status=` | Admin |
| Analytics | `GET /analytics/overview`, `/revenue` | Admin |
| | `/bookings-trend`, `/meal-distribution`, `/ratings-trend`, `/top-dishes`, `/wastage` | Admin + Staff |
| Reports | `GET /reports/bills.csv?month=` | Admin |
| | `GET /reports/attendance.csv?from=&to=` | Admin + Staff |
| Notices | `GET /announcements` (any), `POST/DELETE /announcements` | Admin |

Payments use a **mock gateway** (no money moves; the UPI link is a demo payload). Scheduled jobs can be switched off with `app.scheduling.enabled=false`.

## Running the tests
```bash
cd backend && ./mvnw verify      # 72 unit + 125 integration tests; integration tests need Docker running (Testcontainers starts PostgreSQL 16)
cd frontend && npm test          # 17 Vitest tests
```
Unit tests only (no Docker): `./mvnw test`. See [TEST-SUMMARY.md](TEST-SUMMARY.md) for results and coverage.

## Phase 5: frontend foundation
**What you can do in the browser now** (http://localhost:5173)
- **Log in** at `/login` with a password *or* an emailed code; one-click demo buttons (dev, or `VITE_SHOW_DEMO=true`).
- **Register** at `/register`: college email → 6-digit code → profile and password → waits for admin approval (`/pending-approval`).
- **Forgot password** at `/forgot-password`.
- Each role lands on its own home: students `/app/home` (today's meals, latest bill, notices), staff `/app/serve` (live headcount rings), admins `/app/dashboard` (KPI tiles). Other sidebar items show a "coming in Phase N" placeholder.
- Light and dark themes (toggle in the top bar; follows your OS by default), mobile layout with bottom bar + drawer.

**How it fits together** (`frontend/src`)
| Folder | Purpose |
|---|---|
| `api/client.ts` | axios instance: attaches the access token, and on a 401 refreshes it **once** (shared by concurrent requests) and retries; if refresh fails the user is signed out. Every failure becomes an `ApiError` with the backend's `code`. |
| `store/` | Zustand stores: `authStore` (tokens + user, persisted) and `themeStore`. |
| `components/ui/` | Reusable primitives: Button, Input, OtpInput, Tabs, StatTile, Skeleton, ... |
| `components/layout/` | App shell: sidebar, top bar, mobile bottom nav + drawer; `nav.ts` is the single list of menu items per role. |
| `components/guards/` | `ProtectedRoute` (login + role check), `PublicOnlyRoute`, `RoleRedirect`. |
| `features/<area>/` | Pages, split per role and lazy-loaded (a student never downloads the admin dashboard). |
| `styles/globals.css`, `tailwind.config.ts` | Design tokens as CSS variables for the ivory/forest/terracotta light theme and the "Midnight Forest" dark theme. |

Frontend env vars: `VITE_API_URL` (empty in dev; the deployed API URL in production) and `VITE_SHOW_DEMO` (`true` shows the demo-account buttons on the login page).
**Security note:** tokens are kept in `localStorage` so a refresh keeps you signed in; the access token lives 15 minutes and refresh tokens rotate, but a hardened deployment would use an httpOnly cookie.
