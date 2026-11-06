package com.openbas.intelligence.application.service;

import com.openbas.intelligence.infrastructure.persistence.entity.TelemetryEventEntity;
import com.openbas.intelligence.infrastructure.persistence.repository.TelemetryEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Telemetry ingestion pipeline.
 * Handles: duplicate detection, out-of-order detection, stale detection,
 *          value validation, concurrent ingestion from multiple adapters.
 *
 * Key design decisions:
 * - Async so telemetry ingestion never blocks the protocol adapter or MQTT handler.
 * - Idempotent: duplicate events (same device+point+timestamp) are recorded but flagged.
 * - Does NOT block on AI analysis — that is triggered asynchronously.
 */
@Service
public class TelemetryIngestionService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryIngestionService.class);

    // A reading is considered stale if the event timestamp is > 5 minutes behind ingestion time
    private static final long STALE_THRESHOLD_SECONDS = 300;

    private final TelemetryEventRepository telemetryEventRepository;
    private final AnomalyDetectionService anomalyDetectionService;
    private final DeviceStatusTracker deviceStatusTracker;

    private final Counter ingestedCounter;
    private final Counter duplicateCounter;
    private final Counter staleCounter;
    private final Counter invalidCounter;
    private final Timer ingestionTimer;

    public TelemetryIngestionService(TelemetryEventRepository telemetryEventRepository,
                                     AnomalyDetectionService anomalyDetectionService,
                                     DeviceStatusTracker deviceStatusTracker,
                                     MeterRegistry meterRegistry) {
        this.telemetryEventRepository = telemetryEventRepository;
        this.anomalyDetectionService = anomalyDetectionService;
        this.deviceStatusTracker = deviceStatusTracker;

        this.ingestedCounter = Counter.builder("openbas.telemetry.ingested")
            .description("Total telemetry events ingested")
            .register(meterRegistry);
        this.duplicateCounter = Counter.builder("openbas.telemetry.duplicates")
            .description("Duplicate telemetry events detected")
            .register(meterRegistry);
        this.staleCounter = Counter.builder("openbas.telemetry.stale")
            .description("Stale telemetry events detected")
            .register(meterRegistry);
        this.invalidCounter = Counter.builder("openbas.telemetry.invalid")
            .description("Invalid telemetry events rejected")
            .register(meterRegistry);
        this.ingestionTimer = Timer.builder("openbas.telemetry.ingestion.duration")
            .description("Telemetry ingestion processing time")
            .register(meterRegistry);
    }

    /**
     * Ingests a single normalized telemetry event asynchronously.
     * Called by protocol adapters (MQTT handler, BACnet reader, Simulator).
     */
    @Async("telemetryIngestionExecutor")
    @Transactional
    public CompletableFuture<Void> ingest(TelemetryEventEntity event) {
        return ingestionTimer.record(() -> doIngest(event));
    }

    private CompletableFuture<Void> doIngest(TelemetryEventEntity event) {
        try {
            // 1. Basic validation
            if (!isValid(event)) {
                invalidCounter.increment();
                log.warn("Rejected invalid telemetry: device={} point={}", event.getDeviceId(), event.getPointId());
                return CompletableFuture.completedFuture(null);
            }

            // 2. Set ingestion timestamp
            event.setIngestedAt(Instant.now());

            // 3. Duplicate detection
            if (telemetryEventRepository.existsDuplicate(
                    event.getDeviceId(), event.getPointId(), event.getEventTimestamp())) {
                event.setDuplicate(true);
                duplicateCounter.increment();
                log.debug("Duplicate telemetry: device={} point={} ts={}",
                    event.getDeviceId(), event.getPointId(), event.getEventTimestamp());
                // Still persist (marked as duplicate) for audit
            }

            // 4. Out-of-order detection (event timestamp > 10s behind now when first seen)
            long lagSeconds = Instant.now().getEpochSecond() - event.getEventTimestamp().getEpochSecond();
            if (lagSeconds > 10 && !event.isDuplicate()) {
                event.setOutOfOrder(true);
                log.debug("Out-of-order telemetry: device={} lag={}s", event.getDeviceId(), lagSeconds);
            }

            // 5. Stale detection
            if (lagSeconds > STALE_THRESHOLD_SECONDS) {
                event.setDataQuality("STALE");
                staleCounter.increment();
            }

            // 6. Persist
            telemetryEventRepository.save(event);
            ingestedCounter.increment();

            // 7. Update device last-seen (non-blocking)
            deviceStatusTracker.recordSeen(event.getDeviceId(), event.getEventTimestamp());

            // 8. Trigger anomaly evaluation (non-blocking, async)
            if (!event.isDuplicate() && "GOOD".equals(event.getDataQuality())
                    || "SIMULATED".equals(event.getDataQuality())) {
                anomalyDetectionService.evaluateAsync(event);
            }

        } catch (Exception e) {
            log.error("Failed to ingest telemetry event device={} point={}",
                event.getDeviceId(), event.getPointId(), e);
        }
        return CompletableFuture.completedFuture(null);
    }

    private boolean isValid(TelemetryEventEntity event) {
        if (event.getDeviceId() == null || event.getDeviceId().isBlank()) return false;
        if (event.getEquipmentId() == null || event.getEquipmentId().isBlank()) return false;
        if (event.getPointId() == null || event.getPointId().isBlank()) return false;
        if (event.getBuildingId() == null || event.getBuildingId().isBlank()) return false;
        if (event.getEventTimestamp() == null) return false;
        if (event.getSourceProtocol() == null || event.getSourceProtocol().isBlank()) return false;
        // All three value fields null = no data
        if (event.getNumericValue() == null && event.getStringValue() == null
                && event.getBooleanValue() == null) return false;
        // Reject future timestamps (> 60 seconds ahead)
        if (event.getEventTimestamp().isAfter(Instant.now().plusSeconds(60))) return false;
        return true;
    }
}
