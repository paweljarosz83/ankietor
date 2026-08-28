package pl.ankietor.matching;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.ankietor.knowledge.models.QuestionAnswer;
import pl.ankietor.knowledge.repos.QuestionAnswerRepository;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MOCKUP trybu semantycznego - slownik pojec zamiast embeddingow.
 *
 * PO CO ISTNIEJE
 * Ekran porownania trybow ma pokazac, czego dopasowanie leksykalne nie potrafi.
 * Prawdziwy tryb semantyczny wymaga dostawcy embeddingow, a wiec zewnetrznej uslugi,
 * klucza API i zgody na wysylanie tresci pytan poza infrastrukture. Ten mockup daje
 * ten sam efekt demonstracyjny bez zadnej z tych rzeczy.
 *
 * CZYM NIE JEST
 * To nie sa embeddingi i nie jest to AI. Nie generuje tez zmyslonych wynikow -
 * dopasowuje po recznie zapisanym slowniku pojec, wiec kazdy wynik jest prawdziwym
 * trafieniem w istniejaca pare. Wynik podobienstwa jest natomiast **umowny**
 * i zawsze taki sam, bo slownik nie ma pojecia stopnia podobienstwa. Dlatego
 * {@link #mode()} zwraca jawna informacje, ze to mockup, a ekran porownania
 * oznacza te kolumne ostrzezeniem.
 *
 * DOCELOWO
 * Miejsce, w ktore wejdzie {@link EmbeddingProvider} i
 * {@link SemanticQuestionMatcher}. Kontrakt {@link QuestionMatcher} jest ten sam,
 * wiec podmiana nie dotknie ekranu porownania ani zadnego kontrolera.
 */
@Component
public class MockSemanticQuestionMatcher implements QuestionMatcher {

    /** Umowny wynik dla trafienia ze slownika - slownik nie mierzy stopnia podobienstwa. */
    private static final double UMOWNY_WYNIK = 0.80;

    /**
     * Pojecie -> warianty jezykowe, ktore w domenie ankiet klienckich znacza to samo.
     * Dopasowanie dziala krzyzowo: jesli zapytanie trafia w jeden wariant, szukamy
     * par zawierajacych dowolny **inny** wariant tego samego pojecia.
     *
     * Klucze i warianty bez znakow diakrytycznych - normalizacja w
     * {@link #normalizuj(String)} usuwa je z obu stron.
     */
    private static final Map<String, List<String>> SLOWNIK_POJEC = new LinkedHashMap<>();

    static {
        SLOWNIK_POJEC.put("zarzadzanie jakoscia", List.of(
                "iso 9001", "zarzadzania jakoscia", "systemy zarzadzania jakoscia",
                "system jakosci", "audyty wewnetrzne systemu jakosci"));

        SLOWNIK_POJEC.put("srodowisko", List.of(
                "iso 14001", "srodowiskow", "sladu weglowego", "sladem weglowym",
                "odpadami produkcyjnymi", "odpady"));

        SLOWNIK_POJEC.put("bezpieczenstwo pracy", List.of(
                "iso 45001", "bhp", "bezpieczenstwem i higiena pracy",
                "dobrostan zatrudnionych", "wypadkow przy pracy", "wypadki"));

        SLOWNIK_POJEC.put("ubezpieczenia", List.of(
                "odpowiedzialnosci cywilnej", "polisy oc", "polise", "ubezpieczenie",
                "mienie", "zdarzen losowych", "cargo", "suma gwarancyjna",
                "utraty zysku"));

        SLOWNIK_POJEC.put("dane rejestrowe", List.of(
                "krs", "nip", "regon", "kapitalu zakladowego", "forma prawna",
                "reprezentowania spolki"));

        SLOWNIK_POJEC.put("podatki", List.of(
                "podatnikiem vat", "rezydencji podatkowej", "niezaleganiu z podatkami"));

        SLOWNIK_POJEC.put("poufnosc", List.of(
                "zachowaniu poufnosci", "poufnosci", "tajemnice przedsiebiorstwa"));

        SLOWNIK_POJEC.put("dane osobowe", List.of(
                "danych osobowych", "administratorem danych", "naruszenie ochrony danych",
                "europejski obszar gospodarczy"));

        SLOWNIK_POJEC.put("zgodnosc produktowa", List.of(
                "rohs", "reach", "deklaracje zgodnosci", "swiadectwa badan",
                "substancji niebezpiecznych"));

        SLOWNIK_POJEC.put("lancuch dostaw", List.of(
                "dostawcow", "kodeks postepowania", "mineraly konfliktowe",
                "minerałow konfliktowych", "ciaglosci dostaw"));

        SLOWNIK_POJEC.put("warunki handlowe", List.of(
                "terminy platnosci", "platnosci", "czas realizacji zamowienia",
                "zdolnoscia produkcyjna", "walutach", "gwarancji na wyroby"));

        SLOWNIK_POJEC.put("reklamacje", List.of(
                "reklamacji", "reklamacje", "przyczyn zrodlowych"));

        SLOWNIK_POJEC.put("audyty", List.of(
                "audyt drugiej strony", "audytu klienta", "audyty klienckie"));
    }

    private final QuestionAnswerRepository repository;

    public MockSemanticQuestionMatcher(QuestionAnswerRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionMatch> findSimilar(String question, int limit, double minSimilarity) {
        if (question == null || question.isBlank()) {
            return List.of();
        }
        if (UMOWNY_WYNIK < minSimilarity) {
            return List.of();
        }

        String zapytanie = normalizuj(question);

        List<String> trafionePojecia = SLOWNIK_POJEC.entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(zapytanie::contains))
                .map(Map.Entry::getKey)
                .toList();

        if (trafionePojecia.isEmpty()) {
            return List.of();
        }

        List<QuestionMatch> wynik = new ArrayList<>();

        for (QuestionAnswer para : repository.findAll()) {
            String pytanie = normalizuj(para.getQuestion());

            boolean pasuje = trafionePojecia.stream()
                    .flatMap(pojecie -> SLOWNIK_POJEC.get(pojecie).stream())
                    // wariant, ktory juz jest w zapytaniu, nie wnosi nic nowego -
                    // szukamy trafienia przez INNE sformulowanie tego samego pojecia
                    .filter(wariant -> !zapytanie.contains(wariant))
                    .anyMatch(pytanie::contains);

            if (pasuje) {
                wynik.add(new QuestionMatch(
                        para.getId(), para.getQuestion(), para.getAnswer(), UMOWNY_WYNIK));
            }
            if (wynik.size() >= limit) {
                break;
            }
        }

        return wynik;
    }

    @Override
    public String mode() {
        return "mockup slownika pojec";
    }

    /** Male litery bez znakow diakrytycznych - odpowiednik question_norm po stronie Javy. */
    private static String normalizuj(String s) {
        return Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('ł', 'l')
                .replace('Ł', 'L');
    }
}
