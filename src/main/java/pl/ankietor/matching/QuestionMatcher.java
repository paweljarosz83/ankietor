package pl.ankietor.matching;

import java.util.List;

/**
 * Kontrakt dopasowania pytan - rdzen logiki biznesowej aplikacji.
 *
 * Implementacje sa wymienne i wybierane wlasciwoscia ankietor.matching.mode.
 * Kazda musi zwracac ten sam kszalt wyniku i te sama skale podobienstwa,
 * inaczej przelaczenie trybu przestaje byc przezroczyste dla reszty aplikacji.
 */
public interface QuestionMatcher {

    /**
     * Znajduje wczesniej odpowiedziane pytania dotyczace tej samej sprawy.
     *
     * @param question      tresc nowego pytania z ankiety
     * @param limit         maksymalna liczba propozycji
     * @param minSimilarity prog odciecia; wyniki ponizej nie sa zwracane
     * @return dopasowania uszeregowane malejaco po podobienstwie, nigdy null
     */
    List<QuestionMatch> findSimilar(String question, int limit, double minSimilarity);

    /** Nazwa aktywnego trybu - prezentowana w interfejsie i uzywana w testach. */
    String mode();
}
