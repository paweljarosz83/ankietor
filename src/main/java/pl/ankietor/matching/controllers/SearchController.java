package pl.ankietor.matching.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.ankietor.matching.MatchingProperties;
import pl.ankietor.matching.QuestionMatch;
import pl.ankietor.matching.QuestionMatcher;
import pl.ankietor.security.models.User;
import pl.ankietor.security.models.UserDetailsImpl;
import pl.ankietor.surveys.models.Ankieta;
import pl.ankietor.surveys.services.AnkietaService;

import java.util.List;

/**
 * Glowny przeplyw aplikacji: wklejenie pytania z ankiety i otrzymanie propozycji
 * odpowiedzi udzielonych wczesniej na pytania o tym samym znaczeniu.
 *
 * Ekran udostepnia tez liste ankiet zalogowanego uzytkownika, zeby wybrana odpowiedz
 * dalo sie zapisac bez opuszczania widoku.
 */
@Controller
@RequestMapping("/szukaj")
public class SearchController {

    private final QuestionMatcher matcher;
    private final MatchingProperties properties;
    private final AnkietaService ankietaService;

    public SearchController(QuestionMatcher matcher,
                            MatchingProperties properties,
                            AnkietaService ankietaService) {
        this.matcher = matcher;
        this.properties = properties;
        this.ankietaService = ankietaService;
    }

    @GetMapping
    public String search(@RequestParam(name = "pytanie", required = false) String pytanie,
                         @AuthenticationPrincipal UserDetailsImpl principal,
                         Model model) {

        boolean szukano = pytanie != null && !pytanie.isBlank();

        List<QuestionMatch> propozycje = szukano
                ? matcher.findSimilar(pytanie, properties.getMaxResults(), properties.getMinSimilarity())
                : List.of();

        User me = principal == null ? null : principal.getUser();
        List<Ankieta> mojeAnkiety = me == null ? List.of() : ankietaService.widoczne(me, false);

        model.addAttribute("pytanie", pytanie == null ? "" : pytanie);
        model.addAttribute("szukano", szukano);
        model.addAttribute("propozycje", propozycje);
        model.addAttribute("tryb", matcher.mode());
        model.addAttribute("prog", Math.round(properties.getMinSimilarity() * 100));
        model.addAttribute("mojeAnkiety", mojeAnkiety);

        return "matching/search";
    }
}
