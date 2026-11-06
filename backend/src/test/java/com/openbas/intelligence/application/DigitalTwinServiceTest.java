package com.openbas.intelligence.application;

import com.openbas.intelligence.application.service.DigitalTwinService;
import com.openbas.intelligence.infrastructure.persistence.entity.CommandEntity;
import com.openbas.intelligence.infrastructure.persistence.repository.EquipmentRepository;
import com.openbas.intelligence.infrastructure.persistence.repository.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("DigitalTwinService Physical Simulation Tests")
class DigitalTwinServiceTest {

    @Mock private EquipmentRepository equipmentRepository;
    @Mock private ZoneRepository zoneRepository;

    private DigitalTwinService digitalTwin;

    @BeforeEach
    void setUp() {
        digitalTwin = new DigitalTwinService(equipmentRepository, zoneRepository);
    }

    @Test
    @DisplayName("Simulating setpoint increase predicts thermal settling and energy curtailment")
    void simulateAction_temperatureSetpoint_predictsSavingsAndThermalSettling() {
        CommandEntity cmd = new CommandEntity();
        cmd.setTargetEquipmentId("VAV-F1-Z1");
        cmd.setTargetPointId("ZONE_TEMP_SETPOINT");
        cmd.setCurrentValue(21.0);
        cmd.setProposedValue(23.0); // +2°C

        var result = digitalTwin.simulateAction(cmd);

        assertThat(result.safeToExecute()).isTrue();
        assertThat(result.modelType()).isEqualTo("FIRST_ORDER_THERMAL_RC_MODEL");
        assertThat(result.predictedZoneTempCelsius()).isBetween(22.0, 23.5);
        assertThat(result.estimatedEnergySavingsPct()).isGreaterThan(10.0); // ~13%
        assertThat(result.predictedOutcome()).contains("1st-Order RC Model");
    }

    @Test
    @DisplayName("Simulating high setpoint increase flags MEDIUM or HIGH comfort risk")
    void simulateAction_excessiveTemperatureSetpoint_flagsComfortRisk() {
        CommandEntity cmd = new CommandEntity();
        cmd.setTargetEquipmentId("VAV-F1-Z1");
        cmd.setTargetPointId("ZONE_TEMP_SETPOINT");
        cmd.setCurrentValue(22.0);
        cmd.setProposedValue(26.0); // +4°C

        var result = digitalTwin.simulateAction(cmd);

        assertThat(result.comfortRiskLevel()).isIn("MEDIUM", "HIGH");
    }

    @Test
    @DisplayName("Simulating fan speed modulation uses cubic Fan Affinity Law")
    void simulateAction_fanSpeed_usesCubicAffinityLaw() {
        CommandEntity cmd = new CommandEntity();
        cmd.setTargetEquipmentId("AHU-F2");
        cmd.setTargetPointId("FAN_SPEED_SETPOINT");
        cmd.setCurrentValue(100.0);
        cmd.setProposedValue(80.0); // 0.8 ratio -> 0.8^3 = 0.512 (approx 48.8% power reduction)

        var result = digitalTwin.simulateAction(cmd);

        assertThat(result.modelType()).isEqualTo("FAN_AFFINITY_CUBIC_MODEL");
        assertThat(result.predictedOutcome()).contains("Fan Affinity Law P ~ N^3");
        assertThat(result.estimatedEnergySavingsPct()).isGreaterThan(40.0);
    }
}
