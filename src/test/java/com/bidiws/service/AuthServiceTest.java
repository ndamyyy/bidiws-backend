package com.bidiws.service;

import com.bidiws.dto.utilisateur.UtilisateurLoginRequestDto;
import com.bidiws.entity.Utilisateur;
import com.bidiws.enums.Role;
import com.bidiws.repository.UtilisateurRepository;
import com.bidiws.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifie le verrouillage de compte par echecs de connexion successifs :
 * seuil/duree en constantes nommees, verrouillage pose avant toute
 * verification du mot de passe, deverrouillage automatique une fois le
 * delai expire, et reinitialisation du compteur sur login reussi.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private static final String EMAIL = "user@bidiws.com";
    private static final String MOT_DE_PASSE = "MotDePasse123!";

    private UtilisateurLoginRequestDto dto() {
        return new UtilisateurLoginRequestDto(EMAIL, MOT_DE_PASSE);
    }

    private Utilisateur utilisateur(short tentativesEchouees, LocalDateTime verrouilleJusqua) {
        return Utilisateur.builder()
                .id(1L)
                .email(EMAIL)
                .motDePasse("hash")
                .role(Role.HABITANT)
                .actif(true)
                .tentativesEchouees(tentativesEchouees)
                .verrouilleJusqua(verrouilleJusqua)
                .build();
    }

    @Test
    void cinquiemeEchecPoseLeVerrouillage() {
        Utilisateur utilisateur = utilisateur((short) 4, null);
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(utilisateur));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("mauvais mot de passe"));

        assertThatThrownBy(() -> authService.login(dto())).isInstanceOf(BadCredentialsException.class);

        assertThat(utilisateur.getTentativesEchouees()).isEqualTo((short) 5);
        assertThat(utilisateur.getVerrouilleJusqua()).isAfter(LocalDateTime.now());
    }

    @Test
    void unEchecAvantLeSeuilNeVerrouillePas() {
        Utilisateur utilisateur = utilisateur((short) 0, null);
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(utilisateur));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("mauvais mot de passe"));

        assertThatThrownBy(() -> authService.login(dto())).isInstanceOf(BadCredentialsException.class);

        assertThat(utilisateur.getTentativesEchouees()).isEqualTo((short) 1);
        assertThat(utilisateur.getVerrouilleJusqua()).isNull();
    }

    @Test
    void tentativePendantLeVerrouillageEchoueSansVerifierLeMotDePasse() {
        Utilisateur utilisateur = utilisateur((short) 5, LocalDateTime.now().plusMinutes(10));
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(utilisateur));

        assertThatThrownBy(() -> authService.login(dto()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("verrouillé");

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void verrouillageExpireDeverrouilleAutomatiquementEtReinitialiseLeCompteur() {
        Utilisateur utilisateur = utilisateur((short) 5, LocalDateTime.now().minusMinutes(1));
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(utilisateur));
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("un-token");

        String token = authService.login(dto());

        assertThat(token).isEqualTo("un-token");
        assertThat(utilisateur.getTentativesEchouees()).isEqualTo((short) 0);
        assertThat(utilisateur.getVerrouilleJusqua()).isNull();
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void loginReussiReinitialiseLeCompteurMemeApresDesEchecsPrecedents() {
        Utilisateur utilisateur = utilisateur((short) 3, null);
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(utilisateur));
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("un-token");

        String token = authService.login(dto());

        assertThat(token).isEqualTo("un-token");
        assertThat(utilisateur.getTentativesEchouees()).isEqualTo((short) 0);
        assertThat(utilisateur.getVerrouilleJusqua()).isNull();
    }

    @Test
    void loginReussiSansEchecsPrealablesNeDeclencheAucuneSauvegardeInutile() {
        Utilisateur utilisateur = utilisateur((short) 0, null);
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(utilisateur));
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("un-token");

        authService.login(dto());

        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void emailInconnuLaisseAuthenticationManagerEchouerNormalement() {
        when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("mauvais mot de passe"));

        assertThatThrownBy(() -> authService.login(dto())).isInstanceOf(BadCredentialsException.class);

        verify(utilisateurRepository, never()).save(any());
    }
}
