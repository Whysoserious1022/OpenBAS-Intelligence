package com.openbas.intelligence.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity representing a physical building in the platform.
 * A building is the top-level container in the equipment hierarchy:
 * Building → Floor → Zone → Equipment → Device
 */
@Entity
@Table(name = "buildings", indexes = {
    @Index(name = "idx_buildings_external_id", columnList = "external_id", unique = true),
    @Index(name = "idx_buildings_active", columnList = "active")
})
public class BuildingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "external_id", nullable = false, unique = true, length = 100)
    private String externalId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "timezone", length = 50)
    private String timezone;

    @Column(name = "total_area_sqm")
    private Double totalAreaSqm;

    @Column(name = "floor_count")
    private Integer floorCount;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "simulation_enabled", nullable = false)
    private boolean simulationEnabled = false;

    @OneToMany(mappedBy = "building", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<FloorEntity> floors = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    public BuildingEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public Double getTotalAreaSqm() { return totalAreaSqm; }
    public void setTotalAreaSqm(Double totalAreaSqm) { this.totalAreaSqm = totalAreaSqm; }
    public Integer getFloorCount() { return floorCount; }
    public void setFloorCount(Integer floorCount) { this.floorCount = floorCount; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isSimulationEnabled() { return simulationEnabled; }
    public void setSimulationEnabled(boolean simulationEnabled) { this.simulationEnabled = simulationEnabled; }
    public List<FloorEntity> getFloors() { return floors; }
    public void setFloors(List<FloorEntity> floors) { this.floors = floors; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
