package com.bidiws.repository;

import com.bidiws.entity.ArretConteneur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArretConteneurRepository extends JpaRepository<ArretConteneur, Long> {

    List<ArretConteneur> findByArretId(Long arretId);

    Optional<ArretConteneur> findByArretIdAndConteneurId(Long arretId, Long conteneurId);
}
