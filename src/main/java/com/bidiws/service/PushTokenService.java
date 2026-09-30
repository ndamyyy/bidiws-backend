package com.bidiws.service;

import com.bidiws.dto.pushtoken.PushTokenRequestDto;
import com.bidiws.dto.pushtoken.PushTokenResponseDto;
import com.bidiws.entity.PushToken;
import com.bidiws.entity.Utilisateur;
import com.bidiws.repository.PushTokenRepository;
import com.bidiws.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PushTokenService {

    private final PushTokenRepository pushTokenRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional
    public PushTokenResponseDto enregistrer(Long utilisateurId, PushTokenRequestDto dto) {

        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        PushToken pushToken = pushTokenRepository.findByUtilisateurIdAndPlateforme(utilisateurId, dto.plateforme())
                .map(existant -> {
                    existant.setToken(dto.token());
                    existant.setActif(true);
                    return existant;
                })
                .orElseGet(() -> PushToken.builder()
                        .utilisateur(utilisateur)
                        .token(dto.token())
                        .plateforme(dto.plateforme())
                        .actif(true)
                        .build());

        return toResponseDto(pushTokenRepository.save(pushToken));
    }

    @Transactional
    public void desactiver(String token, Long utilisateurId) {
        PushToken pushToken = pushTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Token introuvable"));

        verifierProprietaire(pushToken, utilisateurId);

        pushToken.setActif(false);
        pushTokenRepository.save(pushToken);
    }

    @Transactional
    public void delete(String token, Long utilisateurId) {
        PushToken pushToken = pushTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Token introuvable"));

        verifierProprietaire(pushToken, utilisateurId);
        pushTokenRepository.delete(pushToken);
    }

    public List<PushTokenResponseDto> getByUtilisateur(Long utilisateurId) {
        return pushTokenRepository.findByUtilisateurIdAndActifTrue(utilisateurId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    private PushTokenResponseDto toResponseDto(PushToken p) {
        return new PushTokenResponseDto(
                p.getId(),
                p.getUtilisateur().getId(),
                p.getToken(),
                p.getPlateforme(),
                p.getActif(),
                p.getCreatedAt()
        );
    }

    private void verifierProprietaire(PushToken pushToken, Long utilisateurId) {
        if (!pushToken.getUtilisateur().getId().equals(utilisateurId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès non autorisé");
        }
    }
}
