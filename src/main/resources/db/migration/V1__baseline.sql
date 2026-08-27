-- Ankietor - schemat podstawowy.
-- Swiadomie bez zaleznosci od pgvector: tryb semantyczny dodaje osobna,
-- opcjonalna migracja. Dzieki temu aplikacja stawia sie na dowolnym Postgresie.

CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- --- Uzytkownicy i role ------------------------------------------------------

CREATE TABLE roles (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE users_roles (
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- --- Baza wiedzy: pary pytanie -> odpowiedz ----------------------------------

CREATE TABLE question_answers (
    id         BIGSERIAL   PRIMARY KEY,
    question   TEXT        NOT NULL,
    answer     TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    author_id  BIGINT      REFERENCES users (id) ON DELETE SET NULL
);

-- Indeks trigramowy pod dopasowanie leksykalne.
-- unaccent nie jest IMMUTABLE w standardowej instalacji, wiec normalizacja
-- znakow diakrytycznych odbywa sie w zapytaniu, nie w indeksie.
CREATE INDEX idx_question_answers_question_trgm
    ON question_answers USING GIN (question gin_trgm_ops);

CREATE INDEX idx_question_answers_created_at
    ON question_answers (created_at DESC);

-- --- Dane startowe: role ------------------------------------------------------

INSERT INTO roles (name) VALUES ('ROLE_USER'), ('ROLE_ADMIN');
