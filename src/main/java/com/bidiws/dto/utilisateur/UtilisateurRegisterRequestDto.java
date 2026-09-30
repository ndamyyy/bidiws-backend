package com.bidiws.dto.utilisateur;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UtilisateurRegisterRequestDto(

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, message = "Le mot de passe doit au moins contenir 8 caractères")
        String motDePasse,

        @NotBlank
        String nom,

        @NotBlank
        String prenom,

        String telephone,

        // Adresse choisie via l'autocomplete API Adresse (data.gouv.fr) sur
        // le formulaire d'inscription — tous optionnels : un habitant peut
        // toujours s'inscrire sans indiquer d'adresse, auquel cas aucun
        // rattachement automatique n'est tente (voir UtilisateurService.
        // register). Quand fournis, adresse/ville/latitude/longitude sont
        // tous attendus ensemble (ce que le frontend garantit deja).
        String adresse,

        String codePostal,

        String ville,

        BigDecimal latitude,

        BigDecimal longitude
) {}
