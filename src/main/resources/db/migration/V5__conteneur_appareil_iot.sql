-- ============================================================
-- BIDIWS — Migration V5 : Conteneur + AppareilIot (detection IoT)
-- Fichier : V5__conteneur_appareil_iot.sql
-- Dossier : src/main/resources/db/migration/
--
-- Rien n'identifiait un bac individuellement jusqu'ici (juste
-- residence.nb_conteneurs, un simple compteur).
--
-- AppareilIot couvre deux scenarios materiels sans reecriture future :
-- - lecteur RFID monte sur le camion, qui scanne plusieurs bacs au
--   passage -> camion_id renseigne, le rfid_tag recu dans la requete
--   identifie le conteneur au moment de la detection
-- - capteur colle au meme boitier qu'un bac precis (ex: accelerometre
--   + ESP32) -> conteneur_id renseigne directement, pas besoin
--   d'envoyer de tag
--
-- cle_api_hash : hash SHA-256 (deterministe), PAS bcrypt. La cle est
-- generee par le backend (haute entropie, pas choisie par un humain),
-- donc un hash rapide suffit contre le vol de dump, et permet un
-- lookup direct par hash (une requete /iot/detections ne contient que
-- la cle, sans identifiant device separe) — meme logique que les cles
-- d'API GitHub/Stripe.
-- ============================================================

CREATE TABLE conteneur (
    id            BIGSERIAL      PRIMARY KEY,
    code          VARCHAR(50)    NOT NULL,
    residence_id  BIGINT         NOT NULL REFERENCES residence(id) ON DELETE CASCADE,
    rfid_tag      VARCHAR(100)   UNIQUE,
    actif         BOOLEAN        DEFAULT TRUE,

    CONSTRAINT uq_conteneur_residence_code UNIQUE (residence_id, code)
);

CREATE INDEX idx_conteneur_residence_id ON conteneur (residence_id);
CREATE INDEX idx_conteneur_rfid_tag ON conteneur (rfid_tag);

CREATE TABLE appareil_iot (
    id                    BIGSERIAL      PRIMARY KEY,
    identifiant_materiel  VARCHAR(100)   NOT NULL UNIQUE,
    cle_api_hash          VARCHAR(64)    NOT NULL UNIQUE,
    type_appareil         VARCHAR(30)    NOT NULL
                                         CHECK (type_appareil IN ('CAPTEUR_BENNE', 'LECTEUR_RFID')),
    conteneur_id          BIGINT         REFERENCES conteneur(id) ON DELETE SET NULL,
    camion_id             BIGINT         REFERENCES camion(id) ON DELETE SET NULL,
    actif                 BOOLEAN        DEFAULT TRUE,
    created_at            TIMESTAMP      DEFAULT NOW(),

    CONSTRAINT chk_appareil_iot_un_seul_rattachement
        CHECK (conteneur_id IS NULL OR camion_id IS NULL)
);

CREATE INDEX idx_appareil_iot_cle_api_hash ON appareil_iot (cle_api_hash);
CREATE INDEX idx_appareil_iot_conteneur_id ON appareil_iot (conteneur_id);
CREATE INDEX idx_appareil_iot_camion_id ON appareil_iot (camion_id);
