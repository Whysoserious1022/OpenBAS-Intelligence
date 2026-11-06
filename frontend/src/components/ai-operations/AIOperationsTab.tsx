import React, { useState } from 'react';
import { Bot, Zap, HeartPulse, Wrench, GitMerge, AlertCircle, CheckCircle2, ArrowRight, Play, Check } from 'lucide-react';
import { api } from '../../services/api';

export const AIOperationsTab: React.FC = () => {
  const [orchestrating, setOrchestrating] = useState(false);
  const [rcaLoading, setRcaLoading] = useState(false);
  const [stageMessage, setStageMessage] = useState<string | null>(null);

  const [decision, setDecision] = useState<any>({
    decision_id: 'DEC-5590BF8A',
    has_conflicts: true,
    selection_rationale: 'Thermal comfort in verified active occupied spaces supersedes energy curtailment under ASHRAE 55 compliance rule.',
    safety_policy_status: 'REQUIRES_OPERATOR_APPROVAL',
    selected_recommendation: {
      equipment_id: 'VAV-F3-Z1',
      point_id: 'ZONE_TEMP_SETPOINT',
      current_value: 24.0,
      proposed_value: 22.5,
      unit: 'CELSIUS',
      command_type: 'SET_SETPOINT',
      reason: 'Comfort Monitoring Agent: Lower cooling setpoint to 22.5°C because 18 occupants are present in Conference Room.',
      confidence: 0.94
    }
  });

  const [rca, setRca] = useState<any>({
    diagnosis_id: 'RCA-7F912B',
    suspected_root_cause: 'AHU-F3 Chilled Water Actuator Valve Jammed at 15% Open Position',
    confidence: 0.91,
    supporting_evidence: [
      'Cooling valve command signal is 85%, but position feedback is stuck at 15%.',
      'AHU-F3 supply air temperature is 19.5°C despite central chiller supplying 6.8°C water.',
      'All 4 downstream VAVs on Floor 3 reporting thermal deficits and starving of chilled air.'
    ],
    affected_equipment: ['AHU-F3', 'VAV-F3-Z1', 'VAV-F3-Z2', 'VAV-F3-Z3', 'VAV-F3-Z4'],
    affected_zones: ['ZONE-3-1', 'ZONE-3-2', 'ZONE-3-3', 'ZONE-3-4'],
    recommended_investigation: 'Inspect physical mechanical linkage of Belimo actuator on AHU-F3 cooling coil valve. Perform manual stroke calibration test.'
  });

  const runLiveOrchestration = async () => {
    setOrchestrating(true);
    setStageMessage(null);
    try {
      const res = await api.runOrchestration({
        buildingId: 'BUILDING-01',
        floorId: 'FLOOR-3',
        zoneId: 'ZONE-3-1',
        equipmentId: 'AHU-F3',
        currentZoneTemp: 26.8,
        zoneTempSetpoint: 22.0,
        supplyAirTemp: 19.5,
        ambientTemp: 31.0,
        occupancy: 18,
        powerKw: 28.5,
        powerBaselineKw: 20.0,
        anomalies: ['SUPPLY_TEMP_HIGH', 'VALVE_MISMATCH']
      });
      if (res) {
        setDecision(res);
      }
    } catch (err: any) {
      console.warn('Orchestration error:', err);
    } finally {
      setOrchestrating(false);
    }
  };

  const runLiveRca = async () => {
    setRcaLoading(true);
    try {
      const res = await api.runRootCause({
        incident_id: 'INC-' + Math.floor(Math.random() * 9000 + 1000),
        equipment_id: 'AHU-F3',
        building_id: 'BUILDING-01',
        symptom: 'SUPPLY_AIR_TEMP_HIGH',
        telemetry: {
          supply_air_temp: 19.5,
          chilled_water_supply_temp: 6.5,
          valve_command: 85.0,
          valve_feedback: 14.0
        }
      });
      if (res) {
        setRca(res);
      }
    } catch (err: any) {
      console.warn('RCA error:', err);
    } finally {
      setRcaLoading(false);
    }
  };

  const stageCommandToApprovalCenter = async () => {
    if (!decision || !decision.selected_recommendation) return;
    const rec = decision.selected_recommendation;

    try {
      await api.proposeCommand({
        targetEquipmentId: rec.equipment_id,
        targetPointId: rec.point_id,
        commandType: rec.command_type || 'SET_SETPOINT',
        proposedValue: rec.proposed_value,
        unit: rec.unit || 'CELSIUS',
        reason: rec.reason,
        expectedBenefit: 'Restores zone compliance with ASHRAE 55 and halts compressor overdrive',
        potentialRisk: 'Transient airflow surge during damper repositioning',
        initiatingAgent: 'AI_ORCHESTRATOR',
        confidence: rec.confidence || 0.92
      });
      setStageMessage(`Action successfully staged in Operator Approval Center! Check the "Approval Center" tab to review and authorize.`);
    } catch (err: any) {
      setStageMessage(`Policy Engine Evaluation: ${err.message || 'Action staged'}`);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight">AI Operations Center</h1>
          <p className="text-xs text-slate-400">
            Multi-agent coordination, conflict resolution, and grounded root-cause diagnosis across building dependencies.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={runLiveOrchestration}
            disabled={orchestrating}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-cyan-600 hover:bg-cyan-500 text-white text-xs font-semibold shadow transition disabled:opacity-50"
          >
            <Play className={`w-3.5 h-3.5 ${orchestrating ? 'animate-spin' : ''}`} />
            <span>{orchestrating ? 'Agents Deliberating...' : 'Run Multi-Agent Deliberation'}</span>
          </button>
        </div>
      </div>

      {stageMessage && (
        <div className="p-3.5 rounded-xl bg-emerald-950/80 border border-emerald-800 text-emerald-200 text-xs flex items-center justify-between">
          <span>{stageMessage}</span>
          <button onClick={() => setStageMessage(null)} className="text-emerald-400 hover:text-white">✕</button>
        </div>
      )}

      {/* 3 Specialized Agents Row */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Energy Agent */}
        <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Zap className="w-4 h-4 text-emerald-400" />
              <h2 className="text-xs font-semibold text-white">Energy Optimization Agent</h2>
            </div>
            <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-emerald-950 text-emerald-400 border border-emerald-800">
              Active
            </span>
          </div>
          <p className="text-xs text-slate-300">
            Identifies low-occupancy periods and proposes cooling setpoint relaxation to curtail fan & compressor power.
          </p>
          <div className="p-2.5 rounded bg-slate-950 border border-slate-800 text-[11px] font-mono space-y-1">
            <div className="text-slate-400">Objective:</div>
            <div className="text-emerald-400">Raise Zone 3-1 setpoint to 24.5°C to save 2.1 kW peak electrical load</div>
            <div className="text-slate-500 text-[10px]">Est. Savings: 2.1 kW • Priority: Efficiency</div>
          </div>
        </div>

        {/* Comfort Agent */}
        <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <HeartPulse className="w-4 h-4 text-cyan-400" />
              <h2 className="text-xs font-semibold text-white">Comfort Monitoring Agent</h2>
            </div>
            <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-cyan-950 text-cyan-400 border border-cyan-800">
              Active
            </span>
          </div>
          <p className="text-xs text-slate-300">
            Monitors zone temperatures and occupant density to prevent productivity loss under ASHRAE 55 standards.
          </p>
          <div className="p-2.5 rounded bg-slate-950 border border-slate-800 text-[11px] font-mono space-y-1">
            <div className="text-slate-400">Objective:</div>
            <div className="text-cyan-400">Lower Zone 3-1 setpoint to 22.5°C (18 occupants verified in conference room)</div>
            <div className="text-slate-500 text-[10px]">Restores Comfort • Priority: Occupant Well-being</div>
          </div>
        </div>

        {/* Maintenance Agent */}
        <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Wrench className="w-4 h-4 text-amber-400" />
              <h2 className="text-xs font-semibold text-white">Predictive Maintenance Agent</h2>
            </div>
            <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-amber-950 text-amber-400 border border-amber-800">
              Active
            </span>
          </div>
          <p className="text-xs text-slate-300">
            Detects mechanical wear, filter differential pressure anomalies, and actuator tracking deviations.
          </p>
          <div className="p-2.5 rounded bg-slate-950 border border-slate-800 text-[11px] font-mono space-y-1">
            <div className="text-slate-400">Objective:</div>
            <div className="text-amber-400">Derate AHU-F2 fan to 70% to protect motor bearings from filter differential spike</div>
            <div className="text-slate-500 text-[10px]">Prevents Bearing Wear • Priority: Asset Health</div>
          </div>
        </div>
      </div>

      {/* Conflict Resolution Section */}
      <div className="p-5 rounded-xl bg-slate-900 border border-slate-800 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <GitMerge className="w-4 h-4 text-purple-400" />
            <h2 className="text-sm font-semibold text-white">Deterministic Multi-Agent Conflict Resolution</h2>
          </div>
          <span className="text-xs font-mono text-purple-400 px-2 py-0.5 rounded bg-purple-950 border border-purple-800">
            Decision: {decision.decision_id || 'ACTIVE'}
          </span>
        </div>

        <div className="p-4 rounded-lg bg-slate-950 border border-slate-800 space-y-3 text-xs">
          <div className="flex items-center gap-2 text-amber-400 font-semibold">
            <AlertCircle className="w-4 h-4" />
            <span>Energy vs. Comfort Conflict Arbitrated on Zone 3-1:</span>
          </div>

          <div className="p-3 rounded bg-purple-950/30 border border-purple-800/60 text-purple-200">
            <div className="font-semibold mb-1">Arbitration Rationale:</div>
            <div>{decision.selection_rationale}</div>
          </div>

          {decision.selected_recommendation && (
            <div className="p-4 rounded-lg bg-slate-900 border border-slate-800 flex flex-wrap items-center justify-between gap-3">
              <div>
                <div className="text-slate-400 text-[11px] uppercase font-mono">Selected Action</div>
                <div className="text-sm font-bold text-cyan-400 font-mono mt-0.5">
                  {decision.selected_recommendation.equipment_id} • {decision.selected_recommendation.point_id} → {decision.selected_recommendation.proposed_value} {decision.selected_recommendation.unit}
                </div>
                <div className="text-slate-400 text-[11px] mt-1">{decision.selected_recommendation.reason}</div>
              </div>
              <button
                onClick={stageCommandToApprovalCenter}
                className="px-4 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold font-mono transition flex items-center gap-2"
              >
                <Check className="w-4 h-4" />
                <span>Stage for Operator Approval</span>
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Root Cause Analysis Tree */}
      <div className="p-5 rounded-xl bg-slate-900 border border-slate-800 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-semibold text-white">Equipment-Aware Root Cause Analysis Engine</h2>
          <div className="flex items-center gap-3">
            <span className="text-xs font-mono text-cyan-400">Diagnosis ID: {rca.diagnosis_id || 'RCA-7F912B'}</span>
            <button
              onClick={runLiveRca}
              disabled={rcaLoading}
              className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-mono transition"
            >
              {rcaLoading ? 'Analyzing...' : 'Re-run RCA'}
            </button>
          </div>
        </div>

        <div className="p-4 rounded-lg bg-slate-950 border border-slate-800 space-y-4 text-xs">
          <div className="flex items-start justify-between">
            <div>
              <div className="text-slate-400 font-medium">Suspected Root Cause:</div>
              <div className="text-base font-bold text-rose-400 mt-0.5">
                {rca.suspected_root_cause}
              </div>
            </div>
            <div className="text-right">
              <div className="text-slate-400 text-[11px]">Confidence Score</div>
              <div className="text-lg font-bold font-mono text-emerald-400">
                {typeof rca.confidence === 'number' ? (rca.confidence * 100).toFixed(0) : 91}%
              </div>
            </div>
          </div>

          {/* Causal chain visualization */}
          <div className="space-y-2">
            <div className="text-slate-400 font-medium">Dependency Traversal Path:</div>
            <div className="flex flex-wrap items-center gap-2 font-mono text-[11px]">
              <span className="px-2 py-1 rounded bg-slate-900 border border-slate-700 text-slate-300">CHILLER-01 (6.8°C Normal)</span>
              <ArrowRight className="w-3.5 h-3.5 text-slate-500" />
              <span className="px-2 py-1 rounded bg-rose-950 border border-rose-800 text-rose-300 font-bold">AHU-F3 Valve Jammed (15%)</span>
              <ArrowRight className="w-3.5 h-3.5 text-slate-500" />
              <span className="px-2 py-1 rounded bg-amber-950 border border-amber-800 text-amber-300">VAV-F3-Z1 (Supply Deficit)</span>
              <ArrowRight className="w-3.5 h-3.5 text-slate-500" />
              <span className="px-2 py-1 rounded bg-rose-950 border border-rose-800 text-rose-300">Zone 3-1 Overheating (26.8°C)</span>
            </div>
          </div>

          {/* Supporting Evidence points */}
          <div className="space-y-1.5 pt-2 border-t border-slate-800">
            <div className="text-slate-400 font-medium">Supporting Telemetry Evidence:</div>
            <div className="space-y-1 text-slate-300 text-[11px]">
              {rca.supporting_evidence && rca.supporting_evidence.map((ev: string, idx: number) => (
                <div key={idx} className="flex items-center gap-2">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                  <span>{ev}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="p-3 rounded bg-cyan-950/40 border border-cyan-800 text-cyan-200">
            <span className="font-semibold">Recommended Investigation: </span>
            {rca.recommended_investigation}
          </div>
        </div>
      </div>
    </div>
  );
};
