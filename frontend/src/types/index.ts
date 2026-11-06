export interface Building {
  id: string;
  externalId: string;
  name: string;
  description: string;
  totalAreaSqm: number;
  floorCount: number;
  active: boolean;
  simulationEnabled: boolean;
}

export interface Floor {
  id: string;
  externalId: string;
  buildingId: string;
  name: string;
  floorNumber: number;
  areaSqm: number;
  active: boolean;
  zones?: Zone[];
}

export interface Zone {
  id: string;
  externalId: string;
  name: string;
  zoneType: string;
  areaSqm: number;
  comfortTempMinCelsius: number;
  comfortTempMaxCelsius: number;
}

export interface Equipment {
  id: string;
  externalId: string;
  name: string;
  equipmentType: string;
  operationalStatus: 'NORMAL' | 'DEGRADED' | 'FAULT' | 'OFFLINE' | 'UNKNOWN';
  healthScore: number;
  minOperatingTempCelsius?: number;
  maxOperatingTempCelsius?: number;
  ratedPowerKw?: number;
  ratedAirflowCfm?: number;
  simulationEnabled: boolean;
  parentEquipmentId?: string;
}

export interface Device {
  id: string;
  externalId: string;
  name: string;
  protocol: 'BACNET_IP' | 'MQTT' | 'OPC_UA' | 'SIMULATED';
  communicationStatus: 'ONLINE' | 'OFFLINE' | 'DEGRADED' | 'UNKNOWN';
  lastSeenAt: string;
  simulated: boolean;
}

export interface TelemetryPoint {
  id?: string;
  deviceId: string;
  equipmentId: string;
  pointId: string;
  numericValue: number;
  unit: string;
  eventTimestamp: string;
  dataQuality: 'GOOD' | 'BAD' | 'STALE' | 'SIMULATED';
}

export interface Anomaly {
  id: string;
  equipmentId: string;
  zoneId?: string;
  pointId?: string;
  anomalyType: string;
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  observedValue: number;
  expectedMin?: number;
  expectedMax?: number;
  detectionMethod: string;
  description: string;
  detectedAt: string;
  resolved: boolean;
}

export interface Command {
  id: string;
  idempotencyKey: string;
  targetEquipmentId: string;
  targetPointId: string;
  commandType: string;
  currentValue?: number;
  proposedValue: number;
  unit: string;
  reason: string;
  expectedBenefit?: string;
  potentialRisk?: string;
  initiatingAgent?: string;
  confidence?: number;
  status: 'PROPOSED' | 'VALIDATED' | 'PENDING_APPROVAL' | 'APPROVED' | 'EXECUTING' | 'EXECUTED' | 'REJECTED' | 'EXPIRED';
  policyValidationResult?: string;
  digitalTwinSimulated: boolean;
  digitalTwinPredictedOutcome?: string;
  requiredApprovalLevel: string;
  proposedBy: string;
  createdAt: string;
}

export interface DashboardOverview {
  totalBuildings: number;
  activeEquipment: number;
  offlineDevices: number;
  activeAnomalies: number;
  pendingApprovals: number;
  faultedEquipment: number;
}

