import React, { useState, useEffect } from 'react';
import { Navbar } from './components/layout/Navbar';
import { Sidebar, TabType } from './components/layout/Sidebar';
import { OverviewTab } from './components/overview/OverviewTab';
import { BuildingExplorerTab } from './components/building/BuildingExplorerTab';
import { TelemetryTab } from './components/telemetry/TelemetryTab';
import { AIOperationsTab } from './components/ai-operations/AIOperationsTab';
import { ApprovalCenterTab } from './components/approval/ApprovalCenterTab';
import { MaintenanceCenterTab } from './components/maintenance/MaintenanceCenterTab';
import { SystemMonitoringTab } from './components/monitoring/SystemMonitoringTab';
import { AssistantModal } from './components/assistant/AssistantModal';
import { api } from './services/api';
import { DashboardOverview, Equipment, Anomaly, Command } from './types';

export const App: React.FC = () => {
  const [currentTab, setCurrentTab] = useState<TabType>('overview');
  const [isAssistantOpen, setIsAssistantOpen] = useState(false);
  const [overview, setOverview] = useState<DashboardOverview>({
    totalBuildings: 1,
    activeEquipment: 26,
    offlineDevices: 0,
    activeAnomalies: 2,
    pendingApprovals: 2,
    faultedEquipment: 1
  });
  const [equipmentList, setEquipmentList] = useState<Equipment[]>([]);
  const [anomalies, setAnomalies] = useState<Anomaly[]>([]);
  const [pendingCommands, setPendingCommands] = useState<Command[]>([]);

  const loadData = async () => {
    try {
      const [ov, eq, anm, cmds] = await Promise.all([
        api.getOverview(),
        api.getEquipment(),
        api.getAnomalies(),
        api.getPendingApprovals()
      ]);
      setOverview(ov);
      setEquipmentList(eq);
      setAnomalies(anm);
      setPendingCommands(cmds);
    } catch (err) {
      console.warn('Backend unavailable, using rich mock data:', err);
    }
  };

  useEffect(() => {
    loadData();
    const interval = setInterval(loadData, 10000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans">
      <Navbar
        onOpenAssistant={() => setIsAssistantOpen(true)}
        pendingApprovalsCount={pendingCommands.length}
      />

      <div className="flex flex-1 overflow-hidden">
        <Sidebar
          currentTab={currentTab}
          onSelectTab={setCurrentTab}
          pendingApprovals={pendingCommands.length}
          activeAnomalies={anomalies.length}
        />

        <main className="flex-1 overflow-y-auto p-6 bg-slate-950">
          <div className="max-w-7xl mx-auto">
            {currentTab === 'overview' && (
              <OverviewTab overview={overview} onNavigate={setCurrentTab} />
            )}
            {currentTab === 'building' && (
              <BuildingExplorerTab equipmentList={equipmentList} />
            )}
            {currentTab === 'telemetry' && (
              <TelemetryTab />
            )}
            {currentTab === 'ai-operations' && (
              <AIOperationsTab />
            )}
            {currentTab === 'approvals' && (
              <ApprovalCenterTab
                pendingCommands={pendingCommands}
                onRefresh={loadData}
              />
            )}
            {currentTab === 'maintenance' && (
              <MaintenanceCenterTab
                anomalies={anomalies}
                equipmentList={equipmentList}
                onRefresh={loadData}
              />
            )}
            {currentTab === 'monitoring' && (
              <SystemMonitoringTab />
            )}
          </div>
        </main>
      </div>

      <AssistantModal
        isOpen={isAssistantOpen}
        onClose={() => setIsAssistantOpen(false)}
      />
    </div>
  );
};
export default App;

