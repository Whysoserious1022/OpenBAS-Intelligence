package com.openbas.intelligence.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TelemetryEventDto(
    UUID id,
    String deviceId,
    String equipmentId,
    String pointId,
    String buildingId,
    String floorId,
    String zoneId,
    Double numericValue,
    String stringValue,
    Boolean booleanValue,
    String unit,
    Instant eventTimestamp,
    Instant ingestedAt,
    String dataQuality,
    String communicationStatus,
    String sourceProtocol,
    String sourceDevice,
    boolean duplicate,
    boolean outOfOrder
) {}
