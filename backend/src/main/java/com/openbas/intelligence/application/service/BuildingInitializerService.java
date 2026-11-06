package com.openbas.intelligence.application.service;

import com.openbas.intelligence.infrastructure.persistence.entity.*;
import com.openbas.intelligence.infrastructure.persistence.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Initializes the simulated building hierarchy in the database on startup.
 * Creates Nexus Tower with 5 floors, 4 zones per floor, and representative equipment.
 *
 * This runs only if the building has not already been initialized (idempotent).
 * Equipment created here includes: AHUs, VAVs, Chiller, Pumps, and Sensors.
 * All equipment is marked simulation_enabled = true.
 */
@Service
public class BuildingInitializerService {

    private static final Logger log = LoggerFactory.getLogger(BuildingInitializerService.class);

    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;
    private final ZoneRepository zoneRepository;
    private final EquipmentRepository equipmentRepository;
    private final DeviceRepository deviceRepository;
    private final AppUserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final CommandRepository commandRepository;
    private final AnomalyEventRepository anomalyRepository;

    public BuildingInitializerService(BuildingRepository buildingRepository,
                                      FloorRepository floorRepository,
                                      ZoneRepository zoneRepository,
                                      EquipmentRepository equipmentRepository,
                                      DeviceRepository deviceRepository,
                                      AppUserRepository userRepository,
                                      org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                                      CommandRepository commandRepository,
                                      AnomalyEventRepository anomalyRepository) {
        this.buildingRepository = buildingRepository;
        this.floorRepository = floorRepository;
        this.zoneRepository = zoneRepository;
        this.equipmentRepository = equipmentRepository;
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.commandRepository = commandRepository;
        this.anomalyRepository = anomalyRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeBuildingHierarchy() {
        seedUsersIfMissing();
        seedCommandsIfMissing();
        seedAnomaliesIfMissing();

        Optional<BuildingEntity> existing = buildingRepository.findByExternalId("BUILDING-01");
        BuildingEntity building;
        if (existing.isEmpty()) {
            log.info("Creating default building BUILDING-01 (Nexus Tower)...");
            BuildingEntity newBuilding = new BuildingEntity();
            newBuilding.setExternalId("BUILDING-01");
            newBuilding.setName("Nexus Tower");
            newBuilding.setAddress("100 Innovation Way, Tech District");
            newBuilding.setTotalAreaSqm(12000.0);
            newBuilding.setFloorCount(5);
            newBuilding.setActive(true);
            building = buildingRepository.save(newBuilding);
        } else {
            building = existing.get();
        }

        if (!floorRepository.findAllByBuilding_IdAndActiveTrueOrderByFloorNumberAsc(building.getId()).isEmpty()) {
            log.info("Building {} already initialized with floors. Skipping initialization.", building.getExternalId());
            return;
        }

        log.info("Initializing simulated building: {} ({})", building.getName(), building.getExternalId());
        initializeFloors(building);
        log.info("Building initialization complete. Floors=5, Zones=20, Equipment=~30");
    }

    private void seedUsersIfMissing() {
        createUserIfMissing("admin", "admin@openbas.internal", "admin123", "ADMIN", "System Administrator");
        createUserIfMissing("operator", "operator@openbas.internal", "operator123", "OPERATOR", "HVAC Operations Engineer");
        createUserIfMissing("supervisor", "supervisor@openbas.internal", "supervisor123", "SUPERVISOR", "Facilities Supervisor");
    }

    private void createUserIfMissing(String username, String email, String password, String role, String fullName) {
        if (!userRepository.existsByUsername(username)) {
            AppUserEntity user = new AppUserEntity();
            user.setUsername(username);
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setRole(role);
            user.setFullName(fullName);
            user.setActive(true);
            userRepository.save(user);
            log.info("Seeded default user: {} ({})", username, role);
        }
    }

    private void seedCommandsIfMissing() {
        if (commandRepository.count() == 0) {
            CommandEntity cmd1 = new CommandEntity();
            cmd1.setIdempotencyKey("IDEMP-SETPOINT-F1Z2");
            cmd1.setTargetEquipmentId("VAV-F1-Z2");
            cmd1.setTargetPointId("ZONE_TEMP_SETPOINT");
            cmd1.setCommandType("SET_SETPOINT");
            cmd1.setCurrentValue(21.0);
            cmd1.setProposedValue(23.5);
            cmd1.setUnit("CELSIUS");
            cmd1.setReason("Energy Optimization Agent: Relax cooling setpoint in unoccupied office zone.");
            cmd1.setExpectedBenefit("Saves 3.4 kW fan & compressor power (approx 12% zone energy)");
            cmd1.setPotentialRisk("Zone temperature settles at 23.2°C; fully satisfies ASHRAE 55 standards.");
            cmd1.setInitiatingAgent("ENERGY_OPTIMIZATION_AGENT");
            cmd1.setConfidence(0.90);
            cmd1.setStatus("PENDING_APPROVAL");
            cmd1.setPolicyValidationResult("REQUIRES_APPROVAL");
            cmd1.setDigitalTwinSimulated(true);
            cmd1.setDigitalTwinPredictedOutcome("Digital Twin (1st-Order RC Model): Thermal settling at 23.2°C after 25 mins. Power change: -1.8 kW (-14.2%). Comfort risk: LOW.");
            cmd1.setRequiredApprovalLevel("OPERATOR");
            cmd1.setProposedBy("Energy Optimization Agent");
            cmd1.setBuildingId("BUILDING-01");
            cmd1.setExpiresAt(java.time.Instant.now().plus(60, java.time.temporal.ChronoUnit.MINUTES));
            commandRepository.save(cmd1);

            CommandEntity cmd2 = new CommandEntity();
            cmd2.setIdempotencyKey("IDEMP-THROTTLE-AHU2");
            cmd2.setTargetEquipmentId("AHU-F2");
            cmd2.setTargetPointId("FAN_SPEED_SETPOINT");
            cmd2.setCommandType("SET_SETPOINT");
            cmd2.setCurrentValue(85.0);
            cmd2.setProposedValue(70.0);
            cmd2.setUnit("PERCENT");
            cmd2.setReason("Predictive Maintenance Agent: Derate fan speed to 70% to protect motor bearings from filter overload.");
            cmd2.setExpectedBenefit("Avoids premature bearing seizure while service ticket dispatches.");
            cmd2.setPotentialRisk("Minor reduction in airflow; floor remains within ventilation code.");
            cmd2.setInitiatingAgent("PREDICTIVE_MAINTENANCE_AGENT");
            cmd2.setConfidence(0.89);
            cmd2.setStatus("PENDING_APPROVAL");
            cmd2.setPolicyValidationResult("REQUIRES_APPROVAL");
            cmd2.setDigitalTwinSimulated(true);
            cmd2.setDigitalTwinPredictedOutcome("Digital Twin (Fan Affinity Law): Fan speed 70% cuts motor heat dissipation by 41%. Lowers mechanical stress.");
            cmd2.setRequiredApprovalLevel("OPERATOR");
            cmd2.setProposedBy("Predictive Maintenance Agent");
            cmd2.setBuildingId("BUILDING-01");
            cmd2.setExpiresAt(java.time.Instant.now().plus(60, java.time.temporal.ChronoUnit.MINUTES));
            commandRepository.save(cmd2);

            log.info("Seeded initial pending commands in Approval Center: cmd1 (VAV-F1-Z2), cmd2 (AHU-F2)");
        }
    }

    private void seedAnomaliesIfMissing() {
        if (anomalyRepository.count() == 0) {
            AnomalyEventEntity anm1 = new AnomalyEventEntity();
            anm1.setEquipmentId("AHU-F3");
            anm1.setDeviceId("DEV-AHU-F3");
            anm1.setZoneId("FLOOR-3");
            anm1.setBuildingId("BUILDING-01");
            anm1.setAnomalyType("TEMP_HIGH");
            anm1.setSeverity("HIGH");
            anm1.setPointId("SUPPLY_AIR_TEMP");
            anm1.setObservedValue(19.5);
            anm1.setExpectedMin(10.0);
            anm1.setExpectedMax(15.0);
            anm1.setDetectionMethod("THRESHOLD");
            anm1.setDescription("Supply air temperature 19.5°C exceeds maximum cooling threshold (15.0°C). Valve actuator feedback stuck at 15%.");
            anm1.setEvidence("{\"deviceId\":\"DEV-AHU-F3\",\"pointId\":\"SUPPLY_AIR_TEMP\",\"observedValue\":19.5}");
            anm1.setDetectedAt(java.time.Instant.now().minus(18, java.time.temporal.ChronoUnit.MINUTES));
            anomalyRepository.save(anm1);

            AnomalyEventEntity anm2 = new AnomalyEventEntity();
            anm2.setEquipmentId("AHU-F2");
            anm2.setDeviceId("DEV-AHU-F2");
            anm2.setZoneId("FLOOR-2");
            anm2.setBuildingId("BUILDING-01");
            anm2.setAnomalyType("FILTER_PRESSURE_HIGH");
            anm2.setSeverity("MEDIUM");
            anm2.setPointId("FILTER_DP_PA");
            anm2.setObservedValue(285.0);
            anm2.setExpectedMin(50.0);
            anm2.setExpectedMax(220.0);
            anm2.setDetectionMethod("MOVING_AVERAGE");
            anm2.setDescription("Filter bank differential pressure 285 Pa indicates severe particulate loading. Motor vibration elevated.");
            anm2.setEvidence("{\"deviceId\":\"DEV-AHU-F2\",\"pointId\":\"FILTER_DP_PA\",\"observedValue\":285.0}");
            anm2.setDetectedAt(java.time.Instant.now().minus(45, java.time.temporal.ChronoUnit.MINUTES));
            anomalyRepository.save(anm2);

            log.info("Seeded initial active anomalies in Maintenance Center: anm1 (AHU-F3), anm2 (AHU-F2)");
        }
    }

    private void initializeFloors(BuildingEntity building) {
        String[][] zoneTypes = {
            {"LOBBY", "CONFERENCE", "OFFICE", "SERVER_ROOM"},
            {"OFFICE", "OFFICE", "CONFERENCE", "BREAK_ROOM"},
            {"OFFICE", "OFFICE", "OFFICE", "CONFERENCE"},
            {"EXECUTIVE", "CONFERENCE", "OFFICE", "BREAK_ROOM"},
            {"PLANT_ROOM", "STORAGE", "OFFICE", "CONFERENCE"}
        };

        for (int floorNum = 1; floorNum <= 5; floorNum++) {
            FloorEntity floor = createFloor(building, floorNum);
            UUID chillerPlantFloor = floorNum == 5 ? floor.getId() : null;

            // One AHU per floor
            EquipmentEntity ahu = createEquipment(building.getId(), floor.getId(), null,
                "AHU-F" + floorNum, "Floor " + floorNum + " Air Handling Unit",
                "AHU", "Carrier", "AHU-39EG", null, floor);

            // 4 zones per floor, each with a VAV
            for (int zoneNum = 1; zoneNum <= 4; zoneNum++) {
                String zoneType = zoneTypes[floorNum - 1][zoneNum - 1];
                ZoneEntity zone = createZone(building, floor, floorNum, zoneNum, zoneType);

                EquipmentEntity vav = createEquipment(building.getId(), floor.getId(), zone.getId(),
                    "VAV-F" + floorNum + "-Z" + zoneNum,
                    "Floor " + floorNum + " Zone " + zoneNum + " VAV",
                    "VAV", "Siemens", "VAV-2000", ahu, zone);

                // Temperature sensor in each zone
                EquipmentEntity tempSensor = createEquipment(building.getId(), floor.getId(), zone.getId(),
                    "SENSOR-TEMP-F" + floorNum + "-Z" + zoneNum,
                    "Zone " + zoneNum + " Temperature Sensor",
                    "SENSOR", "Honeywell", "T6861", vav, zone);

                createDevice(tempSensor,
                    "DEV-TEMP-F" + floorNum + "-Z" + zoneNum,
                    "Temperature Sensor Device",
                    "openbas/telemetry/BUILDING-01/FLOOR-" + floorNum + "/ZONE-" + floorNum + "-" + zoneNum +
                    "/SENSOR-TEMP-F" + floorNum + "-Z" + zoneNum +
                    "/DEV-TEMP-F" + floorNum + "-Z" + zoneNum + "/ZONE_TEMP");
            }

            // Energy meter per floor
            EquipmentEntity meter = createEquipment(building.getId(), floor.getId(), null,
                "METER-F" + floorNum, "Floor " + floorNum + " Energy Meter",
                "ENERGY_METER", "Schneider", "PowerLogic", null, floor);

            createDevice(ahu,
                "DEV-AHU-F" + floorNum,
                "AHU Controller Device",
                "openbas/telemetry/BUILDING-01/FLOOR-" + floorNum + "/+/AHU-F" + floorNum +
                "/DEV-AHU-F" + floorNum + "/SUPPLY_AIR_TEMP");
        }

        // Central chiller plant (basement, not in floors)
        EquipmentEntity chiller = createEquipment(building.getId(), null, null,
            "CHILLER-01", "Central Chiller Plant",
            "CHILLER", "Carrier", "30XA", null, null);

        log.info("Floors and zones initialized for building {}", building.getExternalId());
    }

    private FloorEntity createFloor(BuildingEntity building, int floorNum) {
        FloorEntity floor = new FloorEntity();
        floor.setExternalId("FLOOR-" + floorNum);
        floor.setBuilding(building);
        floor.setName("Floor " + floorNum);
        floor.setFloorNumber(floorNum);
        floor.setAreaSqm(2400.0);
        floor.setActive(true);
        return floorRepository.save(floor);
    }

    private ZoneEntity createZone(BuildingEntity building, FloorEntity floor,
                                   int floorNum, int zoneNum, String zoneType) {
        ZoneEntity zone = new ZoneEntity();
        zone.setExternalId("ZONE-" + floorNum + "-" + zoneNum);
        zone.setFloor(floor);
        zone.setBuildingId(building.getId());
        zone.setName("Floor " + floorNum + " Zone " + zoneNum + " (" + zoneType + ")");
        zone.setZoneType(zoneType);
        zone.setAreaSqm(600.0);
        zone.setDesignOccupancy(zoneType.contains("OFFICE") ? 25 : 10);
        zone.setComfortTempMinCelsius("SERVER_ROOM".equals(zoneType) ? 18.0 : 20.0);
        zone.setComfortTempMaxCelsius("SERVER_ROOM".equals(zoneType) ? 22.0 : 26.0);
        zone.setActive(true);
        return zoneRepository.save(zone);
    }

    private EquipmentEntity createEquipment(UUID buildingId, UUID floorId, UUID zoneIdValue,
                                             String externalId, String name, String type,
                                             String manufacturer, String model,
                                             EquipmentEntity parent, Object location) {
        if (equipmentRepository.existsByExternalId(externalId)) {
            return equipmentRepository.findByExternalId(externalId).orElseThrow();
        }
        EquipmentEntity eq = new EquipmentEntity();
        eq.setExternalId(externalId);
        eq.setBuildingId(buildingId);
        eq.setFloorId(floorId);
        if (zoneIdValue != null) {
            ZoneEntity zone = new ZoneEntity();
            zone.setId(zoneIdValue);
        }
        eq.setName(name);
        eq.setEquipmentType(type);
        eq.setManufacturer(manufacturer);
        eq.setModel(model);
        eq.setOperationalStatus("NORMAL");
        eq.setHealthScore(0.95);
        eq.setSimulationEnabled(true);
        eq.setActive(true);
        if (parent != null) eq.setParentEquipment(parent);
        if (location instanceof ZoneEntity z) eq.setZone(z);
        return equipmentRepository.save(eq);
    }

    private void createDevice(EquipmentEntity equipment, String externalId,
                               String name, String mqttTopicPrefix) {
        if (deviceRepository.existsByExternalId(externalId)) return;
        DeviceEntity device = new DeviceEntity();
        device.setExternalId(externalId);
        device.setEquipment(equipment);
        device.setName(name);
        device.setProtocol("SIMULATED");
        device.setMqttTopicPrefix(mqttTopicPrefix);
        device.setCommunicationStatus("ONLINE");
        device.setSimulated(true);
        device.setActive(true);
        deviceRepository.save(device);
    }
}
