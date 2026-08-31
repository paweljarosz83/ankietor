package pl.ankietor.surveys.repos;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.ankietor.security.models.User;
import pl.ankietor.surveys.models.Ankieta;

import java.util.List;
import java.util.Optional;

/**
 * Widoki ankiet siegaja po pozycje (liczba, tresc) i po wlasciciela (login), a aplikacja
 * dziala z open-in-view = false — sesja jest zamknieta, zanim szablon zacznie sie
 * renderowac. Oba powiazania musza wiec zostac dociagniete zapytaniem.
 *
 * WAZNE: attributePaths tworzy FETCH graph, wiec wszystko spoza listy staje sie LAZY —
 * takze pola zadeklarowane w encji jako EAGER. Dlatego owner jest wymieniony jawnie,
 * mimo ze w {@link Ankieta} ma FetchType.EAGER. Pominiecie go konczy sie
 * LazyInitializationException dopiero przy renderowaniu widoku.
 */
public interface AnkietaRepository extends JpaRepository<Ankieta, Long> {

    /** Ankiety przypisane do uzytkownika — podstawowy widok dla roli USER. */
    @EntityGraph(attributePaths = {"pozycje", "owner"})
    List<Ankieta> findByOwnerOrderByCreatedAtDesc(User owner);

    /** Wszystkie ankiety — dostepne wylacznie dla roli ADMIN. */
    @EntityGraph(attributePaths = {"pozycje", "owner"})
    List<Ankieta> findAllByOrderByCreatedAtDesc();

    /** Pojedyncza ankieta wraz z pozycjami — widok szczegolow. */
    @EntityGraph(attributePaths = {"pozycje", "owner"})
    Optional<Ankieta> findWithPozycjeById(Long id);

    long countByOwner(User owner);
}
