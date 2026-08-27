-- MIGRACJA OPCJONALNA - NIE JEST STOSOWANA DOMYSLNIE.
--
-- Lezy poza classpath:db/migration, wiec Flyway jej nie widzi przy normalnym
-- starcie aplikacji. To celowe: schemat podstawowy nie moze zalezec od pgvector,
-- zeby aplikacja stawiala sie na dowolnym Postgresie.
--
-- Jak zastosowac, przy wlaczaniu trybu semantycznego:
--   spring.flyway.locations=classpath:db/migration,classpath:db/semantic
--
-- Wymagania:
--   - rozszerzenie pgvector dostepne na serwerze
--   - liczba wymiarow zgodna z EmbeddingProvider.dimensions()
--
-- Numer 100 zostawia miejsce na migracje schematu podstawowego (V2, V3, ...)
-- bez kolizji porzadku.

CREATE EXTENSION IF NOT EXISTS vector;

-- 1536 wymiarow odpowiada typowym modelom embeddingowym. Przy zmianie modelu
-- na inna liczbe wymiarow potrzebna jest nowa migracja - pgvector nie pozwala
-- zmienic wymiarowosci istniejacej kolumny w miejscu.
ALTER TABLE question_answers
    ADD COLUMN IF NOT EXISTS embedding vector(1536);

COMMENT ON COLUMN question_answers.embedding IS
    'Wektor pytania dla trybu semantycznego. NULL oznacza pare jeszcze nieprzetworzona.';

-- HNSW pod odleglosc kosinusowa - operator <=> uzywany przez SemanticQuestionMatcher.
CREATE INDEX IF NOT EXISTS idx_question_answers_embedding_cos
    ON question_answers USING hnsw (embedding vector_cosine_ops);
