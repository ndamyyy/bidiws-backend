package com.bidiws.dto.calendriercollecte;

// Résidence suggérée pour l'ajout d'un arrêt sur une tournée, déduite du
// calendrier de collecte récurrent (GET /calendriers-collecte/residences-a-desservir).
// nbConteneurs reprend la valeur enregistrée sur la résidence — juste une
// valeur par défaut pratique pour préremplir le formulaire d'arrêt côté
// frontend, pas une donnée propre au calendrier.
public record ResidenceADesservirDto(
        Long id,
        String nom,
        String adresse,
        String codePostal,
        Integer nbConteneurs
) {}
