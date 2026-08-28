package pl.ankietor.knowledge.repos;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.ankietor.knowledge.models.QuestionAnswer;
import pl.ankietor.security.models.User;

public interface QuestionAnswerRepository extends JpaRepository<QuestionAnswer, Long> {

    Page<QuestionAnswer> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Pary przypisane do konkretnego uzytkownika - widok "moje pary". */
    Page<QuestionAnswer> findByAuthorOrderByCreatedAtDesc(User author, Pageable pageable);

    long countByAuthor(User author);

    long count();
}
