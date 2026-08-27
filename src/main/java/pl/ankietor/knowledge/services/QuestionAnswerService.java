package pl.ankietor.knowledge.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.ankietor.knowledge.dtos.QuestionAnswerForm;
import pl.ankietor.knowledge.models.QuestionAnswer;
import pl.ankietor.knowledge.repos.QuestionAnswerRepository;
import pl.ankietor.security.models.User;

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class QuestionAnswerService {

    private final QuestionAnswerRepository repository;

    public QuestionAnswerService(QuestionAnswerRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<QuestionAnswer> page(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public long total() {
        return repository.count();
    }

    @Transactional(readOnly = true)
    public Optional<QuestionAnswer> find(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public QuestionAnswerForm formFor(Long id) {
        return find(id)
                .map(qa -> new QuestionAnswerForm(qa.getId(), qa.getQuestion(), qa.getAnswer()))
                .orElseThrow(() -> new NoSuchElementException("Nie ma pary o id " + id));
    }

    @Transactional
    public QuestionAnswer save(QuestionAnswerForm form, User author) {
        if (form.isNew()) {
            return repository.save(new QuestionAnswer(
                    form.question().trim(), form.answer().trim(), author));
        }

        QuestionAnswer existing = repository.findById(form.id())
                .orElseThrow(() -> new NoSuchElementException("Nie ma pary o id " + form.id()));
        existing.setQuestion(form.question().trim());
        existing.setAnswer(form.answer().trim());
        return repository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
