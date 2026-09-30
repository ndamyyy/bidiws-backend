package com.bidiws.dto.conteneur;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConteneurRequestDto(

        @NotBlank
        String code,

        @NotNull
        Long residenceId,

        String rfidTag
) {}
