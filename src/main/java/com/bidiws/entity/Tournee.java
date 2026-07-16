package com.bidiws.entity;

import com.bidiws.enums.StatutTournee;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "tournee")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Tournee extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_tournee", nullable = false)
    private LocalDate dateTournee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_collecte_id",  nullable = false)
    private TypeCollecte typeCollecte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "camion_id",  nullable = false)
    private Camion camion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chauffeur_id",  nullable = false)
    private Utilisateur chauffeur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone  zone;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private StatutTournee statut = StatutTournee.PLANIFIEE;

    @Column(name = "heure_debut")
    private LocalDateTime heureDebut;

    @Column(name = "heure_fin")
    private LocalDateTime heureFin;

}
