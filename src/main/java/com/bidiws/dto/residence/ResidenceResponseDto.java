package com.bidiws.dto.residence;

import java.math.BigDecimal;

public record ResidenceResponseDto(
        Long id,
        String nom,
        String adresse,
        String complement,
        String codePostal,
        Long villeId,
        String villeNom,
        Long zoneId,
        String zoneNom,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer rayonDetection,
        Integer nbConteneurs,
        Boolean actif
) {}
