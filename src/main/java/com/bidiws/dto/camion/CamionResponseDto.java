package com.bidiws.dto.camion;

import java.math.BigDecimal;

public record CamionResponseDto(
        Long id,
        String immatriculation,
        String modele,
        String typeBenne,
        BigDecimal capaciteTonnes,
        boolean gpsActif,
        boolean capteurBenne,
        Boolean actif,
        Long villeId,
        String villeNom
) {}
