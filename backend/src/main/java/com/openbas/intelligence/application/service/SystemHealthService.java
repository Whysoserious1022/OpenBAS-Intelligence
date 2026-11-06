package com.openbas.intelligence.application.service;

import com.openbas.intelligence.api.controller.SystemHealthController.DashboardOverviewDto;
import com.openbas.intelligence.api.dto.SystemHealthDto;
import com.openbas.intelligence.infrastructure.persistence.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class SystemHealthService {

    private static final Logger log = LoggerFactory.getLogger(SystemHealthService.class);

    private final BuildingRepository buildingRepository;
    private final DeviceRepository deviceRepository;
    private final EquipmentRepository equipmentRepository;
    private final AnomalyEventRepository anomalyEventRepository;
    private final CommandRepository commandRepository;
    private final JdbcTemplate jdbcTemplate;

    public SystemHealthService(BuildingRepository buildingRepository,
                               DeviceRepository deviceRepository,
                               EquipmentRepository equipmentRepository,
                               AnomalyEventRepository anomalyEventRepository,
                               CommandRepository commandRepository,
                               JdbcTemplate jdbcTemplate) {
        this.buildingRepository = buildingRepository;
        this.deviceRepository = deviceRepository;
        this.equipmentRepository = equipmentRepository;
        this.anomalyEventRepository = anomalyEventRepository;
        this.commandRepository = commandRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public SystemHealthDto getHealth() {
        Map<String, SystemHealthDto.ComponentHealthDto> components = new LinkedHashMap<>();

        // Database health
        try {
            long start = System.currentTimeMillis();
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            long responseTime = System.currentTimeMillis() - start;
            components.put("database", new SystemHealthDto.ComponentHealthDto("UP",
                "PostgreSQL responding", responseTime));
        } catch (Exception e) {
            components.put("database", new SystemHealthDto.ComponentHealthDto("DOWN",
                "Database error: " + e.getMessage(), null));
        }

        // Building hierarchy
        try {
            long count = buildingRepository.countActive();
            components.put("buildings", new SystemHealthDto.ComponentHealthDto("UP",
                count + " active buildings registered", null));
        } catch (Exception e) {
            components.put("buildings", new SystemHealthDto.ComponentHealthDto("DEGRADED",
                "Could not query buildings: " + e.getMessage(), null));
        }

        // Device health
        try {
            long offline = deviceRepository.countOffline();
            String status = offline > 0 ? "DEGRADED" : "UP";
            components.put("devices", new SystemHealthDto.ComponentHealthDto(status,
                offline + " device(s) offline", null));
        } catch (Exception e) {
            components.put("devices", new SystemHealthDto.ComponentHealthDto("UNKNOWN",
                e.getMessage(), null));
        }

        String overallStatus = components.values().stream()
            .anyMatch(c -> "DOWN".equals(c.status())) ? "DOWN" :
            components.values().stream().anyMatch(c -> "DEGRADED".equals(c.status())) ? "DEGRADED" : "UP";

        return new SystemHealthDto(overallStatus, Instant.now(), components);
    }

    public DashboardOverviewDto getDashboardOverview() {
        long buildings = buildingRepository.countActive();
        long activeEquipment = equipmentRepository.countActive();
        long offlineDevices = deviceRepository.countOffline();
        long faultedEquipment = equipmentRepository.countFaulted();
        long activeAnomalies = anomalyEventRepository.countAllActive();
        long pendingApprovals = commandRepository.countPendingApprovals();

        return new DashboardOverviewDto(buildings, activeEquipment, offlineDevices,
            activeAnomalies, pendingApprovals, faultedEquipment);
    }
}
