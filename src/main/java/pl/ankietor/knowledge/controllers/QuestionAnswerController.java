package pl.ankietor.knowledge.controllers;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.ankietor.knowledge.dtos.QuestionAnswerForm;
import pl.ankietor.knowledge.models.QuestionAnswer;
import pl.ankietor.knowledge.services.QuestionAnswerService;
import pl.ankietor.security.models.User;
import pl.ankietor.security.models.UserDetailsImpl;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/baza-wiedzy")
public class QuestionAnswerController {

    private static final int PAGE_SIZE = 20;

    private final QuestionAnswerService service;

    public QuestionAnswerController(QuestionAnswerService service) {
        this.service = service;
    }

    /**
     * Lista bazy wiedzy. Parametr moje przelacza na widok zasobow przypisanych
     * do zalogowanego uzytkownika.
     */
    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "false") boolean moje,
                       @AuthenticationPrincipal UserDetailsImpl principal,
                       Model model) {

        User me = principal == null ? null : principal.getUser();
        PageRequest request = PageRequest.of(Math.max(page, 0), PAGE_SIZE);

        Page<QuestionAnswer> result = (moje && me != null)
                ? service.pageByAuthor(me, request)
                : service.page(request);

        // Uprawnienie do edycji liczone raz per wiersz - widok nie powinien wolac
        // serwisu w petli.
        Map<Long, Boolean> mozeEdytowac = new HashMap<>();
        result.getContent().forEach(qa -> mozeEdytowac.put(qa.getId(), service.canModify(qa, me)));

        model.addAttribute("pary", result);
        model.addAttribute("moje", moje);
        model.addAttribute("total", service.total());
        model.addAttribute("totalMoje", me == null ? 0 : service.totalByAuthor(me));
        model.addAttribute("mozeEdytowac", mozeEdytowac);
        return "knowledge/list";
    }

    @GetMapping("/nowa")
    public String createForm(@RequestParam(name = "pytanie", required = false) String pytanie,
                             @RequestParam(name = "odpowiedz", required = false) String odpowiedz,
                             Model model) {
        model.addAttribute("form", new QuestionAnswerForm(
                null,
                pytanie == null ? "" : pytanie,
                odpowiedz == null ? "" : odpowiedz));
        return "knowledge/form";
    }

    @GetMapping("/{id}/edycja")
    public String editForm(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetailsImpl principal,
                           Model model) {
        User me = principal == null ? null : principal.getUser();
        model.addAttribute("form", service.formFor(id, me));
        return "knowledge/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") QuestionAnswerForm form,
                       BindingResult binding,
                       @AuthenticationPrincipal UserDetailsImpl principal,
                       RedirectAttributes flash) {

        if (binding.hasErrors()) {
            return "knowledge/form";
        }

        boolean isNew = form.isNew();
        service.save(form, principal == null ? null : principal.getUser());
        flash.addFlashAttribute("komunikat",
                isNew ? "Dodano nowa pare pytanie-odpowiedz."
                      : "Zapisano zmiany.");
        return "redirect:/baza-wiedzy";
    }

    @PostMapping("/{id}/usun")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetailsImpl principal,
                         RedirectAttributes flash) {
        service.delete(id, principal == null ? null : principal.getUser());
        flash.addFlashAttribute("komunikat", "Usunieto pare pytanie-odpowiedz.");
        return "redirect:/baza-wiedzy";
    }
}
