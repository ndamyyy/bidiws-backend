package com.bidiws.dto.residence;

// Sous-ensemble volontairement restreint de ResidenceResponseDto : servi
// sans authentification (formulaire d'inscription) donc pas de champs
// operationnels internes (zoneId, latitude/longitude, rayonDetection,
// nbConteneurs) — seulement de quoi identifier/afficher la residence
// dans une recherche.
public record ResidencePublicDto(
        Long id,
        String nom,
        String adresse,
        String codePostal,
        String villeNom
) {}
