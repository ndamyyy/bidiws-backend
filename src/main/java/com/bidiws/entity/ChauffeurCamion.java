package com.bidiws.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "chauffeur_camion")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChauffeurCamion {

    @EmbeddedId
    private ChauffeurCamionId chauffeurCamionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("camionId")
    @JoinColumn(name = "camion_id", nullable = false)
    private Camion camion;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("chauffeurId")
    @JoinColumn(name = "chauffeur_id", nullable = false)
    private Utilisateur chauffeur;

    @Column(name = "date_fin")
    private LocalDate dateFin;
}
