package com.openbas.intelligence.infrastructure.persistence.repository;

import com.openbas.intelligence.infrastructure.persistence.entity.CommandEntity;
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
public interface CommandRepository extends JpaRepository<CommandEntity, UUID> {

    Optional<CommandEntity> findByIdempotencyKey(String idempotencyKey);

    List<CommandEntity> findAllByStatusOrderByCreatedAtDesc(String status);

    Page<CommandEntity> findAllByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    @Query("SELECT c FROM CommandEntity c WHERE c.targetEquipmentId = :equipmentId " +
           "AND c.status IN ('PROPOSED','VALIDATED','PENDING_APPROVAL','APPROVED','EXECUTING') " +
           "ORDER BY c.createdAt DESC")
    List<CommandEntity> findActiveByEquipment(@Param("equipmentId") String equipmentId);

    @Query("SELECT c FROM CommandEntity c WHERE c.status = 'PENDING_APPROVAL' ORDER BY c.createdAt ASC")
    List<CommandEntity> findPendingApprovals();

    @Query("SELECT c FROM CommandEntity c WHERE c.status IN ('PENDING_APPROVAL','APPROVED') " +
           "AND c.expiresAt < :now")
    List<CommandEntity> findExpiredCommands(@Param("now") Instant now);

    @Query("SELECT COUNT(c) FROM CommandEntity c WHERE c.status = 'PENDING_APPROVAL'")
    long countPendingApprovals();

    Page<CommandEntity> findAllByBuildingIdOrderByCreatedAtDesc(String buildingId, Pageable pageable);
}
