package com.bidiws.repository;

import com.bidiws.entity.Residence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResidenceRepository extends JpaRepository<Residence, Long> {

    List<Residence> findByVilleId(Long villeId);

    List<Residence> findByZoneId(Long zoneId);

    List<Residence> findByZoneIdAndActifTrue(Long zoneId);

    List<Residence> findByNomContainingIgnoreCase(String nom);

    List<Residence> findByActifTrue();

    List<Residence> findByVilleIdAndActifTrue(Long villeId);

    boolean existsByVilleIdAndAdresseIgnoreCase(Long villeId, String adresse);

    boolean existsByIdAndVilleId(Long id, Long villeId);
}
