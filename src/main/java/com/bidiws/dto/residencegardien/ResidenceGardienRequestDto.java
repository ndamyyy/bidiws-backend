package com.bidiws.dto.residencegardien;

import jakarta.validation.constraints.NotNull;

public record ResidenceGardienRequestDto(

        @NotNull
        Long residenceId,

        @NotNull
        Long gardienId,

        boolean principal
) {}
