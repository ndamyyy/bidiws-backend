package com.bidiws.entity;

import com.bidiws.enums.Plateforme;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "push_token",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_push_token_utilisateur_plateforme",
                columnNames = {"utilisateur_id", "plateforme"}
        )
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PushToken extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(name = "token", nullable = false, length = 500)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "plateforme", nullable = false, length = 10)
    private Plateforme plateforme;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private Boolean actif = true;
}
