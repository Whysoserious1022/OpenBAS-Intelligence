package com.openbas.intelligence.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "floors", indexes = {
    @Index(name = "idx_floors_building_id", columnList = "building_id"),
    @Index(name = "idx_floors_external_id", columnList = "external_id")
})
public class FloorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private BuildingEntity building;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "floor_number", nullable = false)
    private Integer floorNumber;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "area_sqm")
    private Double areaSqm;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "floor", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ZoneEntity> zones = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public FloorEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public BuildingEntity getBuilding() { return building; }
    public void setBuilding(BuildingEntity building) { this.building = building; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getFloorNumber() { return floorNumber; }
    public void setFloorNumber(Integer floorNumber) { this.floorNumber = floorNumber; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getAreaSqm() { return areaSqm; }
    public void setAreaSqm(Double areaSqm) { this.areaSqm = areaSqm; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<ZoneEntity> getZones() { return zones; }
    public void setZones(List<ZoneEntity> zones) { this.zones = zones; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
