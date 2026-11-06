# ADR-003: Multi-Agent Conflict Resolution Hierarchy

## Context
Specialized agents often generate contradictory recommendations for the same building zone.
- **Energy Optimization Agent**: Seeks to raise zone temperature setpoint to minimize electrical draw.
- **Comfort Monitoring Agent**: Seeks to lower setpoint in high-occupancy zones to prevent thermal discomfort.
- **Predictive Maintenance Agent**: Seeks to throttle equipment speeds to avoid mechanical failure.

## Decision
We implemented a **Central Agent Orchestrator** enforcing a deterministic priority hierarchy:

```
Priority 1: Safety & Asset Protection (Maintenance Agent throttle)
       ↓
Priority 2: Occupant Comfort in verified occupied spaces (Comfort Agent)
       ↓
Priority 3: Energy Optimization (Energy Agent relaxation)
```

## Guarantees
- No agent may silently overwrite another agent's recommendation.
- When an override occurs, the conflict, rationale, and discarded alternative are permanently recorded in the `OrchestratedDecision` contract.

