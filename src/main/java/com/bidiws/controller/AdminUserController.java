package com.bidiws.controller;

import com.bidiws.dto.utilisateur.UtilisateurAdminCreateRequestDto;
import com.bidiws.dto.utilisateur.UtilisateurResponseDto;
import com.bidiws.dto.utilisateur.ResetPasswordRequestDto;
import com.bidiws.entity.Utilisateur;
import com.bidiws.enums.Role;
import com.bidiws.repository.UtilisateurRepository;
import com.bidiws.service.UtilisateurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/admin/utilisateurs")
@RequiredArgsConstructor
public class AdminUserController {

    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurService utilisateurService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
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
        List<Utilisateur> resultats;

        if (nom != null || prenom != null) {
            resultats = utilisateurRepository.findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(
                    nom != null ? nom : "", prenom != null ? prenom : "");
        } else if (role != null) {
            resultats = utilisateurRepository.findByRole(role);
        } else {
            resultats = utilisateurRepository.findAll();
        }

        return ResponseEntity.ok(resultats.stream().map(this::toResponseDto).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UtilisateurResponseDto> getById(@PathVariable Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        return ResponseEntity.ok(toResponseDto(utilisateur));
    }

    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<Void> desactiver(@PathVariable Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        utilisateur.setActif(false);
        utilisateurRepository.save(utilisateur);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activer")
    public ResponseEntity<Void> activer(@PathVariable Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        utilisateur.setActif(true);
        utilisateurRepository.save(utilisateur);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/mot-de-passe")
    @PreAuthorize("hasRole('ADMIN')")
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
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        utilisateur.setRole(role);
        return ResponseEntity.ok(toResponseDto(utilisateurRepository.save(utilisateur)));
    }

    private UtilisateurResponseDto toResponseDto(Utilisateur u) {
        return new UtilisateurResponseDto(
                u.getId(), u.getEmail(), u.getNom(), u.getPrenom(),
                u.getTelephone(), u.getRole(), u.getActif()
        );
    }
}
