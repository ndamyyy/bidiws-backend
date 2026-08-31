package com.bidiws.dto.utilisateur;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestDto(

        @NotBlank
        String ancienMotDePasse,

        @NotBlank
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        String nouveauMotDePasse
) {}