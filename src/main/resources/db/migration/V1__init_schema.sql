-- ============================================================
-- BIDIWS — Migration V1 : Schéma initial
-- Fichier : V1__init_schema.sql
-- Dossier : src/main/resources/db/migration/
-- ============================================================

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- 1. UTILISATEURS
-- ============================================================
CREATE TABLE utilisateur (
    id            BIGSERIAL       PRIMARY KEY,
    nom           VARCHAR(100)    NOT NULL,
    prenom        VARCHAR(100)    NOT NULL,
    email         VARCHAR(150)    UNIQUE NOT NULL,
    mot_de_passe  VARCHAR(255)    NOT NULL,
    telephone     VARCHAR(20),
    role          VARCHAR(30)     NOT NULL
                                  CHECK (role IN (
                                      'HABITANT',
                                      'GARDIEN',
                                      'CHAUFFEUR',
                                      'SYNDIC',
                                      'MAIRIE',
                                      'BAILLEUR',
                                      'ADMIN'
                                  )),
    actif         BOOLEAN         DEFAULT TRUE,
    created_at    TIMESTAMP       DEFAULT NOW(),
    updated_at    TIMESTAMP       DEFAULT NOW()
);

-- ============================================================
-- 2. VILLES
-- ============================================================
CREATE TABLE ville (
    id          BIGSERIAL       PRIMARY KEY,
    nom         VARCHAR(100)    NOT NULL,
    code_postal VARCHAR(10)     NOT NULL,
    departement VARCHAR(100),
    actif       BOOLEAN         DEFAULT TRUE
);

-- ============================================================
-- 3. ZONES
-- ============================================================
CREATE TABLE zone (
    id          BIGSERIAL       PRIMARY KEY,
    ville_id    BIGINT          NOT NULL REFERENCES ville(id) ON DELETE CASCADE,
    nom         VARCHAR(100)    NOT NULL,
    code        VARCHAR(10)     NOT NULL,
    description TEXT,

    CONSTRAINT uq_zone_ville_code UNIQUE (ville_id, code)
);

-- ============================================================
-- 4. RÉSIDENCES
-- ============================================================
CREATE TABLE residence (
    id               BIGSERIAL        PRIMARY KEY,
    nom              VARCHAR(200)     NOT NULL,
    adresse          VARCHAR(255)     NOT NULL,
    complement       VARCHAR(255),
    code_postal      VARCHAR(10)      NOT NULL,
    ville_id         BIGINT           NOT NULL REFERENCES ville(id),
    zone_id          BIGINT           REFERENCES zone(id),
    latitude         DECIMAL(10, 8),
    longitude        DECIMAL(11, 8),
    rayon_detection  INT              DEFAULT 50
                                      CHECK (rayon_detection > 0),
    nb_conteneurs    INT              DEFAULT 1
                                      CHECK (nb_conteneurs > 0),
    actif            BOOLEAN          DEFAULT TRUE,
    created_at       TIMESTAMP        DEFAULT NOW()
);

-- ============================================================
-- 5. RÉSIDENCE ↔ GARDIEN (table de liaison)
-- ============================================================
CREATE TABLE residence_gardien (
    residence_id  BIGINT   NOT NULL REFERENCES residence(id) ON DELETE CASCADE,
    gardien_id    BIGINT   NOT NULL REFERENCES utilisateur(id) ON DELETE CASCADE,
    principal     BOOLEAN  DEFAULT TRUE,

    PRIMARY KEY (residence_id, gardien_id)
);

-- ============================================================
-- 6. TYPES DE COLLECTE
-- ============================================================
CREATE TABLE type_collecte (
    id      BIGSERIAL      PRIMARY KEY,
    code    VARCHAR(30)    UNIQUE NOT NULL,
    libelle VARCHAR(100)   NOT NULL,
    couleur VARCHAR(7),
    icone   VARCHAR(50)
);

