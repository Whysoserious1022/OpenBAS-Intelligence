package com.openbas.intelligence.api.controller;

import com.openbas.intelligence.api.dto.EquipmentDto;
import com.openbas.intelligence.api.dto.DeviceDto;
import com.openbas.intelligence.application.service.EquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/equipment")
@Tag(name = "Equipment", description = "Building equipment and device management")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping
    @Operation(summary = "List equipment, optionally filtered by building or type")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<List<EquipmentDto>> getEquipment(
            @RequestParam(required = false) UUID buildingId,
            @RequestParam(required = false) UUID zoneId,
            @RequestParam(required = false) String equipmentType,
            @RequestParam(required = false) String operationalStatus) {
        return ResponseEntity.ok(equipmentService.getEquipment(buildingId, zoneId, equipmentType, operationalStatus));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get equipment by internal ID")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<EquipmentDto> getEquipmentById(@PathVariable UUID id) {
        return ResponseEntity.ok(equipmentService.getEquipmentById(id));
    }

    @GetMapping("/by-external-id/{externalId}")
    @Operation(summary = "Get equipment by external ID (e.g., AHU-03)")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<EquipmentDto> getEquipmentByExternalId(@PathVariable String externalId) {
        return ResponseEntity.ok(equipmentService.getEquipmentByExternalId(externalId));
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "Get child equipment (dependency traversal)")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<List<EquipmentDto>> getChildEquipment(@PathVariable UUID id) {
        return ResponseEntity.ok(equipmentService.getChildEquipment(id));
    }

    @GetMapping("/{id}/devices")
    @Operation(summary = "Get devices for equipment")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<List<DeviceDto>> getDevicesForEquipment(@PathVariable UUID id) {
        return ResponseEntity.ok(equipmentService.getDevicesForEquipment(id));
    }

    @GetMapping("/faulted")
    @Operation(summary = "Get equipment in FAULT or DEGRADED state")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<List<EquipmentDto>> getFaultedEquipment(
            @RequestParam(required = false) UUID buildingId) {
        return ResponseEntity.ok(equipmentService.getFaultedEquipment(buildingId));
    }
}
