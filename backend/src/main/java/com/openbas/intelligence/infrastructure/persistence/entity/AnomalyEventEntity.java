package com.openbas.intelligence.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for detected anomaly events.
 * Maps to a TimescaleDB hypertable for efficient time-series queries.
 */
@Entity
@Table(name = "anomaly_events", indexes = {
    @Index(name = "idx_anomaly_equipment_id_ts", columnList = "equipment_id, detected_at DESC"),
    @Index(name = "idx_anomaly_building_id", columnList = "building_id"),
    @Index(name = "idx_anomaly_severity", columnList = "severity"),
    @Index(name = "idx_anomaly_type", columnList = "anomaly_type"),
    @Index(name = "idx_anomaly_resolved", columnList = "resolved")
})
public class AnomalyEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "equipment_id", nullable = false, length = 100)
    private String equipmentId;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "zone_id", length = 100)
    private String zoneId;

    @Column(name = "building_id", nullable = false, length = 100)
    private String buildingId;

    @Column(name = "anomaly_type", nullable = false, length = 50)
    private String anomalyType;
    // TEMP_HIGH, TEMP_LOW, POWER_HIGH, POWER_LOW, AIRFLOW_LOW, STALE_READING,
    // COMM_FAILURE, RAPID_CHANGE, UNUSUAL_CYCLE, HEALTH_DEGRADATION

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;  // CRITICAL, HIGH, MEDIUM, LOW, INFO

    @Column(name = "point_id", length = 100)
    private String pointId;

    @Column(name = "observed_value")
    private Double observedValue;

    @Column(name = "expected_min")
    private Double expectedMin;

    @Column(name = "expected_max")
    private Double expectedMax;

    @Column(name = "threshold_violated", length = 100)
    private String thresholdViolated;

    @Column(name = "detection_method", nullable = false, length = 50)
    private String detectionMethod;
    // THRESHOLD, MOVING_AVERAGE, ROLLING_STDDEV, RATE_OF_CHANGE, MISSING_DATA, ML_MODEL

    @Column(name = "evidence", length = 2000)
    private String evidence;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "resolved", nullable = false)
    private boolean resolved = false;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;

    @Column(name = "linked_command_id")
    private UUID linkedCommandId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AnomalyEventEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getZoneId() { return zoneId; }
    public void setZoneId(String zoneId) { this.zoneId = zoneId; }
    public String getBuildingId() { return buildingId; }
    public void setBuildingId(String buildingId) { this.buildingId = buildingId; }
    public String getAnomalyType() { return anomalyType; }
    public void setAnomalyType(String anomalyType) { this.anomalyType = anomalyType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getPointId() { return pointId; }
    public void setPointId(String pointId) { this.pointId = pointId; }
    public Double getObservedValue() { return observedValue; }
    public void setObservedValue(Double observedValue) { this.observedValue = observedValue; }
    public Double getExpectedMin() { return expectedMin; }
    public void setExpectedMin(Double expectedMin) { this.expectedMin = expectedMin; }
    public Double getExpectedMax() { return expectedMax; }
    public void setExpectedMax(Double expectedMax) { this.expectedMax = expectedMax; }
    public String getThresholdViolated() { return thresholdViolated; }
    public void setThresholdViolated(String thresholdViolated) { this.thresholdViolated = thresholdViolated; }
    public String getDetectionMethod() { return detectionMethod; }
    public void setDetectionMethod(String detectionMethod) { this.detectionMethod = detectionMethod; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }
    public UUID getLinkedCommandId() { return linkedCommandId; }
    public void setLinkedCommandId(UUID linkedCommandId) { this.linkedCommandId = linkedCommandId; }
    public Instant getCreatedAt() { return createdAt; }
}
