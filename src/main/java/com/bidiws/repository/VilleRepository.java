package com.bidiws.repository;

import com.bidiws.entity.Ville;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VilleRepository extends JpaRepository<Ville, Long> {

    Optional<Ville> findByNom(String nom);
    boolean existsByNom(String nom);

    Optional<Ville> findByCodePostal(String codePostal);
    boolean existsByCodePostal(String codePostal);

    List<Ville> findByDepartementContainingIgnoreCase(String departement);

    List<Ville> findByNomContainingIgnoreCase(String nom);
}
