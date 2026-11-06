package com.openbas.intelligence.infrastructure.gateway.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openbas.intelligence.application.service.TelemetryIngestionService;
import com.openbas.intelligence.infrastructure.gateway.protocol.IndustrialProtocolAdapter;
import com.openbas.intelligence.infrastructure.persistence.entity.TelemetryEventEntity;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MQTT Protocol Adapter using Eclipse Paho.
 * Subscribes to the OpenBAS telemetry topic and routes incoming messages
 * to the TelemetryIngestionService.
 *
 * Message format expected:
 * Topic: openbas/telemetry/{buildingId}/{floorId}/{zoneId}/{equipmentId}/{deviceId}/{pointId}
 * Payload: JSON with { value, unit, timestamp, quality }
 */
@Component("mqttAdapter")
public class MqttProtocolAdapter implements IndustrialProtocolAdapter, MqttCallback {

    private static final Logger log = LoggerFactory.getLogger(MqttProtocolAdapter.class);

    @Value("${openbas.gateway.mqtt.broker-url:tcp://localhost:1883}")
    private String brokerUrl;

    @Value("${openbas.gateway.mqtt.client-id:openbas-gateway}")
    private String clientId;

    @Value("${openbas.gateway.mqtt.username:}")
    private String username;

    @Value("${openbas.gateway.mqtt.password:}")
    private String password;

    @Value("${openbas.gateway.mqtt.topics.telemetry:openbas/telemetry/#}")
    private String telemetryTopic;

    private MqttClient mqttClient;
    private volatile boolean connected = false;
    private volatile Instant lastSuccessfulRead;
    private volatile String lastError;
    private final AtomicLong readsSucceeded = new AtomicLong(0);
    private final AtomicLong readsFailed = new AtomicLong(0);

    private final TelemetryIngestionService ingestionService;
    private final ObjectMapper objectMapper;

    public MqttProtocolAdapter(TelemetryIngestionService ingestionService, ObjectMapper objectMapper) {
        this.ingestionService = ingestionService;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeOnStartup() {
        log.info("Initializing MQTT adapter: {}", brokerUrl);
        connect();
    }

    @Override
    public String getProtocolId() { return "MQTT"; }

    @Override
    public boolean isConnected() { return connected && mqttClient != null && mqttClient.isConnected(); }

    @Override
    public boolean connect() {
        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(30);
            options.setKeepAliveInterval(60);
            options.setAutomaticReconnect(true);
            if (username != null && !username.isBlank()) {
                options.setUserName(username);
                options.setPassword(password.toCharArray());
            }

            mqttClient = new MqttClient(brokerUrl, clientId + "-" + System.currentTimeMillis(),
                new MemoryPersistence());
            mqttClient.setCallback(this);
            mqttClient.connect(options);
            mqttClient.subscribe(telemetryTopic, 1);  // QoS 1

            connected = true;
            log.info("MQTT adapter connected to {} and subscribed to {}", brokerUrl, telemetryTopic);
            return true;
        } catch (MqttException e) {
            connected = false;
            lastError = e.getMessage();
            log.warn("MQTT adapter failed to connect to {}: {} — will operate in degraded mode",
                brokerUrl, e.getMessage());
            return false;
        }
    }

