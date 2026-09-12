package com.bidiws.dto.signalement;

import com.bidiws.enums.StatutSignalement;
import com.bidiws.enums.TypeSignalement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SignalementResponseDto(
        Long id,
        Long auteurId,
        String auteurNom,
        String auteurPrenom,
        Long residenceId,
        String residenceNom,
        Long arretId,
        TypeSignalement type,
        String description,
        String photoUrl,
        BigDecimal latitude,
        BigDecimal longitude,
        StatutSignalement statut,
        LocalDateTime createdAt,
        LocalDateTime resoluAt
) {}