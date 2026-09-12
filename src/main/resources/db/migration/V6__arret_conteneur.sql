-- ============================================================
-- BIDIWS — Migration V6 : Suivi par conteneur (ArretConteneur)
-- Fichier : V6__arret_conteneur.sql
-- Dossier : src/main/resources/db/migration/
--
-- IotDetectionService confirmait jusqu'ici l'Arret entier des le premier
-- capteur declenche : une residence a 4 bacs, le 1er confirme -> tout
-- l'arret passe COLLECTE_CONFIRMEE, et transitionAutorisee bloque toute
-- transition ulterieure (idempotence) -> les 3 autres capteurs qui se
-- declenchent ensuite sont silencieusement ignores.
--
-- arret_conteneur suit le statut de CHAQUE bac individuellement ; l'Arret
-- parent ne passe COLLECTE_CONFIRMEE que quand TOUS ses arret_conteneur le
-- sont (cf. ArretConteneurDetectionService). Une residence sans conteneurs
-- enregistres n'a aucune ligne ici : comportement inchange, l'Arret reste
-- confirme directement (retrocompatibilite totale, pas de regression).
-- ============================================================

CREATE TABLE arret_conteneur (
    id                        BIGSERIAL      PRIMARY KEY,
    arret_id                  BIGINT         NOT NULL REFERENCES arret(id) ON DELETE CASCADE,
    conteneur_id              BIGINT         NOT NULL REFERENCES conteneur(id) ON DELETE CASCADE,
    statut                    VARCHAR(30)    NOT NULL DEFAULT 'EN_ATTENTE'
                                              CHECK (statut IN (
                                                  'EN_ATTENTE',
                                                  'EN_APPROCHE',
                                                  'COLLECTE_PROBABLE',
                                                  'COLLECTE_CONFIRMEE',
                                                  'INCIDENT'
                                              )),
    mode_detection            VARCHAR(30)    CHECK (mode_detection IN (
                                                  'GPS_AUTO',
                                                  'VALIDATION_CHAUFFEUR',
                                                  'CAPTEUR_BENNE',
                                                  'RFID',
                                                  'COMMUNAUTAIRE'
                                              )),
    score_confiance           SMALLINT       CHECK (score_confiance BETWEEN 0 AND 100),
    horodatage_confirmation   TIMESTAMP,

    CONSTRAINT uq_arret_conteneur UNIQUE (arret_id, conteneur_id)
);

CREATE INDEX idx_arret_conteneur_arret_id ON arret_conteneur (arret_id);
CREATE INDEX idx_arret_conteneur_conteneur_id ON arret_conteneur (conteneur_id);

-- Mesure de remplissage continue (POST /iot/mesures-remplissage) : ne touche
-- jamais a un statut d'arret, juste l'etat courant du bac.
ALTER TABLE conteneur
    ADD COLUMN niveau_remplissage_pct SMALLINT
        CHECK (niveau_remplissage_pct BETWEEN 0 AND 100),
    ADD COLUMN derniere_mesure_remplissage TIMESTAMP;
