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

    // Insensible a la casse : le nom de commune renvoye par l'API Adresse
    // (data.gouv.fr) ne correspond pas forcement exactement a la casse
    // saisie lors de l'embarquement de la Ville en base.
    Optional<Ville> findByNomIgnoreCase(String nom);

    Optional<Ville> findByCodePostal(String codePostal);
    boolean existsByCodePostal(String codePostal);

    List<Ville> findByDepartementContainingIgnoreCase(String departement);

    List<Ville> findByNomContainingIgnoreCase(String nom);
}
