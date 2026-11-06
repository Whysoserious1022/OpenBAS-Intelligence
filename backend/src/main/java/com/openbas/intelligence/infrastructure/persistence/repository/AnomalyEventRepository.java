package com.openbas.intelligence.infrastructure.persistence.repository;

import com.openbas.intelligence.infrastructure.persistence.entity.AnomalyEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AnomalyEventRepository extends JpaRepository<AnomalyEventEntity, UUID> {

    List<AnomalyEventEntity> findAllByEquipmentIdAndResolvedFalseOrderByDetectedAtDesc(String equipmentId);

    Page<AnomalyEventEntity> findAllByBuildingIdOrderByDetectedAtDesc(String buildingId, Pageable pageable);

    /**
     * Find active (unresolved) anomalies for a building, ordered by severity then recency.
     * Uses an explicit JPQL query because JPA query derivation cannot handle mixed
     * ascending/descending ordering on multiple columns in a single derived method name.
     */
    @Query("SELECT a FROM AnomalyEventEntity a WHERE a.buildingId = :buildingId " +
           "AND a.resolved = false ORDER BY a.severity ASC, a.detectedAt DESC")
    List<AnomalyEventEntity> findActiveByBuildingOrderBySeverityAndRecency(@Param("buildingId") String buildingId);

    @Query("SELECT a FROM AnomalyEventEntity a WHERE a.buildingId = :buildingId " +
           "AND a.detectedAt BETWEEN :from AND :to ORDER BY a.detectedAt DESC")
    List<AnomalyEventEntity> findByBuildingInRange(@Param("buildingId") String buildingId,
                                                    @Param("from") Instant from,
                                                    @Param("to") Instant to);

    @Query("SELECT COUNT(a) FROM AnomalyEventEntity a WHERE a.resolved = false AND a.buildingId = :buildingId")
    long countActiveByBuilding(@Param("buildingId") String buildingId);

    @Query("SELECT COUNT(a) FROM AnomalyEventEntity a WHERE a.resolved = false")
    long countAllActive();
}
