import React, { useState } from 'react';
import { Building2, Layers, Cpu, ChevronRight, Activity, ShieldAlert, Check, Server } from 'lucide-react';
import { Equipment } from '../../types';

interface BuildingExplorerTabProps {
  equipmentList: Equipment[];
}

export const BuildingExplorerTab: React.FC<BuildingExplorerTabProps> = ({ equipmentList }) => {
  const [selectedFloor, setSelectedFloor] = useState<number>(3);

  const floors = [1, 2, 3, 4, 5];

  const getStatusBadge = (status?: string) => {
    switch (status) {
      case 'NORMAL':
        return <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-950 text-emerald-400 border border-emerald-800">NORMAL</span>;
      case 'DEGRADED':
        return <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-amber-950 text-amber-400 border border-amber-800">DEGRADED</span>;
      case 'FAULT':
        return <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-rose-950 text-rose-400 border border-rose-800">FAULT</span>;
      default:
        return <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-slate-800 text-slate-400">NORMAL</span>;
    }
  };

  // Find AHU for selected floor
  const floorAhu = equipmentList.find(e => e.externalId === `AHU-F${selectedFloor}`) || {
    externalId: `AHU-F${selectedFloor}`,
    name: `Floor ${selectedFloor} Air Handling Unit`,
    equipmentType: 'AHU',
    operationalStatus: selectedFloor === 3 ? 'FAULT' : selectedFloor === 2 ? 'DEGRADED' : 'NORMAL',
    healthScore: selectedFloor === 3 ? 0.52 : selectedFloor === 2 ? 0.74 : 0.94
  };

  // Find VAVs for selected floor
  const floorVavs = equipmentList.filter(e => 
    e.externalId && e.externalId.startsWith(`VAV-F${selectedFloor}`)
  );

  const displayVavs = floorVavs.length > 0 ? floorVavs : [1, 2, 3, 4].map(z => ({
    id: `vav-f${selectedFloor}-z${z}`,
    externalId: `VAV-F${selectedFloor}-Z${z}`,
    name: `Floor ${selectedFloor} Zone ${z} VAV Box`,
    equipmentType: 'VAV',
    operationalStatus: selectedFloor === 3 && z === 1 ? 'DEGRADED' : 'NORMAL',
    healthScore: selectedFloor === 3 && z === 1 ? 0.70 : 0.95
  }));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight">Building Hierarchy Explorer</h1>
          <p className="text-xs text-slate-400">
            Nexus Tower (BUILDING-01) • 5 Floors • 20 Thermal Zones • Parent-Child Equipment Dependencies
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-xs px-2.5 py-1 rounded bg-slate-800 text-slate-300 font-mono">
            Chiller Plant: CHILLER-01 (6.8°C Nominal)
          </span>
        </div>
      </div>

      {/* Floor Selector Pills */}
      <div className="flex items-center gap-2 border-b border-slate-800 pb-4">
        {floors.map((floorNum) => (
          <button
            key={floorNum}
            onClick={() => setSelectedFloor(floorNum)}
            className={`px-4 py-2 rounded-lg text-xs font-semibold transition flex items-center gap-2 ${
              selectedFloor === floorNum
                ? 'bg-cyan-600 text-white shadow-md shadow-cyan-950/40'
                : 'bg-slate-900 text-slate-400 hover:text-white hover:bg-slate-800 border border-slate-800'
            }`}
          >
            <Layers className="w-3.5 h-3.5" />
            <span>Floor {floorNum}</span>
            {floorNum === 3 && (
              <span className="w-2 h-2 rounded-full bg-rose-400 animate-ping"></span>
            )}
            {floorNum === 2 && (
              <span className="w-2 h-2 rounded-full bg-amber-400"></span>
            )}
          </button>
        ))}
      </div>

      {/* Equipment Grid for Selected Floor */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Air Handling Unit (Parent) */}
        <div className="p-5 rounded-xl bg-slate-900 border border-slate-800 space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Cpu className="w-4 h-4 text-cyan-400" />
              <h2 className="text-sm font-semibold text-white">Floor {selectedFloor} Primary AHU</h2>
            </div>
            {getStatusBadge(floorAhu.operationalStatus)}
          </div>

          <div className="p-4 rounded-lg bg-slate-950 border border-slate-800 space-y-3 text-xs">
            <div className="flex justify-between">
              <span className="text-slate-400">Identifier:</span>
              <span className="text-slate-200 font-mono font-bold">{floorAhu.externalId}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Equipment Name:</span>
              <span className="text-slate-200">{floorAhu.name}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Health Index:</span>
              <span className="font-mono font-bold text-cyan-400">
                {((floorAhu.healthScore || 0.95) * 100).toFixed(0)}%
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Supply Air Status:</span>
              <span className={`font-mono font-bold ${selectedFloor === 3 ? 'text-rose-400' : 'text-emerald-400'}`}>
                {selectedFloor === 3 ? '19.5°C (Target: 13.0°C - Starved)' : '13.2°C (Nominal)'}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Actuator Feedback:</span>
              <span className={`font-mono ${selectedFloor === 3 ? 'text-rose-400' : 'text-slate-200'}`}>
                {selectedFloor === 3 ? '15% open (Demand: 80% - Stuck)' : '42% open'}
              </span>
            </div>
          </div>
        </div>

        {/* Downstream VAV Boxes (Children) */}
        <div className="lg:col-span-2 p-5 rounded-xl bg-slate-900 border border-slate-800 space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Server className="w-4 h-4 text-cyan-400" />
              <h2 className="text-sm font-semibold text-white">
                Downstream VAV Distribution Boxes ({displayVavs.length} zones)
              </h2>
            </div>
            <span className="text-[11px] font-mono text-slate-400">Parent: {floorAhu.externalId}</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {displayVavs.map((vav, idx) => (
              <div key={idx} className="p-3.5 rounded-lg bg-slate-950 border border-slate-800 space-y-2 text-xs">
                <div className="flex items-center justify-between">
                  <span className="font-mono font-bold text-slate-200">{vav.externalId}</span>
                  {getStatusBadge(vav.operationalStatus)}
                </div>
                <div className="text-[11px] text-slate-400">{vav.name}</div>
                <div className="flex justify-between text-[11px] pt-1 border-t border-slate-900">
                  <span className="text-slate-500">Health Score:</span>
                  <span className="font-mono text-slate-300">{((vav.healthScore || 0.95) * 100).toFixed(0)}%</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
