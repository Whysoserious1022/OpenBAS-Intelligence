-- ============================================================
-- V5: Application Users and Initial Seed Data
-- ============================================================

CREATE TABLE IF NOT EXISTS app_users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(100) NOT NULL UNIQUE,
    email           VARCHAR(200) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(200),
    role            VARCHAR(30) NOT NULL DEFAULT 'VIEWER',
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_username ON app_users(username);
CREATE INDEX idx_users_email ON app_users(email);

COMMENT ON COLUMN app_users.role IS 'ADMIN | OPERATOR | VIEWER | AI_SERVICE';
COMMENT ON COLUMN app_users.password_hash IS 'BCrypt hashed password. Never store plaintext.';

-- ─────────────────────────────────────────────────────────────
-- Seed: Default users (passwords are BCrypt hashes)
-- admin: admin123    (CHANGE IN PRODUCTION)
-- operator: operator123
-- viewer: viewer123
-- ─────────────────────────────────────────────────────────────
INSERT INTO app_users (username, email, password_hash, full_name, role)
VALUES
  ('admin',
   'admin@openbas.local',
   '$2a$12$LF3YO5TE8LWVR1i/WFe4NeClLGnBzj4FZbPHnFGQ8bDmV1OcZ1R2',
   'System Administrator',
   'ADMIN'),
  ('operator',
   'operator@openbas.local',
   '$2a$12$QVC9Zu7E3KsW5B.pBXI3zeKb3XFMiLQLbTTpzMvK.FT8M5PFqj3rC',
   'Building Operator',
   'OPERATOR'),
  ('viewer',
   'viewer@openbas.local',
   '$2a$12$kNOXL6J8bJvxTOLkdPGMr.yXsJ8bMH5G01cLKNE5pxhDQNNZT3hVS',
   'Dashboard Viewer',
   'VIEWER')
ON CONFLICT (username) DO NOTHING;

-- ─────────────────────────────────────────────────────────────
-- Seed: Nexus Tower Building (simulated)
-- ─────────────────────────────────────────────────────────────
INSERT INTO buildings (external_id, name, description, address, city, country, timezone, total_area_sqm, floor_count, simulation_enabled)
VALUES (
  'BUILDING-01',
  'Nexus Tower',
  'Primary demonstration building - 5 floors, 20 zones. Fully simulated.',
  '1 Innovation Drive',
  'Silicon City',
  'US',
  'America/Los_Angeles',
  12000.0,
  5,
  TRUE
) ON CONFLICT (external_id) DO NOTHING;
