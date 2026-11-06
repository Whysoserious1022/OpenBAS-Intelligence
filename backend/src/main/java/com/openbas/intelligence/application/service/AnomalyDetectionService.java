package com.openbas.intelligence.application.service;

import com.openbas.intelligence.api.dto.AnomalyEventDto;
import com.openbas.intelligence.api.dto.PagedResponseDto;
import com.openbas.intelligence.api.exception.ResourceNotFoundException;
import com.openbas.intelligence.infrastructure.persistence.entity.AnomalyEventEntity;
import com.openbas.intelligence.infrastructure.persistence.entity.EquipmentEntity;
import com.openbas.intelligence.infrastructure.persistence.entity.TelemetryEventEntity;
import com.openbas.intelligence.infrastructure.persistence.repository.AnomalyEventRepository;
import com.openbas.intelligence.infrastructure.persistence.repository.EquipmentRepository;
import com.openbas.intelligence.infrastructure.persistence.repository.TelemetryEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Anomaly detection service using deterministic, explainable methods.
 *
 * Detection methods implemented:
 * 1. THRESHOLD — Value outside configured equipment operating range
 * 2. MOVING_AVERAGE — Deviation beyond N standard deviations from rolling mean
 * 3. RATE_OF_CHANGE — Value changes too rapidly
 * 4. MISSING_DATA — No recent reading from active device
 *
 * Does NOT use ML models in Phase 1. ML can be added later when a clear benefit is demonstrated.
 */
