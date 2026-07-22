package com.bidiws.repository;

import com.bidiws.entity.CalendrierCollecte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CalendrierCollecteRepository extends JpaRepository<CalendrierCollecte,Long> {

    List<CalendrierCollecte> findByResidenceId(Long residenceId);

    List<CalendrierCollecte> findByResidenceIdAndActifTrue(Long residenceId);

    List<CalendrierCollecte> findByResidenceIdQndJourSemaine(Long residenceId, Short jourSemaine);

    List<CalendrierCollecte> findByTypeCollecteId(Long typeCollecteId);

}
