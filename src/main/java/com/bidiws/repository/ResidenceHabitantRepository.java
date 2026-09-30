package com.bidiws.repository;

import com.bidiws.entity.ResidenceHabitant;
import com.bidiws.entity.ResidenceHabitantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResidenceHabitantRepository extends JpaRepository<ResidenceHabitant, ResidenceHabitantId> {

    List<ResidenceHabitant> findByResidenceId(Long residenceId);

    List<ResidenceHabitant> findByHabitantId(Long habitantId);

    boolean existsByResidenceIdAndHabitantId(Long residenceId, Long habitantId);

    void deleteByResidenceIdAndHabitantId(Long residenceId, Long habitantId);
}
