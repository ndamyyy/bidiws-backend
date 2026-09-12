package com.bidiws.entity;

import com.bidiws.enums.TypeAppareilIot;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "appareil_iot")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AppareilIot extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identifiant materiel (ex: adresse MAC de l'ESP32), utilise pour
    // l'affichage/la gestion — jamais pour l'authentification, qui passe
    // uniquement par cleApiHash (cf. DeviceApiKeyAuthenticationFilter).
    @Column(name = "identifiant_materiel", nullable = false, unique = true, length = 100)
    private String identifiantMateriel;

    // Hash SHA-256 (deterministe, pas bcrypt) de la cle API en clair.
    // Permet un lookup direct par hash : la requete /iot/detections ne
    // contient que la cle, pas d'identifiant device separe.
    @Column(name = "cle_api_hash", nullable = false, unique = true, length = 64)
    private String cleApiHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_appareil", nullable = false, length = 30)
    private TypeAppareilIot typeAppareil;

    // Rempli si l'appareil est colle a UN bac precis (capteur embarque).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conteneur_id")
    private Conteneur conteneur;

    // Rempli si l'appareil est un lecteur monte sur le camion, qui scanne
    // plusieurs bacs au passage. Mutuellement exclusif avec conteneur
    // (contrainte chk_appareil_iot_un_seul_rattachement en base).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "camion_id")
    private Camion camion;

    @Column(name = "actif")
    @Builder.Default
    private Boolean actif = true;
}
