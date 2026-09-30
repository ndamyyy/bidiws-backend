package com.bidiws.repository;

import com.bidiws.entity.PushToken;
import com.bidiws.enums.Plateforme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    List<PushToken> findByUtilisateur(Long utilisateurId);

    List<PushToken> findByUtilisateurIdAndActifTrue(Long utilisateurId);

    Optional<PushToken> findByUtilisateurIdAndPlateforme(Long utilisateurId, Plateforme plateforme);

    Optional<PushToken> findByToken(String token);

    void deleteByToken(String token);

}