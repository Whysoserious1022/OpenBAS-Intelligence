import pytest
import sys
import os

# Add ai-service to path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from schemas.contracts import AgentType, RiskLevel
from agents.energy_agent import EnergyOptimizationAgent
from agents.comfort_agent import ComfortMonitoringAgent
from agents.maintenance_agent import PredictiveMaintenanceAgent
from agents.root_cause_engine import RootCauseAnalysisEngine
from orchestration.orchestrator import AgentOrchestrator

def test_energy_agent_analysis():
    agent = EnergyOptimizationAgent()
    context = {"building_id": "BUILDING-01", "unoccupied_zones": ["ZONE-1-2"]}
    result = agent.analyze(context)
    
    assert result.agent_type == AgentType.ENERGY
    assert result.proposed_action is not None
    assert result.proposed_action.proposed_value > 21.0
    assert result.confidence >= 0.8
    assert len(result.supporting_telemetry) > 0

def test_comfort_agent_analysis():
    agent = ComfortMonitoringAgent()
    context = {"building_id": "BUILDING-01", "hot_zones": ["ZONE-3-1"]}
    result = agent.analyze(context)
    
    assert result.agent_type == AgentType.COMFORT
    assert result.proposed_action is not None
    assert result.proposed_action.proposed_value <= 23.0
    assert result.confidence >= 0.8

def test_predictive_maintenance_agent_analysis():
    agent = PredictiveMaintenanceAgent()
    context = {"equipment_id": "AHU-F2", "vibration_high": True}
    result = agent.analyze(context)
    
    assert result.agent_type == AgentType.MAINTENANCE
    assert result.risk_level in [RiskLevel.MEDIUM, RiskLevel.HIGH]
    assert result.proposed_action.command_type == "SET_SETPOINT"

def test_root_cause_diagnosis():
    rca = RootCauseAnalysisEngine()
    incident = {
        "equipment_id": "AHU-F3",
        "anomaly_type": "TEMP_HIGH",
        "zone_temp": 26.8
    }
    diagnosis = rca.diagnose(incident)
    
    assert "AHU-F3" in diagnosis.affected_equipment
    assert diagnosis.confidence >= 0.85
    assert len(diagnosis.supporting_evidence) >= 2
    assert len(diagnosis.alternative_explanations) >= 1

def test_orchestrator_conflict_resolution():
    orchestrator = AgentOrchestrator()
    incident_context = {
        "incident_id": "INC-TEST-01",
        "is_abnormal": True,
        "equipment_id": "AHU-F3"
    }
    decision = orchestrator.orchestrate(incident_context)
    
    assert decision.decision_id.startswith("DEC-")
    assert decision.incident_id == "INC-TEST-01"
    assert len(decision.participating_agents) >= 3
    assert decision.safety_policy_status == "REQUIRES_OPERATOR_APPROVAL"
    assert decision.selected_recommendation is not None
    assert len(decision.deterministic_validation_notes) > 0

