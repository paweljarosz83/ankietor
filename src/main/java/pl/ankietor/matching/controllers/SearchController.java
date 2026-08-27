package pl.ankietor.matching.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.ankietor.matching.MatchingProperties;
import pl.ankietor.matching.QuestionMatch;
import pl.ankietor.matching.QuestionMatcher;

import java.util.List;

/**
 * Glowny przeplyw aplikacji: wklejenie pytania z ankiety i otrzymanie propozycji
 * odpowiedzi udzielonych wczesniej na pytania o tym samym znaczeniu.
 */
@Controller
@RequestMapping("/szukaj")
public class SearchController {

    private final QuestionMatcher matcher;
    private final MatchingProperties properties;

    public SearchController(QuestionMatcher matcher, MatchingProperties properties) {
        this.matcher = matcher;
        this.properties = properties;
    }

    @GetMapping
    public String search(@RequestParam(name = "pytanie", required = false) String pytanie,
                         Model model) {

        boolean szukano = pytanie != null && !pytanie.isBlank();

        List<QuestionMatch> propozycje = szukano
                ? matcher.findSimilar(pytanie, properties.getMaxResults(), properties.getMinSimilarity())
                : List.of();

        model.addAttribute("pytanie", pytanie == null ? "" : pytanie);
        model.addAttribute("szukano", szukano);
        model.addAttribute("propozycje", propozycje);
        model.addAttribute("tryb", matcher.mode());
        model.addAttribute("prog", Math.round(properties.getMinSimilarity() * 100));

        return "matching/search";
    }
}
