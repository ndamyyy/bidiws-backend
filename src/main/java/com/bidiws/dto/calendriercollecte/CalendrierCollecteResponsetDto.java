package com.bidiws.dto.calendriercollecte;

import java.time.LocalTime;

public record CalendrierCollecteResponsetDto(
        Long id,
        Long residenceId,
        String residenceNom,
        Long typeCollecteId,
        String typeCollecteLibelle,
        Integer jourSemaine,
        LocalTime heureEstimee,
        Boolean actif
) {}