package com.bidiws.dto.calendriercollecte;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record CalendrierCollecteRequestDto(

        @NotNull
        Long residenceId,

        @NotNull
        Long typeCollecteId,

        @NotNull
        @Min(1)
        @Max(7)
        Integer jourSemaine,

        LocalTime heureEstimee

) {}
