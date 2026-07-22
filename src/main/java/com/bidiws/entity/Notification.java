package com.bidiws.entity;

import com.bidiws.enums.CanalNotification;
import com.bidiws.enums.TypeNotification;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Notification extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinataire_id", nullable = false)
    private Utilisateur destinataire;

    // Nullable : ON DELETE SET NULL en base, ne pas mettre nullable=false ici
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arret_id")
    private Arret arret;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "residence_id")
    private Residence residence;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TypeNotification type;

    @Column(name = "titre", nullable = false, length = 200)
    private String titre;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false)
    @Builder.Default
    private CanalNotification canal = CanalNotification.PUSH;

    @Column(name = "lu", nullable = false)
    @Builder.Default
    private Boolean lu = false;

    @Column(name = "envoye", nullable = false)
    @Builder.Default
    private Boolean envoye = false;

    @Column(name = "heure_envoi")
    private LocalDateTime heureEnvoi;

    @Column(name = "heure_lecture")
    private LocalDateTime heureLecture;
}
