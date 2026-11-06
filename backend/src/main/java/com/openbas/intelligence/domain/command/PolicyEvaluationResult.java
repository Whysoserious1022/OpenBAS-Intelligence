package com.openbas.intelligence.domain.command;

public class PolicyEvaluationResult {

    public enum Status {
        APPROVED,
        REJECTED,
        REQUIRES_APPROVAL
    }

    private final Status status;
    private final String ruleId;
    private final String reason;

    private PolicyEvaluationResult(Status status, String ruleId, String reason) {
        this.status = status;
        this.ruleId = ruleId;
        this.reason = reason;
    }

    public static PolicyEvaluationResult approved(String ruleId, String reason) {
        return new PolicyEvaluationResult(Status.APPROVED, ruleId, reason);
    }

    public static PolicyEvaluationResult rejected(String ruleId, String reason) {
        return new PolicyEvaluationResult(Status.REJECTED, ruleId, reason);
    }

    public static PolicyEvaluationResult requiresApproval(String ruleId, String reason) {
        return new PolicyEvaluationResult(Status.REQUIRES_APPROVAL, ruleId, reason);
    }

    public boolean isPassed() {
        return status == Status.APPROVED || status == Status.REQUIRES_APPROVAL;
    }

    public Status getStatus() {
        return status;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getReason() {
        return reason;
    }
}

