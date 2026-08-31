package pl.ankietor.surveys.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.ankietor.security.models.User;
import pl.ankietor.surveys.models.Ankieta;

import java.util.List;

public interface AnkietaRepository extends JpaRepository<Ankieta, Long> {

    /** Ankiety przypisane do uzytkownika — podstawowy widok dla roli USER. */
    List<Ankieta> findByOwnerOrderByCreatedAtDesc(User owner);

    /** Wszystkie ankiety — dostepne wylacznie dla roli ADMIN. */
    List<Ankieta> findAllByOrderByCreatedAtDesc();

    long countByOwner(User owner);
}
