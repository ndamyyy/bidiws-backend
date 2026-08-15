package com.bidiws.repository;

import com.bidiws.entity.Arret;
import com.bidiws.enums.StatutArret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArretRepository extends JpaRepository<Arret, Long> {

    List<Arret> findByTourneeId(Long tourneeId);

    // Resout "l'arret du jour" pour une residence, utilise par la detection
    // IoT qui ne connait que le conteneur/residence, pas la tournee. Retourne
    // une liste (pas Optional<Arret>) car rien n'empeche plusieurs tournees
    // le meme jour pour la meme residence (types de collecte differents).
    List<Arret> findByResidenceIdAndTournee_DateTournee(Long residenceId, LocalDate dateTournee);

    List<Arret> findByTourneeIdOrderByOrdreAsc(Long tourneeId);

    Optional<Arret> findByTourneeIdAndResidenceId(Long tourneeId, Long residenceId);

    List<Arret> findByResidenceId(Long residenceId);

    List<Arret> findByStatut(StatutArret statut);

    List<Arret> findByTourneeIdAndStatut(Long tourneeId, StatutArret statut);

    List<Arret> findByIncidentTrue();

    List<Arret> findByResidenceIdOrderByCreatedAtDesc(Long residenceId);

    boolean existsByIdAndTourneeChauffeurId(Long id, Long chauffeurId);

}
