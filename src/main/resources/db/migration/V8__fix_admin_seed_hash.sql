-- ============================================================
-- BIDIWS — Migration V8 : correction du hash admin par défaut
-- Fichier : V8__fix_admin_seed_hash.sql
--
-- Le hash bcrypt seede en V1 faisait 59 caracteres au lieu de 60 —
-- invalide structurellement, aucun mot de passe ne pouvait jamais
-- matcher. Corrige avec un vrai hash de "Admin1234!" (a changer
-- immediatement apres premiere connexion, comme deja indique en V1).
-- ============================================================

UPDATE utilisateur
SET mot_de_passe = '$2a$12$/7IJ7ZsXoU80TzUZq0YwieJGZMuaZ/K5lfcXE2nAol1Jk9Z0en8ry'
WHERE email = 'admin@bidiws.com'
  AND mot_de_passe = '$2a$12$7Jz3Q5z1k8L9mN2pR4sT6uW8xY0aB2cD4eF6gH8iJ0kL2mN4oP6q';