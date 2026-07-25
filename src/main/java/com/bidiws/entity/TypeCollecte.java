package com.bidiws.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "type_collecte")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypeCollecte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", length = 30, unique = true, nullable = false)
    private String code;

    @Column(name = "libelle", length = 100, nullable = false)
    private String libelle;

    @Column(name = "couleur", length = 7)
    private String couleur;

    @Column(name = "icone", length = 50)
    private String icone;
}
