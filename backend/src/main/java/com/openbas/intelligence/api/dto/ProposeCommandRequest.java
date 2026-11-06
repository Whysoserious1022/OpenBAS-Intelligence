package com.openbas.intelligence.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProposeCommandRequest(
    @NotBlank String targetEquipmentId,
    @NotBlank String targetPointId,
    @NotBlank String commandType,
    @NotNull Double proposedValue,
    String unit,
    String reason,
    String expectedBenefit,
    String potentialRisk,
    String initiatingAgent,
    Double confidence
) {}

