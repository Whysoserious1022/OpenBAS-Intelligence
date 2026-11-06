package com.openbas.intelligence.api.controller;

import com.openbas.intelligence.api.dto.*;
import com.openbas.intelligence.application.service.BuildingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/buildings")
@Tag(name = "Buildings", description = "Building hierarchy management")
public class BuildingController {

    private final BuildingService buildingService;

    public BuildingController(BuildingService buildingService) {
        this.buildingService = buildingService;
    }

    @GetMapping
    @Operation(summary = "List all active buildings")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<List<BuildingDto>> getAllBuildings() {
        return ResponseEntity.ok(buildingService.getAllBuildings());
    }

    @GetMapping("/paged")
    @Operation(summary = "List buildings with pagination")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<PagedResponseDto<BuildingDto>> getAllBuildingsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<BuildingDto> result = buildingService.getAllBuildingsPaged(
            PageRequest.of(page, size, Sort.by("name")));
        return ResponseEntity.ok(PagedResponseDto.of(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a building by internal UUID")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<BuildingDto> getBuildingById(@PathVariable UUID id) {
        return ResponseEntity.ok(buildingService.getBuildingById(id));
    }

    @GetMapping("/by-external-id/{externalId}")
    @Operation(summary = "Get a building by external ID (e.g., BUILDING-01)")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<BuildingDto> getBuildingByExternalId(@PathVariable String externalId) {
        return ResponseEntity.ok(buildingService.getBuildingByExternalId(externalId));
    }

    @GetMapping("/{id}/hierarchy")
    @Operation(summary = "Get building with floors and zones")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<BuildingDto> getBuildingWithHierarchy(@PathVariable UUID id) {
        return ResponseEntity.ok(buildingService.getBuildingWithFloors(id));
    }

    @PostMapping
    @Operation(summary = "Create a new building")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BuildingDto> createBuilding(@Valid @RequestBody CreateBuildingRequest request) {
        BuildingDto created = buildingService.createBuilding(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a building")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateBuilding(@PathVariable UUID id) {
        buildingService.deactivateBuilding(id);
        return ResponseEntity.noContent().build();
    }

    // ── Floors under building ─────────────────────────────────────────────────

    @GetMapping("/{buildingId}/floors")
    @Operation(summary = "Get floors for a building")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<List<FloorDto>> getFloors(@PathVariable UUID buildingId) {
        return ResponseEntity.ok(buildingService.getFloorsByBuilding(buildingId));
    }

    @GetMapping("/{buildingId}/floors/{floorId}")
    @Operation(summary = "Get a floor by ID")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<FloorDto> getFloor(@PathVariable UUID buildingId, @PathVariable UUID floorId) {
        return ResponseEntity.ok(buildingService.getFloorById(floorId));
    }

    @GetMapping("/{buildingId}/floors/{floorId}/zones")
    @Operation(summary = "Get zones for a floor")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<List<ZoneDto>> getZones(@PathVariable UUID buildingId, @PathVariable UUID floorId) {
        return ResponseEntity.ok(buildingService.getZonesByFloor(floorId));
    }

    @GetMapping("/{buildingId}/floors/{floorId}/zones/{zoneId}")
    @Operation(summary = "Get a zone by ID")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<ZoneDto> getZone(@PathVariable UUID buildingId,
                                           @PathVariable UUID floorId,
                                           @PathVariable UUID zoneId) {
        return ResponseEntity.ok(buildingService.getZoneById(zoneId));
    }
}
