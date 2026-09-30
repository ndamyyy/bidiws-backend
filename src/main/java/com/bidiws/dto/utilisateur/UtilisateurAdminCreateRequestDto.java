package com.bidiws.dto.utilisateur;

import com.bidiws.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UtilisateurAdminCreateRequestDto (

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        String motDePasse,

        @NotBlank
        String nom,

        @NotBlank
        String prenom,

        String telephone,

        @NotNull(message = "Le role est obligatoire")
        Role role
) {
}
