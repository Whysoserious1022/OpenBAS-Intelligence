-- ============================================================
-- V4: Command Lifecycle and Audit History
-- Creates: commands, command_audit_events
-- ============================================================

CREATE TABLE IF NOT EXISTS commands (
    id                            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key               VARCHAR(100) NOT NULL UNIQUE,
    target_equipment_id           VARCHAR(100) NOT NULL,
    target_device_id              VARCHAR(100),
    target_point_id               VARCHAR(100) NOT NULL,
    building_id                   VARCHAR(100) NOT NULL,
    command_type                  VARCHAR(50) NOT NULL,
    current_value                 DOUBLE PRECISION,
    proposed_value                DOUBLE PRECISION,
    proposed_string_value         VARCHAR(100),
    unit                          VARCHAR(30),
    reason                        VARCHAR(1000) NOT NULL,
    expected_benefit              VARCHAR(500),
    potential_risk                VARCHAR(500),
    initiating_agent              VARCHAR(100),
    agent_analysis_id             VARCHAR(100),
    confidence                    DOUBLE PRECISION,
    status                        VARCHAR(30) NOT NULL DEFAULT 'PROPOSED',
    policy_validation_result      VARCHAR(30),
    policy_rejection_reason       VARCHAR(1000),
    digital_twin_simulated        BOOLEAN NOT NULL DEFAULT FALSE,
    digital_twin_predicted_outcome VARCHAR(500),
    required_approval_level       VARCHAR(20) NOT NULL DEFAULT 'OPERATOR',
    approved_by                   VARCHAR(100),
    approved_at                   TIMESTAMPTZ,
    rejected_by                   VARCHAR(100),
    rejected_at                   TIMESTAMPTZ,
    rejection_reason              VARCHAR(500),
    executed_at                   TIMESTAMPTZ,
    execution_result              VARCHAR(500),
    verified_value                DOUBLE PRECISION,
    verified_at                   TIMESTAMPTZ,
    expires_at                    TIMESTAMPTZ,
    proposed_by                   VARCHAR(100) NOT NULL,
    created_at                    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                       BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_commands_status ON commands(status);
CREATE INDEX idx_commands_equipment_id ON commands(target_equipment_id);
CREATE INDEX idx_commands_building_id ON commands(building_id);
CREATE INDEX idx_commands_created_at ON commands(created_at DESC);
CREATE INDEX idx_commands_expires_at ON commands(expires_at);
CREATE INDEX idx_commands_proposed_by ON commands(proposed_by);

COMMENT ON TABLE commands IS 'Building control commands. AI agents submit proposals; humans approve; policy engine validates.';
COMMENT ON COLUMN commands.status IS 'PROPOSED|VALIDATED|PENDING_APPROVAL|APPROVED|EXECUTING|EXECUTED|REJECTED|EXPIRED|FAILED|CANCELLED|VERIFICATION_FAILED';
COMMENT ON COLUMN commands.idempotency_key IS 'Prevents duplicate command submission from retries or agent loops';

-- ────────────────────────────────────────────────────────────
-- Immutable audit trail for every command state transition
-- ────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS command_audit_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    command_id      UUID NOT NULL REFERENCES commands(id),
    event_type      VARCHAR(50) NOT NULL,
    from_status     VARCHAR(30),
    to_status       VARCHAR(30) NOT NULL,
    actor           VARCHAR(100),
    actor_role      VARCHAR(50),
    details         TEXT,
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cmd_audit_command_id ON command_audit_events(command_id);
CREATE INDEX idx_cmd_audit_occurred_at ON command_audit_events(occurred_at DESC);

COMMENT ON TABLE command_audit_events IS 'Immutable audit trail for command lifecycle transitions. Never update or delete rows.';
