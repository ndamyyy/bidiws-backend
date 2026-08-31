package com.bidiws.dto.camion;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CamionRequestDto(


        String immatriculation,

        String modele,

        String typeBenne,

        @Positive
        BigDecimal  capaciteTonnes,

        boolean gpsActif,

        boolean capteurBenne,

        @NotNull
        Long villeId
) {}
