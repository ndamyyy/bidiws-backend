package com.bidiws.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyHasherTest {

    private final ApiKeyHasher apiKeyHasher = new ApiKeyHasher();

    @Test
    void genererCleProduitDesClesDistinctesEtSuffisammentLongues() {
        String cle1 = apiKeyHasher.genererCle();
        String cle2 = apiKeyHasher.genererCle();

        assertThat(cle1).isNotEqualTo(cle2);
        assertThat(cle1.length()).isGreaterThanOrEqualTo(32);
    }

    @Test
    void hashEstDeterministe() {
        String hash1 = apiKeyHasher.hash("cle-de-test");
        String hash2 = apiKeyHasher.hash("cle-de-test");

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void hashDeDeuxClesDifferentesDonneDesResultatsDifferents() {
        assertThat(apiKeyHasher.hash("cle-a")).isNotEqualTo(apiKeyHasher.hash("cle-b"));
    }

    @Test
    void hashEstUnHexadecimalSha256De64Caracteres() {
        String hash = apiKeyHasher.hash("cle-de-test");

        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]+");
    }
}
