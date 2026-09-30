package com.bidiws.dto.conteneur;

public record ConteneurResponseDto(
        Long id,
        String code,
        Long residenceId,
        String residenceNom,
        String rfidTag,
        Boolean actif
) {}
