package com.bidiws.repository;

import com.bidiws.entity.Arret;
import com.bidiws.enums.StatutArret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // Utilisee pour scoper une LISTE de tournees (TourneeService.filtrerParRole,
    // cas SYNDIC) : une seule requete plutot qu'un aller-retour par tournee.
    List<Arret> findByResidenceIdIn(List<Long> residenceIds);

    List<Arret> findByStatut(StatutArret statut);

    // Utilisee par la detection GPS de proximite : ne remonte que les arrets
    // dont la tournee parente est encore PLANIFIEE ou EN_COURS. Filtre pose
    // directement ici (plutot qu'en boucle Java cote appelant) pour qu'un
    // arret orphelin sur une tournee TERMINEE/ANNULEE ne soit jamais meme
    // charge, encore moins pousse en COLLECTE_PROBABLE par un signal GPS.
    @Query("SELECT a FROM Arret a WHERE a.tournee.id = :tourneeId AND a.statut = :statut "
            + "AND a.tournee.statut IN (com.bidiws.enums.StatutTournee.PLANIFIEE, com.bidiws.enums.StatutTournee.EN_COURS)")
    List<Arret> findByTourneeIdAndStatut(@Param("tourneeId") Long tourneeId, @Param("statut") StatutArret statut);

    List<Arret> findByIncidentTrue();

    List<Arret> findByResidenceIdOrderByCreatedAtDesc(Long residenceId);

    boolean existsByIdAndTourneeChauffeurId(Long id, Long chauffeurId);

}
