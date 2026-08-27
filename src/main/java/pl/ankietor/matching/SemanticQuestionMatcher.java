package pl.ankietor.matching;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Dopasowanie semantyczne: embedding pytania i podobienstwo kosinusowe w pgvector.
 *
 * Klasa istnieje i jest kompletna, ale nie jest aktywna w MVP. Wlaczenie wymaga
 * trzech rzeczy naraz:
 *   1. ankietor.matching.mode = semantic
 *   2. beana {@link EmbeddingProvider} w kontekscie
 *   3. zastosowania opcjonalnej migracji dodajacej rozszerzenie pgvector,
 *      kolumne embedding i indeks podobienstwa
 *
 * Brak punktu 2 zatrzyma start aplikacji z jasnym komunikatem, zamiast cicho
 * degradowac dopasowanie do trybu leksykalnego. To celowe - milcząca degradacja
 * bylaby gorsza od bledu.
 */
@Component
@ConditionalOnProperty(name = "ankietor.matching.mode", havingValue = "semantic")
public class SemanticQuestionMatcher implements QuestionMatcher {

    /**
     * Operator <=> to odleglosc kosinusowa pgvector (0 = identyczne).
     * Zamieniamy ja na podobienstwo, zeby zachowac wspolna skale 0..1
     * wymagana kontraktem {@link QuestionMatcher}.
     */
    private static final String MATCH_SQL = """
            SELECT id,
                   question,
                   answer,
                   1 - (embedding <=> CAST(? AS vector)) AS sim
            FROM question_answers
            WHERE embedding IS NOT NULL
              AND 1 - (embedding <=> CAST(? AS vector)) >= ?
            ORDER BY embedding <=> CAST(? AS vector)
            LIMIT ?
            """;

    private final JdbcClient jdbc;
    private final EmbeddingProvider embeddings;

    public SemanticQuestionMatcher(JdbcClient jdbc, EmbeddingProvider embeddings) {
        this.jdbc = jdbc;
        this.embeddings = embeddings;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionMatch> findSimilar(String question, int limit, double minSimilarity) {
        if (question == null || question.isBlank()) {
            return List.of();
        }

        String vector = toVectorLiteral(embeddings.embed(question.trim()));

        return jdbc.sql(MATCH_SQL)
                .param(1, vector)
                .param(2, vector)
                .param(3, minSimilarity)
                .param(4, vector)
                .param(5, limit)
                .query((rs, rowNum) -> new QuestionMatch(
                        rs.getLong("id"),
                        rs.getString("question"),
                        rs.getString("answer"),
                        rs.getDouble("sim")))
                .list();
    }

    @Override
    public String mode() {
        return "semantic (" + embeddings.modelName() + ")";
    }

    /** pgvector przyjmuje wektor jako tekst w postaci [1.0,2.0,3.0]. */
    private static String toVectorLiteral(float[] v) {
        StringBuilder sb = new StringBuilder(v.length * 8 + 2).append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }
}
