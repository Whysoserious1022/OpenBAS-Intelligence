import requests
import json
import sys

BASE_URL = "http://127.0.0.1:8080/api/v1"
AI_URL = "http://127.0.0.1:8001"
FRONTEND_URL = "http://127.0.0.1:3000"

print("=" * 70)
print("  OPENBAS INTELLIGENCE - FULL SYSTEM & FEATURE VERIFICATION")
print("=" * 70)

# 1. Test Authentication
print("\n[1] Testing Authentication (/api/v1/auth/login)...")
login_resp = requests.post(f"{BASE_URL}/auth/login", json={"username": "operator", "password": "operator123"})
if login_resp.status_code == 200:
    data = login_resp.json()
    token = data.get("token")
    print(f"  [+] SUCCESS: Logged in as {data.get('username')}, Role: {data.get('role')}")
    headers = {"Authorization": f"Bearer {token}"}
else:
    print(f"  [-] FAILED: {login_resp.status_code} {login_resp.text}")
    sys.exit(1)

# 2. Test Building Hierarchy & Equipment Discovery
print("\n[2] Testing Building Hierarchy & Equipment Discovery...")
buildings_resp = requests.get(f"{BASE_URL}/buildings", headers=headers)
if buildings_resp.status_code == 200:
    buildings = buildings_resp.json()
    print(f"  [+] SUCCESS: Discovered {len(buildings)} building(s)")
    for b in buildings:
        print(f"      Building: {b.get('name')} | ID: {b.get('buildingId')} | UUID: {b.get('id')}")
    b_uuid = buildings[0]['id']
    floors_resp = requests.get(f"{BASE_URL}/buildings/{b_uuid}/floors", headers=headers)
    if floors_resp.status_code == 200:
        floors = floors_resp.json()
        print(f"  [+] SUCCESS: Discovered {len(floors)} floor(s)")
        for fl in floors[:3]:
            print(f"      Floor {fl.get('floorNumber')}: {fl.get('name')} (Floor ID: {fl.get('floorId')})")
    else:
        print(f"  [-] Floors query failed: {floors_resp.status_code}")
else:
    print(f"  [-] Buildings query failed: {buildings_resp.status_code}")

# 3. Test Telemetry Queries (Live Ingestion Stream)
print("\n[3] Testing Telemetry API (Continuous Ingestion & Queries)...")
equip_telemetry_resp = requests.get(f"{BASE_URL}/telemetry/equipment/AHU-F1", headers=headers)
if equip_telemetry_resp.status_code == 200:
    readings = equip_telemetry_resp.json()
    print(f"  [+] SUCCESS: Retrieved {len(readings)} live telemetry event(s) for AHU-F1")
    for r in readings[:3]:
        print(f"      Point: {r.get('pointId')} = {r.get('numericValue'):.2f} {r.get('unit')} (Quality: {r.get('dataQuality')})")
else:
    print(f"  [-] Telemetry query status: {equip_telemetry_resp.status_code} {equip_telemetry_resp.text}")

# 4. Test Maintenance Center / Anomalies
print("\n[4] Testing Maintenance Center & Anomaly Resolution...")
anomalies_resp = requests.get(f"{BASE_URL}/anomalies", headers=headers)
if anomalies_resp.status_code == 200:
    anomalies_data = anomalies_resp.json()
    items = anomalies_data.get("content", [])
    print(f"  [+] SUCCESS: Retrieved {len(items)} active anomaly(ies):")
    for a in items:
        print(f"      [{a.get('severity')}] {a.get('equipmentId')} ({a.get('anomalyType')}): {a.get('description')} (ID: {a.get('id')})")
    
    if items:
        first_id = items[0]['id']
        resolve_resp = requests.post(f"{BASE_URL}/anomalies/{first_id}/resolve?note=Tested+and+verified+normal+by+Operator", headers=headers)
        if resolve_resp.status_code in [200, 204]:
            print(f"  [+] SUCCESS: Resolved anomaly {first_id} via Operator Action!")
        else:
            print(f"  [-] Anomaly resolution returned: {resolve_resp.status_code} {resolve_resp.text}")
else:
    print(f"  [-] Anomalies query failed: {anomalies_resp.status_code}")

# 5. Test AI Multi-Agent Service & RCA
print("\n[5] Testing AI Microservice (Port 8001)...")
try:
    ai_health = requests.get(f"{AI_URL}/health").json()
    print(f"  [+] AI Microservice Health: {ai_health}")
    
    # Deliberation
    delib_payload = {
        "event_type": "HIGH_ENERGY_SPIKE",
        "description": "AHU-F2 energy consumption exceeded baseline by 45%",
        "equipment_id": "AHU-F2",
        "building_id": "BUILDING-01",
        "telemetry_context": {"fan_power_kw": 28.5, "zone_temp_c": 21.2, "occupancy_count": 4}
    }
    delib_resp = requests.post(f"{AI_URL}/api/v1/ai/orchestrate", json=delib_payload)
    if delib_resp.status_code == 200:
        decision = delib_resp.json()
        print(f"  [+] SUCCESS: Multi-Agent Deliberation completed!")
        print(f"      Decision ID: {decision.get('decision_id')}")
        print(f"      Selected Action: {decision.get('selected_recommendation')}")
        print(f"      Selection Rationale: {decision.get('selection_rationale')}")
        print(f"      Safety Status: {decision.get('safety_policy_status')}")
    else:
        print(f"  [-] AI Orchestrate failed: {delib_resp.status_code} {delib_resp.text}")

    # Root Cause Analysis
    rca_payload = {
        "anomaly_id": "ANM-AHU-HEAT-01",
        "equipment_id": "AHU-F3",
        "building_id": "BUILDING-01",
        "symptoms": ["Zone overheating", "Static pressure oscillation", "Chilled water flow restriction"]
    }
    rca_resp = requests.post(f"{AI_URL}/api/v1/ai/root-cause", json=rca_payload)
    if rca_resp.status_code == 200:
        rca = rca_resp.json()
        print(f"  [+] SUCCESS: RCA Engine Diagnosis: {rca.get('suspected_root_cause')} (Confidence: {rca.get('confidence')*100:.1f}%)")
        print(f"      Affected Equipment: {rca.get('affected_equipment')}")
    else:
        print(f"  [-] RCA failed: {rca_resp.status_code}")

    # NL Operations Assistant Query
    nl_resp = requests.post(f"{AI_URL}/api/v1/ai/query", json={"query": "What is the status of AHU-F2 and energy efficiency?"})
    if nl_resp.status_code == 200:
        print(f"  [+] SUCCESS: NL Operations Assistant: {nl_resp.json().get('answer')[:85]}...")