-- ============================================================
-- 7. CALENDRIER DE COLLECTE
-- ============================================================
CREATE TABLE calendrier_collecte (
    id                BIGSERIAL   PRIMARY KEY,
    residence_id      BIGINT      NOT NULL REFERENCES residence(id) ON DELETE CASCADE,
    type_collecte_id  BIGINT      NOT NULL REFERENCES type_collecte(id),
    jour_semaine      SMALLINT    NOT NULL
                                  CHECK (jour_semaine BETWEEN 1 AND 7),
    heure_estimee     TIME,
    actif             BOOLEAN     DEFAULT TRUE
);

-- ============================================================
-- 8. CAMIONS
-- ============================================================
CREATE TABLE camion (
    id               BIGSERIAL      PRIMARY KEY,
    immatriculation  VARCHAR(20)    UNIQUE NOT NULL,
    modele           VARCHAR(100),
    type_benne       VARCHAR(50),
    capacite_tonnes  DECIMAL(5, 2),
    gps_actif        BOOLEAN        DEFAULT FALSE,
    capteur_benne    BOOLEAN        DEFAULT FALSE,
    actif            BOOLEAN        DEFAULT TRUE
);

-- ============================================================
-- 9. CHAUFFEUR ↔ CAMION (historique)
-- ============================================================
CREATE TABLE chauffeur_camion (
    chauffeur_id  BIGINT  NOT NULL REFERENCES utilisateur(id) ON DELETE CASCADE,
    camion_id     BIGINT  NOT NULL REFERENCES camion(id) ON DELETE CASCADE,
    date_debut    DATE    NOT NULL,
    date_fin      DATE,

    PRIMARY KEY (chauffeur_id, camion_id, date_debut),
    CONSTRAINT chk_dates CHECK (date_fin IS NULL OR date_fin >= date_debut)
);

-- ============================================================
-- 10. TOURNÉES
-- ============================================================
CREATE TABLE tournee (
    id                BIGSERIAL      PRIMARY KEY,
    date_tournee      DATE           NOT NULL,
    type_collecte_id  BIGINT         NOT NULL REFERENCES type_collecte(id),
    camion_id         BIGINT         NOT NULL REFERENCES camion(id),
    chauffeur_id      BIGINT         NOT NULL REFERENCES utilisateur(id),
    zone_id           BIGINT         REFERENCES zone(id),
    statut            VARCHAR(20)    DEFAULT 'PLANIFIEE'
                                     CHECK (statut IN (
                                         'PLANIFIEE',
                                         'EN_COURS',
                                         'TERMINEE',
                                         'ANNULEE'
                                     )),
    heure_debut       TIMESTAMP,
    heure_fin         TIMESTAMP,
    created_at        TIMESTAMP      DEFAULT NOW(),

    CONSTRAINT chk_heures CHECK (heure_fin IS NULL OR heure_fin >= heure_debut)
);

-- ============================================================
-- 11. ARRÊTS (cœur du système)
-- ============================================================
CREATE TABLE arret (
    id                    BIGSERIAL       PRIMARY KEY,
    tournee_id            BIGINT          NOT NULL REFERENCES tournee(id) ON DELETE CASCADE,
    residence_id          BIGINT          NOT NULL REFERENCES residence(id),
    ordre                 SMALLINT        NOT NULL CHECK (ordre > 0),

    -- Statut
    statut                VARCHAR(30)     DEFAULT 'EN_ATTENTE'
                                          CHECK (statut IN (
                                              'EN_ATTENTE',
                                              'EN_APPROCHE',
                                              'COLLECTE_PROBABLE',
                                              'COLLECTE_CONFIRMEE',
                                              'INCIDENT'
                                          )),

    -- Horodatages
    heure_estimee         TIMESTAMP,
    heure_approche        TIMESTAMP,
    heure_collecte        TIMESTAMP,

    -- Score de confiance multi-signaux (0 à 100)
    score_confiance       SMALLINT        DEFAULT 0
                                          CHECK (score_confiance BETWEEN 0 AND 100),
    mode_detection        VARCHAR(30)     CHECK (mode_detection IN (
                                              'GPS_AUTO',
                                              'VALIDATION_CHAUFFEUR',
                                              'CAPTEUR_BENNE',
                                              'RFID',
                                              'COMMUNAUTAIRE'
                                          )),

    -- Conteneurs
    nb_conteneurs         SMALLINT        DEFAULT 1,
    types_conteneurs      VARCHAR(200),
    poids_kg              DECIMAL(8, 2),

    -- Incident
    incident              BOOLEAN         DEFAULT FALSE,
    description_incident  TEXT,
    photo_incident_url    VARCHAR(500),

    created_at            TIMESTAMP       DEFAULT NOW(),
    updated_at            TIMESTAMP       DEFAULT NOW(),

    CONSTRAINT uq_arret_tournee_residence UNIQUE (tournee_id, residence_id)
);

