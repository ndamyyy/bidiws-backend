package com.bidiws.controller;

import com.bidiws.dto.utilisateur.UtilisateurAdminCreateRequestDto;
import com.bidiws.dto.utilisateur.UtilisateurResponseDto;
import com.bidiws.dto.utilisateur.ResetPasswordRequestDto;
import com.bidiws.enums.Role;
import com.bidiws.service.UtilisateurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/utilisateurs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UtilisateurService utilisateurService;

    @PostMapping
    public ResponseEntity<UtilisateurResponseDto> creerUtilisateur(
            @Valid @RequestBody UtilisateurAdminCreateRequestDto dto) {
        UtilisateurResponseDto response = utilisateurService.createByAdmin(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<UtilisateurResponseDto>> rechercher(
            @RequestParam(required = false) String nom,
            @RequestParam(required = false) String prenom,
            @RequestParam(required = false) Role role
    ) {
        return ResponseEntity.ok(utilisateurService.rechercher(nom, prenom, role));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UtilisateurResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.getById(id));
    }

    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<Void> desactiver(@PathVariable Long id) {
        utilisateurService.setActif(id, false);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activer")
    public ResponseEntity<Void> activer(@PathVariable Long id) {
        utilisateurService.setActif(id, true);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/mot-de-passe")
    public ResponseEntity<Void> resetMotDePasse(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequestDto dto
    ) {
        utilisateurService.resetMotDePasseByAdmin(id, dto);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UtilisateurResponseDto> changerRole(
            @PathVariable Long id,
            @RequestParam Role role
    ) {
        return ResponseEntity.ok(utilisateurService.changerRole(id, role));
    }
}
