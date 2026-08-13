package com.bidiws.dto.iot;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record MesureRemplissageRequestDto(

        @NotNull
        @Min(0)
        @Max(100)
        Integer niveauPct,

        @NotNull
        LocalDateTime horodatage
) {}
