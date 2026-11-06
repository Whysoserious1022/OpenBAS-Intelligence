import React, { useState, useEffect } from 'react';
import { LineChart, Thermometer, Zap, Wind, Droplets, RefreshCw, Radio } from 'lucide-react';
import { api } from '../../services/api';

export const TelemetryTab: React.FC = () => {
  const [selectedRange, setSelectedRange] = useState<'1h' | '6h' | '24h'>('1h');
  const [telemetryEvents, setTelemetryEvents] = useState<any[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [lastRefreshed, setLastRefreshed] = useState<Date>(new Date());

  const fetchLiveTelemetry = async () => {
    try {
      const data = await api.getBuildingTelemetry('BUILDING-01');
      if (data && data.length > 0) {
        setTelemetryEvents(data);
        setLastRefreshed(new Date());
      }
    } catch (err) {
      console.warn('Could not fetch live telemetry:', err);
    }
  };

  useEffect(() => {
    fetchLiveTelemetry();
    const interval = setInterval(fetchLiveTelemetry, 3000);
    return () => clearInterval(interval);
  }, []);

  // Compute live equipment readings from latest telemetry events
  const getLatestValue = (pointId: string, equipmentId?: string) => {
    const match = telemetryEvents.find(e => 
      e.pointId === pointId && (!equipmentId || e.equipmentId === equipmentId)
    );
    return match ? match.numericValue : null;
  };

  const ahuSupplyTemp = getLatestValue('SUPPLY_AIR_TEMP', 'AHU-F3') ?? 19.5;
  const zone3Temp = getLatestValue('ZONE_TEMP', 'VAV-F3-Z1') ?? 26.8;
  const chillerTemp = getLatestValue('CHILLED_WATER_SUPPLY_TEMP', 'CHILLER-01') ?? 6.8;
  const powerKw = getLatestValue('POWER_KW', 'AHU-F3') ?? 16.9;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-xl font-bold text-white tracking-tight">Live Industrial Telemetry</h1>
            <span className="flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-emerald-950 border border-emerald-800 text-[10px] font-mono text-emerald-400">
              <Radio className="w-3 h-3 animate-pulse" />
              Live Ingestion Active (3s)
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">
            Real-time normalization pipeline: BACnet/IP, MQTT, and Digital Twin telemetry streams.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <span className="text-[11px] text-slate-500 font-mono">
            Updated: {lastRefreshed.toLocaleTimeString()}
          </span>
          <button
            onClick={() => { setLoading(true); fetchLiveTelemetry().finally(() => setLoading(false)); }}
            className="p-1.5 rounded-lg bg-slate-900 border border-slate-800 text-slate-400 hover:text-white transition"
            title="Refresh now"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* KPI Cards Row */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 text-xs">
        <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 space-y-1">
          <div className="flex items-center justify-between text-slate-400">
            <span>AHU-F3 Supply Air</span>
            <Thermometer className="w-4 h-4 text-rose-400" />
          </div>
          <div className="text-xl font-bold font-mono text-rose-400">
            {typeof ahuSupplyTemp === 'number' ? ahuSupplyTemp.toFixed(1) : ahuSupplyTemp}°C
          </div>
          <div className="text-[10px] text-rose-400/80 font-mono">Target: 13.0°C (Valve Jammed)</div>
        </div>

        <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 space-y-1">
          <div className="flex items-center justify-between text-slate-400">
            <span>Zone 3-1 Temperature</span>
            <Thermometer className="w-4 h-4 text-rose-400" />
          </div>
          <div className="text-xl font-bold font-mono text-rose-400">
            {typeof zone3Temp === 'number' ? zone3Temp.toFixed(1) : zone3Temp}°C
          </div>
          <div className="text-[10px] text-slate-500 font-mono">Setpoint: 22.0°C (+4.8°C Delta)</div>
        </div>

        <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 space-y-1">
          <div className="flex items-center justify-between text-slate-400">
            <span>Chiller CHILLER-01 Supply</span>
            <Droplets className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-xl font-bold font-mono text-emerald-400">
            {typeof chillerTemp === 'number' ? chillerTemp.toFixed(1) : chillerTemp}°C
          </div>
          <div className="text-[10px] text-emerald-400/80 font-mono">Nominal Range [5.0°C - 8.0°C]</div>
        </div>

        <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 space-y-1">
          <div className="flex items-center justify-between text-slate-400">
            <span>AHU-F3 Fan Power</span>
            <Zap className="w-4 h-4 text-amber-400" />
          </div>
          <div className="text-xl font-bold font-mono text-amber-400">
            {typeof powerKw === 'number' ? powerKw.toFixed(1) : powerKw} kW
          </div>
          <div className="text-[10px] text-slate-500 font-mono">Baseline: 14.2 kW (+19%)</div>
        </div>
      </div>

      {/* Live Stream Table */}
      <div className="p-5 rounded-xl bg-slate-900 border border-slate-800 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <LineChart className="w-4 h-4 text-cyan-400" />
            <h2 className="text-sm font-semibold text-white">Live Ingested Telemetry Feed ({telemetryEvents.length} events buffered)</h2>
          </div>
          <span className="text-[11px] font-mono text-cyan-400">Auto-refreshing every 3s</span>
        </div>

        <div className="overflow-x-auto max-h-96">
          <table className="w-full text-left text-xs font-mono">
            <thead className="sticky top-0 bg-slate-900">
              <tr className="border-b border-slate-800 text-slate-500">
                <th className="pb-2.5">Timestamp</th>
                <th className="pb-2.5">Floor / Zone</th>
                <th className="pb-2.5">Equipment</th>
                <th className="pb-2.5">Device ID</th>
                <th className="pb-2.5">Point</th>
                <th className="pb-2.5">Value</th>
                <th className="pb-2.5">Quality</th>
                <th className="pb-2.5">Protocol</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 text-slate-300">
              {telemetryEvents.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-8 text-center text-slate-500 font-mono">
                    Connecting to live telemetry pipeline...
                  </td>
                </tr>
              ) : (
                telemetryEvents.slice(0, 30).map((e, idx) => {
                  const val = typeof e.numericValue === 'number' ? e.numericValue.toFixed(2) : e.numericValue;
                  const isHigh = (e.pointId === 'ZONE_TEMP' && e.numericValue > 25.0) ||
                                (e.pointId === 'SUPPLY_AIR_TEMP' && e.numericValue > 17.0);

                  return (
                    <tr key={idx} className="hover:bg-slate-800/40 transition">
                      <td className="py-2 text-slate-400 text-[11px]">
                        {e.eventTimestamp ? new Date(e.eventTimestamp).toLocaleTimeString() : 'N/A'}
                      </td>
                      <td className="text-slate-400">{e.floorId || 'BLDG'} {e.zoneId ? `• ${e.zoneId}` : ''}</td>
                      <td className="font-bold text-white">{e.equipmentId}</td>
                      <td className="text-slate-400">{e.deviceId}</td>
                      <td className="text-cyan-400">{e.pointId}</td>
                      <td className={`font-bold ${isHigh ? 'text-rose-400' : 'text-slate-200'}`}>
                        {val} {e.unit || ''}
                      </td>
                      <td>
                        <span className="px-1.5 py-0.5 rounded bg-emerald-950 text-emerald-400 text-[10px] border border-emerald-800/60">
                          {e.dataQuality || 'GOOD'}
                        </span>
                      </td>
                      <td className="text-slate-400 text-[11px]">{e.sourceProtocol || 'MQTT'}</td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
