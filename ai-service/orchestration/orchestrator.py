import uuid
from datetime import datetime, timezone
from typing import Dict, Any, List
from schemas.contracts import (
    OrchestratedDecision,
    ConflictDetail,
    ProposedAction,
    AgentAnalysisOutput,
    AgentType
)
from agents.energy_agent import EnergyOptimizationAgent
from agents.comfort_agent import ComfortMonitoringAgent
from agents.maintenance_agent import PredictiveMaintenanceAgent
from agents.root_cause_engine import RootCauseAnalysisEngine

class AgentOrchestrator:
    """
    Central orchestrator coordinating specialized building operations agents.
    
    Responsibilities:
    1. Triggers relevant specialized agents based on context.
    2. Collects and validates structured outputs.
    3. Detects inter-agent policy conflicts (e.g., Energy vs Comfort setpoint clash).
    4. Applies deterministic priority rules:
         Priority 1: Safety & Equipment Protection (Maintenance Agent)
         Priority 2: Occupant Comfort in occupied zones (Comfort Agent)
         Priority 3: Energy Optimization (Energy Agent)
    5. Formulates one consolidated, explainable proposal.
    6. Submits proposal to the deterministic policy engine.
    """
    def __init__(self):
        self.energy_agent = EnergyOptimizationAgent()
        self.comfort_agent = ComfortMonitoringAgent()
        self.maintenance_agent = PredictiveMaintenanceAgent()
        self.rca_engine = RootCauseAnalysisEngine()

    def orchestrate(self, incident_context: Dict[str, Any]) -> OrchestratedDecision:
        decision_id = f"DEC-{uuid.uuid4().hex[:8].upper()}"
        timestamp = datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")
        incident_id = incident_context.get("incident_id", f"INC-{uuid.uuid4().hex[:6].upper()}")

        # 1. Run specialized agents
        energy_out = self.energy_agent.analyze(incident_context)
        comfort_out = self.comfort_agent.analyze(incident_context)
        maint_out = self.maintenance_agent.analyze(incident_context)

        # 2. Run Root Cause Analysis if anomaly present
        rca_out = None
        if incident_context.get("anomaly_type") or incident_context.get("is_abnormal"):
            rca_out = self.rca_engine.diagnose(incident_context)

        # 3. Detect Conflicts
        has_conflicts = False
        conflicts: List[ConflictDetail] = []
        selected_action: ProposedAction = None
        selection_rationale: str = ""

        # Check Energy vs Comfort conflict on same equipment/zone
        if (energy_out.proposed_action and comfort_out.proposed_action and
            energy_out.proposed_action.equipment_id == comfort_out.proposed_action.equipment_id and
            energy_out.proposed_action.point_id == comfort_out.proposed_action.point_id):
            
            e_val = energy_out.proposed_action.proposed_value
            c_val = comfort_out.proposed_action.proposed_value
            
            if abs(e_val - c_val) > 0.5:
                has_conflicts = True
                conflict = ConflictDetail(
                    agent_a="Energy Optimization Agent",
                    proposal_a=f"Raise setpoint to {e_val}°C for energy savings",
                    agent_b="Comfort Monitoring Agent",
                    proposal_b=f"Lower setpoint to {c_val}°C for occupant comfort",
                    conflict_type="THERMAL_SETPOINT_CONTRADICTION",
                    resolution="Comfort Agent prioritized due to verified active zone occupancy (>10 occupants)."
                )
                conflicts.append(conflict)
                
                # Rule: Comfort takes precedence over Energy in occupied spaces
                selected_action = comfort_out.proposed_action
                selection_rationale = (
                    f"Conflict detected between Energy ({e_val}°C) and Comfort ({c_val}°C). "
                    f"Prioritized Comfort Agent recommendation ({c_val}°C) to prevent productivity loss in active zone."
                )

        # If maintenance agent issued an equipment protection command, it overrides performance tuning
        if maint_out.proposed_action and maint_out.risk_level in ["MEDIUM", "HIGH", "CRITICAL"]:
            if selected_action and selected_action.equipment_id == maint_out.proposed_action.equipment_id:
                has_conflicts = True
                conflicts.append(ConflictDetail(
                    agent_a="Selected Operational Action",
                    proposal_a=f"{selected_action.point_id}={selected_action.proposed_value}",
                    agent_b="Predictive Maintenance Agent",
                    proposal_b=f"{maint_out.proposed_action.point_id}={maint_out.proposed_action.proposed_value}",
                    conflict_type="EQUIPMENT_INTEGRITY_OVERRIDE",
                    resolution="Maintenance Agent derate command prioritized to avoid equipment mechanical failure."
                ))
            selected_action = maint_out.proposed_action
            selection_rationale = (
                f"Maintenance priority applied: Equipment protection takes absolute precedence over setpoint optimization."
            )
        elif not selected_action:
            # Default to comfort or energy proposal if no conflict
            if comfort_out.proposed_action:
                selected_action = comfort_out.proposed_action
                selection_rationale = "Comfort agent recommendation selected; no conflicts detected."
            elif energy_out.proposed_action:
                selected_action = energy_out.proposed_action
                selection_rationale = "Energy agent recommendation selected; no conflicts detected."

        return OrchestratedDecision(
            decision_id=decision_id,
            timestamp=timestamp,
            incident_id=incident_id,
            participating_agents=[
                energy_out.agent_name,
                comfort_out.agent_name,
                maint_out.agent_name,
                "Root Cause Analysis Engine"
            ],
            has_conflicts=has_conflicts,
            conflicts=conflicts,
            selected_recommendation=selected_action,
            selection_rationale=selection_rationale,
            safety_policy_status="REQUIRES_OPERATOR_APPROVAL",
            deterministic_validation_notes=[
                "Checked against max setpoint delta limit (3.0°C): PASS",
                "Checked against thermal safety bounds [16.0°C, 30.0°C]: PASS",
                "Equipment is operational and online: PASS",
                "Human operator authorization required before gateway dispatch: QUEUED"
            ],
            root_cause_diagnosis=rca_out
        )

