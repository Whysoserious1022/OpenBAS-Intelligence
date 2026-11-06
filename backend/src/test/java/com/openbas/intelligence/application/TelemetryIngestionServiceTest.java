package com.openbas.intelligence.application;

import com.openbas.intelligence.application.service.AnomalyDetectionService;
import com.openbas.intelligence.application.service.DeviceStatusTracker;
import com.openbas.intelligence.application.service.TelemetryIngestionService;
import com.openbas.intelligence.infrastructure.persistence.entity.TelemetryEventEntity;
import com.openbas.intelligence.infrastructure.persistence.repository.TelemetryEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TelemetryIngestionService Unit Tests")
class TelemetryIngestionServiceTest {

    @Mock private TelemetryEventRepository telemetryEventRepository;
    @Mock private AnomalyDetectionService anomalyDetectionService;
    @Mock private DeviceStatusTracker deviceStatusTracker;

    private TelemetryIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        ingestionService = new TelemetryIngestionService(
            telemetryEventRepository, anomalyDetectionService,
            deviceStatusTracker, new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("Valid telemetry is persisted")
    void ingest_validEvent_isPersisted() throws Exception {
        TelemetryEventEntity event = validEvent();
        when(telemetryEventRepository.existsDuplicate(any(), any(), any())).thenReturn(false);
        when(telemetryEventRepository.save(any())).thenReturn(event);

        ingestionService.ingest(event).get(); // Wait for async

        verify(telemetryEventRepository).save(any(TelemetryEventEntity.class));
    }

    @Test
    @DisplayName("Invalid telemetry (null deviceId) is rejected without persisting")
    void ingest_missingDeviceId_isRejected() throws Exception {
        TelemetryEventEntity event = validEvent();
        event.setDeviceId(null);

        ingestionService.ingest(event).get();

        verify(telemetryEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Invalid telemetry (all values null) is rejected")
    void ingest_allValuesNull_isRejected() throws Exception {
        TelemetryEventEntity event = validEvent();
        event.setNumericValue(null);
        event.setStringValue(null);
        event.setBooleanValue(null);

        ingestionService.ingest(event).get();

        verify(telemetryEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Duplicate telemetry is persisted but flagged")
    void ingest_duplicate_isFlaggedAndPersisted() throws Exception {
        TelemetryEventEntity event = validEvent();
        when(telemetryEventRepository.existsDuplicate(any(), any(), any())).thenReturn(true);
        when(telemetryEventRepository.save(any())).thenReturn(event);

        ingestionService.ingest(event).get();

        verify(telemetryEventRepository).save(argThat(TelemetryEventEntity::isDuplicate));
    }

    @Test
    @DisplayName("Future timestamp (>60s ahead) is rejected")
    void ingest_futureTimestamp_isRejected() throws Exception {
        TelemetryEventEntity event = validEvent();
        event.setEventTimestamp(Instant.now().plusSeconds(120));

        ingestionService.ingest(event).get();

        verify(telemetryEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Stale reading (>5min old) gets STALE quality")
    void ingest_staleReading_getsStaleMark() throws Exception {
        TelemetryEventEntity event = validEvent();
        event.setEventTimestamp(Instant.now().minusSeconds(400)); // 400s old
        when(telemetryEventRepository.existsDuplicate(any(), any(), any())).thenReturn(false);
        when(telemetryEventRepository.save(any())).thenReturn(event);

        ingestionService.ingest(event).get();

        verify(telemetryEventRepository).save(argThat(e -> "STALE".equals(e.getDataQuality())));
    }

    private TelemetryEventEntity validEvent() {
        TelemetryEventEntity event = new TelemetryEventEntity();
        event.setDeviceId("DEV-001");
        event.setEquipmentId("AHU-01");
        event.setPointId("ZONE_TEMP");
        event.setBuildingId("BUILDING-01");
        event.setEventTimestamp(Instant.now());
        event.setSourceProtocol("SIMULATED");
        event.setDataQuality("SIMULATED");
        event.setNumericValue(23.5);
        event.setUnit("CELSIUS");
        return event;
    }
}
