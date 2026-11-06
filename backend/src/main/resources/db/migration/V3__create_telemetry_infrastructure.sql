-- ============================================================
-- V3: Telemetry Infrastructure
-- Creates: telemetry_events (TimescaleDB hypertable)
--          anomaly_events (TimescaleDB hypertable)
-- Note: TimescaleDB extension is required.
--       If not installed, falls back to standard PostgreSQL table.
-- ============================================================

CREATE TABLE IF NOT EXISTS telemetry_events (
    id                   UUID NOT NULL DEFAULT gen_random_uuid(),
    device_id            VARCHAR(100) NOT NULL,
    equipment_id         VARCHAR(100) NOT NULL,
    point_id             VARCHAR(100) NOT NULL,
    building_id          VARCHAR(100) NOT NULL,
    floor_id             VARCHAR(100),
    zone_id              VARCHAR(100),
    numeric_value        DOUBLE PRECISION,
    string_value         VARCHAR(500),
    boolean_value        BOOLEAN,
    unit                 VARCHAR(30),
    event_timestamp      TIMESTAMPTZ NOT NULL,
    ingested_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    data_quality         VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN',
    communication_status VARCHAR(30),
    source_protocol      VARCHAR(30) NOT NULL,
    source_device        VARCHAR(100),
    is_duplicate         BOOLEAN NOT NULL DEFAULT FALSE,
    is_out_of_order      BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id, event_timestamp)
);

CREATE INDEX idx_telemetry_device_point_ts ON telemetry_events(device_id, point_id, event_timestamp DESC);
CREATE INDEX idx_telemetry_equipment_id_ts ON telemetry_events(equipment_id, event_timestamp DESC);
CREATE INDEX idx_telemetry_zone_id_ts ON telemetry_events(zone_id, event_timestamp DESC);
CREATE INDEX idx_telemetry_building_id_ts ON telemetry_events(building_id, event_timestamp DESC);
CREATE INDEX idx_telemetry_quality ON telemetry_events(data_quality);
CREATE INDEX idx_telemetry_ingested_at ON telemetry_events(ingested_at DESC);

COMMENT ON TABLE telemetry_events IS 'Canonical time-series telemetry. All protocol readings normalized here.';
COMMENT ON COLUMN telemetry_events.event_timestamp IS 'Device measurement time, normalized to UTC';
COMMENT ON COLUMN telemetry_events.ingested_at IS 'Pipeline ingestion time (may differ from event_timestamp)';
COMMENT ON COLUMN telemetry_events.data_quality IS 'GOOD | BAD | UNCERTAIN | STALE | SIMULATED';

-- Attempt to create TimescaleDB hypertable
-- If TimescaleDB is not available, this will fail gracefully (the table still works as plain PostgreSQL)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'timescaledb') THEN
        PERFORM create_hypertable('telemetry_events', 'event_timestamp',
                                  chunk_time_interval => INTERVAL '1 day',
                                  if_not_exists => TRUE);
        RAISE NOTICE 'TimescaleDB hypertable created for telemetry_events';
    ELSE
        RAISE NOTICE 'TimescaleDB not available. telemetry_events will use standard PostgreSQL partitioning.';
    END IF;
END $$;

-- ────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS anomaly_events (
    id                   UUID NOT NULL DEFAULT gen_random_uuid(),
    equipment_id         VARCHAR(100) NOT NULL,
    device_id            VARCHAR(100),
    zone_id              VARCHAR(100),
    building_id          VARCHAR(100) NOT NULL,
    anomaly_type         VARCHAR(50) NOT NULL,
    severity             VARCHAR(20) NOT NULL,
    point_id             VARCHAR(100),
    observed_value       DOUBLE PRECISION,
    expected_min         DOUBLE PRECISION,
    expected_max         DOUBLE PRECISION,
    threshold_violated   VARCHAR(100),
    detection_method     VARCHAR(50) NOT NULL,
    evidence             TEXT,
    description          VARCHAR(1000),
    detected_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved             BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at          TIMESTAMPTZ,
    resolution_note      VARCHAR(500),
    linked_command_id    UUID,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, detected_at)
);

CREATE INDEX idx_anomaly_equipment_id_ts ON anomaly_events(equipment_id, detected_at DESC);
CREATE INDEX idx_anomaly_building_id ON anomaly_events(building_id);
CREATE INDEX idx_anomaly_severity ON anomaly_events(severity);
CREATE INDEX idx_anomaly_type ON anomaly_events(anomaly_type);
CREATE INDEX idx_anomaly_resolved ON anomaly_events(resolved);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'timescaledb') THEN
        PERFORM create_hypertable('anomaly_events', 'detected_at',
                                  chunk_time_interval => INTERVAL '7 days',
                                  if_not_exists => TRUE);
        RAISE NOTICE 'TimescaleDB hypertable created for anomaly_events';
    END IF;
END $$;
