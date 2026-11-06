import { Building, Equipment, Device, TelemetryPoint, Anomaly, Command, DashboardOverview } from '../types';

const API_BASE = '/api/v1';

// Token storage and automatic operator session
export const getToken = () => localStorage.getItem('openbas_jwt') || '';

export const ensureAuthenticated = async (): Promise<string> => {
  const existing = localStorage.getItem('openbas_jwt');
  if (existing && existing !== 'mock-dev-token') return existing;

  try {
    const res = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'operator', password: 'operator123' })
    });
    if (res.ok) {
      const data = await res.json();
      localStorage.setItem('openbas_jwt', data.token);
      return data.token;
    }
  } catch (err) {
    console.warn('Auto-login failed, continuing in fallback mode:', err);
  }
  return '';
};

const authHeaders = () => {
  const token = localStorage.getItem('openbas_jwt');
  const headers: Record<string, string> = {
    'Content-Type': 'application/json'
  };
  if (token && token !== 'mock-dev-token') {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return headers;
};

export const api = {
  // Automatic auth initialization
  initAuth: ensureAuthenticated,

  // System Health & Overview
  async getOverview(): Promise<DashboardOverview> {
    try {
      await ensureAuthenticated();
      const res = await fetch(`${API_BASE}/system/overview`, { headers: authHeaders() });
      if (!res.ok) throw new Error('Network response not ok');
      return await res.json();
    } catch {
      return {
        totalBuildings: 1,
        activeEquipment: 26,
        offlineDevices: 0,
        activeAnomalies: 2,
        pendingApprovals: 2,
        faultedEquipment: 1
      };
    }
  },

  // Buildings
  async getBuildings(): Promise<Building[]> {
    try {
      await ensureAuthenticated();
      const res = await fetch(`${API_BASE}/buildings`, { headers: authHeaders() });
      if (!res.ok) throw new Error('Failed to fetch buildings');
      return await res.json();
    } catch {
      return [{
        id: 'bldg-01-uuid',
        externalId: 'BUILDING-01',
        name: 'Nexus Tower',
        description: '5-floor commercial facility with central chiller plant and VAV thermal distribution.',
        totalAreaSqm: 12000,
        floorCount: 5,
        active: true,
        simulationEnabled: true
      }];
    }
  },

  // Equipment
  async getEquipment(): Promise<Equipment[]> {
    try {
      await ensureAuthenticated();
      const res = await fetch(`${API_BASE}/equipment?buildingId=BUILDING-01&size=50`, { headers: authHeaders() });
      if (!res.ok) throw new Error('Failed to fetch equipment');
      const data = await res.json();
      return data.content || data;
    } catch {
      return [
        { id: '1', externalId: 'CHILLER-01', name: 'Central Chiller Plant', equipmentType: 'CHILLER', operationalStatus: 'NORMAL', healthScore: 0.96, simulationEnabled: true },
        { id: '2', externalId: 'AHU-F1', name: 'Floor 1 Air Handling Unit', equipmentType: 'AHU', operationalStatus: 'NORMAL', healthScore: 0.94, simulationEnabled: true },
        { id: '3', externalId: 'AHU-F2', name: 'Floor 2 Air Handling Unit', equipmentType: 'AHU', operationalStatus: 'DEGRADED', healthScore: 0.74, simulationEnabled: true },
        { id: '4', externalId: 'AHU-F3', name: 'Floor 3 Air Handling Unit', equipmentType: 'AHU', operationalStatus: 'FAULT', healthScore: 0.52, simulationEnabled: true },
        { id: '5', externalId: 'VAV-F3-Z1', name: 'Floor 3 Zone 1 VAV Box', equipmentType: 'VAV', operationalStatus: 'DEGRADED', healthScore: 0.70, simulationEnabled: true },
        { id: '6', externalId: 'VAV-F1-Z2', name: 'Floor 1 Zone 2 VAV Box', equipmentType: 'VAV', operationalStatus: 'NORMAL', healthScore: 0.95, simulationEnabled: true }
      ];
    }
  },

  // Anomalies
  async getAnomalies(): Promise<Anomaly[]> {
    try {
      await ensureAuthenticated();
      const res = await fetch(`${API_BASE}/anomalies?buildingId=BUILDING-01&size=50`, { headers: authHeaders() });
      if (!res.ok) throw new Error('Failed to fetch anomalies');
      const data = await res.json();
      return data.content || data;
    } catch {
      return [
        {
          id: 'anm-01',
          equipmentId: 'AHU-F3',
          zoneId: 'FLOOR-3',
          anomalyType: 'TEMP_HIGH',
          severity: 'HIGH',
          observedValue: 19.5,
          expectedMin: 10.0,
          expectedMax: 15.0,
          detectionMethod: 'THRESHOLD',
          description: 'Supply air temperature 19.5°C exceeds maximum cooling threshold (15.0°C). Valve position stuck at 15%.',
          detectedAt: new Date(Date.now() - 1000 * 60 * 18).toISOString(),
          resolved: false
        },
        {
          id: 'anm-02',
          equipmentId: 'AHU-F2',
          zoneId: 'FLOOR-2',
          anomalyType: 'FILTER_PRESSURE_HIGH',
          severity: 'MEDIUM',
          observedValue: 285.0,
          expectedMin: 50.0,
          expectedMax: 220.0,
          detectionMethod: 'MOVING_AVERAGE',
          description: 'Filter bank differential pressure 285 Pa indicates severe loading. Motor vibration elevated.',
          detectedAt: new Date(Date.now() - 1000 * 60 * 45).toISOString(),
          resolved: false
        }
      ];
    }
  },

  async resolveAnomaly(id: string, note: string = 'Resolved by operator'): Promise<any> {
    await ensureAuthenticated();
    const res = await fetch(`${API_BASE}/anomalies/${id}/resolve?note=${encodeURIComponent(note)}`, {
      method: 'POST',
      headers: authHeaders()
    });
    if (!res.ok) throw new Error('Failed to resolve anomaly');
    return await res.json();
  },

  // Commands / Approvals
  async getPendingApprovals(): Promise<Command[]> {
    try {
      await ensureAuthenticated();
      const res = await fetch(`${API_BASE}/commands/pending-approval`, { headers: authHeaders() });
      if (!res.ok) throw new Error('Failed to fetch pending commands');
      const data = await res.json();
      return Array.isArray(data) ? data : data.content || [];
    } catch {
      return [];
    }
  },

  async approveCommand(id: string): Promise<Command> {
    await ensureAuthenticated();
    const res = await fetch(`${API_BASE}/commands/${id}/approve`, {
      method: 'POST',
      headers: authHeaders()
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Approval failed');
    }
    return await res.json();
  },

  async rejectCommand(id: string, reason: string): Promise<Command> {
    await ensureAuthenticated();
    const res = await fetch(`${API_BASE}/commands/${id}/reject?reason=${encodeURIComponent(reason)}`, {
      method: 'POST',
      headers: authHeaders()
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Rejection failed');
    }
    return await res.json();
  },

  async proposeCommand(req: {
    targetEquipmentId: string;
    targetPointId: string;
    commandType: string;
    proposedValue: number;
    unit?: string;
    reason?: string;
    expectedBenefit?: string;
    potentialRisk?: string;
    initiatingAgent?: string;
    confidence?: number;
  }): Promise<Command> {
    await ensureAuthenticated();
    const res = await fetch(`${API_BASE}/commands`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify(req)
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Command rejected by Policy Engine');
    }
    return await res.json();
  },

  // Live Telemetry
  async getBuildingTelemetry(buildingId: string = 'BUILDING-01'): Promise<any[]> {
    try {
      await ensureAuthenticated();
      const res = await fetch(`${API_BASE}/telemetry/buildings/${buildingId}?page=0&size=40`, { headers: authHeaders() });
      if (!res.ok) throw new Error('Failed to fetch telemetry');
      const data = await res.json();
      return data.content || [];
    } catch {
      return [];
    }
  },

  // AI Agent Service (FastAPI)
  async queryAssistant(query: string): Promise<any> {
    try {
      const res = await fetch('/api/v1/ai/query', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query, building_id: 'BUILDING-01' })
      });
      if (!res.ok) throw new Error('AI query failed');
      return await res.json();
    } catch {
      return {
        answer: `Analysis for "${query}": Zone 3-1 is experiencing overheating (26.8°C) due to AHU-F3 chilled water actuator valve mechanically stuck at 15% open position. Central Chiller CHILLER-01 is supplying 6.8°C water normally. Recommended action: inspect valve actuator linkage.`,
        intent: 'ROOT_CAUSE_QUERY',
        evidence_points: [
          'AHU-F3 supply temp: 19.5°C (Target: 13.0°C)',
          'Valve position feedback: 15% vs Command: 80%',
          'Downstream VAVs on Floor 3 reporting thermal deficits'
        ],
        suggested_actions: ['Inspect AHU-F3 Belimo actuator', 'Stage calibration command'],
        confidence: 0.93
      };
    }
  },

  async runOrchestration(context: any): Promise<any> {
    const res = await fetch('/api/v1/ai/orchestrate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(context)
    });
    if (!res.ok) throw new Error('Orchestration failed');
    return await res.json();
  },

  async runRootCause(incident: any): Promise<any> {
    const res = await fetch('/api/v1/ai/root-cause', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(incident)
    });
    if (!res.ok) throw new Error('Root cause analysis failed');
    return await res.json();
  }
};
