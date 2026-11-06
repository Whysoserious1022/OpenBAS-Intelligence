package com.openbas.intelligence.application.service;

import com.openbas.intelligence.api.dto.PagedResponseDto;
import com.openbas.intelligence.api.dto.TelemetryEventDto;
import com.openbas.intelligence.api.exception.ResourceNotFoundException;
import com.openbas.intelligence.infrastructure.persistence.entity.TelemetryEventEntity;
import com.openbas.intelligence.infrastructure.persistence.repository.TelemetryEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TelemetryQueryService {

    private final TelemetryEventRepository telemetryEventRepository;

    public TelemetryQueryService(TelemetryEventRepository telemetryEventRepository) {
        this.telemetryEventRepository = telemetryEventRepository;
    }

    public TelemetryEventDto getLatest(String deviceId, String pointId) {
        return telemetryEventRepository.findLatest(deviceId, pointId)
            .map(this::toDto)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No telemetry found for device " + deviceId + " point " + pointId));
    }

    public List<TelemetryEventDto> getHistory(String deviceId, String pointId, Instant from, Instant to) {
        return telemetryEventRepository.findByDeviceAndPointInRange(deviceId, pointId, from, to)
            .stream().map(this::toDto).toList();
    }

    public List<TelemetryEventDto> getEquipmentTelemetry(String equipmentId, int hours) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        return telemetryEventRepository.findByEquipmentInRange(
                equipmentId, since, Instant.now())
            .stream().map(this::toDto).toList();
    }

    public PagedResponseDto<TelemetryEventDto> getBuildingTelemetry(
            String buildingId, int page, int size, Instant from, Instant to) {
        Instant effectiveFrom = from != null ? from : Instant.now().minus(24, ChronoUnit.HOURS);
        Instant effectiveTo = to != null ? to : Instant.now();
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 500),
            Sort.by("eventTimestamp").descending());
        Page<TelemetryEventDto> result = telemetryEventRepository
            .findByBuildingInRange(buildingId, effectiveFrom, effectiveTo, pageRequest)
            .map(this::toDto);
        return PagedResponseDto.of(result);
    }

    private TelemetryEventDto toDto(TelemetryEventEntity e) {
        return new TelemetryEventDto(
            e.getId(), e.getDeviceId(), e.getEquipmentId(), e.getPointId(),
            e.getBuildingId(), e.getFloorId(), e.getZoneId(),
            e.getNumericValue(), e.getStringValue(), e.getBooleanValue(),
            e.getUnit(), e.getEventTimestamp(), e.getIngestedAt(),
            e.getDataQuality(), e.getCommunicationStatus(),
            e.getSourceProtocol(), e.getSourceDevice(),
            e.isDuplicate(), e.isOutOfOrder()
        );
    }
}
