package com.bidiws.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

/**
 * À étendre par les entités qui ont created_at ET updated_at
 * (utilisateur, arrêt). Hérite de created_at via CreatedAtEntity —
 * pas de duplication entre les deux classes.
 */
@Getter
@MappedSuperclass
public abstract class Auditable extends CreatedAtEntity {

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
