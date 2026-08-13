-- ============================================================
-- BIDIWS — Migration V3 : Lien HABITANT <-> RESIDENCE
-- Fichier : V3__lien_habitant_residence.sql
-- Dossier : src/main/resources/db/migration/
--
-- Jusqu'ici un habitant ne pouvait etre rattache a aucune residence,
-- donc ne pouvait recevoir aucune notification ciblee (sur le modele
-- de residence_gardien / residence_syndic de la migration V2).
-- ============================================================

CREATE TABLE residence_habitant (
    residence_id  BIGINT   NOT NULL REFERENCES residence(id) ON DELETE CASCADE,
    habitant_id   BIGINT   NOT NULL REFERENCES utilisateur(id) ON DELETE CASCADE,

    PRIMARY KEY (residence_id, habitant_id)
);

CREATE INDEX idx_residence_habitant_habitant_id ON residence_habitant (habitant_id);
