package com.bidiws.repository;

import com.bidiws.entity.Tournee;
import com.bidiws.enums.StatutTournee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TourneeRepository extends JpaRepository<Tournee, Long> {

    List<Tournee> findByDateTournee(LocalDate dateTournee);

    List<Tournee> findByChauffeurId(Long chauffeurId);

    List<Tournee> findByCamionId(Long camionId);

    List<Tournee> findByStatut(StatutTournee statut);

    List<Tournee> findByChauffeurIdAndDateTournee(Long chauffeurId, LocalDate dateTournee);

    List<Tournee> findByDateTourneeAndStatut(LocalDate dateTournee, StatutTournee statut);

    boolean existsByIdAndChauffeurId(Long id, Long chauffeurId);

    boolean existsByCamionIdAndStatut(Long camionId, StatutTournee statut);

    boolean existsByChauffeurIdAndStatut(Long chauffeurId, StatutTournee statut);

    boolean existsByZoneId(Long zoneId);

}
