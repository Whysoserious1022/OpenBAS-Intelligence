import React from 'react';
import { 
  Building, 
  Cpu, 
  AlertTriangle, 
  CheckCircle, 
  Zap, 
  Thermometer, 
  Activity,
  ArrowUpRight
} from 'lucide-react';
import { DashboardOverview } from '../../types';

interface OverviewTabProps {
  overview: DashboardOverview;
  onNavigate: (tab: any) => void;
}

export const OverviewTab: React.FC<OverviewTabProps> = ({ overview, onNavigate }) => {
  const kpis = [
    {
      label: 'Connected Buildings',
      value: overview.totalBuildings,
      sub: 'Nexus Tower (12,000 m²)',
      icon: Building,
      color: 'from-blue-600 to-cyan-600',
      action: () => onNavigate('building')
    },
    {
      label: 'Active Equipment',
      value: overview.activeEquipment,
      sub: '5 AHUs, 20 VAVs, 1 Chiller',
      icon: Cpu,
      color: 'from-cyan-600 to-teal-600',
      action: () => onNavigate('building')
    },
    {
      label: 'Active Anomalies',
      value: overview.activeAnomalies,
      sub: '1 Critical AHU Valve, 1 Loaded Filter',
      icon: AlertTriangle,
      color: 'from-rose-600 to-amber-600',
      badge: 'Attention',
      action: () => onNavigate('maintenance')
    },
    {
      label: 'Pending Approvals',
      value: overview.pendingApprovals,
      sub: 'AI Proposals Awaiting Human Review',
      icon: CheckCircle,
      color: 'from-amber-600 to-orange-600',
      badge: 'Action Required',
      action: () => onNavigate('approvals')
    }
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight">Industrial Operations Overview</h1>
          <p className="text-xs text-slate-400">
            Real-time telemetry, autonomous agent recommendations, and deterministic safety telemetry.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-xs text-slate-400 font-mono">Last refreshed: Just now</span>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {kpis.map((kpi, idx) => {
          const Icon = kpi.icon;
          return (
            <div
              key={idx}
              onClick={kpi.action}
              className="p-4 rounded-xl bg-slate-900 border border-slate-800 hover:border-slate-700 transition cursor-pointer relative overflow-hidden group"
            >
              <div className="flex items-center justify-between mb-3">
                <span className="text-xs font-medium text-slate-400">{kpi.label}</span>
                <div className={`w-8 h-8 rounded-lg bg-gradient-to-tr ${kpi.color} flex items-center justify-center text-white shadow-md`}>
                  <Icon className="w-4 h-4" />
                </div>
              </div>
              <div className="text-2xl font-bold text-white font-mono mb-1">{kpi.value}</div>
              <div className="text-[11px] text-slate-500 flex items-center justify-between">
                <span>{kpi.sub}</span>
                <ArrowUpRight className="w-3 h-3 text-slate-600 group-hover:text-cyan-400 transition" />
              </div>
            </div>
          );
        })}
      </div>

      {/* Operational Highlights */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Real-time incident banner */}
        <div className="lg:col-span-2 p-5 rounded-xl bg-slate-900 border border-slate-800 space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-rose-500 animate-ping"></span>
              <h2 className="text-sm font-semibold text-white">Active Root Cause Incident in Progress</h2>
            </div>
            <span className="text-xs font-mono px-2 py-0.5 rounded bg-rose-950 text-rose-400 border border-rose-800">
              INC-AHU3-JAM
            </span>
          </div>
          
          <div className="p-4 rounded-lg bg-slate-950 border border-slate-800 text-xs space-y-3">
            <div className="flex items-center justify-between border-b border-slate-800/80 pb-2">
              <span className="text-slate-400 font-medium">Suspected Root Cause:</span>
              <span className="text-rose-400 font-semibold">AHU-F3 Cooling Coil Valve Jammed (15% Open)</span>
            </div>
            <p className="text-slate-300 leading-relaxed">
              AHU-F3 supply air temperature is elevated at 19.5°C despite an 85% cooling demand signal.
              Measured actuator position feedback remains at 15%. Downstream terminal zones on Floor 3 (Conference & Executive) are experiencing thermal comfort deficits (26.8°C).
            </p>
            <div className="flex items-center justify-between pt-1">
              <span className="text-[11px] text-slate-500 font-mono">Confidence: 91% • Evidence Points: 4</span>
              <button
                onClick={() => onNavigate('ai-operations')}
                className="text-xs font-semibold text-cyan-400 hover:text-cyan-300 transition flex items-center gap-1"
              >
                Inspect in AI Operations Center →
              </button>
            </div>
          </div>

          <div className="grid grid-cols-3 gap-3 text-center">
            <div className="p-3 rounded-lg bg-slate-950/50 border border-slate-800">
              <div className="text-slate-500 text-[10px] uppercase font-mono">Chiller Plant Supply</div>
              <div className="text-emerald-400 font-mono font-bold text-sm mt-0.5">6.8°C (Normal)</div>
            </div>
            <div className="p-3 rounded-lg bg-slate-950/50 border border-slate-800">
              <div className="text-slate-500 text-[10px] uppercase font-mono">Floor 3 Return Air</div>
              <div className="text-amber-400 font-mono font-bold text-sm mt-0.5">26.8°C (Elevated)</div>
            </div>
            <div className="p-3 rounded-lg bg-slate-950/50 border border-slate-800">
              <div className="text-slate-500 text-[10px] uppercase font-mono">Total Building Power</div>
              <div className="text-cyan-400 font-mono font-bold text-sm mt-0.5">142.6 kW</div>
            </div>
          </div>
        </div>

        {/* Safety Gate Summary */}
        <div className="p-5 rounded-xl bg-slate-900 border border-slate-800 flex flex-col justify-between">
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <Zap className="w-4 h-4 text-cyan-400" />
              <h2 className="text-sm font-semibold text-white">Deterministic Safety Guardrails</h2>
            </div>
            <p className="text-xs text-slate-400 leading-relaxed">
              OpenBAS enforces absolute deterministic barriers. AI agents formulate proposals, but execution is gated by hard rules:
            </p>
            <div className="space-y-2 text-xs">
              <div className="flex items-start gap-2 text-slate-300">
                <span className="text-emerald-400 font-bold">✓</span>
                <span>Max Setpoint Delta: ≤ 3.0°C per step</span>
              </div>
              <div className="flex items-start gap-2 text-slate-300">
                <span className="text-emerald-400 font-bold">✓</span>
                <span>Thermal Safety Limits: [16.0°C - 30.0°C]</span>
              </div>
              <div className="flex items-start gap-2 text-slate-300">
                <span className="text-emerald-400 font-bold">✓</span>
                <span>Read-Only Register Write Protection</span>
              </div>
              <div className="flex items-start gap-2 text-slate-300">
                <span className="text-emerald-400 font-bold">✓</span>
                <span>Operator Approval Required by Default</span>
              </div>
            </div>
          </div>

          <div className="mt-4 pt-4 border-t border-slate-800">
            <button
              onClick={() => onNavigate('approvals')}
              className="w-full py-2 rounded-lg bg-amber-600/20 text-amber-300 border border-amber-600/40 text-xs font-semibold hover:bg-amber-600/30 transition flex items-center justify-center gap-2"
            >
              <span>Review {overview.pendingApprovals} Pending Commands</span>
              <span>→</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

