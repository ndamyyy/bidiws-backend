package com.bidiws.dto.signalement;

import com.bidiws.enums.TypeSignalement;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SignalementRequestDto(

        Long residenceId,

        Long arretId,

        @NotNull
        TypeSignalement type,

        String description,

        String photoUrl,

        BigDecimal latitude,

        BigDecimal longitude
) {}