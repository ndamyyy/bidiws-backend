package com.bidiws.dto.zone;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ZoneRequestDto(

        @NotNull
        Long villeId,

        @NotBlank
        String nom,

        @NotBlank
        String code,

        String description
) {}
