package com.bidiws.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    // Identifie la chaine de rotation issue d'un meme login — voir
    // RefreshTokenService pour la logique de detection de reutilisation.
    @Column(name = "famille_id", nullable = false)
    private UUID familleId;

    @Column(name = "expire_a", nullable = false)
    private LocalDateTime expireA;

    // Non-null des le premier usage (rotation) : un token est a usage
    // unique. Un second usage du meme token est une reutilisation.
    @Column(name = "utilise_a")
    private LocalDateTime utiliseA;

    @Column(name = "revoque", nullable = false)
    @Builder.Default
    private Boolean revoque = false;
}
