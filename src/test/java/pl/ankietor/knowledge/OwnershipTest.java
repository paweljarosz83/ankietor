package pl.ankietor.knowledge;

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
import pl.ankietor.knowledge.models.QuestionAnswer;
import pl.ankietor.knowledge.repos.QuestionAnswerRepository;
import pl.ankietor.security.models.Role;
import pl.ankietor.security.models.User;
import pl.ankietor.security.repos.RoleRepository;
import pl.ankietor.security.repos.UserRepository;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ryzyko R-4 z test-plan.md: czy zalogowany uzytkownik dosiega zasobu innego uzytkownika.
 *
 * Do momentu wprowadzenia modelu wlasnosci to ryzyko nie mialo zadnego pokrycia — kazdy
 * zalogowany mogl edytowac i usuwac wszystko. Te testy pilnuja reguly opisanej
 * w prd.md §6a: odczyt wspolny, modyfikacja wlasnosciowa.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Wlasnosc par pytanie-odpowiedz")
class OwnershipTest {

    private static final String OBCY = "obcy-uzytkownik";

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private QuestionAnswerRepository questionAnswerRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Long paraAdmina;

    @BeforeEach
    void przygotujDwochUzytkownikowIPare() {
        User admin = userRepository.findByUsername("admin").orElseThrow();

        if (userRepository.findByUsername(OBCY).isEmpty()) {
            User obcy = new User(OBCY, passwordEncoder.encode("nieistotne"));
            roleRepository.findByName(Role.USER).ifPresent(obcy::addRole);
            userRepository.save(obcy);
        }

        paraAdmina = questionAnswerRepository.save(new QuestionAnswer(
                "Czy prowadza Panstwo rejestr szkolen stanowiskowych?",
                "Tak, rejestr jest prowadzony elektronicznie i archiwizowany przez 5 lat.",
                admin)).getId();
    }

    // ---------- odczyt jest wspolny ----------

    @Test
    @WithUserDetails(value = OBCY, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("obcy uzytkownik WIDZI pare innego autora — odczyt jest wspolny")
    void odczytJestWspolny() throws Exception {
        mvc.perform(get("/baza-wiedzy"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("rejestr szkolen stanowiskowych")));
    }

    @Test
    @WithUserDetails(value = OBCY, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("obcy uzytkownik moze uzyc odpowiedzi innego autora w dopasowaniu")
    void dopasowanieDzialaNaWspolnejBazie() throws Exception {
        mvc.perform(get("/szukaj").param("pytanie", "rejestr szkolen stanowiskowych"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("archiwizowany")));
    }

    // ---------- modyfikacja jest wlasnosciowa ----------

    @Test
    @WithUserDetails(value = OBCY, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("R-4: obcy uzytkownik NIE MOZE usunac pary innego autora")
    void obcyNieUsuwa() throws Exception {
        mvc.perform(post("/baza-wiedzy/{id}/usun", paraAdmina).with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(questionAnswerRepository.findById(paraAdmina))
                .as("para musi nadal istniec")
                .isPresent();
    }

    @Test
    @WithUserDetails(value = OBCY, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("R-4: obcy uzytkownik NIE MOZE otworzyc formularza edycji pary innego autora")
    void obcyNieEdytuje() throws Exception {
        mvc.perform(get("/baza-wiedzy/{id}/edycja", paraAdmina))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = OBCY, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("R-4: obcy uzytkownik NIE MOZE nadpisac pary innego autora przez POST")
    void obcyNieNadpisujePrzezPost() throws Exception {
        mvc.perform(post("/baza-wiedzy")
                        .with(csrf())
                        .param("id", String.valueOf(paraAdmina))
                        .param("question", "Podmienione pytanie o zupelnie czym innym")
                        .param("answer", "Podmieniona odpowiedz, ktora nie powinna sie zapisac."))
                .andExpect(status().isForbidden());

        assertThat(questionAnswerRepository.findById(paraAdmina).orElseThrow().getQuestion())
                .as("tresc pary nie moze sie zmienic")
                .contains("rejestr szkolen stanowiskowych");
    }

    @Test
    @WithUserDetails(value = OBCY, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("widok ukrywa przyciski edycji dla pary, ktorej uzytkownik nie jest autorem")
    void widokUkrywaPrzyciski() throws Exception {
        mvc.perform(get("/baza-wiedzy"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(
                        "/baza-wiedzy/" + paraAdmina + "/edycja"))));
    }

    // ---------- autor i administrator moga ----------

    @Test
    @WithUserDetails(value = "admin", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("autor MOZE edytowac swoja pare")
    void autorMozeEdytowac() throws Exception {
        mvc.perform(get("/baza-wiedzy/{id}/edycja", paraAdmina))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "admin", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("administrator MOZE modyfikowac pare systemowa bez autora")
    void adminMozeModyfikowacParySystemowe() throws Exception {
        Long systemowa = questionAnswerRepository.findAll().stream()
                .filter(qa -> qa.getAuthor() == null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Dane startowe z migracji V3 powinny miec author_id NULL"))
                .getId();

        mvc.perform(get("/baza-wiedzy/{id}/edycja", systemowa))
                .andExpect(status().isOk());
    }

    // ---------- widok "moje pary" ----------

    @Test
    @WithUserDetails(value = OBCY, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("widok moje pary pokazuje wylacznie zasoby przypisane do uzytkownika")
    void mojeParyPokazujaTylkoWlasne() throws Exception {
        mvc.perform(get("/baza-wiedzy").param("moje", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("rejestr szkolen stanowiskowych"))))
                .andExpect(content().string(containsString("Nie dodałeś jeszcze żadnej pary")));
    }

    @Test
    @WithUserDetails(value = "admin", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("widok moje pary pokazuje pare autora")
    void mojeParyPokazujaWlasnaPare() throws Exception {
        mvc.perform(get("/baza-wiedzy").param("moje", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("rejestr szkolen stanowiskowych")));
    }
}
