package com.bidiws.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "conteneur")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Conteneur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "residence_id", nullable = false)
    private Residence residence;

    // Identifiant physique de la puce collee sur/dans le bac. Nullable :
    // un conteneur peut exister sans RFID (ex. suivi uniquement par
    // AppareilIot.conteneur direct, cf. scenario capteur embarque).
    @Column(name = "rfid_tag", unique = true, length = 100)
    private String rfidTag;

    @Column(name = "actif")
    @Builder.Default
    private Boolean actif = true;

    // Mesure continue (POST /iot/mesures-remplissage), independante de tout
    // statut d'arret : un bac peut etre a 80% de remplissage sans qu'aucune
    // collecte ne soit en cours.
    @Column(name = "niveau_remplissage_pct")
    private Short niveauRemplissagePct;

    @Column(name = "derniere_mesure_remplissage")
    private LocalDateTime derniereMesureRemplissage;
}
