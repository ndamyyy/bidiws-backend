-- ============================================================
-- BIDIWS — Migration V9 : Refresh tokens (rotation + detection de reutilisation)
-- Fichier : V9__refresh_token.sql
-- Dossier : src/main/resources/db/migration/
--
-- Le JWT unique de 24h n'etait jamais renouvele : compromis entre securite
-- (duree de vie longue si vole) et confort (reconnexion frequente si
-- courte). Passe a un access token court (bidiws.jwt.expiration, quelques
-- minutes) + refresh token longue duree, stocke en base (pas un JWT :
-- doit pouvoir etre revoque avant expiration, ce qu'un JWT stateless ne
-- permet pas).
--
-- token_hash, pas le token en clair (meme raisonnement que
-- appareil_iot.cle_api_hash) : un dump de cette table ne doit jamais
-- permettre de rejouer un token.
--
-- famille_id : identifie la chaine de rotation issue d'un meme login.
-- Chaque refresh est a usage unique (utilise_a pose au premier usage) ;
-- si un token deja utilise est represente, c'est un signal de vol
-- (attaquant et utilisateur legitime disposent tous deux d'un maillon de
-- la meme chaine) — toute la famille est alors revoquee, pas seulement
-- la requete rejouee, sinon un maillon plus recent vole resterait valide.
-- ============================================================

CREATE TABLE refresh_token (
    id             BIGSERIAL PRIMARY KEY,
    utilisateur_id BIGINT      NOT NULL REFERENCES utilisateur(id) ON DELETE CASCADE,
    token_hash     VARCHAR(64) NOT NULL UNIQUE,
    famille_id     UUID        NOT NULL,
    expire_a       TIMESTAMP   NOT NULL,
    utilise_a      TIMESTAMP,
    revoque        BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_token_token_hash ON refresh_token (token_hash);
CREATE INDEX idx_refresh_token_famille_id ON refresh_token (famille_id);
