package com.openbas.intelligence.infrastructure.persistence.repository;

import com.openbas.intelligence.infrastructure.persistence.entity.TelemetryEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TelemetryEventRepository extends JpaRepository<TelemetryEventEntity, UUID> {

    // Latest reading per device/point
    @Query("SELECT t FROM TelemetryEventEntity t WHERE t.deviceId = :deviceId AND t.pointId = :pointId " +
           "ORDER BY t.eventTimestamp DESC LIMIT 1")
    Optional<TelemetryEventEntity> findLatest(@Param("deviceId") String deviceId,
                                               @Param("pointId") String pointId);

    // Historical data for a device/point in a time range
    @Query("SELECT t FROM TelemetryEventEntity t WHERE t.deviceId = :deviceId AND t.pointId = :pointId " +
           "AND t.eventTimestamp BETWEEN :from AND :to ORDER BY t.eventTimestamp ASC")
    List<TelemetryEventEntity> findByDeviceAndPointInRange(@Param("deviceId") String deviceId,
                                                            @Param("pointId") String pointId,
                                                            @Param("from") Instant from,
                                                            @Param("to") Instant to);

    // All readings for an equipment in a time range (for anomaly analysis)
    @Query("SELECT t FROM TelemetryEventEntity t WHERE t.equipmentId = :equipmentId " +
           "AND t.eventTimestamp BETWEEN :from AND :to ORDER BY t.eventTimestamp ASC")
    List<TelemetryEventEntity> findByEquipmentInRange(@Param("equipmentId") String equipmentId,
                                                       @Param("from") Instant from,
                                                       @Param("to") Instant to);

    // For a building/zone in a time range
    @Query("SELECT t FROM TelemetryEventEntity t WHERE t.buildingId = :buildingId " +
           "AND t.eventTimestamp BETWEEN :from AND :to ORDER BY t.eventTimestamp DESC")
    Page<TelemetryEventEntity> findByBuildingInRange(@Param("buildingId") String buildingId,
                                                      @Param("from") Instant from,
                                                      @Param("to") Instant to,
                                                      Pageable pageable);

    // Duplicate detection
    @Query("SELECT COUNT(t) > 0 FROM TelemetryEventEntity t WHERE t.deviceId = :deviceId " +
           "AND t.pointId = :pointId AND t.eventTimestamp = :ts")
    boolean existsDuplicate(@Param("deviceId") String deviceId,
                             @Param("pointId") String pointId,
                             @Param("ts") Instant ts);

    // For moving average anomaly detection
    @Query("SELECT t FROM TelemetryEventEntity t WHERE t.equipmentId = :equipmentId " +
           "AND t.pointId = :pointId AND t.eventTimestamp > :since " +
           "AND t.dataQuality IN ('GOOD', 'SIMULATED') ORDER BY t.eventTimestamp DESC")
    List<TelemetryEventEntity> findRecentForAnomaly(@Param("equipmentId") String equipmentId,
                                                     @Param("pointId") String pointId,
                                                     @Param("since") Instant since);

    @Query("SELECT COUNT(t) FROM TelemetryEventEntity t WHERE t.ingestedAt BETWEEN :from AND :to")
    long countIngestedInRange(@Param("from") Instant from, @Param("to") Instant to);
}
