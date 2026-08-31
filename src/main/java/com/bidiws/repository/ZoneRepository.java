package com.bidiws.repository;

import com.bidiws.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, Long> {

    List<Zone> findByVilleId(Long villeId);

    Optional<Zone> findByVilleIdAndCode(Long villeId, String code);
    boolean existsByVilleIdAndCode(Long villeId, String code);

    Optional<Zone> findByNom(String nom);
    boolean existsByNom(String nom);

    List<Zone> findByNomContainingIgnoreCase(String nom);
}
