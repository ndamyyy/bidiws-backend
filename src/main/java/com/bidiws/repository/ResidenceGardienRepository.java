package com.bidiws.repository;

import com.bidiws.entity.ResidenceGardien;
import com.bidiws.entity.ResidenceGardienId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResidenceGardienRepository extends JpaRepository<ResidenceGardien, ResidenceGardienId> {

    List<ResidenceGardien> findByResidenceId(Long residenceId);

    List<ResidenceGardien> findByGardienId(Long gardienId);

    Optional<ResidenceGardien> findByResidenceIdAndPrincipalTrue(Long residenceId);

    void deleteByResidenceIdAndGardienId(Long residenceId, Long gardienId);
}