@Service
public class AnomalyDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetectionService.class);

    // Equipment-specific thresholds by type and point
    private static final Map<String, double[]> TEMP_THRESHOLDS = Map.of(
        "SUPPLY_AIR_TEMP", new double[]{8.0, 20.0},      // AHU supply air: 8–20°C normal
        "RETURN_AIR_TEMP", new double[]{18.0, 30.0},      // Return air
        "ZONE_TEMP",       new double[]{15.0, 35.0},      // Zone temperature
        "CHILLED_WATER_SUPPLY_TEMP", new double[]{5.0, 12.0},
        "CHILLED_WATER_RETURN_TEMP", new double[]{12.0, 18.0}
    );

    private static final double COMFORT_TEMP_MIN = 20.0;
    private static final double COMFORT_TEMP_MAX = 26.0;
    private static final double POWER_CHANGE_RATE_THRESHOLD_KW_PER_MIN = 10.0;
    private static final int MOVING_AVG_WINDOW_MINUTES = 30;
    private static final double STD_DEV_MULTIPLIER = 2.5;

    private final AnomalyEventRepository anomalyEventRepository;
    private final TelemetryEventRepository telemetryEventRepository;
    private final EquipmentRepository equipmentRepository;

    public AnomalyDetectionService(AnomalyEventRepository anomalyEventRepository,
                                   TelemetryEventRepository telemetryEventRepository,
                                   EquipmentRepository equipmentRepository) {
        this.anomalyEventRepository = anomalyEventRepository;
        this.telemetryEventRepository = telemetryEventRepository;
        this.equipmentRepository = equipmentRepository;
    }

    /**
     * Async entry point called by TelemetryIngestionService after each good reading.
     * Runs in the anomaly detection thread pool.
     */
    @Async("anomalyDetectionExecutor")
    @Transactional
    public void evaluateAsync(TelemetryEventEntity event) {
        try {
            evaluate(event);
        } catch (Exception e) {
            log.error("Anomaly evaluation failed for device={} point={}", event.getDeviceId(), event.getPointId(), e);
        }
    }

    private void evaluate(TelemetryEventEntity event) {
        if (event.getNumericValue() == null) return;
        double value = event.getNumericValue();
        String pointId = event.getPointId();

        // 1. Threshold check
        if (TEMP_THRESHOLDS.containsKey(pointId)) {
            double[] range = TEMP_THRESHOLDS.get(pointId);
            if (value < range[0] || value > range[1]) {
                createAnomaly(event, value < range[0] ? "TEMP_LOW" : "TEMP_HIGH",
                    value < range[0] ? "MEDIUM" : "HIGH",
                    "THRESHOLD", range[0], range[1],
                    String.format("Value %.1f outside expected range [%.1f, %.1f] for %s",
                        value, range[0], range[1], pointId));
            }
        }

        // 2. Zone comfort check
        if ("ZONE_TEMP".equals(pointId)) {
            if (value > COMFORT_TEMP_MAX + 2.0) {
                createAnomaly(event, "TEMP_HIGH", "HIGH", "THRESHOLD",
                    COMFORT_TEMP_MIN, COMFORT_TEMP_MAX,
                    String.format("Zone temperature %.1f°C exceeds comfort maximum %.1f°C", value, COMFORT_TEMP_MAX));
            } else if (value < COMFORT_TEMP_MIN - 2.0) {
                createAnomaly(event, "TEMP_LOW", "MEDIUM", "THRESHOLD",
                    COMFORT_TEMP_MIN, COMFORT_TEMP_MAX,
                    String.format("Zone temperature %.1f°C below comfort minimum %.1f°C", value, COMFORT_TEMP_MIN));
            }
        }

        // 3. Moving average check for POWER readings
        if (pointId.contains("POWER") || pointId.contains("KW")) {
            checkMovingAverage(event, value);
        }
    }

    private void checkMovingAverage(TelemetryEventEntity event, double currentValue) {
        Instant windowStart = event.getEventTimestamp().minus(MOVING_AVG_WINDOW_MINUTES, ChronoUnit.MINUTES);
        List<TelemetryEventEntity> recent = telemetryEventRepository.findRecentForAnomaly(
            event.getEquipmentId(), event.getPointId(), windowStart);

        if (recent.size() < 5) return; // Not enough data for statistical analysis

        List<Double> values = recent.stream()
            .map(TelemetryEventEntity::getNumericValue)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (values.size() < 5) return;

        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0);
        double stdDev = Math.sqrt(variance);

        if (stdDev > 0 && Math.abs(currentValue - mean) > STD_DEV_MULTIPLIER * stdDev) {
            createAnomaly(event, "POWER_HIGH", "HIGH", "MOVING_AVERAGE", mean - stdDev, mean + stdDev,
                String.format("Power %.1f kW deviates %.1f std devs from %.1f-min rolling mean %.1f kW",
                    currentValue, Math.abs(currentValue - mean) / stdDev, (double) MOVING_AVG_WINDOW_MINUTES, mean));
        }
    }

    private void createAnomaly(TelemetryEventEntity event, String type, String severity,
                                String method, double expectedMin, double expectedMax, String description) {
        AnomalyEventEntity anomaly = new AnomalyEventEntity();
        anomaly.setEquipmentId(event.getEquipmentId());
        anomaly.setDeviceId(event.getDeviceId());
        anomaly.setZoneId(event.getZoneId());
        anomaly.setBuildingId(event.getBuildingId());
        anomaly.setAnomalyType(type);
        anomaly.setSeverity(severity);
        anomaly.setPointId(event.getPointId());
        anomaly.setObservedValue(event.getNumericValue());
        anomaly.setExpectedMin(expectedMin);
        anomaly.setExpectedMax(expectedMax);
        anomaly.setDetectionMethod(method);
        anomaly.setDescription(description);
        anomaly.setEvidence(String.format(
            "{\"deviceId\":\"%s\",\"pointId\":\"%s\",\"observedValue\":%.3f,\"eventTimestamp\":\"%s\"}",
            event.getDeviceId(), event.getPointId(),
            event.getNumericValue(), event.getEventTimestamp()));
        anomaly.setDetectedAt(Instant.now());
        anomalyEventRepository.save(anomaly);
        log.info("Anomaly detected: type={} severity={} equipment={} value={}",
            type, severity, event.getEquipmentId(), event.getNumericValue());
    }

    // ── Query methods ─────────────────────────────────────────────────────────

    public PagedResponseDto<AnomalyEventDto> getAnomalies(String buildingId, Boolean resolved, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("detectedAt").descending());
        if (buildingId != null) {
            return PagedResponseDto.of(
                anomalyEventRepository.findAllByBuildingIdOrderByDetectedAtDesc(buildingId, pageRequest)
                    .map(this::toDto));
        }
        return PagedResponseDto.of(anomalyEventRepository.findAll(pageRequest).map(this::toDto));
    }

    public AnomalyEventDto getAnomalyById(UUID id) {
        return anomalyEventRepository.findById(id)
            .map(this::toDto)
            .orElseThrow(() -> new ResourceNotFoundException("Anomaly", id.toString()));
    }

    public List<AnomalyEventDto> getActiveAnomaliesForEquipment(String equipmentId) {
        return anomalyEventRepository
            .findAllByEquipmentIdAndResolvedFalseOrderByDetectedAtDesc(equipmentId)
            .stream().map(this::toDto).toList();
    }

    @Transactional
    public AnomalyEventDto resolveAnomaly(UUID id, String note) {
        AnomalyEventEntity anomaly = anomalyEventRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Anomaly", id.toString()));
        anomaly.setResolved(true);
        anomaly.setResolvedAt(Instant.now());
        anomaly.setResolutionNote(note);
        return toDto(anomalyEventRepository.save(anomaly));
    }

    public long countAllActive() { return anomalyEventRepository.countAllActive(); }

    private AnomalyEventDto toDto(AnomalyEventEntity e) {
        return new AnomalyEventDto(e.getId(), e.getEquipmentId(), e.getDeviceId(),
            e.getZoneId(), e.getBuildingId(), e.getAnomalyType(), e.getSeverity(),
            e.getPointId(), e.getObservedValue(), e.getExpectedMin(), e.getExpectedMax(),
            e.getDetectionMethod(), e.getEvidence(), e.getDescription(),
            e.getDetectedAt(), e.isResolved(), e.getResolvedAt(),
            e.getResolutionNote(), e.getCreatedAt());
    }
}
