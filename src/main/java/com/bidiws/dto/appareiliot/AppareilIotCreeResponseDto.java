package com.bidiws.dto.appareiliot;

/**
 * Retournee UNIQUEMENT a la creation : cle contient la cle API en clair,
 * generee a la volee et jamais restockee ni renvoyee ensuite (seul le hash
 * SHA-256 est conserve en base). A copier immediatement sur l'appareil —
 * il n'y aura pas d'autre occasion de la recuperer.
 */
public record AppareilIotCreeResponseDto(
        Long id,
        String identifiantMateriel,
        String cleApi
) {}
