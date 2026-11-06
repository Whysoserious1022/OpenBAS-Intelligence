package com.openbas.intelligence.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommandDto(
    UUID id,
    String idempotencyKey,
    String targetEquipmentId,
    String targetDeviceId,
    String targetPointId,
    String buildingId,
    String commandType,
    Double currentValue,
    Double proposedValue,
    String unit,
    String reason,
    String expectedBenefit,
    String potentialRisk,
    String initiatingAgent,
    Double confidence,
    String status,
    String policyValidationResult,
    String policyRejectionReason,
    boolean digitalTwinSimulated,
    String digitalTwinPredictedOutcome,
    String requiredApprovalLevel,
    String approvedBy,
    Instant approvedAt,
    String rejectedBy,
    Instant rejectedAt,
    String rejectionReason,
    Instant executedAt,
    String executionResult,
    Double verifiedValue,
    Instant verifiedAt,
    Instant expiresAt,
    String proposedBy,
    Instant createdAt,
    Instant updatedAt
) {}
