-- ============================================================
-- BIDIWS — Migration V4 : Rattachement CAMION -> VILLE
-- Fichier : V4__ville_camion.sql
-- Dossier : src/main/resources/db/migration/
--
-- Jusqu'ici un camion n'avait aucune notion de ville : une MAIRIE
-- pouvait lister/gerer les camions (et les affectations chauffeur-
-- camion) de n'importe quelle ville, pas seulement la sienne.
-- Nullable car les camions existants n'ont pas de ville renseignee ;
-- un camion sans ville_id reste invisible pour une MAIRIE (fail-closed,
-- meme regle que utilisateur.ville_id / residence).
-- ============================================================

ALTER TABLE camion
    ADD COLUMN ville_id BIGINT REFERENCES ville(id);

CREATE INDEX idx_camion_ville_id ON camion (ville_id);
