package com.bidiws.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "residence_syndic")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResidenceSyndic {

    @EmbeddedId
    private ResidenceSyndicId residenceSyndicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("residenceId")
    @JoinColumn(name = "residence_id", nullable = false)
    private Residence residence;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("syndicId")
    @JoinColumn(name = "syndic_id", nullable = false)
    private Utilisateur syndic;
}
