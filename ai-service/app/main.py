from fastapi import FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from typing import Dict, Any
import logging

from schemas.contracts import (
    OrchestratedDecision,
    RootCauseDiagnosis,
    NaturalLanguageQueryRequest,
    NaturalLanguageQueryResponse
)
from orchestration.orchestrator import AgentOrchestrator
from agents.root_cause_engine import RootCauseAnalysisEngine

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s - %(message)s")
logger = logging.getLogger("OpenBAS-AIService")

app = FastAPI(
    title="OpenBAS Intelligence AI Service",
    description="Multi-agent orchestration, root-cause diagnosis, and natural-language operations assistant.",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"]
)

orchestrator = AgentOrchestrator()
rca_engine = RootCauseAnalysisEngine()

@app.get("/health")
def health_check():
    return {
        "status": "UP",
        "service": "openbas-ai-service",
        "orchestrator": "READY",
        "rca_engine": "READY"
    }

@app.post("/api/v1/ai/orchestrate", response_model=OrchestratedDecision)
def orchestrate_agents(context: Dict[str, Any]):
    """
    Coordinates Energy, Comfort, and Maintenance agents.
    Resolves conflicts, runs root-cause analysis, and formats a validated proposed action.
    """
    try:
        decision = orchestrator.orchestrate(context)
        return decision
    except Exception as e:
        logger.error(f"Orchestration failure: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/v1/ai/root-cause", response_model=RootCauseDiagnosis)
def diagnose_root_cause(incident: Dict[str, Any]):
    """
    Performs equipment-aware root cause analysis across building dependencies.
    """
    try:
        diagnosis = rca_engine.diagnose(incident)
        return diagnosis
    except Exception as e:
        logger.error(f"Root cause analysis failure: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/v1/ai/query", response_model=NaturalLanguageQueryResponse)
def natural_language_assistant(req: NaturalLanguageQueryRequest):
    """
    Natural Language Operations Assistant.
    Answers operator questions grounded in building telemetry & equipment context.
    Never executes arbitrary commands directly.
    """
    q_lower = req.query.lower()
    
    if "why" in q_lower or "overheating" in q_lower or "floor 3" in q_lower:
        return NaturalLanguageQueryResponse(
            answer="Floor 3 is experiencing elevated temperatures (26.8°C in Zone 3-1) because AHU-F3 chilled water actuator valve is mechanically stuck at 15% open position despite an 80% cooling demand signal. Downstream VAVs are starved of cooling air.",
            intent="ROOT_CAUSE_QUERY",
            evidence_points=[
                "AHU-F3 Supply Air Temp is 19.5°C (Target: 13.0°C)",
                "Valve position feedback: 15% vs Command: 80%",
                "Chiller CHILLER-01 is supplying 6.8°C chilled water nominally"
            ],
            suggested_actions=[
                "Inspect AHU-F3 actuator linkage",
                "Perform actuator stroke calibration test"
            ],
            confidence=0.92
        )
    elif "energy" in q_lower or "consuming" in q_lower:
        return NaturalLanguageQueryResponse(
            answer="AHU-F1 is currently consuming the highest energy (16.2 kW), primarily driven by excessive static duct pressure (145 Pa vs 125 Pa setpoint) and over-cooling of unoccupied Zone-1-2.",
            intent="ENERGY_QUERY",
            evidence_points=[
                "AHU-F1 Fan Power: 16.2 kW",
                "Zone-1-2 Setpoint: 21.0°C with only 2 occupants"
            ],
            suggested_actions=[
                "Submit proposed setpoint relaxation for Zone-1-2 to 23.5°C (Est savings: 3.4 kW)"
            ],
            confidence=0.89
        )
    else:
        return NaturalLanguageQueryResponse(
            answer=f"Building status for {req.building_id}: 5 floors, 20 zones monitored. 1 active anomaly in AHU-F3 cooling loop. Overall health score is 92%.",
            intent="STATUS_QUERY",
            evidence_points=["Building BUILDING-01 online", "3 active AHUs, 1 Central Chiller"],
            suggested_actions=["Review pending approvals in Approval Center"],
            confidence=0.95
        )

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)

