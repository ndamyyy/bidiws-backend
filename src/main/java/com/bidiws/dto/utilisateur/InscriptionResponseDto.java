package com.bidiws.dto.utilisateur;

// Reponse de POST /auth/register — enrichit UtilisateurResponseDto (inchange,
// reutilise ailleurs) du resultat du rattachement automatique a une residence
// (voir UtilisateurService.register + RattachementResidenceService) :
// - residenceId/residenceNom renseignes  : rattache (residence existante a
//   proximite immediate, ou nouvellement creee).
// - zoneNonCouverte=true                 : aucune Ville embarquee ne
//   correspond a l'adresse choisie, le compte est cree sans residence.
// - ni l'un ni l'autre                   : aucune adresse fournie a
//   l'inscription (champ optionnel), le rattachement n'a pas ete tente.
public record InscriptionResponseDto(
        UtilisateurResponseDto utilisateur,
        Long residenceId,
        String residenceNom,
        boolean zoneNonCouverte
) {}
