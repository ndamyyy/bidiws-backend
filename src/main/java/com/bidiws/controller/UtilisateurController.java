package com.bidiws.controller;

import com.bidiws.dto.utilisateur.ChangePasswordRequestDto;
import com.bidiws.dto.utilisateur.UtilisateurResponseDto;
import com.bidiws.dto.utilisateur.UtilisateurUpdateRequestDto;
import com.bidiws.security.CustomUserDetails;
import com.bidiws.service.UtilisateurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/utilisateurs")
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @PutMapping("/{id}")
    public ResponseEntity<UtilisateurResponseDto> update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UtilisateurUpdateRequestDto dto
            ) {
        verifierProprietaireOuAdmin(userDetails, id);
        return ResponseEntity.ok(utilisateurService.update(id, dto));
    }

    @PatchMapping("/{id}/mot-de-passe")
    public ResponseEntity<Void> changerMotDePasse(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequestDto dto
            ) {
        verifierProprietaireOuAdmin(userDetails, id);
        utilisateurService.changerMotDePasse(id, dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/moi")
    public ResponseEntity<UtilisateurResponseDto> moi(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(utilisateurService.getById(userDetails.getId()));
    }

    private void verifierProprietaireOuAdmin(CustomUserDetails userDetails, Long id) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !userDetails.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous ne pouvez agir que sur votre propre compte");
        }
    }
}
