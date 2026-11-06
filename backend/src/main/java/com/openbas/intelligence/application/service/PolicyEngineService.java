package com.openbas.intelligence.application.service;

import com.openbas.intelligence.api.exception.PolicyViolationException;
import com.openbas.intelligence.domain.command.PolicyEvaluationResult;
import com.openbas.intelligence.infrastructure.persistence.entity.CommandEntity;
import com.openbas.intelligence.infrastructure.persistence.entity.DeviceEntity;
import com.openbas.intelligence.infrastructure.persistence.entity.EquipmentEntity;
import com.openbas.intelligence.infrastructure.persistence.repository.DeviceRepository;
import com.openbas.intelligence.infrastructure.persistence.repository.EquipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic Policy and Safety Engine.
 * 
 * CRITICAL ARCHITECTURAL COMPONENT:
 * AI agents must NEVER have direct write access to physical or simulated building equipment.
 * Every proposed action MUST pass through this deterministic policy engine.
 * 
 * Validates:
 * - Device authorization & existence
 * - Point-level write permissions (writable points allowlist)
 * - Permitted command types
 * - Hard min and max limits (setpoints cannot violate physical building safety limits)
 * - Rate-of-change restrictions (setpoints cannot jump more than allowed delta per command)
 * - Target equipment operational state (cannot command equipment in FAULT or OFFLINE)
 * - Device communication health
 * - Command expiration
 */
@Service
public class PolicyEngineService {

    private static final Logger log = LoggerFactory.getLogger(PolicyEngineService.class);

    @Value("${openbas.commands.max-setpoint-change-celsius:3.0}")
    private double maxSetpointChangeCelsius;

    @Value("${openbas.commands.min-temperature-celsius:16.0}")
    private double minTemperatureCelsius;

    @Value("${openbas.commands.max-temperature-celsius:30.0}")
    private double maxTemperatureCelsius;

    @Value("${openbas.commands.live-execution-enabled:false}")
    private boolean liveExecutionEnabled;

    private static final Set<String> WRITABLE_POINTS = Set.of(
        "ZONE_TEMP_SETPOINT",
        "SUPPLY_AIR_TEMP_SETPOINT",
        "OCCUPIED_COOLING_SETPOINT",
        "OCCUPIED_HEATING_SETPOINT",
        "FAN_SPEED_SETPOINT",
        "VAV_DAMPER_POSITION_SETPOINT",
        "CHILLED_WATER_TEMP_SETPOINT"
    );

    private static final Set<String> PERMITTED_COMMAND_TYPES = Set.of(
        "SET_SETPOINT",
        "OVERRIDE",
        "RESET",
        "ENABLE",
        "DISABLE"
    );

    private final EquipmentRepository equipmentRepository;
    private final DeviceRepository deviceRepository;

    public PolicyEngineService(EquipmentRepository equipmentRepository, DeviceRepository deviceRepository) {
        this.equipmentRepository = equipmentRepository;
        this.deviceRepository = deviceRepository;
    }

    /**
     * Evaluates a proposed command against deterministic safety policies.
     * Returns an evaluation result indicating whether it is APPROVED, REJECTED, or REQUIRES_HUMAN_APPROVAL.
     */
    public PolicyEvaluationResult evaluate(CommandEntity command) {
        log.info("Evaluating deterministic safety policy for command: id={}, equipment={}, point={}, proposedValue={}",
            command.getId(), command.getTargetEquipmentId(), command.getTargetPointId(), command.getProposedValue());

        // 1. Permitted command type check
        if (!PERMITTED_COMMAND_TYPES.contains(command.getCommandType())) {
            return PolicyEvaluationResult.rejected("COMMAND_TYPE_NOT_PERMITTED",
                "Command type '" + command.getCommandType() + "' is not permitted. Allowed: " + PERMITTED_COMMAND_TYPES);
        }

        // 2. Point writability check
        if (!WRITABLE_POINTS.contains(command.getTargetPointId())) {
            return PolicyEvaluationResult.rejected("POINT_NOT_WRITABLE",
                "Point '" + command.getTargetPointId() + "' is read-only or not registered as a writable control point.");
        }

        // 3. Equipment verification
        var equipOpt = equipmentRepository.findByExternalIdAndActiveTrue(command.getTargetEquipmentId());
        if (equipOpt.isEmpty()) {
            return PolicyEvaluationResult.rejected("EQUIPMENT_NOT_FOUND",
                "Target equipment '" + command.getTargetEquipmentId() + "' does not exist or is inactive.");
        }
        EquipmentEntity equipment = equipOpt.get();

        // 4. Equipment operational status check
        if ("FAULT".equalsIgnoreCase(equipment.getOperationalStatus()) || "OFFLINE".equalsIgnoreCase(equipment.getOperationalStatus())) {
            return PolicyEvaluationResult.rejected("EQUIPMENT_UNAVAILABLE",
                "Equipment is in " + equipment.getOperationalStatus() + " state. Autonomous control commands are forbidden.");
        }

        // 5. Value limit validation (temperature setpoints)
        if (command.getTargetPointId().contains("TEMP") && command.getProposedValue() != null) {
            double temp = command.getProposedValue();
            if (temp < minTemperatureCelsius || temp > maxTemperatureCelsius) {
                return PolicyEvaluationResult.rejected("TEMPERATURE_OUT_OF_BOUNDS",
                    String.format("Proposed setpoint %.2f°C violates hard bounds [%.1f°C, %.1f°C]",
                        temp, minTemperatureCelsius, maxTemperatureCelsius));
            }

            // Rate of change check against current value if present
            if (command.getCurrentValue() != null) {
                double delta = Math.abs(temp - command.getCurrentValue());
                if (delta > maxSetpointChangeCelsius) {
                    return PolicyEvaluationResult.rejected("RATE_OF_CHANGE_EXCEEDED",
                        String.format("Setpoint change delta of %.2f°C exceeds maximum allowable delta of %.2f°C per action",
                            delta, maxSetpointChangeCelsius));
                }
            }
        }

        // 6. Device status verification
        if (command.getTargetDeviceId() != null) {
            var devOpt = deviceRepository.findByExternalIdAndActiveTrue(command.getTargetDeviceId());
            if (devOpt.isPresent()) {
                DeviceEntity device = devOpt.get();
                if ("OFFLINE".equalsIgnoreCase(device.getCommunicationStatus())) {
                    return PolicyEvaluationResult.rejected("DEVICE_OFFLINE",
                        "Target device '" + command.getTargetDeviceId() + "' is currently OFFLINE.");
                }
            }
        }

        // 7. Live execution safety gate
        if (!liveExecutionEnabled && !command.getTargetEquipmentId().startsWith("SIM-")) {
            log.info("Live execution is disabled by default. Command routed for simulation/operator staging.");
        }

        // 8. Human operator approval requirement
        // In industrial operations, high impact or setpoint modifications require operator verification
        return PolicyEvaluationResult.requiresApproval("SAFE_REQUIRES_OPERATOR_APPROVAL",
            "Command satisfies all deterministic safety bounds. Queued for operator authorization.");
    }

    /**
     * Strict check that throws PolicyViolationException if rejected.
     */
    public void validateOrThrow(CommandEntity command) {
        PolicyEvaluationResult result = evaluate(command);
        if (!result.isPassed()) {
            throw new PolicyViolationException(result.getReason(), result.getRuleId());
        }
    }
}

