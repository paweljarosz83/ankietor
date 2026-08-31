package pl.ankietor.knowledge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test glownego przeplywu z perspektywy uzytkownika - wymaganie certyfikacji nr 5.
 *
 * Zadanie przechodzi przez pelny kontekst aplikacji: uwierzytelnienie, kontroler,
 * warstwa serwisowa, baza i renderowanie widoku. Sprawdza ZACHOWANIE, nie implementacje.
 *
 * Klasa jest transakcyjna, wiec wiersze dodane przez test sa wycofywane i nie
 * zanieczyszczaja danych startowych dla pozostalych testow.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Glowny przeplyw uzytkownika")
class MainFlowE2ETest {

    @Autowired
    private MockMvc mvc;

    // ---------- kontrola dostepu: wymaganie nr 1 ----------

    @Test
    @DisplayName("niezalogowany uzytkownik jest odsylany do logowania")
    void bezLogowaniaBrakDostepu() throws Exception {
        mvc.perform(get("/szukaj"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")));

        mvc.perform(get("/baza-wiedzy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")));
    }

    @Test
    @DisplayName("strona logowania jest dostepna bez uwierzytelnienia")
    void logowanieDostepne() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Zaloguj")));
    }

    // ---------- glowny przeplyw ----------

    @Test
    @WithUserDetails("admin")
    @DisplayName("wklejenie pytania zwraca propozycje z wynikiem podobienstwa")
    void wklejeniePytaniaZwracaPropozycje() throws Exception {
        mvc.perform(get("/szukaj").param("pytanie", "Czy posiadacie aktualne certyfikaty ISO 9001?"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ISO 9001")))
                .andExpect(content().string(containsString("Zapisz do ankiety")));
    }

    @Test
    @WithUserDetails("admin")
    @DisplayName("pytanie bez odpowiednika daje komunikat, a nie pusta liste")
    void brakOdpowiednikaDajeKomunikat() throws Exception {
        mvc.perform(get("/szukaj").param("pytanie", "Jaka jest srednia temperatura wrzenia azotu"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Brak dopasowania powyzej progu")))
                .andExpect(content().string(not(containsString("Zapisz do ankiety"))));
    }

    @Test
    @WithUserDetails("admin")
    @DisplayName("A3: zapisana para jest natychmiast widoczna jako propozycja")
    void a3DomknieciePetli() throws Exception {
        String nowePytanie = "Czy stosuja Panstwo znakowanie identyfikacyjne partii wyrobu?";
        String nowaOdpowiedz = "Tak, kazda partia otrzymuje unikalny numer identyfikacyjny "
                + "umozliwiajacy pelna identyfikowalnosc wsteczna.";

        // przed zapisem: parafraza nie ma czego znalezc
        mvc.perform(get("/szukaj").param("pytanie", "znakowanie identyfikacyjne partii"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("identyfikowalnosc wsteczna"))));

        // zapis nowej pary
        mvc.perform(post("/baza-wiedzy")
                        .with(csrf())
                        .param("question", nowePytanie)
                        .param("answer", nowaOdpowiedz))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/baza-wiedzy"));

        // po zapisie: ta sama parafraza znajduje nowa pare
        mvc.perform(get("/szukaj").param("pytanie", "znakowanie identyfikacyjne partii"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("identyfikowalnosc wsteczna")));
    }

    // ---------- CRUD: wymaganie nr 2 ----------

    @Test
    @WithUserDetails("admin")
    @DisplayName("walidacja odrzuca zbyt krotkie pytanie i nie zapisuje")
    void walidacjaOdrzucaKrotkiePytanie() throws Exception {
        mvc.perform(post("/baza-wiedzy")
                        .with(csrf())
                        .param("question", "abc")
                        .param("answer", "Poprawna odpowiedz o wystarczajacej dlugosci."))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Formularz zawiera bledy")));
    }

    @Test
    @WithUserDetails("admin")
    @DisplayName("lista bazy wiedzy pokazuje dane startowe")
    void listaPokazujeDaneStartowe() throws Exception {
        mvc.perform(get("/baza-wiedzy"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Liczba par pytanie-odpowiedz")))
                .andExpect(content().string(containsString("ISO 9001")));
    }
}
