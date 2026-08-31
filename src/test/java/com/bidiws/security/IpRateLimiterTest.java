package com.bidiws.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IpRateLimiterTest {

    @Mock
    private Clock clock;

    private static final String IP_A = "10.0.0.1";
    private static final String IP_B = "10.0.0.2";
    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void resteAutoriseJusquaLaLimiteIncluse() {
        when(clock.instant()).thenReturn(T0);
        IpRateLimiter limiter = new IpRateLimiter(clock);

        for (int i = 0; i < IpRateLimiter.LIMITE_TENTATIVES; i++) {
            assertThat(limiter.autoriser(IP_A)).isTrue();
        }
    }

    @Test
    void refuseAuDelaDeLaLimite() {
        when(clock.instant()).thenReturn(T0);
        IpRateLimiter limiter = new IpRateLimiter(clock);

        for (int i = 0; i < IpRateLimiter.LIMITE_TENTATIVES; i++) {
            limiter.autoriser(IP_A);
        }

        assertThat(limiter.autoriser(IP_A)).isFalse();
    }

    @Test
    void laLimiteEstIndependantePourChaqueIp() {
        when(clock.instant()).thenReturn(T0);
        IpRateLimiter limiter = new IpRateLimiter(clock);

        for (int i = 0; i < IpRateLimiter.LIMITE_TENTATIVES; i++) {
            limiter.autoriser(IP_A);
        }

        // IP_A a atteint sa limite, mais IP_B n'a encore jamais tente : son
        // propre compteur, independant, reste en dessous du seuil.
        assertThat(limiter.autoriser(IP_B)).isTrue();
    }

    @Test
    void laFenetreExpireeReinitialiseLeCompteur() {
        when(clock.instant()).thenReturn(T0);
        IpRateLimiter limiter = new IpRateLimiter(clock);

        for (int i = 0; i < IpRateLimiter.LIMITE_TENTATIVES; i++) {
            limiter.autoriser(IP_A);
        }
        assertThat(limiter.autoriser(IP_A)).isFalse();

        // Fenetre depassee (15 min + 1s) : le compteur redemarre a zero pour
        // cette IP, meme instance de IpRateLimiter (pas de nouvel objet).
        when(clock.instant()).thenReturn(T0.plus(IpRateLimiter.FENETRE).plusSeconds(1));

        assertThat(limiter.autoriser(IP_A)).isTrue();
    }
}
