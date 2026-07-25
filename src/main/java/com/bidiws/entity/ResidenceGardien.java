package com.bidiws.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "residence_gardien")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResidenceGardien {

    @EmbeddedId
    private ResidenceGardienId residenceGardienId;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("residenceId")
    @JoinColumn(name = "residence_id", nullable = false)
    private Residence residence;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("gardienId")
    @JoinColumn(name = "gardien_id", nullable = false)
    private Utilisateur gardien;

    @Column(name= "principal")
    @Builder.Default
    private Boolean principal = true;


}
