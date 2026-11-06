-- ============================================================
-- V2: Equipment and Device Schema
-- Creates: equipment, devices
-- ============================================================

CREATE TABLE IF NOT EXISTS equipment (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id               VARCHAR(100) NOT NULL UNIQUE,
    zone_id                   UUID REFERENCES zones(id),
    building_id               UUID NOT NULL REFERENCES buildings(id),
    floor_id                  UUID REFERENCES floors(id),
    name                      VARCHAR(200) NOT NULL,
    equipment_type            VARCHAR(50) NOT NULL,
    manufacturer              VARCHAR(100),
    model                     VARCHAR(100),
    serial_number             VARCHAR(100),
    installation_date         TIMESTAMPTZ,
    operational_status        VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN',
    health_score              DOUBLE PRECISION,
    min_operating_temp_celsius DOUBLE PRECISION,
    max_operating_temp_celsius DOUBLE PRECISION,
    rated_power_kw            DOUBLE PRECISION,
    rated_airflow_cfm         DOUBLE PRECISION,
    simulation_enabled        BOOLEAN NOT NULL DEFAULT FALSE,
    active                    BOOLEAN NOT NULL DEFAULT TRUE,
    parent_equipment_id       UUID REFERENCES equipment(id),
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                   BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_equipment_zone_id ON equipment(zone_id);
CREATE INDEX idx_equipment_building_id ON equipment(building_id);
CREATE INDEX idx_equipment_external_id ON equipment(external_id);
CREATE INDEX idx_equipment_type ON equipment(equipment_type);
CREATE INDEX idx_equipment_operational_status ON equipment(operational_status);
CREATE INDEX idx_equipment_parent ON equipment(parent_equipment_id);

COMMENT ON TABLE equipment IS 'Physical building equipment: AHU, VAV, Chiller, Pump, Fan, Sensor, Controller, Energy Meter';
COMMENT ON COLUMN equipment.equipment_type IS 'AHU | VAV | CHILLER | PUMP | FAN | SENSOR | CONTROLLER | ENERGY_METER';
COMMENT ON COLUMN equipment.operational_status IS 'NORMAL | DEGRADED | FAULT | OFFLINE | MAINTENANCE | UNKNOWN';
COMMENT ON COLUMN equipment.parent_equipment_id IS 'Dependency relationship: VAV parent is AHU; AHU parent is Chiller';

-- ────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS devices (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id            VARCHAR(100) NOT NULL UNIQUE,
    equipment_id           UUID NOT NULL REFERENCES equipment(id) ON DELETE CASCADE,
    name                   VARCHAR(200) NOT NULL,
    protocol               VARCHAR(30) NOT NULL,
    bacnet_device_id       INTEGER,
    bacnet_ip_address      VARCHAR(50),
    bacnet_port            INTEGER DEFAULT 47808,
    mqtt_topic_prefix      VARCHAR(200),
    communication_status   VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN',
    last_seen_at           TIMESTAMPTZ,
    last_error_at          TIMESTAMPTZ,
    last_error_message     VARCHAR(500),
    simulated              BOOLEAN NOT NULL DEFAULT FALSE,
    active                 BOOLEAN NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_devices_equipment_id ON devices(equipment_id);
CREATE INDEX idx_devices_external_id ON devices(external_id);
CREATE INDEX idx_devices_protocol ON devices(protocol);
CREATE INDEX idx_devices_status ON devices(communication_status);

COMMENT ON TABLE devices IS 'Physical or simulated devices that communicate via industrial protocols';
COMMENT ON COLUMN devices.protocol IS 'BACNET_IP | MQTT | OPC_UA | SIMULATED';
COMMENT ON COLUMN devices.communication_status IS 'ONLINE | OFFLINE | DEGRADED | UNKNOWN';
