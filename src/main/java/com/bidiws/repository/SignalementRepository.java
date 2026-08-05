package com.bidiws.repository;

import com.bidiws.entity.Signalement;
import com.bidiws.enums.StatutSignalement;
import com.bidiws.enums.TypeSignalement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SignalementRepository extends JpaRepository<Signalement,Long> {

    List<Signalement> findByAuteurId(Long auteurId);

    List<Signalement> findByAuteurIdOrderByCreatedAtDesc(Long auteurId);

    List<Signalement> findByResidenceId(Long residenceId);

    List<Signalement> findByStatut(StatutSignalement statut);

    List<Signalement> findByType(TypeSignalement type);

    List<Signalement> findByStatutOrderByCreatedAtAsc(StatutSignalement statut);

    boolean existsByIdAndAuteurId(Long id, Long auteurId);

    boolean existsByIdAndResidence_Gardiens_Gardien_Id(Long id, Long gardienId);

}
