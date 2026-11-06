package com.openbas.intelligence.infrastructure.persistence.repository;

import com.openbas.intelligence.infrastructure.persistence.entity.DeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<DeviceEntity, UUID> {

    Optional<DeviceEntity> findByExternalId(String externalId);

    Optional<DeviceEntity> findByExternalIdAndActiveTrue(String externalId);

    List<DeviceEntity> findAllByEquipment_IdAndActiveTrue(UUID equipmentId);

    List<DeviceEntity> findAllByCommunicationStatusAndActiveTrue(String communicationStatus);

    List<DeviceEntity> findAllByProtocolAndActiveTrue(String protocol);

    @Query("SELECT d FROM DeviceEntity d WHERE d.lastSeenAt < :threshold AND d.communicationStatus = 'ONLINE' AND d.active = true")
    List<DeviceEntity> findStaleDevices(@Param("threshold") Instant threshold);

    @Modifying
    @Query("UPDATE DeviceEntity d SET d.communicationStatus = :status, d.lastSeenAt = :seenAt WHERE d.externalId = :externalId")
    int updateCommunicationStatus(@Param("externalId") String externalId,
                                   @Param("status") String status,
                                   @Param("seenAt") Instant seenAt);

    @Query("SELECT COUNT(d) FROM DeviceEntity d WHERE d.communicationStatus = 'OFFLINE' AND d.active = true")
    long countOffline();

    boolean existsByExternalId(String externalId);
}
