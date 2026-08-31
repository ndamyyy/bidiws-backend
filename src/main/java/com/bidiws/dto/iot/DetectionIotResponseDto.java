package com.bidiws.dto.iot;

import com.bidiws.enums.StatutArret;

import java.time.LocalDateTime;

public record DetectionIotResponseDto(
        Long arretId,
        StatutArret statut,
        LocalDateTime horodatage
) {}
