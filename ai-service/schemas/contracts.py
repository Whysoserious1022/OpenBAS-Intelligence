from enum import Enum
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field
from datetime import datetime

class AgentType(str, Enum):
    ENERGY = "ENERGY_OPTIMIZATION_AGENT"
    COMFORT = "COMFORT_MONITORING_AGENT"
    MAINTENANCE = "PREDICTIVE_MAINTENANCE_AGENT"
    ROOT_CAUSE = "ROOT_CAUSE_ENGINE"
    ORCHESTRATOR = "AGENT_ORCHESTRATOR"

class RiskLevel(str, Enum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    CRITICAL = "CRITICAL"

class TelemetryPointRef(BaseModel):
    device_id: str
    equipment_id: str
    point_id: str
    value: float
    unit: str
    timestamp: str

class ProposedAction(BaseModel):
    equipment_id: str
    point_id: str
    current_value: Optional[float] = None
    proposed_value: float
    unit: str = "CELSIUS"
    command_type: str = "SET_SETPOINT"
    reason: str
    confidence: float = Field(ge=0.0, le=1.0)
    expected_savings_kw: Optional[float] = 0.0

class AgentAnalysisOutput(BaseModel):
    agent_name: str
    agent_type: AgentType
    analysis_id: str
    timestamp: str
    findings: List[str]
    supporting_telemetry: List[TelemetryPointRef] = []
    proposed_action: Optional[ProposedAction] = None
    confidence: float = Field(ge=0.0, le=1.0)
    risk_level: RiskLevel
    constraints: List[str] = []
    explanation: str

class RootCauseDiagnosis(BaseModel):
    diagnosis_id: str
    suspected_root_cause: str
    confidence: float = Field(ge=0.0, le=1.0)
    supporting_evidence: List[str]
    affected_equipment: List[str]
    affected_zones: List[str]
    alternative_explanations: List[str]
    recommended_investigation: str

class ConflictDetail(BaseModel):
    agent_a: str
    proposal_a: str
    agent_b: str
    proposal_b: str
    conflict_type: str
    resolution: str

class OrchestratedDecision(BaseModel):
    decision_id: str
    timestamp: str
    incident_id: str
    participating_agents: List[str]
    has_conflicts: bool
    conflicts: List[ConflictDetail] = []
    selected_recommendation: Optional[ProposedAction] = None
    selection_rationale: str
    safety_policy_status: str  # APPROVED | REQUIRES_OPERATOR_APPROVAL | REJECTED
    deterministic_validation_notes: List[str] = []
    root_cause_diagnosis: Optional[RootCauseDiagnosis] = None

class NaturalLanguageQueryRequest(BaseModel):
    query: str
    building_id: str = "BUILDING-01"
    floor_id: Optional[str] = None
    zone_id: Optional[str] = None
    user_role: str = "OPERATOR"

class NaturalLanguageQueryResponse(BaseModel):
    answer: str
    intent: str
    evidence_points: List[str] = []
    suggested_actions: List[str] = []
    confidence: float

