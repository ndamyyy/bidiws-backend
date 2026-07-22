package com.bidiws.repository;

import com.bidiws.entity.Camion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CamionRepository extends JpaRepository<Camion, Long> {

    Optional<Camion> findByImmatriculation(String immatriculation);

    boolean existsByImmatriculation(String immatriculation);

    List<Camion> findByModele(String modele);

    List<Camion> findByActifTrue();

    List<Camion> findByGpsActifTrue();
}
