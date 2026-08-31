package com.bidiws.repository;

import com.bidiws.entity.Utilisateur;
import com.bidiws.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    // Authentification
    Optional<Utilisateur> findByEmail(String email);
    boolean existsByEmail(String email);

    // Recherche EXACTE
    Optional<Utilisateur> findByTelephone(String telephone);

    // Recherche PARTIELLE (pour auto-complétion)
    List<Utilisateur> findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(String nom, String prenom);

    // Recherche EXACTE par rôle
    List<Utilisateur> findByRole(Role role);
    List<Utilisateur> findByRoleAndActifTrue(Role role);


    // Recherche combinee nom + prénom (EXACT)
    List<Utilisateur> findByNomAndPrenom(String nom, String prenom);

    // Liste des actifs
    List<Utilisateur> findByActifTrue();

    // Comptes MAIRIE rattaches a une ville (cf. Utilisateur.ville, migration V2)
    boolean existsByVilleId(Long villeId);
}