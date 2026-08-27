package pl.ankietor.knowledge.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Formularz pary pytanie-odpowiedz. Walidacja adnotacjami zgodnie z AGENTS.md -
 * bez osobnych klas walidatorow.
 */
public record QuestionAnswerForm(

        Long id,

        @NotBlank(message = "Tresc pytania jest wymagana")
        @Size(min = 5, max = 4000, message = "Pytanie musi miec od {min} do {max} znakow")
        String question,

        @NotBlank(message = "Tresc odpowiedzi jest wymagana")
        @Size(min = 2, max = 8000, message = "Odpowiedz musi miec od {min} do {max} znakow")
        String answer
) {
    public static QuestionAnswerForm empty() {
        return new QuestionAnswerForm(null, "", "");
    }

    public boolean isNew() {
        return id == null;
    }
}
