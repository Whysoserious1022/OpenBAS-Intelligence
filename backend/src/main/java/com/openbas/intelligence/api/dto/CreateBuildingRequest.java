package com.openbas.intelligence.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBuildingRequest(
    @NotBlank(message = "externalId is required")
    @Size(max = 100, message = "externalId must not exceed 100 characters")
    String externalId,

    @NotBlank(message = "name is required")
    @Size(max = 200, message = "name must not exceed 200 characters")
    String name,

    @Size(max = 1000, message = "description must not exceed 1000 characters")
    String description,

    String address,
    String city,
    String country,
    String timezone,
    Double totalAreaSqm,
    Integer floorCount,
    boolean simulationEnabled
) {}
