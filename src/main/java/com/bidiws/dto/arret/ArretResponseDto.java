package com.bidiws.dto.arret;

import com.bidiws.enums.ModeDetection;
import com.bidiws.enums.StatutArret;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ArretResponseDto(
        Long id,
        Long tourneeId,
        Long residenceId,
        String residenceNom,
        Integer ordre,
        StatutArret statut,
        LocalDateTime heureEstimee,
        LocalDateTime heureApproche,
        LocalDateTime heureCollecte,
        Integer scoreConfiance,
        ModeDetection modeDetection,
        Integer nbConteneurs,
        String typesConteneurs,
        BigDecimal poidsKg,
        boolean incident,
        String descriptionIncident,
        String photoIncidentUrl
) {}