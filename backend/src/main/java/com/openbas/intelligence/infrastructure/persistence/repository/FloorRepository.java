package com.openbas.intelligence.infrastructure.persistence.repository;

import com.openbas.intelligence.infrastructure.persistence.entity.FloorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FloorRepository extends JpaRepository<FloorEntity, UUID> {

    List<FloorEntity> findAllByBuilding_IdAndActiveTrueOrderByFloorNumberAsc(UUID buildingId);

    Optional<FloorEntity> findByExternalIdAndActiveTrue(String externalId);

    @Query("SELECT f FROM FloorEntity f LEFT JOIN FETCH f.zones WHERE f.id = :id")
    Optional<FloorEntity> findByIdWithZones(@Param("id") UUID id);

    boolean existsByExternalId(String externalId);
}
