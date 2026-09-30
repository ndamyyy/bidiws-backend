package com.bidiws.dto.chauffeurcamion;

import java.time.LocalDate;

public record ChauffeurCamionResponseDto(
        Long chauffeurId,
        String chauffeurNom,
        String chauffeurPrenom,
        Long camionId,
        String camionImmatriculation,
        LocalDate dateDebut,
        LocalDate dateFin
) {}
