package pl.ankietor.surveys;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pl.ankietor.security.models.Role;
import pl.ankietor.security.models.User;
import pl.ankietor.security.repos.RoleRepository;
import pl.ankietor.security.repos.UserRepository;
import pl.ankietor.surveys.models.Ankieta;
import pl.ankietor.surveys.repos.AnkietaRepository;
import pl.ankietor.surveys.services.AnkietaService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Poziomy uprawnien dla ankiet — reguly opisane w prd.md §6b.
 *
 * Ankieta jest zasobem PRYWATNYM, w odroznieniu od wspolnej bazy wiedzy. Te testy
 * pilnuja trzech granic:
 *   1. uzytkownik nie widzi cudzej ankiety ani na liscie, ani po wpisaniu jej adresu,
 *   2. administrator ma wglad we wszystkie ankiety,
 *   3. administrator mimo wgladu NIE moze cudzej ankiety modyfikowac.
 *
 * Punkt 3 jest tu najwazniejszy: wglad i prawo zapisu to dwa rozne uprawnienia,
 * a nie jedno „admin moze wszystko".
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Uprawnienia do ankiet")
class AnkietaPermissionsTest {

    private static final String ANNA = "anna-ankieter";
    private static final String BOGDAN = "bogdan-ankieter";

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AnkietaRepository ankietaRepository;
    @Autowired private AnkietaService ankietaService;
    @Autowired private PasswordEncoder passwordEncoder;

    private Long ankietaAnny;

    @BeforeEach
    void przygotujDwochUzytkownikowIAnkiete() {
        User anna = uzytkownik(ANNA);
        uzytkownik(BOGDAN);

        Ankieta a = ankietaService.utworz(
                "Ankieta kwalifikacyjna Kontrahent Polnoc", "Kontrahent Polnoc", anna);
        ankietaService.dodajPozycje(a.getId(),
                "Czy posiadaja Panstwo certyfikat ISO 9001?",
                "Tak, certyfikat nr QMS-2019-4471 wazny do 31.03.2027.",
                null, anna);
        ankietaAnny = a.getId();
    }

    private User uzytkownik(String login) {
        return userRepository.findByUsername(login).orElseGet(() -> {
            User u = new User(login, passwordEncoder.encode("nieistotne"));
            roleRepository.findByName(Role.USER).ifPresent(u::addRole);
            return userRepository.save(u);
        });
    }

    // ---------- 1. izolacja miedzy uzytkownikami ----------

