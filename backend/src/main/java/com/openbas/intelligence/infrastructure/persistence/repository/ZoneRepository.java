package com.openbas.intelligence.infrastructure.persistence.repository;

import com.openbas.intelligence.infrastructure.persistence.entity.ZoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ZoneRepository extends JpaRepository<ZoneEntity, UUID> {

    List<ZoneEntity> findAllByFloor_IdAndActiveTrueOrderByNameAsc(UUID floorId);

    List<ZoneEntity> findAllByBuildingIdAndActiveTrue(UUID buildingId);

    Optional<ZoneEntity> findByExternalIdAndActiveTrue(String externalId);

    boolean existsByExternalId(String externalId);

    @Query("SELECT z FROM ZoneEntity z LEFT JOIN FETCH z.equipment WHERE z.id = :id")
    Optional<ZoneEntity> findByIdWithEquipment(@Param("id") UUID id);
}
