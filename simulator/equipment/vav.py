import math
from typing import Dict, Any

class VariableAirVolumeBox:
    """
    Variable Air Volume (VAV) terminal unit model.
    Serves a specific thermal zone.
    Models:
    - Primary damper position (0% - 100%)
    - Zone thermal balance (heat transfer with envelope, solar gain, occupant load)
    - Reheat coil (electric or hot water)
    """
    def __init__(self, equipment_id: str, zone_id: str, max_cfm: float = 1200.0, min_cfm: float = 250.0):
        self.equipment_id = equipment_id
        self.zone_id = zone_id
        self.max_cfm = max_cfm
        self.min_cfm = min_cfm
        
        # State
        self.damper_position_pct = 50.0
        self.reheat_valve_pct = 0.0
        self.zone_temp = 22.5
        self.zone_temp_setpoint = 22.5
        self.zone_humidity = 48.0
        self.occupancy_count = 10
        self.sensor_drift_celsius = 0.0

    def step(self, supply_air_temp: float, ambient_temp: float, hour_of_day: float) -> Dict[str, float]:
        # Occupancy schedule simulation (bell curve during workday 8am - 6pm)
        if 8.0 <= hour_of_day <= 18.0:
            workday_progress = (hour_of_day - 8.0) / 10.0
            occ_factor = math.sin(workday_progress * math.pi)
            self.occupancy_count = int(18 * occ_factor + 2)
            solar_gain_w = 400.0 * occ_factor
        else:
            self.occupancy_count = 1
            solar_gain_w = 20.0

        # Occupant internal thermal load (approx 100W sensible per person)
        internal_heat_w = (self.occupancy_count * 100.0) + solar_gain_w + 300.0 # equipment/lighting

        # Airflow delivered
        current_cfm = self.min_cfm + (self.max_cfm - self.min_cfm) * (self.damper_position_pct / 100.0)
        
        # Cooling delivered to zone: Q = 1.08 * CFM * (T_zone - T_supply)
        cooling_power_w = 1.08 * current_cfm * (self.zone_temp - supply_air_temp) * 0.293 # convert BTU/h to Watts

        # Heat gain from envelope (UA * deltaT)
        envelope_heat_w = 85.0 * (ambient_temp - self.zone_temp)

        # Net thermal rate into zone (C_th * dT/dt)
        net_heat_w = internal_heat_w + envelope_heat_w - cooling_power_w
        zone_thermal_capacitance = 25000.0 # J/K (concrete & furnishings)
        dt_seconds = 60.0
        
        # Update physical zone temperature
        temp_delta = (net_heat_w * dt_seconds) / zone_thermal_capacitance
        self.zone_temp = round(self.zone_temp + temp_delta, 2)
        
        # Thermostat damper controller (proportional feedback)
        error = self.zone_temp - self.zone_temp_setpoint
        if error > 0:
            # Overheating: open damper
            self.damper_position_pct = min(100.0, max(self.damper_position_pct + error * 10.0, 20.0))
        else:
            # Cold: close damper to minimum ventilation
            self.damper_position_pct = max(20.0, self.damper_position_pct + error * 10.0)

        # Measured temperature (subject to sensor drift if fault active)
        measured_temp = round(self.zone_temp + self.sensor_drift_celsius, 2)

        return {
            "ZONE_TEMP": measured_temp,
            "TRUE_ZONE_TEMP": self.zone_temp,
            "ZONE_TEMP_SETPOINT": self.zone_temp_setpoint,
            "VAV_DAMPER_PCT": round(self.damper_position_pct, 1),
            "AIRFLOW_CFM": round(current_cfm, 1),
            "OCCUPANCY": float(self.occupancy_count),
            "ZONE_HUMIDITY": round(self.zone_humidity + (self.occupancy_count * 0.4), 1)
        }

