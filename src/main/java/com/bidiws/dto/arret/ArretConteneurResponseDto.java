package com.bidiws.dto.arret;

import com.bidiws.enums.ModeDetection;
import com.bidiws.enums.StatutArret;

import java.time.LocalDateTime;

public record ArretConteneurResponseDto(
        Long id,
        Long conteneurId,
        String conteneurCode,
        StatutArret statut,
        ModeDetection modeDetection,
        Integer scoreConfiance,
        LocalDateTime horodatageConfirmation,
        Integer niveauRemplissagePct
) {}
