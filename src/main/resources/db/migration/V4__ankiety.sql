-- Ankiety klienckie jako zasob prywatny uzytkownika.
--
-- Rozne od question_answers: baza wiedzy jest wspolna dla zespolu, a ankieta nalezy
-- do konkretnej osoby, ktora ja wypelnia. Inny zalogowany uzytkownik nie widzi cudzych
-- ankiet - wlasciciel jest wymagany (NOT NULL), inaczej niz autor pary w bazie wiedzy.

CREATE TABLE ankiety (
    id          BIGSERIAL   PRIMARY KEY,
    nazwa       VARCHAR(200) NOT NULL,
    klient      VARCHAR(200),
    owner_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ
);

CREATE INDEX idx_ankiety_owner ON ankiety (owner_id, created_at DESC);

-- Pozycja ankiety: pytanie klienta wraz z odpowiedzia, ktora uzytkownik wybral.
--
-- source_id wskazuje pare z bazy wiedzy, z ktorej odpowiedz zostala zaproponowana.
-- ON DELETE SET NULL, bo usuniecie pary z bazy wiedzy nie moze skasowac historii
-- tego, co zostalo wyslane klientowi.
CREATE TABLE ankieta_pozycje (
    id          BIGSERIAL   PRIMARY KEY,
    ankieta_id  BIGINT      NOT NULL REFERENCES ankiety (id) ON DELETE CASCADE,
    pytanie     TEXT        NOT NULL,
    odpowiedz   TEXT        NOT NULL,
    source_id   BIGINT      REFERENCES question_answers (id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ankieta_pozycje_ankieta ON ankieta_pozycje (ankieta_id, created_at);
