-- Dane demonstracyjne: konta i przykladowe ankiety.
--
-- Migracja NIE jest czescia domyslnej sciezki Flyway. Wchodzi wylacznie z profilem
-- demo (application-demo.properties dopisuje classpath:db/demo do lokalizacji).
-- Uruchomienie produkcyjne nigdy jej nie wykona.
--
-- Cel: recenzent po sklonowaniu repozytorium ma sie zalogowac i od razu zobaczyc
-- dzialajacy model uprawnien, bez zakladania kont i wklepywania danych.
--
-- Hasla sa jawne, bo taki jest sens danych demo. To konta na localhoscie, do bazy
-- ktora sam sobie zakladasz. Poza profilem demo haslo administratora pochodzi ze
-- zmiennej ANKIETOR_ADMIN_PASSWORD albo jest losowane przy pierwszym starcie.
--
--   admin  / demo-admin    ROLE_ADMIN + ROLE_USER
--   anna   / demo-anna     ROLE_USER
--   bartek / demo-bartek   ROLE_USER

-- --- Konta -------------------------------------------------------------------

INSERT INTO users (username, password_hash) VALUES
    ('admin',  '$2a$10$nK6g.TEfuyyZbGCZlSefPuxUk3zIkIxEK7K0phsDCkM2FhMmaWmZm'),
    ('anna',   '$2a$10$rxr2LYWVuXq4BmBHH2Zr/.UUrtLk6LQwBl2heLmXzNFSWbEnWcsU2'),
    ('bartek', '$2a$10$0lMt9IVtt/.Z21QW6Uff3exrSqXhA.stuANKw/QLoMCIQC8BVCbWe')
ON CONFLICT (username) DO NOTHING;

INSERT INTO users_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
         JOIN roles r ON r.name = 'ROLE_USER'
WHERE u.username IN ('admin', 'anna', 'bartek')
ON CONFLICT DO NOTHING;

INSERT INTO users_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
         JOIN roles r ON r.name = 'ROLE_ADMIN'
WHERE u.username = 'admin'
ON CONFLICT DO NOTHING;

-- --- Autorstwo w bazie wiedzy -------------------------------------------------
-- Czesc par dostaje autora, czesc zostaje bez niego. Dzieki temu widac oba
-- przypadki z prd.md §6a: pare z autorem moze edytowac tylko on albo administrator,
-- para bez autora jest wspolnym zasobem zespolu.

UPDATE question_answers
SET author_id = (SELECT id FROM users WHERE username = 'anna')
WHERE id IN (SELECT id FROM question_answers ORDER BY id LIMIT 8);

UPDATE question_answers
SET author_id = (SELECT id FROM users WHERE username = 'bartek')
WHERE id IN (SELECT id FROM question_answers ORDER BY id OFFSET 8 LIMIT 6);

-- --- Ankiety Anny ------------------------------------------------------------

INSERT INTO ankiety (nazwa, klient, owner_id)
SELECT 'Audyt dostawcy 2026', 'Kontrahent Poludnie', u.id
FROM users u
WHERE u.username = 'anna'
  AND NOT EXISTS (SELECT 1 FROM ankiety WHERE nazwa = 'Audyt dostawcy 2026');

INSERT INTO ankiety (nazwa, klient, owner_id)
SELECT 'Kwestionariusz ESG', 'Kontrahent Zachod', u.id
FROM users u
WHERE u.username = 'anna'
  AND NOT EXISTS (SELECT 1 FROM ankiety WHERE nazwa = 'Kwestionariusz ESG');

-- --- Ankieta Bartka ----------------------------------------------------------
-- Istnieje po to, zeby bylo widac izolacje: Anna jej nie zobaczy, a proba wejscia
-- na nia po adresie konczy sie odmowa. Administrator zobaczy ja po wlaczeniu
-- przelacznika "wszystkie".

INSERT INTO ankiety (nazwa, klient, owner_id)
SELECT 'Ankieta bezpieczenstwa IT', 'Kontrahent Wschod', u.id
FROM users u
WHERE u.username = 'bartek'
  AND NOT EXISTS (SELECT 1 FROM ankiety WHERE nazwa = 'Ankieta bezpieczenstwa IT');

-- --- Pozycje ankiet ----------------------------------------------------------
-- source_id wskazuje pare z bazy wiedzy, z ktorej odpowiedz zostala zaproponowana,
-- odwzorowujac to, co robi przycisk "Zapisz do ankiety" na ekranie dopasowania.

INSERT INTO ankieta_pozycje (ankieta_id, pytanie, odpowiedz, source_id)
SELECT a.id,
       'Czy firma posiada aktualny certyfikat ISO 9001?',
       qa.answer,
       qa.id
FROM ankiety a
         JOIN question_answers qa ON qa.question ILIKE '%ISO 9001%'
WHERE a.nazwa = 'Audyt dostawcy 2026'
  AND NOT EXISTS (SELECT 1 FROM ankieta_pozycje p WHERE p.ankieta_id = a.id)
ORDER BY qa.id
LIMIT 1;

INSERT INTO ankieta_pozycje (ankieta_id, pytanie, odpowiedz, source_id)
SELECT a.id,
       'Prosimy o podanie numeru KRS oraz NIP.',
       qa.answer,
       qa.id
FROM ankiety a
         JOIN question_answers qa ON qa.question ILIKE '%KRS%'
WHERE a.nazwa = 'Audyt dostawcy 2026'
ORDER BY qa.id
LIMIT 1;

-- Dopasowanie idzie przez immutable_unaccent z migracji V2, nie przez ILIKE na surowym
-- tekscie. Wzorzec ASCII nie trafia w polskie znaki: '%termin%platno%' mija sie
-- z "terminy platnosci", bo w bazie stoi tam "ł".

INSERT INTO ankieta_pozycje (ankieta_id, pytanie, odpowiedz, source_id)
SELECT a.id,
       'Jaki jest stosowany termin platnosci faktur?',
       qa.answer,
       qa.id
FROM ankiety a
         JOIN question_answers qa
              ON immutable_unaccent(lower(qa.question)) LIKE '%terminy platnosci%'
WHERE a.nazwa = 'Kwestionariusz ESG'
ORDER BY qa.id
LIMIT 1;

INSERT INTO ankieta_pozycje (ankieta_id, pytanie, odpowiedz, source_id)
SELECT a.id,
       'Czy zawierana jest umowa o zachowaniu poufnosci?',
       qa.answer,
       qa.id
FROM ankiety a
         JOIN question_answers qa
              ON immutable_unaccent(lower(qa.question)) LIKE '%poufnosci%'
WHERE a.nazwa = 'Ankieta bezpieczenstwa IT'
ORDER BY qa.id
LIMIT 1;
