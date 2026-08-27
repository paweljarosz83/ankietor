package pl.ankietor.matching;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy logiki biznesowej - kryteria sukcesu A1, A2 i B1 z context/foundation/prd.md.
 *
 * Dane pochodza z migracji V3 (45 syntetycznych par), wiec testy nie zaleza od
 * kolejnosci wykonania ani od danych wprowadzonych recznie.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Dopasowanie pytan")
class QuestionMatcherTest {

    private static final int LIMIT = 5;
    private static final double PROG = 0.52;

    @Autowired
    private QuestionMatcher matcher;

    @Autowired
    private MatchingProperties properties;

    @Test
    @DisplayName("aktywny jest tryb leksykalny")
    void trybLeksykalny() {
        assertThat(properties.getMode()).isEqualTo("lexical");
        assertThat(matcher.mode()).isEqualTo("lexical");
    }

    // ---------- A1: odmiana i parafraza z pokryciem slownictwa ----------

    @Test
    @DisplayName("A1: inaczej sformulowane pytanie o ISO 9001 trafia do top 3")
    void a1CertyfikatIso9001() {
        List<QuestionMatch> wynik = matcher.findSimilar(
                "Czy posiadacie aktualne certyfikaty ISO 9001?", LIMIT, PROG);

        assertThat(top3Pytania(wynik))
                .as("wlasciwa para powinna byc w top 3")
                .anySatisfy(q -> assertThat(q).contains("ISO 9001"));
    }

    @Test
    @DisplayName("A1: pytanie o polise OC mimo innej kolejnosci slow")
    void a1PolisaOc() {
        List<QuestionMatch> wynik = matcher.findSimilar(
                "jaki jest numer polisy oc i suma gwarancyjna", LIMIT, PROG);

        assertThat(top3Pytania(wynik))
                .anySatisfy(q -> assertThat(q).containsIgnoringCase("polisy OC"));
    }

    @Test
    @DisplayName("A1: odmiana i skrocona forma pytania o dane rejestrowe")
    void a1DaneRejestrowe() {
        List<QuestionMatch> wynik = matcher.findSimilar(
                "Numer KRS i NIP spolki prosze", LIMIT, PROG);

        assertThat(top3Pytania(wynik))
                .anySatisfy(q -> assertThat(q).contains("KRS"));
    }

    @Test
    @DisplayName("A1: literowka w zapytaniu nie psuje dopasowania")
    void a1TolerancjaLiterowki() {
        // "plobatnosci" zamiast "platnosci" - trigramy sa odporne na literowki
        List<QuestionMatch> wynik = matcher.findSimilar(
                "Terminy plobatnosci standardowe u Panstwa", LIMIT, PROG);

        assertThat(top3Pytania(wynik))
                .anySatisfy(q -> assertThat(q).containsIgnoringCase("terminy p"));
    }

    @Test
    @DisplayName("A1: krotkie zapytanie trafia w dluzsze pytanie w bazie")
    void a1KrotkieZapytanie() {
        // Powod uzycia word_similarity zamiast similarity: dla tego przypadku
        // similarity dawalo 18 procent, czyli ponizej kazdego sensownego progu.
        List<QuestionMatch> wynik = matcher.findSimilar("ISO 9001", LIMIT, PROG);

        assertThat(wynik).isNotEmpty();
        assertThat(top3Pytania(wynik))
                .anySatisfy(q -> assertThat(q).contains("ISO 9001"));
    }

    // ---------- A2: brak falszywych propozycji ----------

    @Test
    @DisplayName("A2: pytanie bez odpowiednika nie zwraca nic")
    void a2BrakOdpowiednika() {
        assertThat(matcher.findSimilar(
                "Jaka jest srednia temperatura wrzenia azotu", LIMIT, PROG)).isEmpty();

        assertThat(matcher.findSimilar(
                "Czy oferujecie dostawy na teren Australii w niedziele?", LIMIT, PROG)).isEmpty();
    }

    @Test
    @DisplayName("A2: puste i biale zapytanie nie wywala sie i nie zwraca nic")
    void a2PusteZapytanie() {
        assertThat(matcher.findSimilar(null, LIMIT, PROG)).isEmpty();
        assertThat(matcher.findSimilar("", LIMIT, PROG)).isEmpty();
        assertThat(matcher.findSimilar("   \n  ", LIMIT, PROG)).isEmpty();
    }

    @Test
    @DisplayName("A2: wyniki sa uszeregowane malejaco i mieszcza sie w limicie")
    void a2RankingILimit() {
        List<QuestionMatch> wynik = matcher.findSimilar("certyfikat", 3, 0.30);

        assertThat(wynik).hasSizeLessThanOrEqualTo(3);
        assertThat(wynik).isSortedAccordingTo(
                (a, b) -> Double.compare(b.similarity(), a.similarity()));
        assertThat(wynik).allSatisfy(m -> {
            assertThat(m.similarity()).isBetween(0.0, 1.0);
            assertThat(m.similarityPercent()).isBetween(0, 100);
        });
    }

    // ---------- B1: granica trybu leksykalnego ----------

    @Test
    @DisplayName("B1: brak pokrycia slownictwa - tryb leksykalny NIE znajduje wlasciwej pary")
    void b1GranicaTrybuLeksykalnego() {
        // "ISO 9001" i "systemy zarzadzania jakoscia" to ta sama sprawa merytorycznie,
        // ale zerowe pokrycie slownictwa. To udokumentowane ograniczenie trybu
        // leksykalnego i powod istnienia trybu semantycznego.
        //
        // Test celowo sprawdza, ze para NIE jest znajdowana. Gdyby zaczela sie
        // pojawiac, znaczyloby to, ze prog jest za niski albo dane startowe sie
        // zmienily - jedno i drugie warto wiedziec.
        List<QuestionMatch> wynik = matcher.findSimilar(
                "Prosze wskazac wdrozone systemy zarzadzania jakoscia", LIMIT, PROG);

        assertThat(top3Pytania(wynik))
                .as("w trybie leksykalnym para o ISO 9001 nie powinna byc znaleziona")
                .noneSatisfy(q -> assertThat(q).contains("ISO 9001"));
    }

    private static List<String> top3Pytania(List<QuestionMatch> wynik) {
        return wynik.stream().limit(3).map(QuestionMatch::question).toList();
    }
}
