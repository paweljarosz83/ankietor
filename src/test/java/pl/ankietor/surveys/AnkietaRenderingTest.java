package pl.ankietor.surveys;

import org.junit.jupiter.api.AfterEach;
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
import pl.ankietor.security.models.Role;
import pl.ankietor.security.models.User;
import pl.ankietor.security.repos.RoleRepository;
import pl.ankietor.security.repos.UserRepository;
import pl.ankietor.surveys.models.Ankieta;
import pl.ankietor.surveys.repos.AnkietaRepository;
import pl.ankietor.surveys.services.AnkietaService;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Renderowanie widokow ankiet BEZ transakcji testowej.
 *
 * Ta klasa celowo NIE ma @Transactional, w odroznieniu od reszty testow. Powod jest
 * konkretny: aplikacja dziala z open-in-view = false, wiec sesja Hibernate jest zamknieta
 * zanim szablon zacznie sie renderowac. Test transakcyjny trzyma sesje otwarta przez caly
 * czas trwania metody i **maskuje** LazyInitializationException — widok przechodzi
 * w tescie, a wywala sie w dzialajacej aplikacji.
 *
 * Kazdy widok siegajacy po kolekcje encji powinien miec pokrycie w tej klasie.
 *
 * Poniewaz nie ma wycofania transakcji, dane testowe sa sprzatane w @AfterEach.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Renderowanie widokow ankiet bez otwartej sesji")
class AnkietaRenderingTest {

    private static final String CELINA = "celina-render";

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AnkietaRepository ankietaRepository;
    @Autowired private AnkietaService ankietaService;
    @Autowired private PasswordEncoder passwordEncoder;

    private Long ankietaId;
    private Long userId;

    @BeforeEach
    void przygotuj() {
        User celina = userRepository.findByUsername(CELINA).orElseGet(() -> {
            User u = new User(CELINA, passwordEncoder.encode("nieistotne"));
            roleRepository.findByName(Role.USER).ifPresent(u::addRole);
            return userRepository.save(u);
        });
        userId = celina.getId();

        Ankieta a = ankietaService.utworz("Ankieta renderowania", "Klient testowy", celina);
        ankietaService.dodajPozycje(a.getId(),
                "Czy prowadza Panstwo ewidencje odpadow?",
                "Tak, ewidencja jest prowadzona w systemie krajowym.",
                null, celina);
        ankietaId = a.getId();
    }

    @AfterEach
    void posprzataj() {
        if (ankietaId != null) {
            ankietaRepository.findById(ankietaId).ifPresent(ankietaRepository::delete);
        }
        if (userId != null) {
            userRepository.findById(userId).ifPresent(userRepository::delete);
        }
    }

    @Test
    @WithUserDetails(value = CELINA, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("lista ankiet renderuje liczbe pozycji poza transakcja")
    void listaRenderujeLiczbePozycji() throws Exception {
        mvc.perform(get("/moje-ankiety"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ankieta renderowania")));
    }

    @Test
    @WithUserDetails(value = CELINA, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("szczegoly ankiety renderuja pozycje poza transakcja")
    void szczegolyRenderujaPozycje() throws Exception {
        mvc.perform(get("/moje-ankiety/{id}", ankietaId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ewidencje odpadow")));
    }

    @Test
    @WithUserDetails(value = CELINA, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("ekran dopasowania renderuje liste ankiet poza transakcja")
    void ekranDopasowaniaRenderujeAnkiety() throws Exception {
        mvc.perform(get("/szukaj").param("pytanie", "certyfikat ISO 9001"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ankieta renderowania")));
    }

    @Test
    @WithUserDetails(value = "admin", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("widok wszystkich ankiet renderuje sie poza transakcja")
    void widokAdminaRenderujeSie() throws Exception {
        mvc.perform(get("/moje-ankiety").param("wszystkie", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ankieta renderowania")));
    }
}
