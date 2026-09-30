-- ============================================================
-- BIDIWS — Migration V2 : Isolation des donnees par SYNDIC/MAIRIE
-- Fichier : V2__isolation_syndic_mairie.sql
-- Dossier : src/main/resources/db/migration/
--
-- Un SYNDIC gere une ou plusieurs residences (table de liaison
-- residence_syndic, sur le modele de residence_gardien).
-- Une MAIRIE est rattachee a une ville (colonne ville_id sur
-- utilisateur, nullable — seuls les comptes MAIRIE l'utilisent).
-- ============================================================

-- ============================================================
-- 1. RATTACHEMENT MAIRIE -> VILLE
-- ============================================================
ALTER TABLE utilisateur
    ADD COLUMN ville_id BIGINT REFERENCES ville(id);

CREATE INDEX idx_utilisateur_ville_id ON utilisateur (ville_id);

-- ============================================================
-- 2. RESIDENCE <-> SYNDIC (table de liaison)
-- ============================================================
CREATE TABLE residence_syndic (
    residence_id  BIGINT   NOT NULL REFERENCES residence(id) ON DELETE CASCADE,
    syndic_id     BIGINT   NOT NULL REFERENCES utilisateur(id) ON DELETE CASCADE,

    PRIMARY KEY (residence_id, syndic_id)
);

CREATE INDEX idx_residence_syndic_syndic_id ON residence_syndic (syndic_id);
