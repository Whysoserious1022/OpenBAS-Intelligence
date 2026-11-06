from abc import ABC, abstractmethod
from typing import Dict, Any, Optional
import os
import json
import logging

logger = logging.getLogger(__name__)

class BaseLLMProvider(ABC):
    @abstractmethod
    def generate_json(self, prompt: str, schema: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """Generate structured JSON output adhering to an optional schema."""
        pass

class MockLLMProvider(BaseLLMProvider):
    """
    Deterministic, offline, zero-cost LLM provider.
    Produces technically credible, deterministic building automation reasoning
    without requiring external API tokens or paid cloud endpoints.
    """
    def generate_json(self, prompt: str, schema: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        prompt_lower = prompt.lower()
        
        if "energy" in prompt_lower:
            return {
                "findings": [
                    "Cooling setpoint in Zone-1-2 is 21.0°C despite low occupancy (2 persons vs design 25)",
                    "AHU-F1 static pressure is 145 Pa, exceeding setpoint by 20 Pa",
                    "Unnecessary chiller load observed during intermediate shoulder hours"
                ],
                "proposed_action": {
                    "equipment_id": "VAV-F1-Z2",
                    "point_id": "ZONE_TEMP_SETPOINT",
                    "current_value": 21.0,
                    "proposed_value": 23.5,
                    "unit": "CELSIUS",
                    "command_type": "SET_SETPOINT",
                    "reason": "Relax unoccupied zone cooling setpoint by 2.5°C to curtail parasitic reheat & fan power",
                    "confidence": 0.88,
                    "expected_savings_kw": 3.4
                },
                "confidence": 0.90,
                "risk_level": "LOW",
                "constraints": ["Zone temperature must remain <= 25.5°C", "Rate of change max 3.0°C"],
                "explanation": "Increasing cooling setpoint from 21.0°C to 23.5°C cuts compressor cycle load by 12% while satisfying ASHRAE 55-2023 comfort standards."
            }

        elif "comfort" in prompt_lower:
            return {
                "findings": [
                    "Zone-3-1 temperature is 26.8°C, exceeding upper comfort limit (26.0°C)",
                    "Active occupancy detected: 18 occupants in Conference Room",
                    "Zone humidity is 58% RH (within acceptable bounds)"
                ],
                "proposed_action": {
                    "equipment_id": "VAV-F3-Z1",
                    "point_id": "ZONE_TEMP_SETPOINT",
                    "current_value": 24.0,
                    "proposed_value": 22.5,
                    "unit": "CELSIUS",
                    "command_type": "SET_SETPOINT",
                    "reason": "Pre-cool conference zone to alleviate thermal discomfort during high occupancy",
                    "confidence": 0.92,
                    "expected_savings_kw": -1.2
                },
                "confidence": 0.94,
                "risk_level": "LOW",
                "constraints": ["Cannot lower setpoint below 20.0°C", "Comfort priority over immediate power saving"],
                "explanation": "High thermal load from occupants in Conference Room requires immediate cooling setpoint reduction to restore comfort compliance."
            }

        elif "maintenance" in prompt_lower or "degradation" in prompt_lower:
            return {
                "findings": [
                    "AHU-F2 supply fan vibration harmonics showing early bearing wear signature",
                    "Airflow CFM is 18% below rated curve for given fan RPM and VFD frequency",
                    "Differential pressure across filter bank reached 280 Pa (warning threshold 250 Pa)"
                ],
                "proposed_action": {
                    "equipment_id": "AHU-F2",
                    "point_id": "FAN_SPEED_SETPOINT",
                    "current_value": 85.0,
                    "proposed_value": 70.0,
                    "unit": "PERCENT",
                    "command_type": "SET_SETPOINT",
                    "reason": "Derate fan speed to 70% to prevent bearing overheating until scheduled filter replacement",
                    "confidence": 0.85,
                    "expected_savings_kw": 2.1
                },
                "confidence": 0.89,
                "risk_level": "MEDIUM",
                "constraints": ["Maintain minimum fresh air exchange for Floor 2", "Dispatch technician inspection ticket"],
                "explanation": "High filter differential pressure and elevated motor temp indicate clogged pre-filters. Operating at full capacity risks premature belt failure."
            }

        elif "root_cause" in prompt_lower or "why" in prompt_lower or "cause" in prompt_lower:
            return {
                "suspected_root_cause": "AHU-F3 chilled water actuator valve jammed at 15% open position",
                "confidence": 0.91,
                "supporting_evidence": [
                    "Chilled water valve command signal is 80%, but measured position feedback is 15%",
                    "AHU-F3 supply air temperature is 19.5°C despite full cooling demand",
                    "All 4 downstream VAVs on Floor 3 report supply air starved of cooling capacity",
                    "Chiller CHILLER-01 is supplying 6.8°C water normally to building header"
                ],
                "affected_equipment": ["AHU-F3", "VAV-F3-Z1", "VAV-F3-Z2", "VAV-F3-Z3", "VAV-F3-Z4"],
                "affected_zones": ["ZONE-3-1", "ZONE-3-2", "ZONE-3-3", "ZONE-3-4"],
                "alternative_explanations": [
                    "Chilled water pump PUMP-01 cavitation (ruled out: header pressure normal at 320 kPa)",
                    "Ductwork static pressure collapse (ruled out: supply fan running nominal RPM)"
                ],
                "recommended_investigation": "Inspect physical mechanical linkage of Belimo actuator on AHU-F3 cooling coil valve. Perform manual stroke calibration test."
            }

        # Generic operational answer
        return {
            "answer": "All systems operating within established safety and environmental bounds.",
            "intent": "STATUS_QUERY",
            "confidence": 0.95
        }

def get_llm_provider() -> BaseLLMProvider:
    provider_name = os.getenv("LLM_PROVIDER", "mock").lower()
    if provider_name == "mock":
        return MockLLMProvider()
    # Fallback to mock for offline reliability
    logger.info(f"Provider {provider_name} requested; defaulting to MockLLMProvider for offline deterministic execution.")
    return MockLLMProvider()

