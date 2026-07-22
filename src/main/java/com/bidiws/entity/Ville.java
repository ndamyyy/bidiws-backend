package com.bidiws.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ville")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Ville {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "code_postal", nullable = false)
    private String codePostal;

    @Column(name = "departement")
    private String departement;

    @Column(name = "actif")
    @Builder.Default
    private Boolean actif =  true;

}
