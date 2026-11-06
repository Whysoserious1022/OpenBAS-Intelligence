package com.openbas.intelligence.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "zones", indexes = {
    @Index(name = "idx_zones_floor_id", columnList = "floor_id"),
    @Index(name = "idx_zones_building_id", columnList = "building_id"),
    @Index(name = "idx_zones_external_id", columnList = "external_id")
})
public class ZoneEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "floor_id", nullable = false)
    private FloorEntity floor;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "zone_type", length = 50)
    private String zoneType;  // OFFICE, CONFERENCE, LOBBY, SERVER_ROOM, etc.

    @Column(name = "area_sqm")
    private Double areaSqm;

    @Column(name = "design_occupancy")
    private Integer designOccupancy;

    @Column(name = "comfort_temp_min_celsius")
    private Double comfortTempMinCelsius = 20.0;

    @Column(name = "comfort_temp_max_celsius")
    private Double comfortTempMaxCelsius = 26.0;

    @Column(name = "comfort_humidity_min_pct")
    private Double comfortHumidityMinPct = 30.0;

    @Column(name = "comfort_humidity_max_pct")
    private Double comfortHumidityMaxPct = 60.0;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EquipmentEntity> equipment = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ZoneEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public FloorEntity getFloor() { return floor; }
    public void setFloor(FloorEntity floor) { this.floor = floor; }
    public UUID getBuildingId() { return buildingId; }
    public void setBuildingId(UUID buildingId) { this.buildingId = buildingId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getZoneType() { return zoneType; }
    public void setZoneType(String zoneType) { this.zoneType = zoneType; }
    public Double getAreaSqm() { return areaSqm; }
    public void setAreaSqm(Double areaSqm) { this.areaSqm = areaSqm; }
    public Integer getDesignOccupancy() { return designOccupancy; }
    public void setDesignOccupancy(Integer designOccupancy) { this.designOccupancy = designOccupancy; }
    public Double getComfortTempMinCelsius() { return comfortTempMinCelsius; }
    public void setComfortTempMinCelsius(Double v) { this.comfortTempMinCelsius = v; }
    public Double getComfortTempMaxCelsius() { return comfortTempMaxCelsius; }
    public void setComfortTempMaxCelsius(Double v) { this.comfortTempMaxCelsius = v; }
    public Double getComfortHumidityMinPct() { return comfortHumidityMinPct; }
    public void setComfortHumidityMinPct(Double v) { this.comfortHumidityMinPct = v; }
    public Double getComfortHumidityMaxPct() { return comfortHumidityMaxPct; }
    public void setComfortHumidityMaxPct(Double v) { this.comfortHumidityMaxPct = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<EquipmentEntity> getEquipment() { return equipment; }
    public void setEquipment(List<EquipmentEntity> equipment) { this.equipment = equipment; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
