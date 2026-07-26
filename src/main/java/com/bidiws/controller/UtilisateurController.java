package com.bidiws.controller;

import com.bidiws.dto.utilisateur.ChangePasswordRequestDto;
import com.bidiws.dto.utilisateur.UtilisateurResponseDto;
import com.bidiws.dto.utilisateur.UtilisateurUpdateRequestDto;
import com.bidiws.service.UtilisateurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/utilisateurs")
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @PutMapping("/{id}")
    public ResponseEntity<UtilisateurResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UtilisateurUpdateRequestDto dto
            ) {
        return ResponseEntity.ok(utilisateurService.update(id, dto));
    }

    @PatchMapping("/{id}/mot-de-passe")
    public ResponseEntity<Void> changerMotDePasse(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequestDto dto
            ) {
        utilisateurService.changerMotDePasse(id, dto);
        return ResponseEntity.noContent().build();
    }
}
