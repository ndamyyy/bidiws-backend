package com.bidiws.repository;

import com.bidiws.entity.ArretConteneur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArretConteneurRepository extends JpaRepository<ArretConteneur, Long> {

    List<ArretConteneur> findByArretId(Long arretId);

    Optional<ArretConteneur> findByArretIdAndConteneurId(Long arretId, Long conteneurId);

    // Utilisee par ConteneurService.desactiver : une ligne EN_ATTENTE sur un
    // arret dont la tournee est toujours active resterait bloquee pour
    // toujours si le conteneur devient inactif (IotDetectionService rejette
    // ensuite toute detection sur un conteneur inactif) — l'arret parent ne
    // pourrait alors plus jamais se confirmer automatiquement.
    @Query("SELECT CASE WHEN COUNT(ac) > 0 THEN true ELSE false END FROM ArretConteneur ac "
            + "WHERE ac.conteneur.id = :conteneurId "
            + "AND ac.statut = com.bidiws.enums.StatutArret.EN_ATTENTE "
            + "AND ac.arret.tournee.statut IN (com.bidiws.enums.StatutTournee.PLANIFIEE, com.bidiws.enums.StatutTournee.EN_COURS)")
    boolean existsActifByConteneurId(@Param("conteneurId") Long conteneurId);
}
