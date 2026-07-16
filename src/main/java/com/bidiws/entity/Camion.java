package com.bidiws.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "camion")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Camion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "immatriculation", unique = true, nullable = false)
    private String immatriculation;

    @Column(name = "modele")
    private String modele;

    @Column(name = "type_benne")
    private String typeBenne;

    @Column(name = "capacite_tonnes", precision = 5, scale = 2)
    private BigDecimal capaciteTonnes;

    @Column(name = "gps_actif")
    @Builder.Default
    private Boolean gpsActif = false;

    @Column(name = "capteur_benne")
    @Builder.Default
    private Boolean capteurBenne = false;

    @Column(name = "actif")
    @Builder.Default
    private Boolean actif = true;
}
