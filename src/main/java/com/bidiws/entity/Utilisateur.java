package com.bidiws.entity;

import com.bidiws.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name= "utilisateur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur extends Auditable{

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "nom")
        private String nom;

        @Column(name = "prenom")
        private String prenom;

        @Column(name = "email")
        private String email;

        @Column(name = "mot_de_passe")
        private String motDePasse;

        @Column(name = "telephone")
        private String telephone;

        @Enumerated(EnumType.STRING)
        @Column(name = "role", nullable = false)
        private Role role;

        @Column(name = "actif")
        @Builder.Default
        private Boolean actif = true;

        // Rattachement utilise uniquement pour le role MAIRIE : delimite la
        // ville dont cette mairie peut voir/gerer les residences.
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "ville_id")
        private Ville ville;

        // Verrouillage de compte apres echecs de connexion successifs
        // (cf. AuthService). Defaut coherent aussi cote Java, pas seulement
        // la contrainte SQL DEFAULT 0.
        @Column(name = "tentatives_echouees", nullable = false)
        @Builder.Default
        private Short tentativesEchouees = 0;

        @Column(name = "verrouille_jusqua")
        private LocalDateTime verrouilleJusqua;

}