-- ============================================================
-- 12. SIGNAUX GPS
-- ============================================================
CREATE TABLE signal_gps (
    id           BIGSERIAL        PRIMARY KEY,
    tournee_id   BIGINT           NOT NULL REFERENCES tournee(id) ON DELETE CASCADE,
    latitude     DECIMAL(10, 8)   NOT NULL,
    longitude    DECIMAL(11, 8)   NOT NULL,
    vitesse_kmh  DECIMAL(5, 2),
    cap          DECIMAL(5, 2),
    precision_m  SMALLINT,
    horodatage   TIMESTAMP        NOT NULL,
    source       VARCHAR(20)      DEFAULT 'APP'
                                  CHECK (source IN ('APP', 'BOITIER_GPS', 'SIMULE'))
);

-- ============================================================
-- 13. NOTIFICATIONS
-- ============================================================
CREATE TABLE notification (
    id               BIGSERIAL       PRIMARY KEY,
    destinataire_id  BIGINT          NOT NULL REFERENCES utilisateur(id) ON DELETE CASCADE,
    arret_id         BIGINT          REFERENCES arret(id) ON DELETE SET NULL,
    residence_id     BIGINT          REFERENCES residence(id) ON DELETE SET NULL,

    type             VARCHAR(30)     NOT NULL
                                     CHECK (type IN (
                                         'APPROCHE',
                                         'COLLECTE_CONFIRMEE',
                                         'COLLECTE_PROBABLE',
                                         'NON_PASSAGE',
                                         'RAPPEL_BAC',
                                         'INCIDENT'
                                     )),
    titre            VARCHAR(200)    NOT NULL,
    message          TEXT            NOT NULL,
    canal            VARCHAR(20)     DEFAULT 'PUSH'
                                     CHECK (canal IN ('PUSH', 'SMS', 'EMAIL')),
    lu               BOOLEAN         DEFAULT FALSE,
    envoye           BOOLEAN         DEFAULT FALSE,
    heure_envoi      TIMESTAMP,
    heure_lecture    TIMESTAMP,
    created_at       TIMESTAMP       DEFAULT NOW()
);

-- ============================================================
-- 14. SIGNALEMENTS
-- ============================================================
CREATE TABLE signalement (
    id            BIGSERIAL        PRIMARY KEY,
    auteur_id     BIGINT           NOT NULL REFERENCES utilisateur(id),
    residence_id  BIGINT           REFERENCES residence(id) ON DELETE SET NULL,
    arret_id      BIGINT           REFERENCES arret(id) ON DELETE SET NULL,

    type          VARCHAR(30)      NOT NULL
                                   CHECK (type IN (
                                       'BAC_PLEIN',
                                       'DEPOT_SAUVAGE',
                                       'BAC_ENDOMMAGE',
                                       'BAC_NON_RENTRE',
                                       'AUTRE'
                                   )),
    description   TEXT,
    photo_url     VARCHAR(500),
    latitude      DECIMAL(10, 8),
    longitude     DECIMAL(11, 8),

    statut        VARCHAR(20)      DEFAULT 'OUVERT'
                                   CHECK (statut IN (
                                       'OUVERT',
                                       'EN_TRAITEMENT',
                                       'RESOLU',
                                       'CLOS'
                                   )),
    created_at    TIMESTAMP        DEFAULT NOW(),
    resolu_at     TIMESTAMP
);

