package com.openbas.intelligence.api.exception;

public class PolicyViolationException extends RuntimeException {
    private final String violatedRule;

    public PolicyViolationException(String message, String violatedRule) {
        super(message);
        this.violatedRule = violatedRule;
    }

    public String getViolatedRule() { return violatedRule; }
}
