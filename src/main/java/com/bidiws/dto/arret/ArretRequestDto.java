package com.bidiws.dto.arret;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ArretRequestDto(

        @NotNull
        Long tourneeId,

        @NotNull
        Long residenceId,

        @NotNull
        @Positive
        Integer ordre,

        Integer nbConteneurs,

        String typesConteneurs
) {}