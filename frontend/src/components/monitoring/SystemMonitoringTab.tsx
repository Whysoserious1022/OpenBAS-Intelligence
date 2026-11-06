import React from 'react';
import { Server, CheckCircle2, ShieldCheck, Cpu, HardDrive, Database, Radio } from 'lucide-react';

export const SystemMonitoringTab: React.FC = () => {
  const components = [
    {
      name: 'OpenBAS Java Backend',
      type: 'Core Platform',
      status: 'UP',
      details: 'Spring Boot 3.3.5 • Java 21 • Bounded thread pools (10 core, 20 max)',
      latency: '4 ms'
    },
    {
      name: 'Deterministic Policy & Safety Engine',
      type: 'Safety Barrier',
      status: 'UP',
      details: 'Enforcing rate-of-change, write registers, bounds [16°C, 30°C] • 0 Bypasses',
      latency: '<1 ms'
    },
    {
      name: 'Industrial Gateway — BACnet/IP',
      type: 'Protocol Adapter',
      status: 'UP',
      details: 'BACnet4J integration • UDP port 47808 • Device Discovery enabled',
      latency: '12 ms'
    },
    {
      name: 'Industrial Gateway — MQTT Broker',
      type: 'Protocol Adapter',
      status: 'UP',
      details: 'Eclipse Mosquitto 2.0 • Paho MQTTv3 client • QoS 1 • Ingesting 189 pts/min',
      latency: '3 ms'
    },
    {
      name: 'TimescaleDB / PostgreSQL 16',
      type: 'Data Infrastructure',
      status: 'UP',
      details: 'Hypertables: telemetry_events (1d chunks), anomaly_events (7d chunks)',
      latency: '2 ms'
    },
    {
      name: 'OpenBAS AI Service (Python)',
      type: 'Intelligent Processing',
      status: 'UP',
      details: 'FastAPI • Energy, Comfort & Maintenance Agents • RCA Engine • Port 8000',
      latency: '18 ms'
    },
    {
      name: 'Digital Twin Simulation Engine',
      type: 'Simulation',
      status: 'UP',
      details: '1st-Order Thermal RC Model & Fan Affinity Laws P ~ N³',
      latency: '1 ms'
    }
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight">System Infrastructure & Health Monitoring</h1>
          <p className="text-xs text-slate-400">
            Real-time status of protocol gateways, database hypertables, AI services, and deterministic safety filters.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-xs px-2.5 py-1 rounded-full bg-emerald-950 text-emerald-400 border border-emerald-800 font-mono">
            All 7 Services Nominal
          </span>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {components.map((c, idx) => (
          <div key={idx} className="p-4 rounded-xl bg-slate-900 border border-slate-800 space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-[10px] font-mono uppercase text-slate-500">{c.type}</span>
              <span className="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-emerald-950 text-emerald-400 border border-emerald-800">
                {c.status}
              </span>
            </div>
            <div>
              <div className="text-sm font-semibold text-white">{c.name}</div>
              <p className="text-xs text-slate-400 mt-1 leading-relaxed">{c.details}</p>
            </div>
            <div className="flex items-center justify-between text-[11px] text-slate-500 font-mono pt-2 border-t border-slate-800/80">
              <span>Latency</span>
              <span className="text-emerald-400 font-bold">{c.latency}</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

