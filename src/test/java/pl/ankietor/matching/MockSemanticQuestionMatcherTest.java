package pl.ankietor.matching;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy mockupu warstwy semantycznej.
 *
 * Sens tych testów jest inny niż zwykle: nie sprawdzają, że mockup jest dobry.
 * Sprawdzają, że **domyka luke, ktora tryb leksykalny zostawia** — czyli że ekran
 * porownania pokazuje realna roznice, a nie efekt przypadku.
 *
 * Kazdy przypadek B1 z ponizszych to zapytanie, ktore w
 * {@link QuestionMatcherTest} nie znajduje wlasciwej pary.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Mockup warstwy semantycznej")
class MockSemanticQuestionMatcherTest {

    private static final int LIMIT = 5;
    private static final double PROG = 0.52;

    @Autowired
    private MockSemanticQuestionMatcher mockup;

    @Autowired
    private QuestionMatcher aktywny;

    @Test
    @DisplayName("jawnie deklaruje, ze jest mockupem")
    void deklarujeSieJakoMockup() {
        assertThat(mockup.mode())
                .as("tryb musi byc rozpoznawalny jako mockup, zeby widok nie podawal go za AI")
                .contains("mockup");
    }

    @Test
    @DisplayName("B1: znajduje ISO 9001 dla zapytania o systemy zarzadzania jakoscia")
    void b1SystemyZarzadzaniaJakoscia() {
        String zapytanie = "Prosze wskazac wdrozone systemy zarzadzania jakoscia";

        assertThat(pytania(mockup.findSimilar(zapytanie, LIMIT, PROG)))
                .as("mockup powinien domknac luke trybu leksykalnego")
                .anySatisfy(q -> assertThat(q).contains("ISO 9001"));

        assertThat(pytania(aktywny.findSimilar(zapytanie, LIMIT, PROG)))
                .as("tryb leksykalny tej pary nie znajduje - to jest cala roznica")
                .noneSatisfy(q -> assertThat(q).contains("ISO 9001"));
    }

    @Test
    @DisplayName("B1: znajduje BHP dla zapytania o dobrostan zatrudnionych")
    void b1DobrostanZatrudnionych() {
        List<QuestionMatch> wynik = mockup.findSimilar(
                "Czy Panstwa organizacja dba o dobrostan zatrudnionych osob?", LIMIT, PROG);

        assertThat(pytania(wynik))
                .anySatisfy(q -> assertThat(q).containsAnyOf("BHP", "45001", "wypadk"));
    }

    @Test
    @DisplayName("B1: znajduje ubezpieczenia dla zapytania o zabezpieczenie mienia")
    void b1ZabezpieczenieMienia() {
        List<QuestionMatch> wynik = mockup.findSimilar(
                "Jak zabezpieczaja Panstwo swoje mienie przed skutkami zdarzen losowych?",
                LIMIT, PROG);

        assertThat(wynik).isNotEmpty();
        assertThat(pytania(wynik))
                .anySatisfy(q -> assertThat(q).containsAnyOf("ubezpieczenie", "polis", "cywilnej"));
    }

    @Test
    @DisplayName("zapytanie poza slownikiem nie zwraca nic")
    void pozaSlownikiemBrakWynikow() {
        assertThat(mockup.findSimilar(
                "Jaka jest srednia temperatura wrzenia azotu", LIMIT, PROG)).isEmpty();
        assertThat(mockup.findSimilar(
                "Czy oferujecie dostawy na teren Australii w niedziele?", LIMIT, PROG)).isEmpty();
    }

    @Test
    @DisplayName("puste zapytanie nie wywala sie")
    void pusteZapytanie() {
        assertThat(mockup.findSimilar(null, LIMIT, PROG)).isEmpty();
        assertThat(mockup.findSimilar("   ", LIMIT, PROG)).isEmpty();
    }

    @Test
    @DisplayName("respektuje limit i prog")
    void limitIProg() {
        assertThat(mockup.findSimilar("iso 9001", 2, PROG)).hasSizeLessThanOrEqualTo(2);

        assertThat(mockup.findSimilar("iso 9001", LIMIT, 0.95))
                .as("umowny wynik 0.80 jest ponizej progu 0.95, wiec nic nie wraca")
                .isEmpty();
    }

    private static List<String> pytania(List<QuestionMatch> wynik) {
        return wynik.stream().map(QuestionMatch::question).toList();
    }
}
