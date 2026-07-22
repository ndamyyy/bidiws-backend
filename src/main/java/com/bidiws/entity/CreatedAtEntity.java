package com.bidiws.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * À étendre par les entités qui n'ont qu'une colonne created_at
 * (résidence, tournée, notification, signalement, push_token...).
 * Nécessite @EnableJpaAuditing sur la classe principale Spring Boot.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class CreatedAtEntity {

    @CreatedDate
    @Column(name = "created_at",  nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
