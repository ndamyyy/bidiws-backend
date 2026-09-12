package com.bidiws.repository;

import com.bidiws.entity.ResidenceSyndic;
import com.bidiws.entity.ResidenceSyndicId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResidenceSyndicRepository extends JpaRepository<ResidenceSyndic, ResidenceSyndicId> {

    List<ResidenceSyndic> findByResidenceId(Long residenceId);

    List<ResidenceSyndic> findBySyndicId(Long syndicId);

    boolean existsByResidenceIdAndSyndicId(Long residenceId, Long syndicId);

    void deleteByResidenceIdAndSyndicId(Long residenceId, Long syndicId);
}
