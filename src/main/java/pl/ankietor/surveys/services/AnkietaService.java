package pl.ankietor.surveys.services;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.ankietor.knowledge.models.QuestionAnswer;
import pl.ankietor.knowledge.repos.QuestionAnswerRepository;
import pl.ankietor.security.models.Role;
import pl.ankietor.security.models.User;
import pl.ankietor.surveys.models.Ankieta;
import pl.ankietor.surveys.models.AnkietaPozycja;
import pl.ankietor.surveys.repos.AnkietaRepository;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * POZIOMY UPRAWNIEN — opisane w prd.md §6b.
 *
 * Ankieta jest zasobem PRYWATNYM, inaczej niz baza wiedzy. Uzytkownik z rola USER widzi
 * i modyfikuje wylacznie wlasne ankiety; cudza ankieta jest dla niego niewidoczna,
 * a proba dostepu konczy sie odmowa, nie pusta lista.
 *
 * ROLE_ADMIN ma wglad we wszystkie ankiety — to funkcja nadzoru, nie wygody. Dzieki niej
 * mozna odtworzyc, co zostalo wyslane klientowi przez osobe, ktora odeszla z firmy.
 */
@Service
public class AnkietaService {

    private final AnkietaRepository repository;
    private final QuestionAnswerRepository knowledgeRepository;

    public AnkietaService(AnkietaRepository repository,
                          QuestionAnswerRepository knowledgeRepository) {
        this.repository = repository;
        this.knowledgeRepository = knowledgeRepository;
    }

    // ---------- odczyt ----------

    /** Ankiety widoczne dla uzytkownika: wlasne, a dla administratora wszystkie na zadanie. */
    @Transactional(readOnly = true)
    public List<Ankieta> widoczne(User user, boolean wszystkie) {
        if (wszystkie && isAdmin(user)) {
            return repository.findAllByOrderByCreatedAtDesc();
        }
        return repository.findByOwnerOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public long liczbaWlasnych(User user) {
        return repository.countByOwner(user);
    }

    /**
     * Pobiera ankiete wraz z pozycjami, sprawdzajac uprawnienia.
     * Rzuca {@link AccessDeniedException}, gdy uzytkownik nie ma do niej prawa.
     */
    @Transactional(readOnly = true)
    public Ankieta pobierz(Long id, User user) {
        Ankieta a = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Nie ma ankiety o id " + id));
        wymagajDostepu(a, user);
        a.getPozycje().size(); // inicjalizacja kolekcji w transakcji
        return a;
    }

    // ---------- uprawnienia ----------

    /** Czy uzytkownik moze ogladac ankiete: wlasciciel albo administrator. */
    public boolean mozeOgladac(Ankieta a, User user) {
        return user != null && (isAdmin(user) || jestWlascicielem(a, user));
    }

    /**
     * Czy uzytkownik moze modyfikowac ankiete: WYLACZNIE wlasciciel.
     *
     * Administrator celowo NIE moze edytowac cudzej ankiety — jego rola to wglad,
     * nie wyreczanie. Ankieta jest dokumentem konkretnej osoby wobec konkretnego klienta.
     */
    public boolean mozeModyfikowac(Ankieta a, User user) {
        return jestWlascicielem(a, user);
    }

    private void wymagajDostepu(Ankieta a, User user) {
        if (!mozeOgladac(a, user)) {
            throw new AccessDeniedException("Ankieta nalezy do innego uzytkownika.");
        }
    }

    private void wymagajModyfikacji(Ankieta a, User user) {
        if (!mozeModyfikowac(a, user)) {
            throw new AccessDeniedException("Ankiete moze modyfikowac wylacznie jej wlasciciel.");
        }
    }

    private static boolean jestWlascicielem(Ankieta a, User user) {
        return user != null && a.getOwner() != null
                && a.getOwner().getId() != null
                && a.getOwner().getId().equals(user.getId());
    }

    private static boolean isAdmin(User user) {
        return user != null && user.getRoles().stream()
                .anyMatch(r -> Role.ADMIN.equals(r.getName()));
    }

    // ---------- zapis ----------

    @Transactional
    public Ankieta utworz(String nazwa, String klient, User owner) {
        return repository.save(new Ankieta(nazwa.trim(),
                klient == null || klient.isBlank() ? null : klient.trim(), owner));
    }

    /**
     * Dopisuje pytanie z odpowiedzia do ankiety uzytkownika. To jest akcja przycisku
     * „Zapisz do ankiety" na ekranie dopasowania.
     */
    @Transactional
    public Ankieta dodajPozycje(Long ankietaId, String pytanie, String odpowiedz,
                                Long sourceId, User user) {
        Ankieta a = repository.findById(ankietaId)
                .orElseThrow(() -> new NoSuchElementException("Nie ma ankiety o id " + ankietaId));
        wymagajModyfikacji(a, user);

        QuestionAnswer source = sourceId == null
                ? null
                : knowledgeRepository.findById(sourceId).orElse(null);

        a.dodajPozycje(new AnkietaPozycja(pytanie.trim(), odpowiedz.trim(), source));
        return repository.save(a);
    }

    @Transactional
    public void usun(Long id, User user) {
        Ankieta a = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Nie ma ankiety o id " + id));
        wymagajModyfikacji(a, user);
        repository.delete(a);
    }

    @Transactional
    public void usunPozycje(Long ankietaId, Long pozycjaId, User user) {
        Ankieta a = repository.findById(ankietaId)
                .orElseThrow(() -> new NoSuchElementException("Nie ma ankiety o id " + ankietaId));
        wymagajModyfikacji(a, user);
        a.getPozycje().removeIf(p -> p.getId().equals(pozycjaId));
        repository.save(a);
    }
}
