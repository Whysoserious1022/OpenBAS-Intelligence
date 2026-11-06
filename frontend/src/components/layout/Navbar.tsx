import React from 'react';
import { ShieldCheck, Cpu, Bell, Activity, Sparkles } from 'lucide-react';

interface NavbarProps {
  onOpenAssistant: () => void;
  pendingApprovalsCount: number;
}

export const Navbar: React.FC<NavbarProps> = ({ onOpenAssistant, pendingApprovalsCount }) => {
  return (
    <header className="h-16 bg-slate-900/90 backdrop-blur border-b border-slate-800 px-6 flex items-center justify-between sticky top-0 z-30">
      <div className="flex items-center gap-3">
        <div className="w-9 h-9 rounded-lg bg-gradient-to-tr from-cyan-600 to-blue-600 flex items-center justify-center shadow-lg shadow-cyan-900/20">
          <Cpu className="w-5 h-5 text-white" />
        </div>
        <div>
          <div className="flex items-center gap-2">
            <span className="font-bold text-lg tracking-tight text-white">OpenBAS</span>
            <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-cyan-950 text-cyan-400 border border-cyan-800">
              Intelligence
            </span>
          </div>
          <p className="text-[10px] text-slate-400 uppercase tracking-widest font-mono">
            Industrial Interoperability & Agentic Operations
          </p>
        </div>
      </div>

      <div className="flex items-center gap-4">
        {/* Gateway Protocol Indicator */}
        <div className="hidden md:flex items-center gap-2 px-3 py-1 rounded-full bg-slate-800/80 border border-slate-700 text-xs text-slate-300 font-mono">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
          <span>BACnet/IP & MQTT: ONLINE</span>
        </div>

        {/* Deterministic Safety Gate Badge */}
        <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-blue-950/70 border border-blue-800 text-xs text-blue-300 font-medium">
          <ShieldCheck className="w-4 h-4 text-blue-400" />
          <span>Deterministic Safety: ENFORCED</span>
        </div>

        {/* AI Assistant Button */}
        <button
          onClick={onOpenAssistant}
          className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-gradient-to-r from-cyan-600 to-blue-600 hover:from-cyan-500 hover:to-blue-500 text-white text-xs font-medium transition shadow-md shadow-cyan-950/30"
        >
          <Sparkles className="w-4 h-4" />
          <span>Ask Operations AI</span>
        </button>

        {/* User Badge */}
        <div className="flex items-center gap-2 pl-2 border-l border-slate-800">
          <div className="w-8 h-8 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center text-xs font-bold text-cyan-400">
            OP
          </div>
          <div className="hidden lg:block text-left">
            <div className="text-xs font-medium text-slate-200">Operator</div>
            <div className="text-[10px] text-slate-500">Nexus Tower Hub</div>
          </div>
        </div>
      </div>
    </header>
  );
};

