package com.bidiws.repository;

import com.bidiws.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // Reutilisation detectee (RefreshTokenService) : revoque toute la
    // chaine de rotation en une requete, pas seulement le token rejoue.
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoque = true WHERE r.familleId = :familleId")
    void revoquerFamille(@Param("familleId") UUID familleId);
}
