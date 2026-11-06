package com.openbas.intelligence.api.controller;

import com.openbas.intelligence.api.dto.AnomalyEventDto;
import com.openbas.intelligence.api.dto.PagedResponseDto;
import com.openbas.intelligence.application.service.AnomalyDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/anomalies")
@Tag(name = "Anomalies", description = "Equipment anomaly detection and history")
public class AnomalyController {

    private final AnomalyDetectionService anomalyService;

    public AnomalyController(AnomalyDetectionService anomalyService) {
        this.anomalyService = anomalyService;
    }

    @GetMapping
    @Operation(summary = "List anomalies, optionally filtered by building and resolved status")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<PagedResponseDto<AnomalyEventDto>> getAnomalies(
            @RequestParam(required = false) String buildingId,
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(anomalyService.getAnomalies(buildingId, resolved, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get anomaly by ID")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<AnomalyEventDto> getAnomalyById(@PathVariable UUID id) {
        return ResponseEntity.ok(anomalyService.getAnomalyById(id));
    }

    @GetMapping("/equipment/{equipmentId}")
    @Operation(summary = "Get active anomalies for equipment")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<List<AnomalyEventDto>> getEquipmentAnomalies(@PathVariable String equipmentId) {
        return ResponseEntity.ok(anomalyService.getActiveAnomaliesForEquipment(equipmentId));
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Mark an anomaly as resolved")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<AnomalyEventDto> resolveAnomaly(
            @PathVariable UUID id,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(anomalyService.resolveAnomaly(id, note));
    }
}
