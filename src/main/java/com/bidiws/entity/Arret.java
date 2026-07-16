package com.bidiws.entity;

import com.bidiws.enums.ModeDetection;
import com.bidiws.enums.StatutArret;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "arret",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_arret_tournee_residence",
                columnNames = {"tournee_id", "residence_id"}
        )
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Arret extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournee_id", nullable = false)
    private Tournee tournee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "residence_id", nullable = false)
    private Residence residence;

    @Positive
    @Column(name = "ordre", nullable = false)
    private Short ordre;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private StatutArret statut = StatutArret.EN_ATTENTE;

    @Column(name = "heure_estimee")
    private LocalDateTime heureEstimee;

    @Column(name = "heure_approche")
    private LocalDateTime heureApproche;

    @Column(name = "heure_collecte")
    private LocalDateTime heureCollecte;

    @Min(0)
    @Max(100)
    @Column(name = "score_confiance", nullable = false)
    @Builder.Default
    private Short scoreConfiance = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_detection")
    private ModeDetection modeDetection;

    @Column(name = "nb_conteneurs")
    @Builder.Default
    private Short nbConteneurs = 1;

    @Column(name = "types_conteneurs", length = 200)
    private String typesConteneurs;

    @Column(name = "poids_kg", precision = 8, scale = 2)
    private BigDecimal poidsKg;

    @Column(name = "incident", nullable = false)
    @Builder.Default
    private Boolean incident = false;

    @Column(name = "description_incident", columnDefinition = "TEXT")
    private String descriptionIncident;

    @Column(name = "photo_incident_url", length = 500)
    private String photoIncidentUrl;
}
