package pl.ankietor.matching;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tryb mieszany: wynik leksykalny jako baza, semantyczny jako warstwa podnoszaca
 * ranking. Dla pary wystepujacej w obu zbiorach brany jest wynik wyzszy.
 *
 * Nie jest aktywny w MVP. Wymaga tych samych warunkow co tryb semantyczny.
 */
@Component
@ConditionalOnProperty(name = "ankietor.matching.mode", havingValue = "hybrid")
public class HybridQuestionMatcher implements QuestionMatcher {

    private final LexicalQuestionMatcher lexical;
    private final SemanticQuestionMatcher semantic;

    public HybridQuestionMatcher(LexicalQuestionMatcher lexical, SemanticQuestionMatcher semantic) {
        this.lexical = lexical;
        this.semantic = semantic;
    }

    @Override
    public List<QuestionMatch> findSimilar(String question, int limit, double minSimilarity) {
        Map<Long, QuestionMatch> best = new LinkedHashMap<>();

        List<QuestionMatch> all = new ArrayList<>();
        all.addAll(lexical.findSimilar(question, limit * 2, minSimilarity));
        all.addAll(semantic.findSimilar(question, limit * 2, minSimilarity));

        for (QuestionMatch m : all) {
            best.merge(m.id(), m,
                    (a, b) -> a.similarity() >= b.similarity() ? a : b);
        }

        return best.values().stream()
                .sorted(Comparator.comparingDouble(QuestionMatch::similarity).reversed()
                        .thenComparing(QuestionMatch::id))
                .limit(limit)
                .toList();
    }

    @Override
    public String mode() {
        return "hybrid";
    }
}
