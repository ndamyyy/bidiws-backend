package com.bidiws.entity;

import com.bidiws.enums.StatutSignalement;
import com.bidiws.enums.TypeSignalement;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "signalement")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Signalement extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auteur_id", nullable = false)
    private Utilisateur auteur;

    // Nullable : ON DELETE SET NULL en base
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "residence_id")
    private Residence residence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arret_id")
    private Arret arret;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TypeSignalement type;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private StatutSignalement statut = StatutSignalement.OUVERT;

    // Pas "updated_at" : c'est une date métier (résolution du signalement),
    // pas la dernière modification technique de la ligne. Ne fait donc
    // pas partie de Auditable, c'est un champ propre à cette entité.
    @Column(name = "resolu_at")
    private LocalDateTime resoluAt;
}