-- ============================================================
-- 15. PUSH TOKENS
-- ============================================================
CREATE TABLE push_token (
    id              BIGSERIAL       PRIMARY KEY,
    utilisateur_id  BIGINT          NOT NULL REFERENCES utilisateur(id) ON DELETE CASCADE,
    token           VARCHAR(500)    NOT NULL,
    plateforme      VARCHAR(10)     NOT NULL
                                    CHECK (plateforme IN ('IOS', 'ANDROID', 'WEB')),
    actif           BOOLEAN         DEFAULT TRUE,
    created_at      TIMESTAMP       DEFAULT NOW(),

    CONSTRAINT uq_push_token_utilisateur_plateforme UNIQUE (utilisateur_id, plateforme)
);

-- ============================================================
-- INDEX DE PERFORMANCE
-- ============================================================

-- utilisateur
CREATE INDEX idx_utilisateur_email   ON utilisateur(email);
CREATE INDEX idx_utilisateur_role    ON utilisateur(role);

-- Résidence
CREATE INDEX idx_residence_ville     ON residence(ville_id);
CREATE INDEX idx_residence_zone      ON residence(zone_id);

-- Tournée
CREATE INDEX idx_tournee_date        ON tournee(date_tournee);
CREATE INDEX idx_tournee_chauffeur   ON tournee(chauffeur_id);
CREATE INDEX idx_tournee_statut      ON tournee(statut);

-- Arrêt
CREATE INDEX idx_arret_tournee       ON arret(tournee_id);
CREATE INDEX idx_arret_residence     ON arret(residence_id);
CREATE INDEX idx_arret_statut        ON arret(statut);

-- Signal GPS
CREATE INDEX idx_signal_gps_tournee      ON signal_gps(tournee_id);
CREATE INDEX idx_signal_gps_horodatage   ON signal_gps(horodatage);

-- Notification
CREATE INDEX idx_notif_destinataire  ON notification(destinataire_id);
CREATE INDEX idx_notif_lu            ON notification(lu);
CREATE INDEX idx_notif_created       ON notification(created_at);

-- Signalement
CREATE INDEX idx_signalement_statut  ON signalement(statut);
CREATE INDEX idx_signalement_auteur  ON signalement(auteur_id);

-- ============================================================
-- DONNÉES INITIALES
-- ============================================================

-- Types de collecte
INSERT INTO type_collecte (code, libelle, couleur, icone) VALUES
    ('OM',          'Ordures ménagères',  '#4CAF50', 'trash'),
    ('TRI',         'Tri sélectif',       '#2196F3', 'recycle'),
    ('VERRE',       'Verre',              '#9C27B0', 'glass'),
    ('ENCOMBRANTS', 'Encombrants',        '#FF9800', 'truck'),
    ('BIODECHETS',  'Biodéchets',         '#795548', 'leaf');

-- Ville de test
INSERT INTO ville (nom, code_postal, departement) VALUES
    ('Étain',  '55400', 'Meuse'),
    ('Verdun', '55100', 'Meuse');

-- Zones de test
INSERT INTO zone (ville_id, nom, code) VALUES
    (1, 'Secteur A', 'A'),
    (1, 'Secteur B', 'B'),
    (2, 'Secteur A', 'A');

-- Admin par défaut (mot de passe : Admin1234! — à changer)
-- Le hash bcrypt ci-dessous correspond à "Admin1234!"
INSERT INTO utilisateur (nom, prenom, email, mot_de_passe, role) VALUES
    ('Admin', 'BIDIWS', 'admin@bidiws.com',
     '$2a$12$7Jz3Q5z1k8L9mN2pR4sT6uW8xY0aB2cD4eF6gH8iJ0kL2mN4oP6q',
     'ADMIN');