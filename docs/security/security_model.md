# OpenBAS Intelligence — Security & Authorization Model

## Architecture
The platform is architected according to defense-in-depth principles:

```
[ Operator / Dashboard ] 
       │ (JWT Bearer Token, Role: OPERATOR)
       ▼
[ Spring Security Filter Chain ]
       │
[ API Endpoint (RBAC Authorization) ]
       │
[ Deterministic Policy Engine (Bounds & Rate-of-Change Filter) ]
       │
[ Industrial Protocol Gateway ]
       │
[ Building Equipment / BACnet / MQTT ]
```

## Role-Based Access Control (RBAC)
- **ADMIN**: Manage buildings, floors, zones, user credentials, system configuration.
- **OPERATOR**: Review and authorize AI command proposals, acknowledge anomalies, trigger manual overrides.
- **VIEWER**: Read-only dashboard access to live telemetry, equipment status, and historical reports.
- **AI_SERVICE**: Limited service-to-service token for submitting proposed commands (status `PROPOSED` only; cannot execute directly).

## Security Controls
1. **BCrypt Password Hashing**: Strength factor 12. Plaintext passwords never stored.
2. **Stateless JWT Authentication**: Signed with HMAC-SHA256, 8-hour expiration.
3. **No Arbitrary Command Execution**: Protocol gateway only accepts structured `GatewayCommand` objects containing validated point IDs and bounded float values.
4. **Read-Only LLM Access**: Natural language queries use read-only database query channels with parameterized queries; arbitrary SQL execution is strictly forbidden.

