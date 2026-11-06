import logging
from typing import List, Dict, Any
from building.building_model import SimulatedBuilding

logger = logging.getLogger("OpenBAS-ScenarioRunner")

class ScenarioRunner:
    """
    Manages repeatable fault-injection and operational scenarios.
    Allows exact replication of benchmark and debugging conditions.
    """
    SCENARIOS = [
        "normal_operation",
        "chiller_valve_stuck",
        "filter_clogged",
        "sensor_drift",
        "unoccupied_overcooling"
    ]

    def __init__(self, building: SimulatedBuilding):
        self.building = building
        self.active_scenario = "normal_operation"

    def apply_scenario(self, scenario_name: str):
        logger.info(f"Applying scenario: {scenario_name}")
        self.active_scenario = scenario_name
        
        # Reset all equipment faults first
        for ahu in self.building.ahus.values():
            ahu.clear_fault()
        for vav in self.building.vavs.values():
            vav.sensor_drift_celsius = 0.0

        if scenario_name == "normal_operation":
            pass
        elif scenario_name == "chiller_valve_stuck":
            # Floor 3 AHU chilled water valve actuator jammed at 15% open
            self.building.ahus["AHU-F3"].inject_fault("VALVE_STUCK_15PCT")
            logger.info("Fault injected: AHU-F3 cooling valve jammed at 15% open")
        elif scenario_name == "filter_clogged":
            # AHU-F2 loaded filter bank
            self.building.ahus["AHU-F2"].inject_fault("FILTER_CLOGGED")
            logger.info("Fault injected: AHU-F2 air filters clogged (>280 Pa)")
        elif scenario_name == "sensor_drift":
            # Zone 2-1 temperature sensor reading +4.0°C higher than true physical temperature
            self.building.vavs["VAV-F2-Z1"].sensor_drift_celsius = 4.2
            logger.info("Fault injected: VAV-F2-Z1 temperature sensor +4.2°C drift")
        elif scenario_name == "unoccupied_overcooling":
            # Setpoint forced low despite night/unoccupied hours
            self.building.vavs["VAV-F1-Z2"].zone_temp_setpoint = 19.5
            logger.info("Anomaly injected: VAV-F1-Z2 setpoint 19.5°C in low occupancy")
        else:
            raise ValueError(f"Unknown scenario: {scenario_name}. Available: {self.SCENARIOS}")

