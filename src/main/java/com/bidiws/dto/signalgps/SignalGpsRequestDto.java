package com.bidiws.dto.signalgps;

import com.bidiws.enums.SourceGps;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SignalGpsRequestDto(

        @NotNull
        Long tourneeId,

        @NotNull
        BigDecimal latitude,

        @NotNull
        BigDecimal longitude,

        BigDecimal vitesseKmh,

        BigDecimal cap,

        Integer precisionM,

        @NotNull
        LocalDateTime horodatage,

        SourceGps source
) {}