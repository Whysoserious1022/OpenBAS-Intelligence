package com.openbas.intelligence.infrastructure.gateway.simulator;

import com.openbas.intelligence.infrastructure.gateway.protocol.IndustrialProtocolAdapter;
import com.openbas.intelligence.infrastructure.persistence.entity.TelemetryEventEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simulator-backed protocol adapter.
 * Satisfies the IndustrialProtocolAdapter interface using simulated equipment behavior.
 * This is NOT a mock — it generates physically plausible values based on equipment type,
 * time of day, and configured scenarios.
 *
 * Clearly labeled as SIMULATED in all telemetry it produces.
 */
@Component("simulatorAdapter")
public class SimulatorProtocolAdapter implements IndustrialProtocolAdapter {

    private static final Logger log = LoggerFactory.getLogger(SimulatorProtocolAdapter.class);

    private volatile boolean connected = true;
    private final AtomicLong readsSucceeded = new AtomicLong(0);
    private final AtomicLong readsFailed = new AtomicLong(0);
    private volatile Instant lastSuccessfulRead;

    private final com.openbas.intelligence.application.service.TelemetryIngestionService ingestionService;

    @org.springframework.beans.factory.annotation.Value("${openbas.gateway.simulator.enabled:true}")
    private boolean simulatorEnabled;

