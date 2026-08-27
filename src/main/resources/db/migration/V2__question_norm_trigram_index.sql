-- Znormalizowana postac pytania pod dopasowanie leksykalne.
--
-- Problem: unaccent() nie jest IMMUTABLE w standardowej instalacji, bo jednoargumentowa
-- wersja zalezy od biezacej konfiguracji slownika. Postgres nie pozwala uzyc funkcji
-- niedeterministycznej ani w wyrazeniu indeksu, ani w kolumnie generowanej.
--
-- Rozwiazanie: opakowanie dwuargumentowej wersji unaccent(), ktora dostaje slownik
-- wprost, wiec jest deterministyczna i mozna ja bezpiecznie oznaczyc jako IMMUTABLE.
-- To standardowy wzorzec, nie obejscie.

CREATE OR REPLACE FUNCTION immutable_unaccent(text)
    RETURNS text
    LANGUAGE sql
    IMMUTABLE
    PARALLEL SAFE
    STRICT
AS $$
SELECT public.unaccent('public.unaccent', $1)
$$;

COMMENT ON FUNCTION immutable_unaccent(text) IS
    'Deterministyczny wariant unaccent() - wymagany w kolumnach generowanych i indeksach.';

-- Kolumna generowana: male litery bez znakow diakrytycznych.
-- STORED, zeby indeks trigramowy mial na czym pracowac.
ALTER TABLE question_answers
    ADD COLUMN question_norm text
        GENERATED ALWAYS AS (immutable_unaccent(lower(question))) STORED;

-- Nowy indeks pracuje na kolumnie znormalizowanej. Stary, zalozony w V1 na surowej
-- kolumnie question, przestaje byc uzywany przez zapytania dopasowania.
CREATE INDEX idx_question_answers_norm_trgm
    ON question_answers USING GIN (question_norm gin_trgm_ops);

DROP INDEX IF EXISTS idx_question_answers_question_trgm;
