package pl.ankietor.knowledge.repos;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.ankietor.knowledge.models.QuestionAnswer;

public interface QuestionAnswerRepository extends JpaRepository<QuestionAnswer, Long> {

    Page<QuestionAnswer> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long count();
}
