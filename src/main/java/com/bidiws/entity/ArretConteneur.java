package com.bidiws.entity;

import com.bidiws.enums.ModeDetection;
import com.bidiws.enums.StatutArret;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "arret_conteneur",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_arret_conteneur",
                columnNames = {"arret_id", "conteneur_id"}
        )
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ArretConteneur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arret_id", nullable = false)
    private Arret arret;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conteneur_id", nullable = false)
    private Conteneur conteneur;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private StatutArret statut = StatutArret.EN_ATTENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_detection")
    private ModeDetection modeDetection;

    @Column(name = "score_confiance")
    private Short scoreConfiance;

    @Column(name = "horodatage_confirmation")
    private LocalDateTime horodatageConfirmation;
}