    public SimulatorProtocolAdapter(com.openbas.intelligence.application.service.TelemetryIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @org.springframework.scheduling.annotation.Scheduled(
        fixedDelayString = "${openbas.gateway.simulator.telemetry-interval-ms:5000}",
        initialDelay = 4000
    )
    public void generatePeriodicTelemetry() {
        if (!simulatorEnabled || !connected) {
            return;
        }

        try {
            // Emits telemetry for central chiller and all floors & zones
            emitPoint("CHILLER-01", "DEV-CHILLER-01", "CHILLED_WATER_SUPPLY_TEMP", "CELSIUS", "BLDG", null);
            emitPoint("CHILLER-01", "DEV-CHILLER-01", "POWER_KW", "KW", "BLDG", null);

            for (int floor = 1; floor <= 5; floor++) {
                String floorId = "FLOOR-" + floor;
                String ahuId = "AHU-F" + floor;
                String ahuDev = "DEV-AHU-F" + floor;

                emitPoint(ahuId, ahuDev, "SUPPLY_AIR_TEMP", "CELSIUS", floorId, null);
                emitPoint(ahuId, ahuDev, "RETURN_AIR_TEMP", "CELSIUS", floorId, null);
                emitPoint(ahuId, ahuDev, "POWER_KW", "KW", floorId, null);

                for (int zone = 1; zone <= 4; zone++) {
                    String zoneId = "ZONE-" + floor + "-" + zone;
                    String vavId = "VAV-F" + floor + "-Z" + zone;
                    String sensorDev = "DEV-TEMP-F" + floor + "-Z" + zone;

                    emitPoint(vavId, sensorDev, "ZONE_TEMP", "CELSIUS", floorId, zoneId);
                    emitPoint(vavId, sensorDev, "ZONE_HUMIDITY", "PERCENT", floorId, zoneId);
                    emitPoint(vavId, sensorDev, "AIRFLOW_CFM", "CFM", floorId, zoneId);
                }
            }
        } catch (Exception e) {
            log.warn("Simulator periodic telemetry generation error: {}", e.getMessage());
        }
    }

    private void emitPoint(String equipmentId, String deviceId, String pointId, String unit, String floorId, String zoneId) {
        TelemetryEventEntity event = new TelemetryEventEntity();
        event.setBuildingId("BUILDING-01");
        event.setFloorId(floorId);
        event.setZoneId(zoneId);
        event.setEquipmentId(equipmentId);
        event.setDeviceId(deviceId);
        event.setPointId(pointId);
        event.setUnit(unit);
        event.setNumericValue(generateSimulatedValue(equipmentId, pointId));
        event.setDataQuality("SIMULATED");
        event.setSourceProtocol("SIMULATED");
        event.setSourceDevice(deviceId);
        event.setCommunicationStatus("ONLINE");
        event.setEventTimestamp(Instant.now());

        ingestionService.ingest(event);
        readsSucceeded.incrementAndGet();
        lastSuccessfulRead = Instant.now();
    }

    @Override
    public String getProtocolId() { return "SIMULATED"; }

    @Override
    public boolean isConnected() { return connected; }

    @Override
    public boolean connect() {
        connected = true;
        log.info("Simulator adapter connected");
        return true;
    }

    @Override
    public void disconnect() {
        connected = false;
        log.info("Simulator adapter disconnected");
    }

    @Override
    public List<DiscoveredDevice> discoverDevices() {
        // Returns a static list representing the simulated building.
        // The actual device list is populated by the building initializer.
        return List.of(
            new DiscoveredDevice("SIM-AHU-01", "Air Handling Unit 01 (Sim)", "simulator://ahu-01", "SIMULATED", true),
            new DiscoveredDevice("SIM-AHU-02", "Air Handling Unit 02 (Sim)", "simulator://ahu-02", "SIMULATED", true),
            new DiscoveredDevice("SIM-VAV-01", "VAV Unit 01 (Sim)", "simulator://vav-01", "SIMULATED", true),
            new DiscoveredDevice("SIM-CHILLER-01", "Chiller 01 (Sim)", "simulator://chiller-01", "SIMULATED", true)
        );
    }

    @Override
    public TelemetryEventEntity readPoint(String deviceId, String pointId) {
        // This is called by the gateway when it wants a single on-demand read.
        // For simulated devices, we generate a plausible value.
        try {
            TelemetryEventEntity event = new TelemetryEventEntity();
            event.setDeviceId(deviceId);
            event.setEquipmentId(deviceId); // 1:1 for simulator
            event.setPointId(pointId);
            event.setBuildingId("BUILDING-01");
            event.setEventTimestamp(Instant.now());
            event.setSourceProtocol("SIMULATED");
            event.setSourceDevice(deviceId);
            event.setDataQuality("SIMULATED");
            event.setCommunicationStatus("ONLINE");
            event.setNumericValue(generateSimulatedValue(deviceId, pointId));
            event.setUnit(inferUnit(pointId));

            readsSucceeded.incrementAndGet();
            lastSuccessfulRead = Instant.now();
            return event;
        } catch (Exception e) {
            readsFailed.incrementAndGet();
            log.error("Simulated read failed for device={} point={}", deviceId, pointId, e);
            throw e;
        }
    }

    @Override
    public CommandResult submitCommand(GatewayCommand command) {
        log.info("[SIMULATED] Command submitted: device={} point={} value={} simOnly={}",
            command.deviceId(), command.pointId(), command.value(), command.simulationOnly());
        // Simulator always "succeeds"
        return new CommandResult(
            command.commandId(), true,
            "Command applied to simulator (no physical equipment affected)",
            command.value(), Instant.now()
        );
    }

    @Override
    public AdapterHealth getHealth() {
        return new AdapterHealth(
            "SIMULATED", connected, 1, lastSuccessfulRead, null,
            readsSucceeded.get(), readsFailed.get()
        );
    }

    private double generateSimulatedValue(String deviceId, String pointId) {
        double hour = java.time.LocalTime.now().getHour() + java.time.LocalTime.now().getMinute() / 60.0;
        double occupancyFactor = (hour >= 8 && hour <= 18) ? 1.0 : 0.3;

        return switch (pointId) {
            case "ZONE_TEMP" -> 20.0 + 4.0 * occupancyFactor + (Math.random() - 0.5) * 1.5;
            case "SUPPLY_AIR_TEMP" -> 13.0 + (Math.random() - 0.5) * 2.0;
            case "RETURN_AIR_TEMP" -> 22.0 + 3.0 * occupancyFactor + (Math.random() - 0.5) * 1.0;
            case "ZONE_HUMIDITY" -> 45.0 + 10.0 * occupancyFactor + (Math.random() - 0.5) * 5.0;
            case "POWER_KW" -> 15.0 * occupancyFactor + (Math.random() - 0.5) * 3.0;
            case "AIRFLOW_CFM" -> 800.0 * occupancyFactor + (Math.random() - 0.5) * 50.0;
            case "CHILLED_WATER_SUPPLY_TEMP" -> 7.0 + (Math.random() - 0.5) * 1.0;
            case "CHILLED_WATER_RETURN_TEMP" -> 13.0 + (Math.random() - 0.5) * 1.0;
            case "OCCUPANCY" -> Math.random() < occupancyFactor ? 1.0 : 0.0;
            case "STATIC_PRESSURE_PA" -> 125.0 + (Math.random() - 0.5) * 20.0;
            default -> 0.0;
        };
    }

    private String inferUnit(String pointId) {
        if (pointId.contains("TEMP")) return "CELSIUS";
        if (pointId.contains("HUMIDITY")) return "PERCENT";
        if (pointId.contains("POWER") || pointId.contains("KW")) return "KW";
        if (pointId.contains("AIRFLOW") || pointId.contains("CFM")) return "CFM";
        if (pointId.contains("PRESSURE") || pointId.contains("PA")) return "PASCAL";
        if (pointId.contains("OCCUPANCY")) return "BOOLEAN_NUMERIC";
        return "UNITLESS";
    }
}
