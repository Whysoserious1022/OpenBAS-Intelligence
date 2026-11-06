import math
from typing import Dict, Any

class AirHandlingUnit:
    """
    Air Handling Unit (AHU) physics simulation model.
    Models:
    - Supply air temperature modulation across cooling coil
    - Supply fan speed, airflow CFM, and power kW (Fan Affinity Laws P ~ N^3)
    - Filter differential pressure (clean ~80 Pa, loaded ~280 Pa)
    - Actuator position tracking (command vs feedback)
    """
    def __init__(self, equipment_id: str, floor_id: str, design_cfm: float = 6000.0, rated_power_kw: float = 15.0):
        self.equipment_id = equipment_id
        self.floor_id = floor_id
        self.design_cfm = design_cfm
        self.rated_power_kw = rated_power_kw
        
        # State
        self.fan_speed_pct = 80.0
        self.supply_air_temp = 13.0          # °C
        self.supply_air_temp_setpoint = 13.0 # °C
        self.cooling_valve_command_pct = 40.0
        self.cooling_valve_feedback_pct = 40.0
        self.filter_dp_pa = 110.0            # Clean baseline
        self.static_pressure_pa = 125.0
        self.vibration_rms_mm_s = 1.2
        self.is_faulted = False
        self.fault_type = None

    def inject_fault(self, fault_type: str):
        self.is_faulted = True
        self.fault_type = fault_type

    def clear_fault(self):
        self.is_faulted = False
        self.fault_type = None

    def step(self, return_air_temp: float, chilled_water_supply_temp: float) -> Dict[str, float]:
        # Handle fault scenarios
        if self.fault_type == "VALVE_STUCK_15PCT":
            self.cooling_valve_command_pct = 85.0
            self.cooling_valve_feedback_pct = 15.0 # Stuck!
            effective_cooling = 0.15
        elif self.fault_type == "FILTER_CLOGGED":
            self.filter_dp_pa = 295.0
            self.vibration_rms_mm_s = 4.8
            effective_cooling = self.cooling_valve_feedback_pct / 100.0
        else:
            self.cooling_valve_feedback_pct = self.cooling_valve_command_pct
            self.filter_dp_pa = 110.0 + (self.fan_speed_pct / 100.0) * 15.0
            self.vibration_rms_mm_s = 1.2 + (self.fan_speed_pct / 100.0) * 0.4
            effective_cooling = self.cooling_valve_feedback_pct / 100.0

        # Thermodynamic air mixing & coil heat exchange
        # T_supply = T_return - effective_cooling * (T_return - T_chw) * coil_effectiveness
        coil_effectiveness = 0.82
        cooling_delta = effective_cooling * (return_air_temp - chilled_water_supply_temp) * coil_effectiveness
        self.supply_air_temp = round(return_air_temp - cooling_delta, 2)

        # Fan power via affinity laws
        flow_fraction = (self.fan_speed_pct / 100.0)
        current_cfm = self.design_cfm * flow_fraction
        if self.fault_type == "FILTER_CLOGGED":
            current_cfm *= 0.82 # 18% reduction due to high backpressure
        
        fan_power_kw = self.rated_power_kw * (flow_fraction ** 3)
        self.static_pressure_pa = 125.0 * (flow_fraction ** 2)

        return {
            "SUPPLY_AIR_TEMP": self.supply_air_temp,
            "SUPPLY_AIR_TEMP_SETPOINT": self.supply_air_temp_setpoint,
            "AIRFLOW_CFM": round(current_cfm, 1),
            "FAN_POWER_KW": round(fan_power_kw, 2),
            "FILTER_DP_PA": round(self.filter_dp_pa, 1),
            "STATIC_PRESSURE_PA": round(self.static_pressure_pa, 1),
            "MOTOR_VIBRATION_RMS": round(self.vibration_rms_mm_s, 2),
            "COOLING_VALVE_COMMAND_PCT": self.cooling_valve_command_pct,
            "COOLING_VALVE_FEEDBACK_PCT": self.cooling_valve_feedback_pct
        }

