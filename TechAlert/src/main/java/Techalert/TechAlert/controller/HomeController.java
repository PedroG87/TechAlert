package Techalert.TechAlert.controller;

import java.time.LocalDate;

import jakarta.servlet.http.HttpSession;

import Techalert.TechAlert.security.AdminAccessInterceptor;
import Techalert.TechAlert.security.AuthService;
import Techalert.TechAlert.security.SessionUser;
import Techalert.TechAlert.service.PlatformSettingService;
import Techalert.TechAlert.service.UserService;
import Techalert.TechAlert.service.UserService.RegistrationResult;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping()
public class HomeController {

    private final AuthService authService;
    private final UserService userService;
    private final PlatformSettingService platformSettingService;

    public HomeController(AuthService authService, UserService userService, PlatformSettingService platformSettingService) {
        this.authService = authService;
        this.userService = userService;
        this.platformSettingService = platformSettingService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String home(@RequestParam(required = false) String erro, Model model) {
        model.addAttribute("erro", erro);
        applyPublicSettings(model);
        return "home";
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String erro,
                        @RequestParam(required = false) String sucesso,
                        HttpSession session,
                        Model model) {
        SessionUser currentUser = (SessionUser) session.getAttribute(AdminAccessInterceptor.SESSION_USER_KEY);
        if (currentUser != null && currentUser.isAuthenticated()) {
            return "redirect:" + (currentUser.isAdmin() ? "/adm" : "/cidadao/notificacoes");
        }

        model.addAttribute("erro", erro);
        model.addAttribute("sucesso", sucesso);
        applyPublicSettings(model);
        return "login";
    }

    @PostMapping("/login")
    public String autenticar(@RequestParam String email,
                             @RequestParam String senha,
                             HttpSession session) {
        return authService.authenticate(email, senha)
                .map(user -> {
                    session.setAttribute(AdminAccessInterceptor.SESSION_USER_KEY, user);
                    return "redirect:" + (user.isAdmin() ? "/adm" : "/cidadao/notificacoes");
                })
                .orElse("redirect:/login?erro=credenciais");
    }

    @GetMapping("/cadastro")
    public String cadastro(@RequestParam(required = false) String sucesso,
                           @RequestParam(required = false) String erro,
                           Model model) {
        model.addAttribute("sucesso", sucesso);
        model.addAttribute("erro", erro);
        model.addAttribute("form", new RegistrationForm());
        applyPublicSettings(model);
        return "cadastro";
    }

    @PostMapping("/cadastro")
    public String registrar(@ModelAttribute("form") RegistrationForm form, Model model) {
        RegistrationResult result = userService.registerCitizen(
                form.nome(),
                form.email(),
                form.senha(),
                form.cpf(),
                form.telefone(),
                form.endereco(),
                form.dataNascimento()
        );
        if (!result.success()) {
            model.addAttribute("erro", result.message());
            return "cadastro";
        }
        return "redirect:/login?sucesso=cadastro";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?sucesso=logout";
    }

    @GetMapping("/adm")
    public String adminDashboard() {
        return "admin-dashboard";
    }

    @GetMapping("/adm/notificacoes")
    public String adminNotifications() {
        return "redirect:/adm";
    }

    @GetMapping("/cidadao/notificacoes")
    public String citizenNotifications() {
        return "cidadao-notificacoes";
    }

    private void applyPublicSettings(Model model) {
        model.addAttribute("platformName", platformSettingService.getValue("platform.name", "TechAlert"));
        model.addAttribute("homeHeroMessage", platformSettingService.getValue(
                "home.hero.message",
                "Receba alertas essenciais e acompanhe a operacao do sistema com simplicidade."
        ));
        model.addAttribute("supportEmail", platformSettingService.getValue("platform.support.email", "suporte@techalert.com"));
    }

    public record RegistrationForm(
            String nome,
            String email,
            String senha,
            String cpf,
            String telefone,
            String endereco,
            LocalDate dataNascimento
    ) {
        public RegistrationForm() {
            this(null, null, null, null, null, null, null);
        }
    }
}

