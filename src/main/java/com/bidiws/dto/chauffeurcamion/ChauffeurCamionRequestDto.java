package com.bidiws.dto.chauffeurcamion;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ChauffeurCamionRequestDto(

        @NotNull
        Long chauffeurId,

        @NotNull
        Long camionId,

        @NotNull
        LocalDate dateDebut,

        LocalDate dateFin
) {}
