package com.bidiws.repository;

import com.bidiws.entity.TypeCollecte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TypeCollecteRepository extends JpaRepository<TypeCollecte,Long> {

    Optional<TypeCollecte> findByCode(String code);

    boolean existsByCode(String code);

    List<TypeCollecte> findByLibelleContainingIgnoreCase(String  libelle);
}
