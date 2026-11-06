-- ============================================================
-- V1: Building Hierarchy Schema
-- Creates: buildings, floors, zones
-- ============================================================

CREATE TABLE IF NOT EXISTS buildings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id     VARCHAR(100) NOT NULL UNIQUE,
    name            VARCHAR(200) NOT NULL,
    description     VARCHAR(1000),
    address         VARCHAR(500),
    city            VARCHAR(100),
    country         VARCHAR(100),
    timezone        VARCHAR(50) DEFAULT 'UTC',
    total_area_sqm  DOUBLE PRECISION,
    floor_count     INTEGER,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    simulation_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_buildings_external_id ON buildings(external_id);
CREATE INDEX idx_buildings_active ON buildings(active);

COMMENT ON TABLE buildings IS 'Physical buildings managed by the OpenBAS Intelligence platform';
COMMENT ON COLUMN buildings.external_id IS 'Human-readable identifier used by operators and simulators (e.g., BUILDING-01)';
COMMENT ON COLUMN buildings.simulation_enabled IS 'True when this building is driven by the simulator rather than real devices';

-- ────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS floors (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id     VARCHAR(100) NOT NULL UNIQUE,
    building_id     UUID NOT NULL REFERENCES buildings(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    floor_number    INTEGER NOT NULL,
    description     VARCHAR(500),
    area_sqm        DOUBLE PRECISION,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_floors_building_id ON floors(building_id);
CREATE INDEX idx_floors_external_id ON floors(external_id);
CREATE UNIQUE INDEX idx_floors_building_floor_num ON floors(building_id, floor_number);

COMMENT ON TABLE floors IS 'Floors within a building';

-- ────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS zones (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id               VARCHAR(100) NOT NULL UNIQUE,
    floor_id                  UUID NOT NULL REFERENCES floors(id) ON DELETE CASCADE,
    building_id               UUID NOT NULL REFERENCES buildings(id),
    name                      VARCHAR(100) NOT NULL,
    zone_type                 VARCHAR(50),
    area_sqm                  DOUBLE PRECISION,
    design_occupancy          INTEGER,
    comfort_temp_min_celsius  DOUBLE PRECISION DEFAULT 20.0,
    comfort_temp_max_celsius  DOUBLE PRECISION DEFAULT 26.0,
    comfort_humidity_min_pct  DOUBLE PRECISION DEFAULT 30.0,
    comfort_humidity_max_pct  DOUBLE PRECISION DEFAULT 60.0,
    active                    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_zones_floor_id ON zones(floor_id);
CREATE INDEX idx_zones_building_id ON zones(building_id);
CREATE INDEX idx_zones_external_id ON zones(external_id);

COMMENT ON TABLE zones IS 'Thermal and occupancy zones within a floor';
COMMENT ON COLUMN zones.zone_type IS 'OFFICE, CONFERENCE, LOBBY, SERVER_ROOM, CORRIDOR, etc.';
