package com.bidiws.repository;

import com.bidiws.entity.Conteneur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConteneurRepository extends JpaRepository<Conteneur, Long> {

    List<Conteneur> findByResidenceId(Long residenceId);

    Optional<Conteneur> findByRfidTag(String rfidTag);

    boolean existsByResidenceIdAndCode(Long residenceId, String code);
}
