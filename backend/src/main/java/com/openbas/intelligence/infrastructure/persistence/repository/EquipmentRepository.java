package com.openbas.intelligence.infrastructure.persistence.repository;

import com.openbas.intelligence.infrastructure.persistence.entity.EquipmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EquipmentRepository extends JpaRepository<EquipmentEntity, UUID> {

    Optional<EquipmentEntity> findByExternalId(String externalId);

    Optional<EquipmentEntity> findByExternalIdAndActiveTrue(String externalId);

    List<EquipmentEntity> findAllByBuildingIdAndActiveTrue(UUID buildingId);

    List<EquipmentEntity> findAllByZone_IdAndActiveTrue(UUID zoneId);

    List<EquipmentEntity> findAllByEquipmentTypeAndActiveTrue(String equipmentType);

    List<EquipmentEntity> findAllByOperationalStatusAndActiveTrue(String operationalStatus);

    @Query("SELECT e FROM EquipmentEntity e WHERE e.buildingId = :buildingId AND e.operationalStatus IN ('FAULT', 'DEGRADED') AND e.active = true")
    List<EquipmentEntity> findFaultedByBuilding(@Param("buildingId") UUID buildingId);

    @Query("SELECT e FROM EquipmentEntity e WHERE e.parentEquipment.id = :parentId AND e.active = true")
    List<EquipmentEntity> findChildEquipment(@Param("parentId") UUID parentId);

    @Query("SELECT COUNT(e) FROM EquipmentEntity e WHERE e.active = true")
    long countActive();

    @Query("SELECT COUNT(e) FROM EquipmentEntity e WHERE e.operationalStatus = 'FAULT' AND e.active = true")
    long countFaulted();

    Page<EquipmentEntity> findAllByBuildingIdAndActiveTrue(UUID buildingId, Pageable pageable);

    boolean existsByExternalId(String externalId);
}
