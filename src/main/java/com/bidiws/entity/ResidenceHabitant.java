package com.bidiws.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "residence_habitant")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResidenceHabitant {

    @EmbeddedId
    private ResidenceHabitantId residenceHabitantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("residenceId")
    @JoinColumn(name = "residence_id", nullable = false)
    private Residence residence;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("habitantId")
    @JoinColumn(name = "habitant_id", nullable = false)
    private Utilisateur habitant;
}
