package com.bidiws.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ResidenceHabitantId implements Serializable {

    private Long residenceId;
    private Long habitantId;
}
