package com.openbas.intelligence.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a physical or simulated device in the building.
 * A device is the lowest level in the hierarchy and communicates via
 * industrial protocols (BACnet, MQTT, OPC UA, Simulated).
 * Devices expose data points that generate telemetry.
 */
@Entity
@Table(name = "devices", indexes = {
    @Index(name = "idx_devices_equipment_id", columnList = "equipment_id"),
    @Index(name = "idx_devices_external_id", columnList = "external_id"),
    @Index(name = "idx_devices_protocol", columnList = "protocol"),
    @Index(name = "idx_devices_status", columnList = "communication_status")
})
public class DeviceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_id", nullable = false, length = 100, unique = true)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private EquipmentEntity equipment;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "protocol", nullable = false, length = 30)
    private String protocol;  // BACNET_IP, MQTT, OPC_UA, SIMULATED

    // BACnet-specific
    @Column(name = "bacnet_device_id")
    private Integer bacnetDeviceId;

    @Column(name = "bacnet_ip_address", length = 50)
    private String bacnetIpAddress;

    @Column(name = "bacnet_port")
    private Integer bacnetPort;

    // MQTT-specific
    @Column(name = "mqtt_topic_prefix", length = 200)
    private String mqttTopicPrefix;

    @Column(name = "communication_status", length = 30)
    private String communicationStatus = "UNKNOWN";  // ONLINE, OFFLINE, DEGRADED, UNKNOWN

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "last_error_at")
    private Instant lastErrorAt;

    @Column(name = "last_error_message", length = 500)
    private String lastErrorMessage;

    @Column(name = "simulated", nullable = false)
    private boolean simulated = false;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DeviceEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public EquipmentEntity getEquipment() { return equipment; }
    public void setEquipment(EquipmentEntity equipment) { this.equipment = equipment; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public Integer getBacnetDeviceId() { return bacnetDeviceId; }
    public void setBacnetDeviceId(Integer bacnetDeviceId) { this.bacnetDeviceId = bacnetDeviceId; }
    public String getBacnetIpAddress() { return bacnetIpAddress; }
    public void setBacnetIpAddress(String bacnetIpAddress) { this.bacnetIpAddress = bacnetIpAddress; }
    public Integer getBacnetPort() { return bacnetPort; }
    public void setBacnetPort(Integer bacnetPort) { this.bacnetPort = bacnetPort; }
    public String getMqttTopicPrefix() { return mqttTopicPrefix; }
    public void setMqttTopicPrefix(String mqttTopicPrefix) { this.mqttTopicPrefix = mqttTopicPrefix; }
    public String getCommunicationStatus() { return communicationStatus; }
    public void setCommunicationStatus(String communicationStatus) { this.communicationStatus = communicationStatus; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public Instant getLastErrorAt() { return lastErrorAt; }
    public void setLastErrorAt(Instant lastErrorAt) { this.lastErrorAt = lastErrorAt; }
    public String getLastErrorMessage() { return lastErrorMessage; }
    public void setLastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; }
    public boolean isSimulated() { return simulated; }
    public void setSimulated(boolean simulated) { this.simulated = simulated; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
