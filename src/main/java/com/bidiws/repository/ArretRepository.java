package com.bidiws.repository;

import com.bidiws.entity.Arret;
import com.bidiws.enums.StatutArret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArretRepository extends JpaRepository<Arret, Long> {

    List<Arret> findByTourneeId(Long tourneeId);

    List<Arret> findByTourneeIdOrderByOrdreAsc(Long tourneeId);

    Optional<Arret> findByTourneeIdAndResidenceId(Long tourneeId, Long residenceId);

    List<Arret> findByResidenceId(Long residenceId);

    List<Arret> findByStatut(StatutArret statut);

    List<Arret> findByIncidentTrue();

    List<Arret> findByResidenceIdOrderByCreatedAtDesc(Long residenceId);

}
