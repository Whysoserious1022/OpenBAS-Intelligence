# ADR-002: Deterministic Safety Layer Between AI Agents and Gateway

## Context
Generative AI models and autonomous multi-agent systems are probabilistic and prone to hallucination, unexpected reasoning loops, or extreme outputs. In building automation, an unchecked setpoint command could freeze chillers, overheat servers, burst chilled water coils, or violate occupant life safety.

## Decision
**AI agents are forbidden from having direct communication channels with industrial protocol adapters or physical equipment.**

All commands must pass through a Java-enforced, deterministic `PolicyEngineService`.

## Enforcement Mechanics
1. **Allowlist of Writable Points**: Only pre-registered setpoints can be written (e.g. `ZONE_TEMP_SETPOINT`, `FAN_SPEED_SETPOINT`).
2. **Physical Bounds**: Temperature setpoints are restricted to `[16.0°C, 30.0°C]`.
3. **Rate of Change Limits**: Maximum allowable change per command is `≤ 3.0°C`.
4. **Equipment Availability**: Commands targeted at equipment in `FAULT` or `OFFLINE` status are rejected immediately.
5. **Human Operator Authorization**: By default, commands require operator sign-off before gateway dispatch.
6. **Immutable Audit Log**: Every proposed, approved, rejected, and executed transition is logged to `command_audit_events`.

