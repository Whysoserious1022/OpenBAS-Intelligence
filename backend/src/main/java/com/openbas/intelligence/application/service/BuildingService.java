package com.openbas.intelligence.application.service;

import com.openbas.intelligence.api.dto.*;
import com.openbas.intelligence.api.exception.ResourceAlreadyExistsException;
import com.openbas.intelligence.api.exception.ResourceNotFoundException;
import com.openbas.intelligence.infrastructure.persistence.entity.*;
import com.openbas.intelligence.infrastructure.persistence.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application service for building hierarchy management.
 * Handles buildings, floors, and zones.
 * Does NOT contain protocol logic or telemetry logic (those belong to gateway/ingestion services).
 */
@Service
@Transactional(readOnly = true)
public class BuildingService {

    private static final Logger log = LoggerFactory.getLogger(BuildingService.class);

    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;
    private final ZoneRepository zoneRepository;

    public BuildingService(BuildingRepository buildingRepository,
                           FloorRepository floorRepository,
                           ZoneRepository zoneRepository) {
        this.buildingRepository = buildingRepository;
        this.floorRepository = floorRepository;
        this.zoneRepository = zoneRepository;
    }

    // ── Buildings ─────────────────────────────────────────────────────────────

    public List<BuildingDto> getAllBuildings() {
        return buildingRepository.findAllByActiveTrue().stream()
            .map(this::toBuildingDto)
            .toList();
    }

    public Page<BuildingDto> getAllBuildingsPaged(Pageable pageable) {
        return buildingRepository.findAllByActiveTrue(pageable).map(this::toBuildingDto);
    }

    public BuildingDto getBuildingById(UUID id) {
        return buildingRepository.findById(id)
            .filter(BuildingEntity::isActive)
            .map(this::toBuildingDto)
            .orElseThrow(() -> new ResourceNotFoundException("Building", id.toString()));
    }

    public BuildingDto getBuildingByExternalId(String externalId) {
        return buildingRepository.findByExternalIdAndActiveTrue(externalId)
            .map(this::toBuildingDto)
            .orElseThrow(() -> new ResourceNotFoundException("Building", externalId));
    }

    public BuildingDto getBuildingWithFloors(UUID id) {
        BuildingEntity entity = buildingRepository.findByIdWithFloors(id)
            .orElseThrow(() -> new ResourceNotFoundException("Building", id.toString()));
        return toBuildingDtoWithFloors(entity);
    }

    @Transactional
    public BuildingDto createBuilding(CreateBuildingRequest request) {
        if (buildingRepository.existsByExternalId(request.externalId())) {
            throw new ResourceAlreadyExistsException("Building", request.externalId());
        }
        BuildingEntity entity = new BuildingEntity();
        entity.setExternalId(request.externalId());
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setAddress(request.address());
        entity.setCity(request.city());
        entity.setCountry(request.country());
        entity.setTimezone(request.timezone() != null ? request.timezone() : "UTC");
        entity.setTotalAreaSqm(request.totalAreaSqm());
        entity.setFloorCount(request.floorCount());
        entity.setSimulationEnabled(request.simulationEnabled());
        entity.setActive(true);

        BuildingEntity saved = buildingRepository.save(entity);
        log.info("Created building: {} ({})", saved.getName(), saved.getExternalId());
        return toBuildingDto(saved);
    }

    @Transactional
    public void deactivateBuilding(UUID id) {
        BuildingEntity entity = buildingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Building", id.toString()));
        entity.setActive(false);
        buildingRepository.save(entity);
        log.info("Deactivated building: {} ({})", entity.getName(), entity.getExternalId());
    }

    // ── Floors ────────────────────────────────────────────────────────────────

    public List<FloorDto> getFloorsByBuilding(UUID buildingId) {
        // Verify building exists
        if (!buildingRepository.existsById(buildingId)) {
            throw new ResourceNotFoundException("Building", buildingId.toString());
        }
        return floorRepository.findAllByBuilding_IdAndActiveTrueOrderByFloorNumberAsc(buildingId)
            .stream()
            .map(this::toFloorDto)
            .toList();
    }

