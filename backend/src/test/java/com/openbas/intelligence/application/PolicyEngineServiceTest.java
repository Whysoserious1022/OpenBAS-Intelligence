package com.openbas.intelligence.application;

import com.openbas.intelligence.api.exception.PolicyViolationException;
import com.openbas.intelligence.application.service.PolicyEngineService;
import com.openbas.intelligence.domain.command.PolicyEvaluationResult;
import com.openbas.intelligence.infrastructure.persistence.entity.CommandEntity;
import com.openbas.intelligence.infrastructure.persistence.entity.DeviceEntity;
import com.openbas.intelligence.infrastructure.persistence.entity.EquipmentEntity;
import com.openbas.intelligence.infrastructure.persistence.repository.DeviceRepository;
import com.openbas.intelligence.infrastructure.persistence.repository.EquipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyEngineService Deterministic Safety Tests")
class PolicyEngineServiceTest {

    @Mock private EquipmentRepository equipmentRepository;
    @Mock private DeviceRepository deviceRepository;

    private PolicyEngineService policyEngine;

    @BeforeEach
    void setUp() {
        policyEngine = new PolicyEngineService(equipmentRepository, deviceRepository);
        ReflectionTestUtils.setField(policyEngine, "maxSetpointChangeCelsius", 3.0);
        ReflectionTestUtils.setField(policyEngine, "minTemperatureCelsius", 16.0);
        ReflectionTestUtils.setField(policyEngine, "maxTemperatureCelsius", 30.0);
        ReflectionTestUtils.setField(policyEngine, "liveExecutionEnabled", false);
    }

    @Test
    @DisplayName("Safe setpoint change within bounds passes deterministic evaluation")
    void evaluate_safeSetpointChange_requiresOperatorApproval() {
        CommandEntity cmd = createCommand("VAV-F1-Z1", "ZONE_TEMP_SETPOINT", 22.0, 23.5);
        EquipmentEntity equip = new EquipmentEntity();
        equip.setOperationalStatus("NORMAL");
        when(equipmentRepository.findByExternalIdAndActiveTrue("VAV-F1-Z1")).thenReturn(Optional.of(equip));

        PolicyEvaluationResult result = policyEngine.evaluate(cmd);

        assertThat(result.getStatus()).isEqualTo(PolicyEvaluationResult.Status.REQUIRES_APPROVAL);
        assertThat(result.isPassed()).isTrue();
    }

    @Test
    @DisplayName("Setpoint violating hard minimum temperature (16°C) is deterministically rejected")
    void evaluate_temperatureBelowMin_isRejected() {
        CommandEntity cmd = createCommand("VAV-F1-Z1", "ZONE_TEMP_SETPOINT", 22.0, 14.5);
        EquipmentEntity equip = new EquipmentEntity();
        equip.setOperationalStatus("NORMAL");
        when(equipmentRepository.findByExternalIdAndActiveTrue("VAV-F1-Z1")).thenReturn(Optional.of(equip));

        PolicyEvaluationResult result = policyEngine.evaluate(cmd);

        assertThat(result.getStatus()).isEqualTo(PolicyEvaluationResult.Status.REJECTED);
        assertThat(result.getRuleId()).isEqualTo("TEMPERATURE_OUT_OF_BOUNDS");
        assertThat(result.isPassed()).isFalse();
    }

    @Test
    @DisplayName("Setpoint rate of change exceeding 3.0°C jump is deterministically rejected")
    void evaluate_rateOfChangeExceeded_isRejected() {
        CommandEntity cmd = createCommand("VAV-F1-Z1", "ZONE_TEMP_SETPOINT", 21.0, 25.5); // Delta = 4.5°C > 3.0°C limit
        EquipmentEntity equip = new EquipmentEntity();
        equip.setOperationalStatus("NORMAL");
        when(equipmentRepository.findByExternalIdAndActiveTrue("VAV-F1-Z1")).thenReturn(Optional.of(equip));

        PolicyEvaluationResult result = policyEngine.evaluate(cmd);

        assertThat(result.getStatus()).isEqualTo(PolicyEvaluationResult.Status.REJECTED);
        assertThat(result.getRuleId()).isEqualTo("RATE_OF_CHANGE_EXCEEDED");
    }

    @Test
    @DisplayName("Write to unapproved / read-only point is deterministically rejected")
    void evaluate_nonWritablePoint_isRejected() {
        CommandEntity cmd = createCommand("AHU-01", "MANUFACTURER_FIRMWARE_REGISTER", null, 1.0);

        PolicyEvaluationResult result = policyEngine.evaluate(cmd);

        assertThat(result.getStatus()).isEqualTo(PolicyEvaluationResult.Status.REJECTED);
        assertThat(result.getRuleId()).isEqualTo("POINT_NOT_WRITABLE");
    }

    @Test
    @DisplayName("Command targeted at faulted equipment is deterministically rejected")
    void evaluate_faultedEquipment_isRejected() {
        CommandEntity cmd = createCommand("AHU-F3", "SUPPLY_AIR_TEMP_SETPOINT", 13.0, 14.0);
        EquipmentEntity equip = new EquipmentEntity();
        equip.setOperationalStatus("FAULT");
        when(equipmentRepository.findByExternalIdAndActiveTrue("AHU-F3")).thenReturn(Optional.of(equip));

        PolicyEvaluationResult result = policyEngine.evaluate(cmd);

        assertThat(result.getStatus()).isEqualTo(PolicyEvaluationResult.Status.REJECTED);
        assertThat(result.getRuleId()).isEqualTo("EQUIPMENT_UNAVAILABLE");
    }

    @Test
    @DisplayName("validateOrThrow throws PolicyViolationException on rejected command")
    void validateOrThrow_rejected_throws() {
        CommandEntity cmd = createCommand("VAV-F1-Z1", "ZONE_TEMP_SETPOINT", 22.0, 35.0); // 35°C > 30°C max
        EquipmentEntity equip = new EquipmentEntity();
        equip.setOperationalStatus("NORMAL");
        when(equipmentRepository.findByExternalIdAndActiveTrue("VAV-F1-Z1")).thenReturn(Optional.of(equip));

        assertThatThrownBy(() -> policyEngine.validateOrThrow(cmd))
            .isInstanceOf(PolicyViolationException.class)
            .hasMessageContaining("bounds");
    }

    private CommandEntity createCommand(String equipId, String pointId, Double currVal, Double proposedVal) {
        CommandEntity cmd = new CommandEntity();
        cmd.setId(UUID.randomUUID());
        cmd.setCommandType("SET_SETPOINT");
        cmd.setTargetEquipmentId(equipId);
        cmd.setTargetPointId(pointId);
        cmd.setCurrentValue(currVal);
        cmd.setProposedValue(proposedVal);
        cmd.setReason("AI Optimization Proposal");
        cmd.setProposedBy("ENERGY_OPTIMIZATION_AGENT");
        return cmd;
    }
}

