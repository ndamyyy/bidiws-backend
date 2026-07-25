package com.bidiws.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "residence")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Residence extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false, length = 200)
    private String nom;

    @Column(name = "adresse", nullable = false)
    private String adresse;

    @Column(name = "complement")
    private String complement;

    @Column(name = "code_postal", nullable = false, length = 10)
    private String codePostal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ville_id", nullable = false)
    private Ville ville;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Positive
    @Column(name = "rayon_detection")
    @Builder.Default
    private Integer rayonDetection = 50;

    @Positive
    @Column(name = "nb_conteneurs")
    @Builder.Default
    private Integer nbConteneurs = 1;

    @Column(name = "actif")
    @Builder.Default
    private Boolean actif = true;

    @OneToMany(mappedBy = "residence", fetch = FetchType.LAZY)
    private List<ResidenceGardien> gardiens =  new ArrayList<>();

    public void ajouterGardien(ResidenceGardien rg) {
        gardiens.add(rg);
        rg.setResidence(this);
    }

    public void retirerGardien(ResidenceGardien rg) {
        gardiens.remove(rg);
        rg.setResidence(null);
    }
}
