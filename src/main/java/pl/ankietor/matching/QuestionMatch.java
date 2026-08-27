package pl.ankietor.matching;

/**
 * Jedno dopasowanie zwrocone przez {@link QuestionMatcher}.
 *
 * @param similarity znormalizowana miara podobienstwa w zakresie 0..1.
 *                   Kazda implementacja musi zwracac te sama skale - to jest
 *                   warunek wymiennosci trybow.
 */
public record QuestionMatch(
        Long id,
        String question,
        String answer,
        double similarity
) {
    /** Podobienstwo jako procent, do prezentacji w widoku. */
    public int similarityPercent() {
        return (int) Math.round(similarity * 100);
    }
}
