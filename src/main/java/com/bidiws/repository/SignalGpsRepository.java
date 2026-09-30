package com.bidiws.repository;

import com.bidiws.entity.SignalGps;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

@Repository
public interface SignalGpsRepository extends JpaRepository<SignalGps, Long> {

    List<SignalGps> findByTourneeId(Long tourneeId);

    Page<SignalGps> findByTourneeIdOrderByHorodatageAsc(Long tourneeId, Pageable pageable);

    Optional<SignalGps> findFirstByTourneeIdOrderByHorodatageDesc(Long tourneeId);

    Page<SignalGps> findByTourneeIdAndHorodatageBetweenOrderByHorodatageAsc(
            Long tourneeId, LocalDateTime debut, LocalDateTime fin, Pageable pageable);

}
