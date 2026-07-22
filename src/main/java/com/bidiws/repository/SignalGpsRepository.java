package com.bidiws.repository;

import com.bidiws.entity.SignalGps;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

@Repository
public interface SignalGpsRepository extends JpaRepository<SignalGps, Long> {

    List<SignalGps> findByTourneeId(Long tourneeId);

    List<SignalGps> findByTourneeIdOrderByHorodatageAsc(Long tourneeId);

    Optional<SignalGps> findFirstByTourneeIdOrderByHorodatageDesc(Long tourneeId);

    List<SignalGps> findByTourneeIdQndHorodatageBetween(Long tourneeId, LocalDateTime debut, LocalDateTime fin);

}
