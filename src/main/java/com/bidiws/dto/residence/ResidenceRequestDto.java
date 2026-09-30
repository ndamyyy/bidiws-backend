package com.bidiws.dto.residence;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ResidenceRequestDto(

        @NotBlank
        String nom,

        @NotBlank
        String adresse,

        String complement,

        @NotBlank
        String codePostal,

        @NotNull
        Long villeId,

        Long zoneId,

        BigDecimal latitude,

        BigDecimal longitude,

        @Positive
        Integer rayonDetection,

        @Positive
        Integer nbConteneurs
) {}
