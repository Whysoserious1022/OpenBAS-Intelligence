package com.openbas.intelligence.infrastructure.gateway.protocol;

import com.openbas.intelligence.infrastructure.persistence.entity.TelemetryEventEntity;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Core contract for all industrial protocol adapters.
 *
 * Every adapter (BACnet, MQTT, Simulator) implements this interface.
 * Business logic interacts only with this interface, never with protocol-specific classes.
 *
 * CRITICAL: submitCommand() is intentionally restrictive.
 * It may only write to points that have been registered and authorized.
 * The gateway itself does NOT perform policy validation — that is the
 * PolicyEngine's responsibility. The gateway simply executes what has
 * been validated and authorized.
 */
public interface IndustrialProtocolAdapter {

    /**
     * Returns the protocol identifier for this adapter.
     */
    String getProtocolId();

    /**
     * Returns true if the adapter is currently connected and operational.
     */
    boolean isConnected();

    /**
     * Initiates connection. Returns true on success.
     */
    boolean connect();

    /**
     * Closes the connection gracefully.
     */
    void disconnect();

    /**
     * Discovers devices reachable via this protocol.
     * Returns a list of discovered device identifiers.
     */
    List<DiscoveredDevice> discoverDevices();

    /**
     * Reads the current value of a specific point from a device.
     */
    TelemetryEventEntity readPoint(String deviceId, String pointId);

    /**
     * Submits a command to a device point.
     * The caller is responsible for ensuring this command has been validated
     * by the PolicyEngine before calling this method.
     *
     * Returns the command result.
     */
    CommandResult submitCommand(GatewayCommand command);

    /**
     * Returns the communication health status of this adapter.
     */
    AdapterHealth getHealth();

    record DiscoveredDevice(
        String deviceId,
        String deviceName,
        String address,
        String protocol,
        boolean reachable
    ) {}

    record GatewayCommand(
        String commandId,
        String deviceId,
        String pointId,
        Object value,
        String unit,
        boolean simulationOnly
    ) {}

    record CommandResult(
        String commandId,
        boolean success,
        String resultMessage,
        Object actualValue,
        java.time.Instant executedAt
    ) {}

    record AdapterHealth(
        String protocolId,
        boolean connected,
        int activeConnections,
        java.time.Instant lastSuccessfulRead,
        String lastError,
        long totalReadsSucceeded,
        long totalReadsFailed
    ) {}
}
