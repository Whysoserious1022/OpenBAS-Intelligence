package com.openbas.intelligence.application.service;

import com.openbas.intelligence.infrastructure.persistence.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks device last-seen timestamps and detects stale devices.
 * Uses an in-memory map for fast updates; periodically flushes to DB.
 * Runs a scheduled job to mark devices that haven't reported as OFFLINE.
 */
@Service
public class DeviceStatusTracker {

    private static final Logger log = LoggerFactory.getLogger(DeviceStatusTracker.class);

    /** Device considered offline after 5 minutes without telemetry */
    private static final long OFFLINE_THRESHOLD_MINUTES = 5;

    private final DeviceRepository deviceRepository;

    /** In-memory last-seen cache: externalDeviceId -> lastSeenInstant */
    private final ConcurrentHashMap<String, Instant> lastSeenCache = new ConcurrentHashMap<>();

    public DeviceStatusTracker(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    /**
     * Records that a device has been seen (called on each ingested telemetry event).
     * Non-blocking: just updates the in-memory cache.
     */
    @Async("telemetryIngestionExecutor")
    public void recordSeen(String deviceExternalId, Instant timestamp) {
        lastSeenCache.merge(deviceExternalId, timestamp, (existing, newTs) ->
            existing.isBefore(newTs) ? newTs : existing);
    }

    /**
     * Flush in-memory last-seen times to the database every 30 seconds.
     */
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void flushLastSeen() {
        if (lastSeenCache.isEmpty()) return;
        lastSeenCache.forEach((deviceId, ts) -> {
            int updated = deviceRepository.updateCommunicationStatus(deviceId, "ONLINE", ts);
            if (updated == 0) {
                log.debug("Device not found in DB for last-seen update: {}", deviceId);
            }
        });
    }

    /**
     * Detect stale devices every 60 seconds and mark them OFFLINE.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void detectStaleDevices() {
        Instant threshold = Instant.now().minus(OFFLINE_THRESHOLD_MINUTES, ChronoUnit.MINUTES);
        deviceRepository.findStaleDevices(threshold).forEach(device -> {
            device.setCommunicationStatus("OFFLINE");
            device.setLastErrorAt(Instant.now());
            device.setLastErrorMessage("No telemetry received for >" + OFFLINE_THRESHOLD_MINUTES + " minutes");
            deviceRepository.save(device);
            log.warn("Device marked OFFLINE (stale): {}", device.getExternalId());
        });
    }
}
