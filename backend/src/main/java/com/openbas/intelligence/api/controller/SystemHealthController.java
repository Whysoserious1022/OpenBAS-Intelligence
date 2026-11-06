package com.openbas.intelligence.api.controller;

import com.openbas.intelligence.api.dto.SystemHealthDto;
import com.openbas.intelligence.application.service.SystemHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System Health", description = "Platform and service health monitoring")
public class SystemHealthController {

    private final SystemHealthService systemHealthService;

    public SystemHealthController(SystemHealthService systemHealthService) {
        this.systemHealthService = systemHealthService;
    }

    @GetMapping("/health")
    @Operation(summary = "Get detailed platform health status")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<SystemHealthDto> getHealth() {
        return ResponseEntity.ok(systemHealthService.getHealth());
    }

    @GetMapping("/overview")
    @Operation(summary = "Get dashboard overview statistics")
    @PreAuthorize("hasAnyRole('VIEWER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<DashboardOverviewDto> getOverview() {
        return ResponseEntity.ok(systemHealthService.getDashboardOverview());
    }

    public record DashboardOverviewDto(
        long totalBuildings,
        long activeEquipment,
        long offlineDevices,
        long activeAnomalies,
        long pendingApprovals,
        long faultedEquipment
    ) {}
}
