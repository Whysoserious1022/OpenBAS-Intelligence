import pytest
import sys
import os

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from building.building_model import SimulatedBuilding
from scenarios.scenario_runner import ScenarioRunner

def test_building_structure_initialization():
    bldg = SimulatedBuilding("BUILDING-01")
    assert len(bldg.ahus) == 5
    assert len(bldg.vavs) == 20
    assert bldg.chiller.equipment_id == "CHILLER-01"

def test_simulation_step_generates_telemetry():
    bldg = SimulatedBuilding("BUILDING-01")
    records = bldg.step(ambient_temp=26.0, hour_of_day=14.0)
    
    assert len(records) > 80 # ~90 telemetry points generated per minute
    points = [r["pointId"] for r in records]
    assert "SUPPLY_AIR_TEMP" in points
    assert "ZONE_TEMP" in points
    assert "CHILLED_WATER_SUPPLY_TEMP" in points

def test_fault_scenario_chiller_valve_stuck():
    bldg = SimulatedBuilding("BUILDING-01")
    runner = ScenarioRunner(bldg)
    runner.apply_scenario("chiller_valve_stuck")
    
    assert bldg.ahus["AHU-F3"].is_faulted is True
    assert bldg.ahus["AHU-F3"].fault_type == "VALVE_STUCK_15PCT"
    
    # Run 5 steps to allow thermal drift
    for _ in range(5):
        bldg.step(ambient_temp=30.0, hour_of_day=14.0)
        
    # With valve at 15%, supply air should be warmer (> 17°C instead of nominal 13°C)
    assert bldg.ahus["AHU-F3"].supply_air_temp > 17.0
    # Downstream zone should heat up
    assert bldg.vavs["VAV-F3-Z1"].zone_temp > 24.5

def test_fault_scenario_filter_clogged():
    bldg = SimulatedBuilding("BUILDING-01")
    runner = ScenarioRunner(bldg)
    runner.apply_scenario("filter_clogged")
    
    bldg.step(ambient_temp=25.0, hour_of_day=10.0)
    ahu2 = bldg.ahus["AHU-F2"]
    
    assert ahu2.filter_dp_pa > 250.0 # High differential pressure
    assert ahu2.vibration_rms_mm_s > 4.0

