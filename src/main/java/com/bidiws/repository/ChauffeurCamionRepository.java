package com.bidiws.repository;

import com.bidiws.entity.ChauffeurCamion;
import com.bidiws.entity.ChauffeurCamionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChauffeurCamionRepository extends JpaRepository<ChauffeurCamion, ChauffeurCamionId> {

    List<ChauffeurCamion> findByChauffeurId(String chauffeurId);

    List<ChauffeurCamion> findByCamionId(Long camionId);

    Optional<ChauffeurCamion> findByCamionIdAndDateFinIsNull(Long camionId);

    Optional<ChauffeurCamion> findByChauffeurIdAndDateFinIsNull(Long chauffeurId);

}
