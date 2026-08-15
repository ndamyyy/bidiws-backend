-- ============================================================
-- BIDIWS — Migration V7 : Verrouillage de compte (brute-force login)
-- Fichier : V7__verrouillage_compte.sql
-- Dossier : src/main/resources/db/migration/
--
-- POST /auth/login acceptait un nombre illimite de tentatives, sans delai
-- ni verrouillage. Verrouillage DB-backed (pas de Bucket4j/Redis — l'app
-- est mono-instance a ce stade, une solution DB-backed suffit et survit
-- a un redemarrage, contrairement a un compteur en memoire). Compteur
-- par compte (email), pas par IP — pas de notion d'IP cote backend
-- aujourd'hui.
-- ============================================================

ALTER TABLE utilisateur
    ADD COLUMN tentatives_echouees SMALLINT NOT NULL DEFAULT 0,
    ADD COLUMN verrouille_jusqua TIMESTAMP;
