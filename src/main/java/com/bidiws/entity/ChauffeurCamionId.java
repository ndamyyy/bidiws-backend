package com.bidiws.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class ChauffeurCamionId implements Serializable {

    private Long chauffeurId;
    private Long camionId;
    private LocalDate dateDebut;
}
