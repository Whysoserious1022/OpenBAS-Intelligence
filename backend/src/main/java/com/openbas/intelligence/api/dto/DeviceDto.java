package com.openbas.intelligence.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeviceDto(
    UUID id,
    String externalId,
    UUID equipmentId,
    String name,
    String protocol,
    Integer bacnetDeviceId,
    String bacnetIpAddress,
    Integer bacnetPort,
    String mqttTopicPrefix,
    String communicationStatus,
    Instant lastSeenAt,
    Instant lastErrorAt,
    String lastErrorMessage,
    boolean simulated,
    boolean active,
    Instant createdAt
) {}
