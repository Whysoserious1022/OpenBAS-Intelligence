package com.openbas.intelligence.application.service;

import com.openbas.intelligence.api.dto.DeviceDto;
import com.openbas.intelligence.api.dto.EquipmentDto;
import com.openbas.intelligence.api.exception.ResourceNotFoundException;
import com.openbas.intelligence.infrastructure.persistence.entity.DeviceEntity;
import com.openbas.intelligence.infrastructure.persistence.entity.EquipmentEntity;
import com.openbas.intelligence.infrastructure.persistence.repository.DeviceRepository;
import com.openbas.intelligence.infrastructure.persistence.repository.EquipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final DeviceRepository deviceRepository;

    public EquipmentService(EquipmentRepository equipmentRepository, DeviceRepository deviceRepository) {
        this.equipmentRepository = equipmentRepository;
        this.deviceRepository = deviceRepository;
    }

    public List<EquipmentDto> getEquipment(UUID buildingId, UUID zoneId, String equipmentType, String operationalStatus) {
        List<EquipmentEntity> entities;

        if (buildingId != null) {
            entities = equipmentRepository.findAllByBuildingIdAndActiveTrue(buildingId);
        } else if (zoneId != null) {
            entities = equipmentRepository.findAllByZone_IdAndActiveTrue(zoneId);
        } else if (equipmentType != null) {
            entities = equipmentRepository.findAllByEquipmentTypeAndActiveTrue(equipmentType);
        } else if (operationalStatus != null) {
            entities = equipmentRepository.findAllByOperationalStatusAndActiveTrue(operationalStatus);
        } else {
            entities = equipmentRepository.findAll().stream().filter(EquipmentEntity::isActive).toList();
        }

        return entities.stream().map(this::toDto).toList();
    }

    public EquipmentDto getEquipmentById(UUID id) {
        return equipmentRepository.findById(id)
            .filter(EquipmentEntity::isActive)
            .map(this::toDto)
            .orElseThrow(() -> new ResourceNotFoundException("Equipment", id.toString()));
    }

    public EquipmentDto getEquipmentByExternalId(String externalId) {
        return equipmentRepository.findByExternalIdAndActiveTrue(externalId)
            .map(this::toDto)
            .orElseThrow(() -> new ResourceNotFoundException("Equipment", externalId));
    }

    public List<EquipmentDto> getChildEquipment(UUID parentId) {
        return equipmentRepository.findChildEquipment(parentId).stream().map(this::toDto).toList();
    }

    public List<DeviceDto> getDevicesForEquipment(UUID equipmentId) {
        return deviceRepository.findAllByEquipment_IdAndActiveTrue(equipmentId)
            .stream().map(this::toDeviceDto).toList();
    }

    public List<EquipmentDto> getFaultedEquipment(UUID buildingId) {
        List<EquipmentEntity> entities;
        if (buildingId != null) {
            entities = equipmentRepository.findFaultedByBuilding(buildingId);
        } else {
            entities = new ArrayList<>();
            entities.addAll(equipmentRepository.findAllByOperationalStatusAndActiveTrue("FAULT"));
            entities.addAll(equipmentRepository.findAllByOperationalStatusAndActiveTrue("DEGRADED"));
        }
        return entities.stream().map(this::toDto).toList();
    }

    public long countActive() { return equipmentRepository.countActive(); }
    public long countFaulted() { return equipmentRepository.countFaulted(); }

    private EquipmentDto toDto(EquipmentEntity e) {
        return new EquipmentDto(
            e.getId(), e.getExternalId(),
            e.getZone() != null ? e.getZone().getId() : null,
            e.getBuildingId(), e.getFloorId(), e.getName(), e.getEquipmentType(),
            e.getManufacturer(), e.getModel(), e.getSerialNumber(),
            e.getOperationalStatus(), e.getHealthScore(),
            e.getMinOperatingTempCelsius(), e.getMaxOperatingTempCelsius(),
            e.getRatedPowerKw(), e.getRatedAirflowCfm(),
            e.isSimulationEnabled(), e.isActive(),
            e.getParentEquipment() != null ? e.getParentEquipment().getId() : null,
            e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    private DeviceDto toDeviceDto(DeviceEntity d) {
        return new DeviceDto(
            d.getId(), d.getExternalId(),
            d.getEquipment() != null ? d.getEquipment().getId() : null,
            d.getName(), d.getProtocol(),
            d.getBacnetDeviceId(), d.getBacnetIpAddress(), d.getBacnetPort(),
            d.getMqttTopicPrefix(), d.getCommunicationStatus(),
            d.getLastSeenAt(), d.getLastErrorAt(), d.getLastErrorMessage(),
            d.isSimulated(), d.isActive(), d.getCreatedAt()
        );
    }
}
