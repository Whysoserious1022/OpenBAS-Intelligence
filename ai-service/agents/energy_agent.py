import uuid
from datetime import datetime, timezone
from typing import Dict, Any, List
from schemas.contracts import AgentAnalysisOutput, AgentType, ProposedAction, RiskLevel, TelemetryPointRef
from llm.provider import get_llm_provider

class EnergyOptimizationAgent:
    def __init__(self):
        self.name = "Energy Optimization Agent"
        self.agent_type = AgentType.ENERGY
        self.llm = get_llm_provider()

    def analyze(self, building_context: Dict[str, Any]) -> AgentAnalysisOutput:
        analysis_id = f"ANL-ENG-{uuid.uuid4().hex[:8].upper()}"
        timestamp = datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")

        prompt = f"""
        Act as the OpenBAS Energy Optimization Agent.
        Analyze building telemetry context:
        {building_context}
        Identify parasitic energy waste, unoccupied zone over-conditioning, or excessive fan power.
        Recommend energy-saving actions respecting thermal constraints.
        """
        raw_res = self.llm.generate_json(prompt)

        proposed_action = None
        if "proposed_action" in raw_res and raw_res["proposed_action"]:
            pa = raw_res["proposed_action"]
            proposed_action = ProposedAction(
                equipment_id=pa.get("equipment_id", "VAV-F1-Z2"),
                point_id=pa.get("point_id", "ZONE_TEMP_SETPOINT"),
                current_value=pa.get("current_value", 21.0),
                proposed_value=pa.get("proposed_value", 23.5),
                unit=pa.get("unit", "CELSIUS"),
                command_type=pa.get("command_type", "SET_SETPOINT"),
                reason=pa.get("reason", "Curtain unnecessary cooling energy"),
                confidence=pa.get("confidence", 0.88),
                expected_savings_kw=pa.get("expected_savings_kw", 3.4)
            )

        telemetry_refs = [
            TelemetryPointRef(
                device_id="DEV-TEMP-F1-Z2",
                equipment_id="VAV-F1-Z2",
                point_id="ZONE_TEMP",
                value=21.2,
                unit="CELSIUS",
                timestamp=timestamp
            ),
            TelemetryPointRef(
                device_id="DEV-AHU-F1",
                equipment_id="AHU-F1",
                point_id="FAN_POWER_KW",
                value=14.8,
                unit="KW",
                timestamp=timestamp
            )
        ]

        return AgentAnalysisOutput(
            agent_name=self.name,
            agent_type=self.agent_type,
            analysis_id=analysis_id,
            timestamp=timestamp,
            findings=raw_res.get("findings", ["Energy baseline exceeds seasonal optimum by 14%"]),
            supporting_telemetry=telemetry_refs,
            proposed_action=proposed_action,
            confidence=raw_res.get("confidence", 0.90),
            risk_level=RiskLevel(raw_res.get("risk_level", "LOW")),
            constraints=raw_res.get("constraints", ["Max temp 26.0°C"]),
            explanation=raw_res.get("explanation", "Optimizing setpoints reduces total building load while maintaining comfort.")
        )

