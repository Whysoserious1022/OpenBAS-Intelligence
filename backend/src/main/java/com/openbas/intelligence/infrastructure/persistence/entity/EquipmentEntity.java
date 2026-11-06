package com.openbas.intelligence.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity representing physical building equipment.
 * Equipment types include: AHU, VAV, CHILLER, PUMP, FAN, SENSOR, CONTROLLER, etc.
 * Equipment participates in dependency relationships: AHU → VAV, Chiller → AHU, etc.
 */
@Entity
@Table(name = "equipment", indexes = {
    @Index(name = "idx_equipment_zone_id", columnList = "zone_id"),
    @Index(name = "idx_equipment_external_id", columnList = "external_id"),
    @Index(name = "idx_equipment_type", columnList = "equipment_type"),
    @Index(name = "idx_equipment_operational_status", columnList = "operational_status")
})
public class EquipmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_id", nullable = false, length = 100, unique = true)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private ZoneEntity zone;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(name = "floor_id")
    private UUID floorId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "equipment_type", nullable = false, length = 50)
    private String equipmentType;  // AHU, VAV, CHILLER, PUMP, FAN, SENSOR, CONTROLLER, ENERGY_METER

    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "installation_date")
    private Instant installationDate;

    @Column(name = "operational_status", nullable = false, length = 30)
    private String operationalStatus = "UNKNOWN";  // NORMAL, DEGRADED, FAULT, OFFLINE, MAINTENANCE, UNKNOWN

    @Column(name = "health_score")
    private Double healthScore;  // 0.0 to 1.0

    // Operating range constraints
    @Column(name = "min_operating_temp_celsius")
    private Double minOperatingTempCelsius;

    @Column(name = "max_operating_temp_celsius")
    private Double maxOperatingTempCelsius;

    @Column(name = "rated_power_kw")
    private Double ratedPowerKw;

    @Column(name = "rated_airflow_cfm")
    private Double ratedAirflowCfm;

    @Column(name = "simulation_enabled", nullable = false)
    private boolean simulationEnabled = false;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    // Self-referential for parent-child relationships (e.g., VAV's parent AHU)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_equipment_id")
    private EquipmentEntity parentEquipment;

    @OneToMany(mappedBy = "parentEquipment", fetch = FetchType.LAZY)
    private List<EquipmentEntity> childEquipment = new ArrayList<>();

    @OneToMany(mappedBy = "equipment", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<DeviceEntity> devices = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public EquipmentEntity() {}

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public ZoneEntity getZone() { return zone; }
    public void setZone(ZoneEntity zone) { this.zone = zone; }
    public UUID getBuildingId() { return buildingId; }
    public void setBuildingId(UUID buildingId) { this.buildingId = buildingId; }
    public UUID getFloorId() { return floorId; }
    public void setFloorId(UUID floorId) { this.floorId = floorId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEquipmentType() { return equipmentType; }
    public void setEquipmentType(String equipmentType) { this.equipmentType = equipmentType; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public Instant getInstallationDate() { return installationDate; }
    public void setInstallationDate(Instant installationDate) { this.installationDate = installationDate; }
    public String getOperationalStatus() { return operationalStatus; }
    public void setOperationalStatus(String operationalStatus) { this.operationalStatus = operationalStatus; }
    public Double getHealthScore() { return healthScore; }
    public void setHealthScore(Double healthScore) { this.healthScore = healthScore; }
    public Double getMinOperatingTempCelsius() { return minOperatingTempCelsius; }
    public void setMinOperatingTempCelsius(Double v) { this.minOperatingTempCelsius = v; }
    public Double getMaxOperatingTempCelsius() { return maxOperatingTempCelsius; }
    public void setMaxOperatingTempCelsius(Double v) { this.maxOperatingTempCelsius = v; }
    public Double getRatedPowerKw() { return ratedPowerKw; }
    public void setRatedPowerKw(Double ratedPowerKw) { this.ratedPowerKw = ratedPowerKw; }
    public Double getRatedAirflowCfm() { return ratedAirflowCfm; }
    public void setRatedAirflowCfm(Double ratedAirflowCfm) { this.ratedAirflowCfm = ratedAirflowCfm; }
    public boolean isSimulationEnabled() { return simulationEnabled; }
    public void setSimulationEnabled(boolean simulationEnabled) { this.simulationEnabled = simulationEnabled; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public EquipmentEntity getParentEquipment() { return parentEquipment; }
    public void setParentEquipment(EquipmentEntity parentEquipment) { this.parentEquipment = parentEquipment; }
    public List<EquipmentEntity> getChildEquipment() { return childEquipment; }
    public List<DeviceEntity> getDevices() { return devices; }
    public void setDevices(List<DeviceEntity> devices) { this.devices = devices; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