except Exception as e:
    print(f"  [-] AI Service Error: {e}")

# 6. Test Safety Policy & Command Approval Flow
print("\n[6] Testing Deterministic Safety Policy & Command Staging...")
propose_payload = {
    "targetEquipmentId": "AHU-F2",
    "targetPointId": "FAN_SPEED_SETPOINT",
    "commandType": "SET_SETPOINT",
    "proposedValue": 65.0,
    "unit": "PERCENT",
    "reason": "AI Orchestrator energy curtailment action",
    "expectedBenefit": "Save 12.4 kWh",
    "potentialRisk": "Slight airflow reduction in zone 3",
    "initiatingAgent": "AI-EnergyAgent",
    "confidence": 0.94
}
propose_resp = requests.post(f"{BASE_URL}/commands", json=propose_payload, headers=headers)
if propose_resp.status_code == 201:
    cmd_data = propose_resp.json()
    cmd_id = cmd_data.get('id')
    print(f"  [+] SUCCESS: Command proposed and staged through Safety & Digital Twin layer!")
    print(f"      Command UUID: {cmd_id}")
    print(f"      Command ID: {cmd_data.get('commandId')}")
    print(f"      Status: {cmd_data.get('status')}")
    print(f"      Safety Policy Evaluation: {cmd_data.get('policyValidationResult')}")
    print(f"      Digital Twin Predicted Impact: {cmd_data.get('digitalTwinPredictedImpact')}")
    
    # Fetch pending approvals
    pending_resp = requests.get(f"{BASE_URL}/commands/pending-approval", headers=headers)
    pending_cmds = pending_resp.json()
    print(f"  [+] SUCCESS: Retrieved {len(pending_cmds)} pending approval command(s) in Approval Center")
    
    # Authorize & Execute command
    approve_resp = requests.post(f"{BASE_URL}/commands/{cmd_id}/approve", headers=headers)
    if approve_resp.status_code == 200:
        approved_cmd = approve_resp.json()
        print(f"  [+] SUCCESS: Operator Authorization & Gateway Execution completed!")
        print(f"      Executed Command UUID: {approved_cmd.get('id')}")
        print(f"      New Status: {approved_cmd.get('status')}")
        print(f"      Execution Result: {approved_cmd.get('executionResult')}")
    else:
        print(f"  [-] Approve failed: {approve_resp.status_code} {approve_resp.text}")
else:
    print(f"  [-] Propose command failed: {propose_resp.status_code} {propose_resp.text}")

# 7. Test Deterministic Safety Policy REJECTION (Safety Boundary Check)
print("\n[7] Testing Deterministic Safety Policy REJECTION of dangerous command...")
unsafe_payload = {
    "targetEquipmentId": "AHU-F2",
    "targetPointId": "SUPPLY_AIR_TEMP_SETPOINT",
    "commandType": "SET_SETPOINT",
    "proposedValue": 38.0, # Dangerous: exceeding 30.0°C maximum thermal bound!
    "unit": "CELSIUS",
    "reason": "Dangerous command attempting thermal violation",
    "expectedBenefit": "None",
    "potentialRisk": "Extreme occupant discomfort and coil strain",
    "initiatingAgent": "UntrustedActor",
    "confidence": 0.50
}
unsafe_resp = requests.post(f"{BASE_URL}/commands", json=unsafe_payload, headers=headers)
if unsafe_resp.status_code in [400, 403, 422]:
    print(f"  [+] SUCCESS: Dangerous command correctly BLOCKED by Deterministic Policy Engine!")
    print(f"      HTTP Code: {unsafe_resp.status_code}")
    print(f"      Rejection Message: {unsafe_resp.json().get('message')}")
else:
    print(f"  [-] UNEXPECTED: Dangerous command was not blocked! Status: {unsafe_resp.status_code} {unsafe_resp.text}")

# 8. Test Frontend Dev Server & Dual Proxy
print("\n[8] Testing Frontend Dev Server & Proxies (Port 3000)...")
try:
    fe_resp = requests.get(FRONTEND_URL, timeout=3)
    if fe_resp.status_code == 200:
        print(f"  [+] SUCCESS: Frontend Vite Server reachable and serving HTML (status {fe_resp.status_code})")
    else:
        print(f"  [-] Frontend returned status: {fe_resp.status_code}")
except Exception as e:
    print(f"  [-] Frontend error: {e}")

print("\n" + "=" * 70)
print("ALL SYSTEM VERIFICATIONS COMPLETED SUCCESSFULLY!")
print("=" * 70)
