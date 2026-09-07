-- Ankietor - przygotowanie bazy pod uruchomienie lokalne.
--
-- Uruchom RAZ, jako superuser, zanim pierwszy raz wystartujesz aplikacje:
--
--     psql -U postgres -f db/setup/00_baza_i_rola.sql
--
-- Skrypt jest idempotentny: mozna go puscic ponownie, nic nie zepsuje.
-- Reszte schematu (tabele, indeksy, dane) zaklada Flyway przy starcie aplikacji.
--
-- HASLO ponizej jest haslem DEMONSTRACYJNYM do bazy na localhoscie. Nie jest to
-- sekret zadnego srodowiska - aplikacja nie jest nigdzie wdrozona, a poza profilem
-- demo haslo i tak pochodzi ze zmiennej DB_PASSWORD. Stawiajac to gdziekolwiek
-- poza wlasnym komputerem, zmien je.

-- --- Rola aplikacyjna --------------------------------------------------------
-- Rola powstaje, jesli jej nie ma, a haslo ustawiane jest zawsze. Bez tego drugiego
-- kroku skrypt puszczony na maszynie, gdzie rola juz istnieje z innym haslem,
-- zostawilby konfiguracje niezgodna z dokumentacja.

SELECT 'CREATE ROLE ankietor LOGIN'
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'ankietor')\gexec

ALTER ROLE ankietor WITH LOGIN PASSWORD 'ankietor-demo';

-- --- Bazy ---------------------------------------------------------------------
-- ankietor      - aplikacja
-- ankietor_test - testy (mvn verify). Osobna, bo suita czysci schemat miedzy
--                 uruchomieniami i nie moze ruszac danych, na ktore patrzysz w UI.
--
-- Rola aplikacyjna celowo nie dostaje uprawnienia CREATEDB, wiec obie bazy zaklada
-- ten skrypt, uruchamiany kontem o wyzszych uprawnieniach.

SELECT 'CREATE DATABASE ankietor OWNER ankietor'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ankietor')\gexec

SELECT 'CREATE DATABASE ankietor_test OWNER ankietor'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ankietor_test')\gexec

-- --- Rozszerzenia -------------------------------------------------------------
-- pg_trgm napedza dopasowanie pytan, unaccent normalizuje polskie znaki.
-- Migracja V1 tworzy je z IF NOT EXISTS, wiec zalozenie ich tutaj tylko
-- oszczedza klopotu, gdyby rola nie miala do tego prawa.

\connect ankietor
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;
GRANT ALL ON SCHEMA public TO ankietor;

\connect ankietor_test
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;
GRANT ALL ON SCHEMA public TO ankietor;

\echo ''
\echo 'Gotowe. Bazy ankietor i ankietor_test oraz rola ankietor sa przygotowane.'
\echo ''
\echo 'Uruchomienie aplikacji:'
\echo '  mvn spring-boot:run -Dspring-boot.run.profiles=demo'
\echo ''
\echo 'Uruchomienie testow (Linux/macOS):'
\echo '  TEST_DB_URL=jdbc:postgresql://localhost:5432/ankietor_test \'
\echo '  TEST_DB_USER=ankietor TEST_DB_PASSWORD=ankietor-demo mvn clean verify'
\echo ''
