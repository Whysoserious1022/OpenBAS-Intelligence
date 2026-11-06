package com.openbas.intelligence.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for canonical telemetry events.
 * Maps to a TimescaleDB hypertable partitioned by event_timestamp.
 * Every telemetry reading from any protocol is normalized into this structure.
 */
@Entity
@Table(name = "telemetry_events", indexes = {
    @Index(name = "idx_telemetry_device_id_ts", columnList = "device_id, event_timestamp DESC"),
    @Index(name = "idx_telemetry_equipment_id_ts", columnList = "equipment_id, event_timestamp DESC"),
    @Index(name = "idx_telemetry_zone_id_ts", columnList = "zone_id, event_timestamp DESC"),
    @Index(name = "idx_telemetry_building_id_ts", columnList = "building_id, event_timestamp DESC"),
    @Index(name = "idx_telemetry_point_id", columnList = "point_id"),
    @Index(name = "idx_telemetry_quality", columnList = "data_quality"),
    @Index(name = "idx_telemetry_ingested_at", columnList = "ingested_at")
})
public class TelemetryEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ── Canonical identifiers ────────────────────────────────────────────────
    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "equipment_id", nullable = false, length = 100)
    private String equipmentId;

    @Column(name = "point_id", nullable = false, length = 100)
    private String pointId;

    @Column(name = "building_id", nullable = false, length = 100)
    private String buildingId;

    @Column(name = "floor_id", length = 100)
    private String floorId;

    @Column(name = "zone_id", length = 100)
    private String zoneId;

    // ── Measurement ───────────────────────────────────────────────────────────
    @Column(name = "numeric_value")
    private Double numericValue;

    @Column(name = "string_value", length = 500)
    private String stringValue;

    @Column(name = "boolean_value")
    private Boolean booleanValue;

    @Column(name = "unit", length = 30)
    private String unit;  // CELSIUS, FAHRENHEIT, PERCENT, KW, CFM, PASCAL, etc.

    // ── Timestamps ────────────────────────────────────────────────────────────
    /** The timestamp when the measurement was taken (device time, normalized to UTC) */
    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    /** The timestamp when this event arrived at the ingestion pipeline */
    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    // ── Quality and provenance ─────────────────────────────────────────────────
    @Column(name = "data_quality", nullable = false, length = 30)
    private String dataQuality;  // GOOD, BAD, UNCERTAIN, STALE, SIMULATED

    @Column(name = "communication_status", length = 30)
    private String communicationStatus;  // ONLINE, OFFLINE, DEGRADED

    @Column(name = "source_protocol", nullable = false, length = 30)
    private String sourceProtocol;  // BACNET_IP, MQTT, OPC_UA, SIMULATED

    @Column(name = "source_device", length = 100)
    private String sourceDevice;

    @Column(name = "is_duplicate", nullable = false)
    private boolean duplicate = false;

    @Column(name = "is_out_of_order", nullable = false)
    private boolean outOfOrder = false;

    public TelemetryEventEntity() {}

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }
    public String getPointId() { return pointId; }
    public void setPointId(String pointId) { this.pointId = pointId; }
    public String getBuildingId() { return buildingId; }
    public void setBuildingId(String buildingId) { this.buildingId = buildingId; }
    public String getFloorId() { return floorId; }
    public void setFloorId(String floorId) { this.floorId = floorId; }
    public String getZoneId() { return zoneId; }
    public void setZoneId(String zoneId) { this.zoneId = zoneId; }
    public Double getNumericValue() { return numericValue; }
    public void setNumericValue(Double numericValue) { this.numericValue = numericValue; }
    public String getStringValue() { return stringValue; }
    public void setStringValue(String stringValue) { this.stringValue = stringValue; }
    public Boolean getBooleanValue() { return booleanValue; }
    public void setBooleanValue(Boolean booleanValue) { this.booleanValue = booleanValue; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public Instant getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(Instant eventTimestamp) { this.eventTimestamp = eventTimestamp; }
    public Instant getIngestedAt() { return ingestedAt; }
    public void setIngestedAt(Instant ingestedAt) { this.ingestedAt = ingestedAt; }
    public String getDataQuality() { return dataQuality; }
    public void setDataQuality(String dataQuality) { this.dataQuality = dataQuality; }
    public String getCommunicationStatus() { return communicationStatus; }
    public void setCommunicationStatus(String communicationStatus) { this.communicationStatus = communicationStatus; }
    public String getSourceProtocol() { return sourceProtocol; }
    public void setSourceProtocol(String sourceProtocol) { this.sourceProtocol = sourceProtocol; }
    public String getSourceDevice() { return sourceDevice; }
    public void setSourceDevice(String sourceDevice) { this.sourceDevice = sourceDevice; }
    public boolean isDuplicate() { return duplicate; }
    public void setDuplicate(boolean duplicate) { this.duplicate = duplicate; }
    public boolean isOutOfOrder() { return outOfOrder; }
    public void setOutOfOrder(boolean outOfOrder) { this.outOfOrder = outOfOrder; }
}
