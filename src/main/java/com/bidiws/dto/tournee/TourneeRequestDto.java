package com.bidiws.dto.tournee;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TourneeRequestDto(

        @NotNull
        LocalDate dateTournee,

        @NotNull
        Long typeCollecteId,

        @NotNull
        Long camionId,

        @NotNull
        Long chauffeurId,

        Long zoneId
) {}