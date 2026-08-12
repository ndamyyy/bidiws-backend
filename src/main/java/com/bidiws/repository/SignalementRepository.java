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

    List<Signalement> findByStatutAndResidence_Gardiens_Gardien_IdOrderByCreatedAtAsc(StatutSignalement statut, Long gardienId);

    List<Signalement> findByStatutAndResidence_Ville_IdOrderByCreatedAtAsc(StatutSignalement statut, Long villeId);

    boolean existsByIdAndAuteurId(Long id, Long auteurId);

    boolean existsByIdAndResidence_Gardiens_Gardien_Id(Long id, Long gardienId);

    boolean existsByIdAndResidence_Syndics_Syndic_Id(Long id, Long syndicId);

    boolean existsByIdAndResidence_Ville_Id(Long id, Long villeId);

}
