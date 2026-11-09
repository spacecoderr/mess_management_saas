-- Phase 0 baseline. Real tables are added in Phase 1 (V2__init_schema.sql).
CREATE TABLE app_info (
    id          SMALLINT PRIMARY KEY,
    app_name    VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
INSERT INTO app_info (id, app_name) VALUES (1, 'Thali - IIIT Bhubaneswar Mess Management');
