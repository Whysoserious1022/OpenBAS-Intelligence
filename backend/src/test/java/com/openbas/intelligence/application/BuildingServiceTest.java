package com.openbas.intelligence.application;

import com.openbas.intelligence.api.dto.BuildingDto;
import com.openbas.intelligence.api.dto.CreateBuildingRequest;
import com.openbas.intelligence.api.exception.ResourceAlreadyExistsException;
import com.openbas.intelligence.api.exception.ResourceNotFoundException;
import com.openbas.intelligence.application.service.BuildingService;
import com.openbas.intelligence.infrastructure.persistence.entity.BuildingEntity;
import com.openbas.intelligence.infrastructure.persistence.repository.BuildingRepository;
import com.openbas.intelligence.infrastructure.persistence.repository.FloorRepository;
import com.openbas.intelligence.infrastructure.persistence.repository.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BuildingService Unit Tests")
class BuildingServiceTest {

    @Mock private BuildingRepository buildingRepository;
    @Mock private FloorRepository floorRepository;
    @Mock private ZoneRepository zoneRepository;

    private BuildingService buildingService;

    @BeforeEach
    void setUp() {
        buildingService = new BuildingService(buildingRepository, floorRepository, zoneRepository);
    }

    @Test
    @DisplayName("getAllBuildings returns mapped DTOs")
    void getAllBuildings_returnsMappedDtos() {
        BuildingEntity entity = createBuildingEntity("BUILDING-01", "Nexus Tower");
        when(buildingRepository.findAllByActiveTrue()).thenReturn(List.of(entity));

        List<BuildingDto> result = buildingService.getAllBuildings();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).externalId()).isEqualTo("BUILDING-01");
        assertThat(result.get(0).name()).isEqualTo("Nexus Tower");
        assertThat(result.get(0).active()).isTrue();
    }

    @Test
    @DisplayName("getBuildingById throws when not found")
    void getBuildingById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(buildingRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> buildingService.getBuildingById(id))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Building");
    }

    @Test
    @DisplayName("getBuildingById throws when deactivated")
    void getBuildingById_deactivated_throws() {
        UUID id = UUID.randomUUID();
        BuildingEntity entity = createBuildingEntity("BUILDING-01", "Nexus Tower");
        entity.setActive(false);
        when(buildingRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> buildingService.getBuildingById(id))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createBuilding persists and returns new building")
    void createBuilding_savesAndReturns() {
        CreateBuildingRequest request = new CreateBuildingRequest(
            "BUILDING-02", "South Tower", "Test building",
            "1 Test St", "Test City", "US", "UTC", 5000.0, 3, true);

        when(buildingRepository.existsByExternalId("BUILDING-02")).thenReturn(false);

        BuildingEntity saved = createBuildingEntity("BUILDING-02", "South Tower");
        when(buildingRepository.save(any())).thenReturn(saved);

        BuildingDto result = buildingService.createBuilding(request);

        assertThat(result.externalId()).isEqualTo("BUILDING-02");
        verify(buildingRepository).save(any(BuildingEntity.class));
    }

    @Test
    @DisplayName("createBuilding throws when externalId already exists")
    void createBuilding_duplicateExternalId_throws() {
        CreateBuildingRequest request = new CreateBuildingRequest(
            "BUILDING-01", "Duplicate", null, null, null, null, null, null, null, false);

        when(buildingRepository.existsByExternalId("BUILDING-01")).thenReturn(true);

        assertThatThrownBy(() -> buildingService.createBuilding(request))
            .isInstanceOf(ResourceAlreadyExistsException.class)
            .hasMessageContaining("BUILDING-01");
    }

    @Test
    @DisplayName("deactivateBuilding marks building inactive")
    void deactivateBuilding_marksInactive() {
        UUID id = UUID.randomUUID();
        BuildingEntity entity = createBuildingEntity("BUILDING-01", "Nexus Tower");
        entity.setId(id);
        when(buildingRepository.findById(id)).thenReturn(Optional.of(entity));
        when(buildingRepository.save(any())).thenReturn(entity);

        buildingService.deactivateBuilding(id);

        verify(buildingRepository).save(argThat(b -> !b.isActive()));
    }

    private BuildingEntity createBuildingEntity(String externalId, String name) {
        BuildingEntity entity = new BuildingEntity();
        entity.setId(UUID.randomUUID());
        entity.setExternalId(externalId);
        entity.setName(name);
        entity.setActive(true);
        entity.setSimulationEnabled(true);
        entity.setTimezone("UTC");
        return entity;
    }
}
