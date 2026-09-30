package com.bidiws.service;

import com.bidiws.entity.RefreshToken;
import com.bidiws.entity.Utilisateur;
import com.bidiws.enums.Role;
import com.bidiws.repository.RefreshTokenRepository;
import com.bidiws.security.ApiKeyHasher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Refresh token : jeton opaque a usage unique (rotation), hash en base
 * (jamais en clair). Trois familles de scenarios exigees :
 * refresh valide -> le token est marque consomme (rotation possible) ;
 * expire/revoque -> 401 ; reutilisation d'un token deja consomme ->
 * detectee et rejetee (toute la chaine est revoquee, pas seulement la
 * requete rejouee).
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private ApiKeyHasher tokenHasher;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private static final String TOKEN_EN_CLAIR = "token-en-clair";
    private static final String TOKEN_HASH = "hash-du-token";
    private static final UUID FAMILLE_ID = UUID.randomUUID();

    private Utilisateur utilisateur(boolean actif) {
        return Utilisateur.builder().id(1L).email("user@bidiws.com").role(Role.HABITANT).actif(actif).build();
    }

    private RefreshToken.RefreshTokenBuilder tokenValideBuilder() {
        return RefreshToken.builder()
                .id(1L)
                .utilisateur(utilisateur(true))
                .tokenHash(TOKEN_HASH)
                .familleId(FAMILLE_ID)
                .expireA(LocalDateTime.now().plusDays(10))
                .revoque(false);
    }

    @Test
    void refreshValideEstMarqueConsommeEtRetourne() {
        when(tokenHasher.hash(TOKEN_EN_CLAIR)).thenReturn(TOKEN_HASH);
        when(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(tokenValideBuilder().build()));

        RefreshToken result = refreshTokenService.consommerPourRotation(TOKEN_EN_CLAIR);

        assertThat(result.getUtiliseA()).isNotNull();
        assertThat(result.getFamilleId()).isEqualTo(FAMILLE_ID);
        verify(refreshTokenRepository).save(result);
    }

    @Test
    void tokenIntrouvableEstRejete() {
        when(tokenHasher.hash(TOKEN_EN_CLAIR)).thenReturn(TOKEN_HASH);
        when(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.consommerPourRotation(TOKEN_EN_CLAIR))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("invalide");
    }

    @Test
    void tokenExpireEstRejeteAvec401() {
        RefreshToken expire = tokenValideBuilder().expireA(LocalDateTime.now().minusMinutes(1)).build();
        when(tokenHasher.hash(TOKEN_EN_CLAIR)).thenReturn(TOKEN_HASH);
        when(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(expire));

        assertThatThrownBy(() -> refreshTokenService.consommerPourRotation(TOKEN_EN_CLAIR))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("expiré");
    }

    @Test
    void tokenRevoqueEstRejeteAvec401() {
        RefreshToken revoque = tokenValideBuilder().revoque(true).build();
        when(tokenHasher.hash(TOKEN_EN_CLAIR)).thenReturn(TOKEN_HASH);
        when(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(revoque));

        assertThatThrownBy(() -> refreshTokenService.consommerPourRotation(TOKEN_EN_CLAIR))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("invalide");
    }

    @Test
    void reutilisationDunTokenDejaConsommeEstDetecteeEtRejetee() {
        RefreshToken dejaConsomme = tokenValideBuilder().utiliseA(LocalDateTime.now().minusMinutes(5)).build();
        when(tokenHasher.hash(TOKEN_EN_CLAIR)).thenReturn(TOKEN_HASH);
        when(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(dejaConsomme));

        assertThatThrownBy(() -> refreshTokenService.consommerPourRotation(TOKEN_EN_CLAIR))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("invalide");

        // Pas seulement la requete rejouee rejetee : toute la chaine de
        // rotation est revoquee (protection contre un maillon plus recent
        // deja obtenu par un attaquant).
        verify(refreshTokenRepository).revoquerFamille(FAMILLE_ID);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void compteDesactiveEstRejeteMemeAvecUnTokenValide() {
        RefreshToken pourCompteInactif = tokenValideBuilder().utilisateur(utilisateur(false)).build();
        when(tokenHasher.hash(TOKEN_EN_CLAIR)).thenReturn(TOKEN_HASH);
        when(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(pourCompteInactif));

        assertThatThrownBy(() -> refreshTokenService.consommerPourRotation(TOKEN_EN_CLAIR))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("invalide");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void creerNouvelleFamilleSauvegardeUnTokenAvecUneNouvelleFamille() {
        Utilisateur utilisateur = utilisateur(true);
        when(tokenHasher.genererCle()).thenReturn(TOKEN_EN_CLAIR);
        when(tokenHasher.hash(TOKEN_EN_CLAIR)).thenReturn(TOKEN_HASH);

        String result = refreshTokenService.creerNouvelleFamille(utilisateur);

        assertThat(result).isEqualTo(TOKEN_EN_CLAIR);
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isEqualTo(TOKEN_HASH);
        assertThat(captor.getValue().getFamilleId()).isNotNull();
        assertThat(captor.getValue().getUtilisateur()).isEqualTo(utilisateur);
    }

    @Test
    void creerTokenReutiliseLaFamilleFournie() {
        Utilisateur utilisateur = utilisateur(true);
        when(tokenHasher.genererCle()).thenReturn(TOKEN_EN_CLAIR);
        when(tokenHasher.hash(TOKEN_EN_CLAIR)).thenReturn(TOKEN_HASH);

        refreshTokenService.creerToken(utilisateur, FAMILLE_ID);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getFamilleId()).isEqualTo(FAMILLE_ID);
    }

    @Test
    void expirationConfigureeEstAppliqueeAuNouveauToken() {
        ReflectionTestUtils.setField(refreshTokenService, "expirationJours", 7);
        Utilisateur utilisateur = utilisateur(true);
        when(tokenHasher.genererCle()).thenReturn(TOKEN_EN_CLAIR);
        when(tokenHasher.hash(TOKEN_EN_CLAIR)).thenReturn(TOKEN_HASH);

        refreshTokenService.creerToken(utilisateur, FAMILLE_ID);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getExpireA())
                .isAfter(LocalDateTime.now().plusDays(6))
                .isBefore(LocalDateTime.now().plusDays(8));
    }
}
