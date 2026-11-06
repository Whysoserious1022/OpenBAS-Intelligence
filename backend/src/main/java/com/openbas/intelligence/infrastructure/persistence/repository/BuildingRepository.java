package com.openbas.intelligence.infrastructure.persistence.repository;

import com.openbas.intelligence.infrastructure.persistence.entity.BuildingEntity;
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
public interface BuildingRepository extends JpaRepository<BuildingEntity, UUID> {

    Optional<BuildingEntity> findByExternalId(String externalId);

    Optional<BuildingEntity> findByExternalIdAndActiveTrue(String externalId);

    List<BuildingEntity> findAllByActiveTrue();

    Page<BuildingEntity> findAllByActiveTrue(Pageable pageable);

    boolean existsByExternalId(String externalId);

    @Query("SELECT COUNT(b) FROM BuildingEntity b WHERE b.active = true")
    long countActive();

    @Query("SELECT b FROM BuildingEntity b LEFT JOIN FETCH b.floors WHERE b.id = :id")
    Optional<BuildingEntity> findByIdWithFloors(@Param("id") UUID id);
}