    @Test
    @WithUserDetails(value = ANNA, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("wlascicielka widzi swoja ankiete na liscie")
    void wlascicielkaWidziSwoja() throws Exception {
        mvc.perform(get("/moje-ankiety"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Kontrahent Polnoc")));
    }

    @Test
    @WithUserDetails(value = BOGDAN, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("inny uzytkownik NIE widzi cudzej ankiety na liscie")
    void obcyNieWidziNaLiscie() throws Exception {
        mvc.perform(get("/moje-ankiety"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Kontrahent Polnoc"))))
                .andExpect(content().string(containsString("Nie masz jeszcze zadnej ankiety")));
    }

    @Test
    @WithUserDetails(value = BOGDAN, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("inny uzytkownik NIE otworzy cudzej ankiety po adresie")
    void obcyNieOtworzyPoAdresie() throws Exception {
        mvc.perform(get("/moje-ankiety/{id}", ankietaAnny))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = BOGDAN, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("inny uzytkownik NIE dopisze pozycji do cudzej ankiety")
    void obcyNieDopiszePozycji() throws Exception {
        mvc.perform(post("/moje-ankiety/zapisz-pozycje")
                        .with(csrf())
                        .param("ankietaId", String.valueOf(ankietaAnny))
                        .param("pytanie", "Podmienione pytanie")
                        .param("odpowiedz", "Podmieniona odpowiedz"))
                .andExpect(status().isForbidden());

        assertThat(ankietaRepository.findById(ankietaAnny).orElseThrow().getPozycje())
                .as("ankieta musi miec nadal jedna pozycje")
                .hasSize(1);
    }

    @Test
    @WithUserDetails(value = BOGDAN, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("inny uzytkownik NIE usunie cudzej ankiety")
    void obcyNieUsunie() throws Exception {
        mvc.perform(post("/moje-ankiety/{id}/usun", ankietaAnny).with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(ankietaRepository.findById(ankietaAnny)).isPresent();
    }

    // ---------- 2. wglad administratora ----------

    @Test
    @WithUserDetails(value = "admin", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("administrator widzi cudza ankiete po wlaczeniu widoku wszystkich")
    void adminWidziWszystkie() throws Exception {
        mvc.perform(get("/moje-ankiety").param("wszystkie", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Kontrahent Polnoc")))
                .andExpect(content().string(containsString(ANNA)));
    }

    @Test
    @WithUserDetails(value = "admin", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("administrator otworzy cudza ankiete do odczytu")
    void adminOtworzyCudza() throws Exception {
        mvc.perform(get("/moje-ankiety/{id}", ankietaAnny))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("tylko do odczytu")));
    }

    @Test
    @WithUserDetails(value = "admin", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("bez parametru wszystkie administrator widzi tylko swoje")
    void adminDomyslnieTylkoSwoje() throws Exception {
        mvc.perform(get("/moje-ankiety"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Kontrahent Polnoc"))));
    }

    // ---------- 3. wglad to nie prawo zapisu ----------

    @Test
    @WithUserDetails(value = "admin", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("administrator mimo wgladu NIE MOZE modyfikowac cudzej ankiety")
    void adminNieModyfikujeCudzej() throws Exception {
        mvc.perform(post("/moje-ankiety/zapisz-pozycje")
                        .with(csrf())
                        .param("ankietaId", String.valueOf(ankietaAnny))
                        .param("pytanie", "Dopisane przez administratora")
                        .param("odpowiedz", "Nie powinno sie zapisac"))
                .andExpect(status().isForbidden());

        assertThat(ankietaRepository.findById(ankietaAnny).orElseThrow().getPozycje()).hasSize(1);
    }

    @Test
    @WithUserDetails(value = "admin", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("administrator mimo wgladu NIE MOZE usunac cudzej ankiety")
    void adminNieUsuwaCudzej() throws Exception {
        mvc.perform(post("/moje-ankiety/{id}/usun", ankietaAnny).with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(ankietaRepository.findById(ankietaAnny)).isPresent();
    }

    // ---------- 4. glowny przeplyw: przycisk zapisu ----------

    @Test
    @WithUserDetails(value = BOGDAN, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("zapis pierwszej odpowiedzi zaklada ankiete w locie")
    void pierwszyZapisTworzyAnkiete() throws Exception {
        User bogdan = userRepository.findByUsername(BOGDAN).orElseThrow();
        assertThat(ankietaRepository.findByOwnerOrderByCreatedAtDesc(bogdan)).isEmpty();

        mvc.perform(post("/moje-ankiety/zapisz-pozycje")
                        .with(csrf())
                        .param("nazwaNowej", "Ankieta Bogdana")
                        .param("pytanie", "Jakie sa Panstwa terminy platnosci?")
                        .param("odpowiedz", "Standardowo 30 dni od daty wystawienia faktury."))
                .andExpect(status().is3xxRedirection());

        var ankiety = ankietaRepository.findByOwnerOrderByCreatedAtDesc(bogdan);
        assertThat(ankiety).hasSize(1);
        assertThat(ankiety.get(0).getNazwa()).isEqualTo("Ankieta Bogdana");
        assertThat(ankiety.get(0).getPozycje()).hasSize(1);
    }

    @Test
    @WithUserDetails(value = ANNA, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("wlascicielka dopisuje kolejna pozycje do swojej ankiety")
    void wlascicielkaDopisuje() throws Exception {
        mvc.perform(post("/moje-ankiety/zapisz-pozycje")
                        .with(csrf())
                        .param("ankietaId", String.valueOf(ankietaAnny))
                        .param("pytanie", "Czy posiadaja Panstwo ubezpieczenie OC?")
                        .param("odpowiedz", "Tak, polisa nr OC-4471-2026."))
                .andExpect(status().is3xxRedirection());

        assertThat(ankietaRepository.findById(ankietaAnny).orElseThrow().getPozycje()).hasSize(2);
    }

    @Test
    @WithUserDetails(value = ANNA, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("ekran dopasowania pokazuje ankiety zalogowanego uzytkownika")
    void ekranDopasowaniaPokazujeAnkiety() throws Exception {
        mvc.perform(get("/szukaj").param("pytanie", "certyfikat ISO 9001"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Zapisz do ankiety")))
                .andExpect(content().string(containsString("Kontrahent Polnoc")));
    }
}
