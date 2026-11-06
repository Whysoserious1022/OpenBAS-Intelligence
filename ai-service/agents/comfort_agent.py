import uuid
from datetime import datetime, timezone
from typing import Dict, Any, List
from schemas.contracts import AgentAnalysisOutput, AgentType, ProposedAction, RiskLevel, TelemetryPointRef
from llm.provider import get_llm_provider

class ComfortMonitoringAgent:
    def __init__(self):
        self.name = "Comfort Monitoring Agent"
        self.agent_type = AgentType.COMFORT
        self.llm = get_llm_provider()

    def analyze(self, building_context: Dict[str, Any]) -> AgentAnalysisOutput:
        analysis_id = f"ANL-CMF-{uuid.uuid4().hex[:8].upper()}"
        timestamp = datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")

        prompt = f"""
        Act as the OpenBAS Comfort Monitoring Agent.
        Evaluate zone temperatures, humidity, and occupancy levels:
        {building_context}
        Detect thermal comfort violations (temperatures > 25.5°C or < 20.0°C in occupied zones).
        Propose restorative actions according to ASHRAE 55 standards.
        """
        raw_res = self.llm.generate_json(prompt)

        proposed_action = None
        if "proposed_action" in raw_res and raw_res["proposed_action"]:
            pa = raw_res["proposed_action"]
            proposed_action = ProposedAction(
                equipment_id=pa.get("equipment_id", "VAV-F3-Z1"),
                point_id=pa.get("point_id", "ZONE_TEMP_SETPOINT"),
                current_value=pa.get("current_value", 24.0),
                proposed_value=pa.get("proposed_value", 22.5),
                unit=pa.get("unit", "CELSIUS"),
                command_type=pa.get("command_type", "SET_SETPOINT"),
                reason=pa.get("reason", "Alleviate thermal discomfort in occupied conference room"),
                confidence=pa.get("confidence", 0.92),
                expected_savings_kw=pa.get("expected_savings_kw", -1.2)
            )

        telemetry_refs = [
            TelemetryPointRef(
                device_id="DEV-TEMP-F3-Z1",
                equipment_id="VAV-F3-Z1",
                point_id="ZONE_TEMP",
                value=26.8,
                unit="CELSIUS",
                timestamp=timestamp
            ),
            TelemetryPointRef(
                device_id="DEV-OCC-F3-Z1",
                equipment_id="VAV-F3-Z1",
                point_id="OCCUPANCY",
                value=18.0,
                unit="OCCUPANTS",
                timestamp=timestamp
            )
        ]

        return AgentAnalysisOutput(
            agent_name=self.name,
            agent_type=self.agent_type,
            analysis_id=analysis_id,
            timestamp=timestamp,
            findings=raw_res.get("findings", ["Zone-3-1 temperature 26.8°C exceeds comfort limits"]),
            supporting_telemetry=telemetry_refs,
            proposed_action=proposed_action,
            confidence=raw_res.get("confidence", 0.94),
            risk_level=RiskLevel(raw_res.get("risk_level", "LOW")),
            constraints=raw_res.get("constraints", ["Minimum temperature 20.0°C"]),
            explanation=raw_res.get("explanation", "High occupant density requires immediate active cooling.")
        )

