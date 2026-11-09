# Test summary — Phase 4 verification (9 Nov 2026)

## Result
| Suite | Command | Tests | Failures | Errors | Skipped | Time |
|---|---|---|---|---|---|---|
| Backend unit (Surefire) | `./mvnw verify` | 72 | 0 | 0 | 0 | 6 s |
| Backend integration (Failsafe, real PostgreSQL 16) | `./mvnw verify` | 125 | 0 | 0 | 0 | 56 s |
| Frontend (Vitest + Testing Library) | `npm test` | 162 | 0 | 0 | 0 | 14 s |
| **Total** | | **359** | **0** | **0** | **0** | |

`./mvnw clean verify` → **BUILD SUCCESS** in 1 min 15 s, 0 compiler warnings, 0 compile errors. `npm run build` (type-check + production bundle) passes.

## Skipped tests
None. There are no `@Disabled`, `@Ignore` or assumption-based tests, and every test class that exists ran.

## Testcontainers
`AbstractIntegrationTest` starts one `postgres:16-alpine` container (plus Testcontainers' Ryuk reaper) for the whole run, applies the real Flyway migrations, and truncates all tables before every test. All 7 integration classes share one container and one Spring context. Verified: the container starts (log: *Creating container for image: postgres:16-alpine*), and **no postgres/ryuk containers remain** a few seconds after Maven exits (Ryuk removes them). Docker Desktop must be running. The integration tests never touch your local `thali` database.

## What is covered
**Unit (Mockito, fixed clock)** — booking cutoff at the exact second, re-booking after cancel (new QR, current price), cancellation rules, ownership (404 not 403), bulk partial success; attendance window/grace, double-scan, cancelled/no-show, live counts; billing math (meals + fixed charge, due dates, paid bills untouched, future months refused); payments (checkout/confirm/cash, idempotent confirm, amount-changed, already-paid); OTP (hash-only storage, cooldown, hourly limit, attempt lock, expiry); no-show sweep; email policy; JWT kinds.

**Integration (full app + MockMvc + real Postgres)**
- `AuthFlowIT` (17): OTP register → approve → login → refresh rotation → logout; college-email enforcement; OTP lock/cooldown/single-use; pending/rejected/disabled login; password reset kills sessions; token misuse; error shapes.
- `SecurityMatrixIT` (56): every endpoint called anonymously and as admin/staff/student — 401 for anonymous, 403 for disallowed roles.
- `BookingFlowIT` (17): publish → book → QR → scan → rate; cutoff boundary; serving window; frozen price; draft menus; unpublish guard; bulk; copy-week; staff search.
- `BillingFlowIT` (12): exact bill math from consumed/no-show/cancelled/stale-booked meals, idempotent regeneration, online + cash payment, overdue sweep, filters, CSV (incl. formula-injection escaping).
- `AnalyticsIT` (9), `ComplaintFlowIT` (6), `AdminUsersIT` (8).

**Frontend (135, updated in Phase 5)** — API client (bearer header, refresh-and-retry, one shared refresh for concurrent 401s, forced sign-out when refresh fails, no refresh loops, error normalisation); stores; validation and formatting helpers; OTP input (typing, backspace, arrows, paste, autofill); UI primitives; route guards for all three roles; login (password, email code, return-to-page, pending/rejected messages); 3-step registration (validation, OTP, server field errors, expired token); forgot password; the three role home pages (data, empty states, error states); landing page; shell navigation and logout.

## Real bug found and fixed by these tests
Bulk booking returned **500** (and discarded the bookings that had succeeded) when any item had no menu. The "menu not found" exception passed through `MenuService.require`'s transactional proxy and marked the caller's transaction rollback-only. Fixed with `noRollbackFor = ApiException.class` on `MenuService.require` and `MealSlotService.get`. My earlier manual test missed it; `BookingFlowIT.bulkBookingKeepsWhatWorkedAndExplainsTheRest` caught it.

## Do the tests actually catch bugs? (mutation spot-check)
- Changed the booking cutoff from "closed at the cutoff" to "closed after it" → 2 unit tests failed. Reverted.
- Removed the admin-only guard from `/analytics/overview` → `SecurityMatrixIT` failed ("STAFF should be forbidden"). Reverted.
- Reverting the rollback fix reproduces the failing bulk-booking test (seen on the first run).

## Known gaps / notes
- No browser/UI end-to-end tests yet (the real UI is built in Phases 5–8).
- The Brevo email sender (`demo` profile) and the scheduled-job triggers (`@Scheduled`) are not exercised; their logic (no-show sweep, overdue sweep) is tested by calling the methods directly.
- `npm audit` (production deps) reports 2 moderate `react-router` advisories (SSR hydration and `<Link>` backslash redirect). The installed 6.30.6 is the latest 6.x and the fixes exist only in v7; the app is client-only and has no user-controlled link targets. Revisit when the real UI is built.
- Mockito prints a harmless "dynamic agent" JDK warning on Java 21.

## Phase 5 additions: bugs found and fixed while building the frontend
- **OTP autofill broken:** each OTP box had `maxLength=1`, so a code autofilled from SMS/email (which lands in the first box) was truncated to one digit. Found by typing a code in a real browser; fixed, with a regression test.
- **"Return to the page you were trying to open" never worked:** storing the session re-rendered `PublicOnlyRoute` synchronously, which redirected to the role home before the login form's own navigation ran. Caught by a test; the guard now owns the redirect.
- **Error parsing depended on axios class identity** (`instanceof AxiosError`); now uses `axios.isAxiosError()`, which also works across package copies.
- Admin sidebar clipped "Log out" on short screens; the nav list now scrolls independently. The nav pill wrapped "Log in" on narrow phones.
- Stale Vite dependency cache after installing packages caused "invalid hook call"; fixed with a cache reset and `resolve.dedupe` for React.

## Phases 7-9
Frontend tests grew to 162 (staff counter, menu editing, complaints queue, approvals, billing, meal settings, dishes, staff accounts, announcements, feedback, menu builder). Verified live in the browser: dashboard charts, student approval, bill generation, staff check-in with a real pass (a repeat scan is refused), menu editing, staff complaints queue. Not testable here: the camera scanner needs a real camera (the pasted-code and manual paths are tested). The backend suite was not re-run (no backend changes since Phase 4).
