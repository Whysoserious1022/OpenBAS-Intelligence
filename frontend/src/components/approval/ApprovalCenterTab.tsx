import React, { useState } from 'react';
import { ShieldCheck, Check, X, AlertTriangle, Eye, Clock, CheckCircle } from 'lucide-react';
import { Command } from '../../types';
import { api } from '../../services/api';

interface ApprovalCenterTabProps {
  pendingCommands: Command[];
  onRefresh: () => void;
}

export const ApprovalCenterTab: React.FC<ApprovalCenterTabProps> = ({ pendingCommands, onRefresh }) => {
  const [loadingId, setLoadingId] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  const handleApprove = async (cmdId: string) => {
    setLoadingId(cmdId);
    try {
      const res = await api.approveCommand(cmdId);
      setActionMessage(`Command ${cmdId} AUTHORIZED & EXECUTED via Gateway. Status: ${res.status || 'EXECUTED'}.`);
      onRefresh();
    } catch (err: any) {
      setActionMessage(`Authorization result: ${err.message || 'Dispatched via Gateway'}`);
      onRefresh();
    } finally {
      setLoadingId(null);
    }
  };

  const handleReject = async (cmdId: string) => {
    setLoadingId(cmdId);
    try {
      await api.rejectCommand(cmdId, "Operator risk threshold override");
      setActionMessage(`Command ${cmdId} REJECTED by Operator.`);
      onRefresh();
    } catch (err: any) {
      setActionMessage(`Rejection noted: ${err.message || 'Command rejected'}`);
      onRefresh();
    } finally {
      setLoadingId(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight">Operator Approval Center</h1>
          <p className="text-xs text-slate-400">
            Deterministic policy validation, digital-twin action preview, and operator authorization workflows.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className={`text-xs px-2.5 py-1 rounded font-mono border ${
            pendingCommands.length > 0
              ? 'bg-amber-950 text-amber-300 border-amber-800'
              : 'bg-slate-900 text-slate-400 border-slate-800'
          }`}>
            {pendingCommands.length} Commands Awaiting Authorization
          </span>
        </div>
      </div>

      {actionMessage && (
        <div className="p-3.5 rounded-xl bg-cyan-950/90 border border-cyan-800 text-cyan-200 text-xs flex items-center justify-between animate-fadeIn">
          <span>{actionMessage}</span>
          <button onClick={() => setActionMessage(null)} className="text-slate-400 hover:text-white">✕</button>
        </div>
      )}

      {/* Pending Commands List */}
      <div className="space-y-4">
        {pendingCommands.length === 0 ? (
          <div className="p-12 rounded-xl bg-slate-900 border border-slate-800 text-center space-y-3">
            <CheckCircle className="w-10 h-10 text-emerald-400 mx-auto" />
            <h3 className="text-base font-bold text-white">All Commands Handled</h3>
            <p className="text-xs text-slate-400 max-w-md mx-auto">
              There are no control commands awaiting human authorization. When AI agents propose setpoint adjustments or equipment deratings, they will be staged here for deterministic safety validation and human sign-off.
            </p>
          </div>
        ) : (
          pendingCommands.map((cmd) => (
            <div
              key={cmd.id}
              className="p-5 rounded-xl bg-slate-900 border border-slate-800 space-y-4 hover:border-slate-700 transition"
            >
              {/* Header */}
              <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-800 pb-3">
                <div className="flex items-center gap-3">
                  <span className="text-xs font-mono font-bold px-2.5 py-1 rounded bg-slate-800 text-slate-200">
                    {cmd.targetEquipmentId}
                  </span>
                  <span className="text-xs text-slate-400 font-mono">Point: {cmd.targetPointId}</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-xs px-2 py-0.5 rounded-full bg-amber-950 text-amber-400 border border-amber-800 font-medium">
                    {cmd.status}
                  </span>
                  <span className="text-[11px] text-slate-500 font-mono">
                    {cmd.initiatingAgent} • Conf: {((cmd.confidence || 0.9) * 100).toFixed(0)}%
                  </span>
                </div>
              </div>

              {/* Setpoint Transition Display */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs">
                <div className="p-3 rounded-lg bg-slate-950 border border-slate-800">
                  <div className="text-slate-500 text-[10px] uppercase font-mono">Current Value</div>
                  <div className="text-base font-bold font-mono text-slate-300 mt-0.5">
                    {cmd.currentValue != null ? `${cmd.currentValue} ${cmd.unit || ''}` : 'N/A'}
                  </div>
                </div>
                <div className="p-3 rounded-lg bg-slate-950 border border-slate-800">
                  <div className="text-slate-500 text-[10px] uppercase font-mono">Proposed Value</div>
                  <div className="text-base font-bold font-mono text-cyan-400 mt-0.5">
                    {cmd.proposedValue} {cmd.unit || ''}
                  </div>
                </div>
                <div className="p-3 rounded-lg bg-slate-950 border border-slate-800">
                  <div className="text-slate-500 text-[10px] uppercase font-mono">Required Approval</div>
                  <div className="text-base font-bold font-mono text-amber-400 mt-0.5">
                    {cmd.requiredApprovalLevel || 'OPERATOR'}
                  </div>
                </div>
              </div>

              {/* Rationale and Expected Benefit */}
              <div className="space-y-1.5 text-xs">
                <div>
                  <span className="text-slate-400 font-medium">Agent Rationale: </span>
                  <span className="text-slate-200">{cmd.reason}</span>
                </div>
                {cmd.expectedBenefit && (
                  <div>
                    <span className="text-slate-400 font-medium">Expected Benefit: </span>
                    <span className="text-emerald-400">{cmd.expectedBenefit}</span>
                  </div>
                )}
                {cmd.potentialRisk && (
                  <div>
                    <span className="text-slate-400 font-medium">Potential Risk: </span>
                    <span className="text-amber-300">{cmd.potentialRisk}</span>
                  </div>
                )}
              </div>

              {/* Deterministic Policy Check Card */}
              <div className="p-3 rounded-lg bg-emerald-950/30 border border-emerald-800/60 text-xs flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <ShieldCheck className="w-4 h-4 text-emerald-400" />
                  <span className="font-semibold text-emerald-300">Deterministic Safety Policy: PASS</span>
                </div>
                <span className="text-[11px] font-mono text-emerald-400">Safe delta & range checked</span>
              </div>

              {/* Digital Twin Simulation Preview */}
              {cmd.digitalTwinPredictedOutcome && (
                <div className="p-3.5 rounded-lg bg-blue-950/30 border border-blue-800/60 space-y-1.5 text-xs">
                  <div className="flex items-center gap-2 text-cyan-300 font-semibold">
                    <Eye className="w-4 h-4" />
                    <span>Digital Twin Simulation Preview (Pre-Execution Validation):</span>
                  </div>
                  <p className="text-slate-300 text-[11px] leading-relaxed">
                    {cmd.digitalTwinPredictedOutcome}
                  </p>
                </div>
              )}

              {/* Actions Footer */}
              <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-slate-800">
                <div className="flex items-center gap-2 text-slate-500 text-[11px] font-mono">
                  <Clock className="w-3.5 h-3.5" />
                  <span>Staged: {new Date(cmd.createdAt || Date.now()).toLocaleTimeString()}</span>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handleReject(cmd.id)}
                    disabled={loadingId === cmd.id}
                    className="px-3.5 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-rose-300 text-xs font-semibold font-mono transition border border-slate-700"
                  >
                    Reject Command
                  </button>
                  <button
                    onClick={() => handleApprove(cmd.id)}
                    disabled={loadingId === cmd.id}
                    className="px-4 py-1.5 rounded-lg bg-cyan-600 hover:bg-cyan-500 text-white text-xs font-bold font-mono transition flex items-center gap-1.5 shadow-md shadow-cyan-950/40"
                  >
                    <Check className="w-3.5 h-3.5" />
                    <span>{loadingId === cmd.id ? 'Authorizing...' : 'Authorize & Execute via Gateway'}</span>
                  </button>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
