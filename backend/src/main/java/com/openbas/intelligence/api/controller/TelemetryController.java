package com.openbas.intelligence.api.controller;

import com.openbas.intelligence.api.dto.TelemetryEventDto;
import com.openbas.intelligence.api.dto.PagedResponseDto;
import com.openbas.intelligence.application.service.TelemetryQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/telemetry")
@Tag(name = "Telemetry", description = "Telemetry ingestion and historical data queries")
public class TelemetryController {

    private final TelemetryQueryService telemetryQueryService;

    public TelemetryController(TelemetryQueryService telemetryQueryService) {
        this.telemetryQueryService = telemetryQueryService;
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest telemetry reading for a device/point")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<TelemetryEventDto> getLatest(
            @RequestParam String deviceId,
            @RequestParam String pointId) {
        return ResponseEntity.ok(telemetryQueryService.getLatest(deviceId, pointId));
    }

    @GetMapping("/history")
    @Operation(summary = "Get telemetry history for a device/point in a time range")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<List<TelemetryEventDto>> getHistory(
            @RequestParam String deviceId,
            @RequestParam String pointId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(telemetryQueryService.getHistory(deviceId, pointId, from, to));
    }

    @GetMapping("/equipment/{equipmentId}")
    @Operation(summary = "Get recent telemetry for all points of an equipment")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<List<TelemetryEventDto>> getEquipmentTelemetry(
            @PathVariable String equipmentId,
            @RequestParam(defaultValue = "1") int hours) {
        return ResponseEntity.ok(telemetryQueryService.getEquipmentTelemetry(equipmentId, hours));
    }

    @GetMapping("/buildings/{buildingId}")
    @Operation(summary = "Get recent telemetry for a building")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<PagedResponseDto<TelemetryEventDto>> getBuildingTelemetry(
            @PathVariable String buildingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(telemetryQueryService.getBuildingTelemetry(buildingId, page, size, from, to));
    }
}
