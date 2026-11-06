import React from 'react';
import {
  LayoutDashboard,
  Building2,
  LineChart,
  Bot,
  CheckCircle2,
  Wrench,
  Server
} from 'lucide-react';

export type TabType = 
  | 'overview' 
  | 'building' 
  | 'telemetry' 
  | 'ai-operations' 
  | 'approvals' 
  | 'maintenance' 
  | 'monitoring';

interface SidebarProps {
  currentTab: TabType;
  onSelectTab: (tab: TabType) => void;
  pendingApprovals: number;
  activeAnomalies: number;
}

export const Sidebar: React.FC<SidebarProps> = ({
  currentTab,
  onSelectTab,
  pendingApprovals,
  activeAnomalies
}) => {
  const navItems = [
    { id: 'overview' as TabType, label: 'Overview', icon: LayoutDashboard },
    { id: 'building' as TabType, label: 'Building Explorer', icon: Building2 },
    { id: 'telemetry' as TabType, label: 'Live Telemetry', icon: LineChart },
    { id: 'ai-operations' as TabType, label: 'AI Operations Center', icon: Bot },
    { 
      id: 'approvals' as TabType, 
      label: 'Approval Center', 
      icon: CheckCircle2,
      badge: pendingApprovals > 0 ? pendingApprovals : null,
      badgeColor: 'bg-amber-500'
    },
    { 
      id: 'maintenance' as TabType, 
      label: 'Maintenance Center', 
      icon: Wrench,
      badge: activeAnomalies > 0 ? activeAnomalies : null,
      badgeColor: 'bg-rose-500'
    },
    { id: 'monitoring' as TabType, label: 'System Monitoring', icon: Server }
  ];

  return (
    <aside className="w-64 bg-slate-900 border-r border-slate-800 flex flex-col justify-between p-4 shrink-0 min-h-[calc(100vh-4rem)]">
      <div className="space-y-1">
        <div className="px-3 py-2 text-[11px] font-semibold tracking-wider text-slate-500 uppercase font-mono">
          Operations Platform
        </div>
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = currentTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => onSelectTab(item.id)}
              className={`w-full flex items-center justify-between px-3 py-2.5 rounded-lg text-xs font-medium transition ${
                isActive
                  ? 'bg-cyan-950/70 text-cyan-400 border border-cyan-800/60 shadow-sm'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
              }`}
            >
              <div className="flex items-center gap-3">
                <Icon className={`w-4 h-4 ${isActive ? 'text-cyan-400' : 'text-slate-500'}`} />
                <span>{item.label}</span>
              </div>
              {item.badge !== null && (
                <span className={`px-1.5 py-0.5 rounded-full text-[10px] font-bold text-white ${item.badgeColor}`}>
                  {item.badge}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {/* Building Status Widget */}
      <div className="p-3 rounded-lg bg-slate-950/60 border border-slate-800/80 text-xs space-y-2">
        <div className="flex items-center justify-between">
          <span className="text-slate-400 font-mono text-[11px]">Nexus Tower</span>
          <span className="text-[10px] px-1.5 py-0.2 rounded bg-emerald-950 text-emerald-400 border border-emerald-800">
            SIMULATED
          </span>
        </div>
        <div className="text-[11px] text-slate-500 space-y-1">
          <div className="flex justify-between">
            <span>Occupancy Rate</span>
            <span className="text-slate-300 font-mono">68% (142 occ)</span>
          </div>
          <div className="flex justify-between">
            <span>Avg Zone Temp</span>
            <span className="text-slate-300 font-mono">23.1°C</span>
          </div>
          <div className="flex justify-between">
            <span>Chiller Power</span>
            <span className="text-slate-300 font-mono">84.2 kW</span>
          </div>
        </div>
      </div>
    </aside>
  );
};

