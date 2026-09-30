package com.bidiws.dto.signalgps;

import com.bidiws.enums.SourceGps;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SignalGpsResponseDto(
        Long id,
        Long tourneeId,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal vitesseKmh,
        BigDecimal cap,
        Integer precisionM,
        LocalDateTime horodatage,
        SourceGps source
) {}