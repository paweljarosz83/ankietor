package pl.ankietor.knowledge.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.ankietor.knowledge.dtos.QuestionAnswerForm;
import pl.ankietor.knowledge.models.QuestionAnswer;
import pl.ankietor.knowledge.repos.QuestionAnswerRepository;
import pl.ankietor.security.models.Role;
import pl.ankietor.security.models.User;

import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * MODEL WIDOCZNOSCI I WLASNOSCI — decyzja podjeta swiadomie, opisana w prd.md §6a.
 *
 * Odczyt jest wspolny. Baza wiedzy to zasob zespolu, nie prywatna notatka: cala wartosc
 * produktu polega na tym, ze dwie osoby odpowiadajace dwoma klientom udziela spojnej
 * odpowiedzi. Prywatna baza per uzytkownik zniszczylaby ten cel.
 *
 * Edycja i usuwanie sa wlasnosciowe. Modyfikowac pare moze jej autor albo administrator.
 * Pary z danych startowych nie maja autora, wiec sa edytowalne tylko przez administratora —
 * traktujemy je jako zawartosc systemowa.
 *
 * Uzytkownik ma tez widok "moje pary" ({@link #pageByAuthor}), czyli zasoby przypisane
 * do siebie.
 */
@Service
public class QuestionAnswerService {

    private final QuestionAnswerRepository repository;

    public QuestionAnswerService(QuestionAnswerRepository repository) {
        this.repository = repository;
    }

    // ---------- odczyt ----------

    @Transactional(readOnly = true)
    public Page<QuestionAnswer> page(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<QuestionAnswer> pageByAuthor(User author, Pageable pageable) {
        return repository.findByAuthorOrderByCreatedAtDesc(author, pageable);
    }

    @Transactional(readOnly = true)
    public long total() {
        return repository.count();
    }

    @Transactional(readOnly = true)
    public long totalByAuthor(User author) {
        return repository.countByAuthor(author);
    }

    @Transactional(readOnly = true)
    public Optional<QuestionAnswer> find(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public QuestionAnswerForm formFor(Long id, User requester) {
        QuestionAnswer qa = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Nie ma pary o id " + id));
        requireCanModify(qa, requester);
        return new QuestionAnswerForm(qa.getId(), qa.getQuestion(), qa.getAnswer());
    }

    // ---------- wlasnosc ----------

    /**
     * Czy uzytkownik moze modyfikowac te pare.
     *
     * Metoda jest publiczna, bo widok uzywa jej do decyzji o pokazaniu przyciskow.
     * Sprawdzenie w widoku jest wygoda dla uzytkownika, nie zabezpieczeniem —
     * zabezpieczeniem jest {@link #requireCanModify}, wolane w kazdej operacji zapisu.
     */
    public boolean canModify(QuestionAnswer qa, User requester) {
        if (requester == null) {
            return false;
        }
        if (isAdmin(requester)) {
            return true;
        }
        User author = qa.getAuthor();
        return author != null && author.getId() != null && author.getId().equals(requester.getId());
    }

    private void requireCanModify(QuestionAnswer qa, User requester) {
        if (!canModify(qa, requester)) {
            throw new AccessDeniedException(
                    "Pare moze modyfikowac jej autor albo administrator.");
        }
    }

    private static boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(r -> Role.ADMIN.equals(r.getName()));
    }

    // ---------- zapis ----------

    @Transactional
    public QuestionAnswer save(QuestionAnswerForm form, User author) {
        if (form.isNew()) {
            return repository.save(new QuestionAnswer(
                    form.question().trim(), form.answer().trim(), author));
        }

        QuestionAnswer existing = repository.findById(form.id())
                .orElseThrow(() -> new NoSuchElementException("Nie ma pary o id " + form.id()));
        requireCanModify(existing, author);

        existing.setQuestion(form.question().trim());
        existing.setAnswer(form.answer().trim());
        return repository.save(existing);
    }

    @Transactional
    public void delete(Long id, User requester) {
        QuestionAnswer qa = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Nie ma pary o id " + id));
        requireCanModify(qa, requester);
        repository.delete(qa);
    }
}
