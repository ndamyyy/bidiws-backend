package com.bidiws.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "calendrier_collecte")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CalendrierCollecte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "residence_id", nullable = false)
    private Residence residence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_collecte_id", nullable = false)
    private TypeCollecte typeCollecte;

    @Column(name = "jour_semaine", nullable = false)
    @Min(1)
    @Max(7)
    private Short jourSemaine;

    @Column(name = "heure_estimee")
    private LocalTime heureEstimee;

    @Column(name = "actif")
    @Builder.Default
    private Boolean actif = true;
}
