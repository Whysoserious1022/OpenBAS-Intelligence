package com.openbas.intelligence.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for building control commands.
 * Commands follow a strict lifecycle:
 * PROPOSED → VALIDATED → PENDING_APPROVAL → APPROVED → EXECUTING → EXECUTED
 * Also: REJECTED, EXPIRED, FAILED, CANCELLED, VERIFICATION_FAILED
 *
 * Critical: AI agents cannot directly write to this table with APPROVED status.
 * All state transitions are enforced by the PolicyEngine service.
 */
@Entity
@Table(name = "commands", indexes = {
    @Index(name = "idx_commands_status", columnList = "status"),
    @Index(name = "idx_commands_equipment_id", columnList = "target_equipment_id"),
    @Index(name = "idx_commands_created_at", columnList = "created_at"),
    @Index(name = "idx_commands_proposed_by", columnList = "proposed_by"),
    @Index(name = "idx_commands_expires_at", columnList = "expires_at")
})
public class CommandEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    // ── Target ─────────────────────────────────────────────────────────────────
    @Column(name = "target_equipment_id", nullable = false, length = 100)
    private String targetEquipmentId;

    @Column(name = "target_device_id", length = 100)
    private String targetDeviceId;

    @Column(name = "target_point_id", nullable = false, length = 100)
    private String targetPointId;

    @Column(name = "building_id", nullable = false, length = 100)
    private String buildingId;

    // ── Command ────────────────────────────────────────────────────────────────
    @Column(name = "command_type", nullable = false, length = 50)
    private String commandType;  // SET_SETPOINT, OVERRIDE, RESET, ENABLE, DISABLE

    @Column(name = "current_value")
    private Double currentValue;

    @Column(name = "proposed_value")
    private Double proposedValue;

    @Column(name = "proposed_string_value", length = 100)
    private String proposedStringValue;

    @Column(name = "unit", length = 30)
    private String unit;

    // ── Context ────────────────────────────────────────────────────────────────
    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Column(name = "expected_benefit", length = 500)
    private String expectedBenefit;

    @Column(name = "potential_risk", length = 500)
    private String potentialRisk;

    @Column(name = "initiating_agent", length = 100)
    private String initiatingAgent;

    @Column(name = "agent_analysis_id", length = 100)
    private String agentAnalysisId;

    @Column(name = "confidence")
    private Double confidence;

    // ── Lifecycle ──────────────────────────────────────────────────────────────
    @Column(name = "status", nullable = false, length = 30)
    private String status = "PROPOSED";

    @Column(name = "policy_validation_result", length = 30)
    private String policyValidationResult;  // APPROVED, REJECTED, REQUIRES_APPROVAL

    @Column(name = "policy_rejection_reason", length = 1000)
    private String policyRejectionReason;

    @Column(name = "digital_twin_simulated", nullable = false)
    private boolean digitalTwinSimulated = false;

    @Column(name = "digital_twin_predicted_outcome", length = 500)
    private String digitalTwinPredictedOutcome;

    // ── Approval ───────────────────────────────────────────────────────────────
    @Column(name = "required_approval_level", nullable = false, length = 20)
    private String requiredApprovalLevel = "OPERATOR";  // OPERATOR, ADMIN, AUTO

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_by", length = 100)
    private String rejectedBy;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    // ── Execution ──────────────────────────────────────────────────────────────
    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "execution_result", length = 500)
    private String executionResult;

    @Column(name = "verified_value")
    private Double verifiedValue;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "proposed_by", nullable = false, length = 100)
    private String proposedBy;  // agent name or username

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public CommandEntity() {}

    // Getters and setters (all fields)
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getTargetEquipmentId() { return targetEquipmentId; }
    public void setTargetEquipmentId(String targetEquipmentId) { this.targetEquipmentId = targetEquipmentId; }
    public String getTargetDeviceId() { return targetDeviceId; }
    public void setTargetDeviceId(String targetDeviceId) { this.targetDeviceId = targetDeviceId; }
    public String getTargetPointId() { return targetPointId; }
    public void setTargetPointId(String targetPointId) { this.targetPointId = targetPointId; }
    public String getBuildingId() { return buildingId; }
    public void setBuildingId(String buildingId) { this.buildingId = buildingId; }
    public String getCommandType() { return commandType; }
    public void setCommandType(String commandType) { this.commandType = commandType; }
    public Double getCurrentValue() { return currentValue; }
    public void setCurrentValue(Double currentValue) { this.currentValue = currentValue; }
    public Double getProposedValue() { return proposedValue; }
    public void setProposedValue(Double proposedValue) { this.proposedValue = proposedValue; }
    public String getProposedStringValue() { return proposedStringValue; }
    public void setProposedStringValue(String proposedStringValue) { this.proposedStringValue = proposedStringValue; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getExpectedBenefit() { return expectedBenefit; }
    public void setExpectedBenefit(String expectedBenefit) { this.expectedBenefit = expectedBenefit; }
    public String getPotentialRisk() { return potentialRisk; }
    public void setPotentialRisk(String potentialRisk) { this.potentialRisk = potentialRisk; }
    public String getInitiatingAgent() { return initiatingAgent; }
    public void setInitiatingAgent(String initiatingAgent) { this.initiatingAgent = initiatingAgent; }
    public String getAgentAnalysisId() { return agentAnalysisId; }
    public void setAgentAnalysisId(String agentAnalysisId) { this.agentAnalysisId = agentAnalysisId; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPolicyValidationResult() { return policyValidationResult; }
    public void setPolicyValidationResult(String policyValidationResult) { this.policyValidationResult = policyValidationResult; }
    public String getPolicyRejectionReason() { return policyRejectionReason; }
    public void setPolicyRejectionReason(String policyRejectionReason) { this.policyRejectionReason = policyRejectionReason; }
    public boolean isDigitalTwinSimulated() { return digitalTwinSimulated; }
    public void setDigitalTwinSimulated(boolean digitalTwinSimulated) { this.digitalTwinSimulated = digitalTwinSimulated; }
    public String getDigitalTwinPredictedOutcome() { return digitalTwinPredictedOutcome; }
    public void setDigitalTwinPredictedOutcome(String digitalTwinPredictedOutcome) { this.digitalTwinPredictedOutcome = digitalTwinPredictedOutcome; }
    public String getRequiredApprovalLevel() { return requiredApprovalLevel; }
    public void setRequiredApprovalLevel(String requiredApprovalLevel) { this.requiredApprovalLevel = requiredApprovalLevel; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public String getRejectedBy() { return rejectedBy; }
    public void setRejectedBy(String rejectedBy) { this.rejectedBy = rejectedBy; }
    public Instant getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(Instant rejectedAt) { this.rejectedAt = rejectedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public Instant getExecutedAt() { return executedAt; }
    public void setExecutedAt(Instant executedAt) { this.executedAt = executedAt; }
    public String getExecutionResult() { return executionResult; }
    public void setExecutionResult(String executionResult) { this.executionResult = executionResult; }
    public Double getVerifiedValue() { return verifiedValue; }
    public void setVerifiedValue(Double verifiedValue) { this.verifiedValue = verifiedValue; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public String getProposedBy() { return proposedBy; }
    public void setProposedBy(String proposedBy) { this.proposedBy = proposedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
