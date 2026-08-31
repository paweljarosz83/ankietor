package pl.ankietor.surveys.controllers;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.ankietor.security.models.Role;
import pl.ankietor.security.models.User;
import pl.ankietor.security.models.UserDetailsImpl;
import pl.ankietor.surveys.models.Ankieta;
import pl.ankietor.surveys.services.AnkietaService;

import java.util.List;

@Controller
@RequestMapping("/moje-ankiety")
public class AnkietaController {

    private final AnkietaService service;

    public AnkietaController(AnkietaService service) {
        this.service = service;
    }

    @GetMapping
    public String lista(@RequestParam(defaultValue = "false") boolean wszystkie,
                        @AuthenticationPrincipal UserDetailsImpl principal,
                        Model model) {

        User me = principal == null ? null : principal.getUser();
        boolean admin = jestAdminem(me);
        boolean pokazWszystkie = wszystkie && admin;

        List<Ankieta> ankiety = service.widoczne(me, pokazWszystkie);

        model.addAttribute("ankiety", ankiety);
        model.addAttribute("wszystkie", pokazWszystkie);
        model.addAttribute("admin", admin);
        model.addAttribute("liczbaWlasnych", me == null ? 0 : service.liczbaWlasnych(me));
        return "surveys/list";
    }

    @GetMapping("/{id}")
    public String szczegoly(@PathVariable Long id,
                            @AuthenticationPrincipal UserDetailsImpl principal,
                            Model model) {

        User me = principal == null ? null : principal.getUser();
        Ankieta a = service.pobierz(id, me);

        model.addAttribute("ankieta", a);
        model.addAttribute("mozeModyfikowac", service.mozeModyfikowac(a, me));
        model.addAttribute("admin", jestAdminem(me));
        return "surveys/detail";
    }

    @PostMapping
    public String utworz(@RequestParam @NotBlank @Size(max = 200) String nazwa,
                         @RequestParam(required = false) @Size(max = 200) String klient,
                         @AuthenticationPrincipal UserDetailsImpl principal,
                         RedirectAttributes flash) {

        if (nazwa == null || nazwa.isBlank()) {
            flash.addFlashAttribute("blad", "Nazwa ankiety jest wymagana.");
            return "redirect:/moje-ankiety";
        }

        Ankieta a = service.utworz(nazwa, klient, principal.getUser());
        flash.addFlashAttribute("komunikat", "Utworzono ankiete \"" + a.getNazwa() + "\".");
        return "redirect:/moje-ankiety/" + a.getId();
    }

    /**
     * Akcja przycisku „Zapisz do ankiety" z ekranu dopasowania.
     *
     * Gdy ankietaId jest puste, a podano nazwaNowej — ankieta powstaje w locie,
     * zeby zapisanie pierwszej odpowiedzi nie wymagalo wczesniejszego zakladania ankiety.
     */
    @PostMapping("/zapisz-pozycje")
    public String zapiszPozycje(@RequestParam(required = false) Long ankietaId,
                                @RequestParam(required = false) String nazwaNowej,
                                @RequestParam String pytanie,
                                @RequestParam String odpowiedz,
                                @RequestParam(required = false) Long sourceId,
                                @AuthenticationPrincipal UserDetailsImpl principal,
                                RedirectAttributes flash) {

        User me = principal.getUser();

        Long docelowa = ankietaId;
        if (docelowa == null) {
            String nazwa = (nazwaNowej == null || nazwaNowej.isBlank())
                    ? "Ankieta bez nazwy"
                    : nazwaNowej;
            docelowa = service.utworz(nazwa, null, me).getId();
        }

        service.dodajPozycje(docelowa, pytanie, odpowiedz, sourceId, me);
        flash.addFlashAttribute("komunikat", "Zapisano odpowiedz do ankiety.");
        return "redirect:/moje-ankiety/" + docelowa;
    }

    @PostMapping("/{id}/usun")
    public String usun(@PathVariable Long id,
                       @AuthenticationPrincipal UserDetailsImpl principal,
                       RedirectAttributes flash) {
        service.usun(id, principal.getUser());
        flash.addFlashAttribute("komunikat", "Usunieto ankiete.");
        return "redirect:/moje-ankiety";
    }

    @PostMapping("/{ankietaId}/pozycje/{pozycjaId}/usun")
    public String usunPozycje(@PathVariable Long ankietaId,
                              @PathVariable Long pozycjaId,
                              @AuthenticationPrincipal UserDetailsImpl principal,
                              RedirectAttributes flash) {
        service.usunPozycje(ankietaId, pozycjaId, principal.getUser());
        flash.addFlashAttribute("komunikat", "Usunieto pozycje z ankiety.");
        return "redirect:/moje-ankiety/" + ankietaId;
    }

    private static boolean jestAdminem(User u) {
        return u != null && u.getRoles().stream().anyMatch(r -> Role.ADMIN.equals(r.getName()));
    }
}
