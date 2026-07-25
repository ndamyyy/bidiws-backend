package com.bidiws.dto.tournee;

import com.bidiws.enums.StatutTournee;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TourneeResponseDto(
        Long id,
        LocalDate dateTournee,
        Long typeCollecteId,
        String typeCollecteLibelle,
        Long camionId,
        String camionImmatriculation,
        Long chauffeurId,
        String chauffeurNom,
        String chauffeurPrenom,
        Long zoneId,
        String zoneNom,
        StatutTournee statut,
        LocalDateTime heureDebut,
        LocalDateTime heureFin
) {}