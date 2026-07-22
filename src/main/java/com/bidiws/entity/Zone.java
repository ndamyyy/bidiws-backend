package com.bidiws.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "zone",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_zone_ville_code",
                columnNames = {"ville_id", "code"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ville_id", nullable = false)
    private Ville ville;

    @Column(name = "nom",nullable = false, length = 100)
    private String nom;

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

}
