-- Phase 1: full domain schema. Enums are VARCHAR + CHECK so migrations stay simple.

CREATE TABLE users (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(120) NOT NULL,
    email          VARCHAR(160) NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,
    role           VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN','STAFF','STUDENT')),
    phone          VARCHAR(20),
    email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE student_profiles (
    user_id         BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    roll_no         VARCHAR(20) NOT NULL UNIQUE,
    batch_year      SMALLINT,
    hostel_block    VARCHAR(30),
    room_no         VARCHAR(20),
    diet_pref       VARCHAR(20) NOT NULL DEFAULT 'VEG' CHECK (diet_pref IN ('VEG','NON_VEG','JAIN')),
    approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (approval_status IN ('PENDING','APPROVED','REJECTED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE otp_codes (
    id         BIGSERIAL PRIMARY KEY,
    email      VARCHAR(160) NOT NULL,
    purpose    VARCHAR(20)  NOT NULL CHECK (purpose IN ('REGISTER','LOGIN','RESET_PASSWORD')),
    code_hash  VARCHAR(100) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    attempts   INT          NOT NULL DEFAULT 0,
    consumed   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_otp_email_purpose ON otp_codes (email, purpose, created_at DESC);

CREATE TABLE refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,   -- SHA-256 hex of the opaque token
    expires_at TIMESTAMPTZ NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_user ON refresh_tokens (user_id);

CREATE TABLE meal_slot_config (
    meal_type      VARCHAR(20) PRIMARY KEY CHECK (meal_type IN ('BREAKFAST','LUNCH','SNACKS','DINNER')),
    start_time     TIME          NOT NULL,
    end_time       TIME          NOT NULL,
    cutoff_minutes INT           NOT NULL DEFAULT 240,
    price          NUMERIC(8,2)  NOT NULL CHECK (price >= 0),
    active         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE menu_items (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(500),
    category    VARCHAR(20)  NOT NULL CHECK (category IN ('MAIN','SIDE','BREAD','RICE','DESSERT','BEVERAGE')),
    is_veg      BOOLEAN      NOT NULL DEFAULT TRUE,
    image_url   VARCHAR(500),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE daily_menus (
    id         BIGSERIAL PRIMARY KEY,
    menu_date  DATE        NOT NULL,
    meal_type  VARCHAR(20) NOT NULL REFERENCES meal_slot_config(meal_type),
    published  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_by BIGINT      REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (menu_date, meal_type)
);

CREATE TABLE daily_menu_items (
    daily_menu_id BIGINT NOT NULL REFERENCES daily_menus(id) ON DELETE CASCADE,
    menu_item_id  BIGINT NOT NULL REFERENCES menu_items(id),
    PRIMARY KEY (daily_menu_id, menu_item_id)
);

CREATE TABLE bookings (
    id               BIGSERIAL PRIMARY KEY,
    student_id       BIGINT       NOT NULL REFERENCES users(id),
    daily_menu_id    BIGINT       NOT NULL REFERENCES daily_menus(id),
    status           VARCHAR(20)  NOT NULL DEFAULT 'BOOKED' CHECK (status IN ('BOOKED','CANCELLED','CONSUMED','NO_SHOW')),
    price_at_booking NUMERIC(8,2) NOT NULL,
    qr_token         UUID         NOT NULL UNIQUE,
    cancelled_at     TIMESTAMPTZ,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (student_id, daily_menu_id)
);
CREATE INDEX idx_bookings_menu_status ON bookings (daily_menu_id, status);

CREATE TABLE attendance (
    id         BIGSERIAL PRIMARY KEY,
    booking_id BIGINT      NOT NULL UNIQUE REFERENCES bookings(id),
    marked_by  BIGINT      NOT NULL REFERENCES users(id),
    marked_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    method     VARCHAR(10) NOT NULL CHECK (method IN ('QR','MANUAL'))
);

CREATE TABLE feedback (
    id            BIGSERIAL PRIMARY KEY,
    student_id    BIGINT      NOT NULL REFERENCES users(id),
    daily_menu_id BIGINT      NOT NULL REFERENCES daily_menus(id),
    rating        SMALLINT    NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment       VARCHAR(1000),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (student_id, daily_menu_id)
);

CREATE TABLE complaints (
    id          BIGSERIAL PRIMARY KEY,
    student_id  BIGINT       NOT NULL REFERENCES users(id),
    assigned_to BIGINT       REFERENCES users(id),
    category    VARCHAR(20)  NOT NULL CHECK (category IN ('FOOD_QUALITY','HYGIENE','SERVICE','BILLING','OTHER')),
    title       VARCHAR(150) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    priority    VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW','MEDIUM','HIGH')),
    status      VARCHAR(20)  NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','IN_PROGRESS','RESOLVED','REJECTED')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_complaints_status ON complaints (status);

CREATE TABLE complaint_updates (
    id           BIGSERIAL PRIMARY KEY,
    complaint_id BIGINT       NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    author_id    BIGINT       NOT NULL REFERENCES users(id),
    message      VARCHAR(1000) NOT NULL,
    new_status   VARCHAR(20)  CHECK (new_status IN ('OPEN','IN_PROGRESS','RESOLVED','REJECTED')),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE bills (
    id            BIGSERIAL PRIMARY KEY,
    student_id    BIGINT        NOT NULL REFERENCES users(id),
    bill_month    DATE          NOT NULL,           -- first day of the month
    meal_count    INT           NOT NULL DEFAULT 0,
    meal_charges  NUMERIC(10,2) NOT NULL DEFAULT 0,
    fixed_charges NUMERIC(10,2) NOT NULL DEFAULT 0,
    total         NUMERIC(10,2) NOT NULL DEFAULT 0,
    status        VARCHAR(10)   NOT NULL DEFAULT 'UNPAID' CHECK (status IN ('UNPAID','PAID','OVERDUE')),
    due_date      DATE          NOT NULL,
    generated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version       BIGINT        NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    UNIQUE (student_id, bill_month)
);

CREATE TABLE payments (
    id          BIGSERIAL PRIMARY KEY,
    bill_id     BIGINT        NOT NULL REFERENCES bills(id),
    amount      NUMERIC(10,2) NOT NULL CHECK (amount > 0),
    method      VARCHAR(10)   NOT NULL CHECK (method IN ('UPI','CARD','CASH')),
    txn_ref     VARCHAR(64)   NOT NULL UNIQUE,
    status      VARCHAR(10)   NOT NULL CHECK (status IN ('PENDING','SUCCESS','FAILED')),
    paid_at     TIMESTAMPTZ,
    recorded_by BIGINT        REFERENCES users(id),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE announcements (
    id         BIGSERIAL PRIMARY KEY,
    title      VARCHAR(150)  NOT NULL,
    body       VARCHAR(2000) NOT NULL,
    author_id  BIGINT        NOT NULL REFERENCES users(id),
    pinned     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);