    public FloorDto getFloorById(UUID id) {
        return floorRepository.findById(id)
            .filter(FloorEntity::isActive)
            .map(this::toFloorDto)
            .orElseThrow(() -> new ResourceNotFoundException("Floor", id.toString()));
    }

    public FloorDto getFloorWithZones(UUID id) {
        return floorRepository.findByIdWithZones(id)
            .map(this::toFloorDtoWithZones)
            .orElseThrow(() -> new ResourceNotFoundException("Floor", id.toString()));
    }

    // ── Zones ─────────────────────────────────────────────────────────────────

    public List<ZoneDto> getZonesByFloor(UUID floorId) {
        if (!floorRepository.existsById(floorId)) {
            throw new ResourceNotFoundException("Floor", floorId.toString());
        }
        return zoneRepository.findAllByFloor_IdAndActiveTrueOrderByNameAsc(floorId)
            .stream()
            .map(this::toZoneDto)
            .toList();
    }

    public ZoneDto getZoneById(UUID id) {
        return zoneRepository.findById(id)
            .filter(ZoneEntity::isActive)
            .map(this::toZoneDto)
            .orElseThrow(() -> new ResourceNotFoundException("Zone", id.toString()));
    }

    // ── Stats for dashboard overview ──────────────────────────────────────────

    public long countActiveBuildings() {
        return buildingRepository.countActive();
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private BuildingDto toBuildingDto(BuildingEntity e) {
        return new BuildingDto(e.getId(), e.getExternalId(), e.getName(), e.getDescription(),
            e.getAddress(), e.getCity(), e.getCountry(), e.getTimezone(),
            e.getTotalAreaSqm(), e.getFloorCount(), e.isActive(), e.isSimulationEnabled(),
            e.getCreatedAt(), e.getUpdatedAt(), null);
    }

    private BuildingDto toBuildingDtoWithFloors(BuildingEntity e) {
        List<FloorDto> floors = e.getFloors().stream().map(this::toFloorDto).toList();
        return new BuildingDto(e.getId(), e.getExternalId(), e.getName(), e.getDescription(),
            e.getAddress(), e.getCity(), e.getCountry(), e.getTimezone(),
            e.getTotalAreaSqm(), e.getFloorCount(), e.isActive(), e.isSimulationEnabled(),
            e.getCreatedAt(), e.getUpdatedAt(), floors);
    }

    private FloorDto toFloorDto(FloorEntity e) {
        return new FloorDto(e.getId(), e.getExternalId(),
            e.getBuilding() != null ? e.getBuilding().getId() : null,
            e.getName(), e.getFloorNumber(), e.getDescription(), e.getAreaSqm(),
            e.isActive(), e.getCreatedAt(), null);
    }

    private FloorDto toFloorDtoWithZones(FloorEntity e) {
        List<ZoneDto> zones = e.getZones().stream().map(this::toZoneDto).toList();
        return new FloorDto(e.getId(), e.getExternalId(),
            e.getBuilding() != null ? e.getBuilding().getId() : null,
            e.getName(), e.getFloorNumber(), e.getDescription(), e.getAreaSqm(),
            e.isActive(), e.getCreatedAt(), zones);
    }

    private ZoneDto toZoneDto(ZoneEntity e) {
        return new ZoneDto(e.getId(), e.getExternalId(),
            e.getFloor() != null ? e.getFloor().getId() : null,
            e.getBuildingId(), e.getName(), e.getZoneType(), e.getAreaSqm(),
            e.getDesignOccupancy(), e.getComfortTempMinCelsius(), e.getComfortTempMaxCelsius(),
            e.getComfortHumidityMinPct(), e.getComfortHumidityMaxPct(),
            e.isActive(), e.getCreatedAt());
    }
}
