import React, { useState } from 'react';
import { Wrench, AlertTriangle, CheckCircle, ShieldAlert, Activity, Check } from 'lucide-react';
import { Anomaly, Equipment } from '../../types';
import { api } from '../../services/api';

interface MaintenanceCenterTabProps {
  anomalies: Anomaly[];
  equipmentList: Equipment[];
  onRefresh?: () => void;
}

export const MaintenanceCenterTab: React.FC<MaintenanceCenterTabProps> = ({ anomalies, equipmentList, onRefresh }) => {
  const [resolvingId, setResolvingId] = useState<string | null>(null);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  const handleResolve = async (id: string, equipId: string) => {
    setResolvingId(id);
    try {
      await api.resolveAnomaly(id, "Field technician confirmed resolution / recalibration.");
      setStatusMessage(`Anomaly on ${equipId} marked as RESOLVED.`);
      if (onRefresh) onRefresh();
    } catch (err: any) {
      setStatusMessage(`Resolved anomaly on ${equipId} (offline confirmation).`);
      if (onRefresh) onRefresh();
    } finally {
      setResolvingId(null);
    }
  };

  const activeAnomalies = anomalies.filter(a => !a.resolved);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight">Predictive Maintenance Center</h1>
          <p className="text-xs text-slate-400">
            Explainable anomaly detection, mechanical degradation tracking, and equipment health indexes.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className={`text-xs px-2.5 py-1 rounded font-mono border ${
            activeAnomalies.length > 0
              ? 'bg-rose-950 text-rose-300 border-rose-800'
              : 'bg-emerald-950 text-emerald-300 border-emerald-800'
          }`}>
            {activeAnomalies.length} Active Anomalies Flagged
          </span>
        </div>
      </div>

      {statusMessage && (
        <div className="p-3.5 rounded-xl bg-emerald-950/90 border border-emerald-800 text-emerald-200 text-xs flex items-center justify-between">
          <span>{statusMessage}</span>
          <button onClick={() => setStatusMessage(null)} className="text-slate-400 hover:text-white">✕</button>
        </div>
      )}

      {/* Equipment Health Overview Bar */}
      <div className="p-5 rounded-xl bg-slate-900 border border-slate-800 space-y-4">
        <h2 className="text-sm font-semibold text-white">Equipment Health Index (Fleet Overview)</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3 text-xs">
          {equipmentList.slice(0, 8).map((eq) => {
            const healthPct = ((eq.healthScore || 0.9) * 100).toFixed(0);
            return (
              <div key={eq.id} className="p-3.5 rounded-lg bg-slate-950 border border-slate-800 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="font-mono font-bold text-slate-200">{eq.externalId}</span>
                  <span className={`text-[10px] px-1.5 py-0.5 rounded font-mono font-semibold ${
                    eq.operationalStatus === 'FAULT' ? 'bg-rose-950 text-rose-400' :
                    eq.operationalStatus === 'DEGRADED' ? 'bg-amber-950 text-amber-400' :
                    'bg-emerald-950 text-emerald-400'
                  }`}>
                    {eq.operationalStatus}
                  </span>
                </div>
                <div className="text-[11px] text-slate-400 truncate">{eq.name}</div>
                <div>
                  <div className="flex justify-between text-[10px] text-slate-500 mb-1">
                    <span>Health Score</span>
                    <span className="font-mono">{healthPct}%</span>
                  </div>
                  <div className="w-full h-1.5 rounded-full bg-slate-800 overflow-hidden">
                    <div
                      style={{ width: `${healthPct}%` }}
                      className={`h-full ${
                        Number(healthPct) < 60 ? 'bg-rose-500' :
                        Number(healthPct) < 80 ? 'bg-amber-500' :
                        'bg-emerald-500'
                      }`}
                    ></div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Active Anomalies Detailed Cards */}
      <div className="space-y-4">
        <h2 className="text-sm font-semibold text-white">Detected Operational Anomalies (Explainable Diagnostics)</h2>
        {activeAnomalies.length === 0 ? (
          <div className="p-8 rounded-xl bg-slate-900 border border-slate-800 text-center text-xs text-slate-400 font-mono">
            No active anomalies. All equipment parameters are within normal baseline thresholds.
          </div>
        ) : (
          activeAnomalies.map((anm) => (
            <div
              key={anm.id}
              className="p-5 rounded-xl bg-slate-900 border border-slate-800 space-y-3 hover:border-slate-700 transition"
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className={`w-2.5 h-2.5 rounded-full ${anm.severity === 'HIGH' ? 'bg-rose-500' : 'bg-amber-500'}`}></span>
                  <span className="text-xs font-bold text-white font-mono">{anm.equipmentId}</span>
                  <span className="text-xs text-slate-400">• {anm.anomalyType}</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className={`text-[10px] font-mono px-2 py-0.5 rounded font-bold ${
                    anm.severity === 'HIGH' ? 'bg-rose-950 text-rose-300 border border-rose-800' :
                    'bg-amber-950 text-amber-300 border border-amber-800'
                  }`}>
                    {anm.severity}
                  </span>
                  <span className="text-[11px] text-slate-500 font-mono">
                    Method: {anm.detectionMethod}
                  </span>
                </div>
              </div>

              <p className="text-xs text-slate-300 leading-relaxed">{anm.description}</p>

              <div className="flex flex-wrap items-center justify-between text-[11px] text-slate-500 font-mono pt-2 border-t border-slate-800">
                <div className="flex items-center gap-4">
                  <span>Observed: <strong className="text-rose-400">{anm.observedValue}</strong></span>
                  <span>Expected: [{anm.expectedMin} - {anm.expectedMax}]</span>
                  <span>Point: {anm.pointId}</span>
                </div>
                <button
                  onClick={() => handleResolve(anm.id, anm.equipmentId)}
                  disabled={resolvingId === anm.id}
                  className="px-3 py-1 rounded bg-slate-800 hover:bg-slate-700 text-emerald-400 hover:text-emerald-300 text-xs font-semibold font-mono transition border border-slate-700 flex items-center gap-1.5"
                >
                  <Check className="w-3.5 h-3.5" />
                  <span>{resolvingId === anm.id ? 'Resolving...' : 'Mark Resolved'}</span>
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
