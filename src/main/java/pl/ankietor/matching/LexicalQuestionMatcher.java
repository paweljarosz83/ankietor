package pl.ankietor.matching;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Dopasowanie leksykalne oparte na trigramach (pg_trgm).
 *
 * DLACZEGO TRIGRAMY, A NIE FULL-TEXT SEARCH
 * Postgres nie ma polskiego slownika FTS w standardowej instalacji, a wgranie
 * hunspella na hostingu managed zwykle nie jest mozliwe. Wariant 'simple' nie robi
 * stemmingu, wiec "certyfikat" i "certyfikatu" bylyby roznymi slowami. Trigramy sa
 * niezalezne od jezyka i formy odmienione dziela wiekszosc trigramow. Ubocznie daje
 * to tolerancje na literowki - zmierzone: "plobatnosci" zamiast "platnosci" nadal
 * trafia we wlasciwa pare.
 *
 * DLACZEGO word_similarity, A NIE similarity
 * similarity() normalizuje przez sume trigramow obu tekstow, wiec krotkie zapytanie
 * wobec dlugiego pytania w bazie dostaje niska ocene. Pomiar na danych startowych:
 * zapytanie "ISO 9001" wobec wlasciwej pary dawalo similarity 18 procent, czyli
 * ponizej kazdego sensownego progu. word_similarity szuka najlepiej pasujacego
 * fragmentu celu i dla tego samego przypadku daje 100 procent.
 *
 * Cena: word_similarity zwraca 100 procent dla kazdego pytania zawierajacego dane
 * slowo, wiec krotkie, ogolne zapytania generuja remisy. Dlatego similarity zostaje
 * jako drugie kryterium sortowania - wsrod remisow wyzej trafia para najbardziej
 * podobna jako calosc.
 *
 * ZNANE OGRANICZENIE, PRZYJETE SWIADOMIE
 * Brak dopasowania dla frazy bez wspolnego slownictwa, np. "ISO 9001" wobec
 * "systemy zarzadzania jakoscia". To kryterium B1 z PRD i domena trybu semantycznego.
 */
@Component
@Primary
@ConditionalOnProperty(name = "ankietor.matching.mode", havingValue = "lexical", matchIfMissing = true)
public class LexicalQuestionMatcher implements QuestionMatcher {

    /**
     * Prog operatora <%% jest ustawiany przez GUC, nie przez set_limit() - ten
     * dotyczy tylko operatora %%. set_config z is_local = true ogranicza zmiane
     * do biezacej transakcji, dlatego metoda musi byc transakcyjna.
     */
    private static final String SET_THRESHOLD_SQL =
            "SELECT set_config('pg_trgm.word_similarity_threshold', ?, true)";

    /**
     * Operator <%% moze korzystac z indeksu GIN na question_norm i zaweza zbior
     * kandydatow. Jawny warunek na word_similarity zostaje, bo wynik liczbowy
     * trafia do prezentacji, a prog moze byc zaostrzony niezaleznie.
     */
    private static final String MATCH_SQL = """
            SELECT qa.id,
                   qa.question,
                   qa.answer,
                   word_similarity(k.q, qa.question_norm) AS sim,
                   similarity(qa.question_norm, k.q)      AS sim_full
            FROM question_answers qa,
                 (SELECT immutable_unaccent(lower(?)) AS q) k
            WHERE k.q <%% qa.question_norm
              AND word_similarity(k.q, qa.question_norm) >= ?
            ORDER BY sim DESC, sim_full DESC, qa.id ASC
            LIMIT ?
            """.replace("<%%", "<%");

    private final JdbcClient jdbc;

    public LexicalQuestionMatcher(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionMatch> findSimilar(String question, int limit, double minSimilarity) {
        if (question == null || question.isBlank()) {
            return List.of();
        }

        String q = question.trim();

        jdbc.sql(SET_THRESHOLD_SQL)
                .param(1, Double.toString(minSimilarity))
                .query(String.class)
                .single();

        return jdbc.sql(MATCH_SQL)
                .param(1, q)
                .param(2, minSimilarity)
                .param(3, limit)
                .query((rs, rowNum) -> new QuestionMatch(
                        rs.getLong("id"),
                        rs.getString("question"),
                        rs.getString("answer"),
                        rs.getDouble("sim")))
                .list();
    }

    @Override
    public String mode() {
        return "lexical";
    }
}
