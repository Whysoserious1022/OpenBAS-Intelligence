import uuid
from datetime import datetime, timezone
from typing import Dict, Any, List
from schemas.contracts import AgentAnalysisOutput, AgentType, ProposedAction, RiskLevel, TelemetryPointRef
from llm.provider import get_llm_provider

class PredictiveMaintenanceAgent:
    def __init__(self):
        self.name = "Predictive Maintenance Agent"
        self.agent_type = AgentType.MAINTENANCE
        self.llm = get_llm_provider()

    def analyze(self, building_context: Dict[str, Any]) -> AgentAnalysisOutput:
        analysis_id = f"ANL-MNT-{uuid.uuid4().hex[:8].upper()}"
        timestamp = datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")

        prompt = f"""
        Act as the OpenBAS Predictive Maintenance Agent.
        Analyze equipment health indicators: vibration, filter differential pressures, fan curves, actuator tracking:
        {building_context}
        Identify component degradation, calculate severity, and advise preventative throttling or maintenance.
        """
        raw_res = self.llm.generate_json(prompt)

        proposed_action = None
        if "proposed_action" in raw_res and raw_res["proposed_action"]:
            pa = raw_res["proposed_action"]
            proposed_action = ProposedAction(
                equipment_id=pa.get("equipment_id", "AHU-F2"),
                point_id=pa.get("point_id", "FAN_SPEED_SETPOINT"),
                current_value=pa.get("current_value", 85.0),
                proposed_value=pa.get("proposed_value", 70.0),
                unit=pa.get("unit", "PERCENT"),
                command_type=pa.get("command_type", "SET_SETPOINT"),
                reason=pa.get("reason", "Throttle fan speed to prevent motor overload due to filter loading"),
                confidence=pa.get("confidence", 0.85),
                expected_savings_kw=pa.get("expected_savings_kw", 2.1)
            )

        telemetry_refs = [
            TelemetryPointRef(
                device_id="DEV-AHU-F2",
                equipment_id="AHU-F2",
                point_id="FILTER_DP_PA",
                value=280.0,
                unit="PASCAL",
                timestamp=timestamp
            ),
            TelemetryPointRef(
                device_id="DEV-AHU-F2",
                equipment_id="AHU-F2",
                point_id="MOTOR_VIBRATION_RMS",
                value=4.6,
                unit="MM_S",
                timestamp=timestamp
            )
        ]

        return AgentAnalysisOutput(
            agent_name=self.name,
            agent_type=self.agent_type,
            analysis_id=analysis_id,
            timestamp=timestamp,
            findings=raw_res.get("findings", ["Filter differential pressure high (280 Pa)"]),
            supporting_telemetry=telemetry_refs,
            proposed_action=proposed_action,
            confidence=raw_res.get("confidence", 0.89),
            risk_level=RiskLevel(raw_res.get("risk_level", "MEDIUM")),
            constraints=raw_res.get("constraints", ["Prevent motor stall"]),
            explanation=raw_res.get("explanation", "Derating fan speed protects bearings while dispatching service.")
        )