    @Override
    public void disconnect() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.unsubscribe(telemetryTopic);
                mqttClient.disconnect();
            }
        } catch (MqttException e) {
            log.warn("Error disconnecting MQTT adapter: {}", e.getMessage());
        } finally {
            connected = false;
        }
    }

    @Override
    public List<DiscoveredDevice> discoverDevices() {
        // MQTT is publish/subscribe; device discovery is not supported in the same way.
        // Devices are registered through the building hierarchy, not through MQTT discovery.
        return List.of();
    }

    @Override
    public TelemetryEventEntity readPoint(String deviceId, String pointId) {
        // MQTT does not support synchronous point reads.
        // Readings come asynchronously via the messageArrived callback.
        throw new UnsupportedOperationException(
            "MQTT adapter does not support synchronous point reads. Use MQTT publish/subscribe.");
    }

    @Override
    public CommandResult submitCommand(GatewayCommand command) {
        if (!isConnected()) {
            return new CommandResult(command.commandId(), false,
                "MQTT adapter not connected", null, Instant.now());
        }
        try {
            String topic = "openbas/commands/" + command.deviceId() + "/" + command.pointId();
            String payload = objectMapper.writeValueAsString(command);
            mqttClient.publish(topic, payload.getBytes(), 1, false);
            log.info("[MQTT] Command published: topic={} command={}", topic, command.commandId());
            return new CommandResult(command.commandId(), true,
                "Command published to " + topic, command.value(), Instant.now());
        } catch (Exception e) {
            log.error("MQTT command publish failed: {}", e.getMessage(), e);
            return new CommandResult(command.commandId(), false, e.getMessage(), null, Instant.now());
        }
    }

    @Override
    public AdapterHealth getHealth() {
        return new AdapterHealth("MQTT", isConnected(),
            isConnected() ? 1 : 0,
            lastSuccessfulRead, lastError,
            readsSucceeded.get(), readsFailed.get());
    }

    // ── MqttCallback ──────────────────────────────────────────────────────────

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            TelemetryEventEntity event = parseMqttMessage(topic, new String(message.getPayload()));
            if (event != null) {
                ingestionService.ingest(event);
                readsSucceeded.incrementAndGet();
                lastSuccessfulRead = Instant.now();
            }
        } catch (Exception e) {
            readsFailed.incrementAndGet();
            lastError = e.getMessage();
            log.warn("Failed to process MQTT message on topic {}: {}", topic, e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        connected = false;
        lastError = cause.getMessage();
        log.warn("MQTT connection lost: {} — Paho will attempt reconnection", cause.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Outgoing message delivery confirmed
    }

    /**
     * Parses an MQTT telemetry message into a canonical TelemetryEventEntity.
     *
     * Expected topic pattern:
     * openbas/telemetry/{buildingId}/{floorId}/{zoneId}/{equipmentId}/{deviceId}/{pointId}
     *
     * Expected JSON payload:
     * { "value": 23.5, "unit": "CELSIUS", "timestamp": "2026-11-06T...", "quality": "GOOD" }
     */
    private TelemetryEventEntity parseMqttMessage(String topic, String payload) {
        try {
            String[] parts = topic.split("/");
            if (parts.length < 8) {
                log.debug("MQTT topic does not match expected pattern: {}", topic);
                return null;
            }
            // parts[0]="openbas" parts[1]="telemetry"
            String buildingId = parts[2];
            String floorId    = parts[3];
            String zoneId     = parts[4];
            String equipmentId = parts[5];
            String deviceId   = parts[6];
            String pointId    = parts[7];

            JsonNode json = objectMapper.readTree(payload);

            TelemetryEventEntity event = new TelemetryEventEntity();
            event.setDeviceId(deviceId);
            event.setEquipmentId(equipmentId);
            event.setPointId(pointId);
            event.setBuildingId(buildingId);
            event.setFloorId(floorId);
            event.setZoneId(zoneId);
            event.setSourceProtocol("MQTT");
            event.setSourceDevice(deviceId);

            if (json.has("value")) {
                JsonNode val = json.get("value");
                if (val.isNumber()) event.setNumericValue(val.asDouble());
                else if (val.isBoolean()) event.setBooleanValue(val.asBoolean());
                else event.setStringValue(val.asText());
            }

            event.setUnit(json.has("unit") ? json.get("unit").asText() : null);
            event.setDataQuality(json.has("quality") ? json.get("quality").asText() : "GOOD");
            event.setCommunicationStatus("ONLINE");

            if (json.has("timestamp")) {
                event.setEventTimestamp(Instant.parse(json.get("timestamp").asText()));
            } else {
                event.setEventTimestamp(Instant.now());
            }

            return event;
        } catch (Exception e) {
            log.warn("Failed to parse MQTT payload on topic {}: {}", topic, e.getMessage());
            return null;
        }
    }
}
