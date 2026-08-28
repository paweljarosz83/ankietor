package pl.ankietor.matching.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.ankietor.matching.MatchingProperties;
import pl.ankietor.matching.MockSemanticQuestionMatcher;
import pl.ankietor.matching.QuestionMatcher;

import java.util.List;

/**
 * Ekran porownania trybow dopasowania.
 *
 * Po co: pokazuje, ktora klasa pytan jest niedostepna dla dopasowania leksykalnego,
 * na tych samych danych i tym samym zapytaniu. To odpowiednik ewaluacji - zamiast
 * zakladac, ze warstwa semantyczna pomoze, widac roznice.
 *
 * Prawa kolumna to MOCKUP oparty na slowniku pojec, nie embeddingi. Widok oznacza
 * to jawnie - zadne wyniki nie sa przedstawiane jako AI.
 */
@Controller
@RequestMapping("/porownanie")
public class ComparisonController {

    private final QuestionMatcher aktywny;
    private final MockSemanticQuestionMatcher mockup;
    private final MatchingProperties properties;

    public ComparisonController(QuestionMatcher aktywny,
                                MockSemanticQuestionMatcher mockup,
                                MatchingProperties properties) {
        this.aktywny = aktywny;
        this.mockup = mockup;
        this.properties = properties;
    }

    @GetMapping
    public String compare(@RequestParam(name = "pytanie", required = false) String pytanie,
                          Model model) {

        boolean szukano = pytanie != null && !pytanie.isBlank();
        int limit = properties.getMaxResults();
        double prog = properties.getMinSimilarity();

        List<?> lewa = szukano ? aktywny.findSimilar(pytanie, limit, prog) : List.of();
        List<?> prawa = szukano ? mockup.findSimilar(pytanie, limit, prog) : List.of();

        model.addAttribute("pytanie", pytanie == null ? "" : pytanie);
        model.addAttribute("szukano", szukano);
        model.addAttribute("lewa", lewa);
        model.addAttribute("prawa", prawa);
        model.addAttribute("trybLewy", aktywny.mode());
        model.addAttribute("trybPrawy", mockup.mode());
        model.addAttribute("prog", Math.round(prog * 100));
        model.addAttribute("przyklady", List.of(
                "Prosze wskazac wdrozone systemy zarzadzania jakoscia",
                "Czy Panstwa organizacja dba o dobrostan zatrudnionych osob?",
                "Jak zabezpieczaja Panstwo swoje mienie przed skutkami zdarzen losowych?",
                "Czy posiadacie aktualne certyfikaty ISO 9001?"
        ));

        return "matching/comparison";
    }
}
