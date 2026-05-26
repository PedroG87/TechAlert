package Techalert.TechAlert;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.UUID;

import Techalert.TechAlert.model.Usuario;
import Techalert.TechAlert.repository.UsuarioRepository;
import Techalert.TechAlert.repository.SystemNotificationRepository;
import Techalert.TechAlert.security.AdminAccessInterceptor;
import Techalert.TechAlert.security.SessionUser;
import Techalert.TechAlert.security.UserRole;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationManagementWebTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SystemNotificationRepository systemNotificationRepository;

    @Test
    void shouldRedirectAnonymousUserFromAdminScreenToLogin() throws Exception {
        mockMvc.perform(get("/adm"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?erro=autenticacao"));
    }

    @Test
    void shouldRedirectCitizenFromAdminScreenToHome() throws Exception {
        mockMvc.perform(get("/adm").session(sessionWithRole(UserRole.CIDADAO)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home?erro=acesso"));
    }

    @Test
    void shouldRedirectAdminFromCitizenScreenToHome() throws Exception {
        mockMvc.perform(get("/cidadao/notificacoes").session(sessionWithRole(UserRole.ADM)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home?erro=acesso-cidadao"));
    }

    @Test
    void shouldRenderAdminDashboardForAdmin() throws Exception {
        mockMvc.perform(get("/adm").session(sessionWithRole(UserRole.ADM)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-dashboard"))
                .andExpect(content().string(containsString("Gestão de usuários")))
                .andExpect(content().string(containsString("Moderação de notificações")))
                .andExpect(content().string(containsString("Configurações básicas")));
    }

    @Test
    void shouldRenderCitizenScreenForCitizen() throws Exception {
        mockMvc.perform(get("/cidadao/notificacoes").session(sessionWithRole(UserRole.CIDADAO)))
                .andExpect(status().isOk())
                .andExpect(view().name("cidadao-notificacoes"))
                .andExpect(content().string(containsString("Minhas notificações")))
                .andExpect(content().string(containsString("summaryActive")));
    }

    @Test
    void shouldExposeDashboardSummaryForAdmin() throws Exception {
        mockMvc.perform(get("/api/adm/dashboard/summary").session(sessionWithRole(UserRole.ADM)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsuarios", greaterThan(0)))
                .andExpect(jsonPath("$.totalCidadaos", greaterThan(0)))
                .andExpect(jsonPath("$.configuracoesBasicas", greaterThan(0)));
    }

    @Test
    void shouldRejectCitizenOnAdminNotificationApi() throws Exception {
        mockMvc.perform(get("/api/adm/dashboard/summary").session(sessionWithRole(UserRole.CIDADAO)))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Acesso negado")));
    }

    @Test
    void shouldRejectAdminOnCitizenNotificationApi() throws Exception {
        mockMvc.perform(get("/api/cidadao/notificacoes").session(sessionWithRole(UserRole.ADM)))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Acesso negado")));
    }

    @Test
    void shouldListNotificationsForAdminWithSeverityField() throws Exception {
        mockMvc.perform(get("/api/adm/notificacoes")
                        .session(sessionWithRole(UserRole.ADM))
                        .param("status", "ATIVA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].titulo").exists())
                .andExpect(jsonPath("$.content[0].usuarioAlvo").exists())
                .andExpect(jsonPath("$.content[0].nivelPericulosidade").exists());
    }

    @Test
    void shouldCreateAndArchiveNotificationFromAdminArea() throws Exception {
        mockMvc.perform(post("/api/adm/notificacoes")
                        .session(sessionWithRole(UserRole.ADM))
                        .contentType("application/json")
                        .content("""
                                {
                                  "titulo":"Novo alerta MVP",
                                  "conteudo":"Conteudo essencial para o cidadao.",
                                  "tipo":"ALERTA",
                                  "nivelPericulosidade":"ALTA"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affected", greaterThan(0)));

        Long notificationId = systemNotificationRepository.findAll().stream()
                .map(notification -> notification.getId())
                .max(Long::compareTo)
                .orElseThrow();

        mockMvc.perform(patch("/api/adm/notificacoes/" + notificationId + "/status")
                        .session(sessionWithRole(UserRole.ADM))
                        .param("status", "ARQUIVADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARQUIVADA"));
    }

    @Test
    void shouldAllowAdminToManageUsersAndSettings() throws Exception {
        MockHttpSession adminSession = sessionWithRole(UserRole.ADM);

        mockMvc.perform(post("/api/adm/users")
                        .session(adminSession)
                        .contentType("application/json")
                        .content("""
                                {
                                  "nome":"Gestor MVP",
                                  "email":"gestor@techalert.com",
                                  "senha":"Gestor123",
                                  "cpf":"55555555555",
                                  "telefone":"(11) 95555-1010",
                                  "endereco":"Rua MVP, 10",
                                  "dataNascimento":"1990-01-01",
                                  "role":"ADM"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("gestor@techalert.com"));

        Long createdUserId = usuarioRepository.findByEmailIgnoreCase("gestor@techalert.com")
                .map(Usuario::getId)
                .orElseThrow();

        mockMvc.perform(put("/api/adm/users/" + createdUserId)
                        .session(adminSession)
                        .contentType("application/json")
                        .content("""
                                {
                                  "nome":"Gestor MVP Atualizado",
                                  "email":"gestor@techalert.com",
                                  "senha":"",
                                  "cpf":"55555555555",
                                  "telefone":"(11) 95555-1010",
                                  "endereco":"Rua MVP, 20",
                                  "dataNascimento":"1990-01-01",
                                  "role":"ADM"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Gestor MVP Atualizado"));

        mockMvc.perform(patch("/api/adm/settings/platform.name")
                        .session(adminSession)
                        .contentType("application/json")
                        .content("""
                                {
                                  "valor":"TechAlert MVP"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value("TechAlert MVP"));

        mockMvc.perform(delete("/api/adm/users/" + createdUserId).session(adminSession))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldListCitizensWithPaginationAndSearch() throws Exception {
        mockMvc.perform(get("/api/adm/citizens")
                        .session(sessionWithRole(UserRole.ADM))
                        .param("search", "cidadao")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].role").value("CIDADAO"))
                .andExpect(jsonPath("$.totalElements", greaterThan(0)))
                .andExpect(jsonPath("$.size").value(1));
    }

    @Test
    void shouldAllowAdminToEditCitizenAndPromoteWithHistory() throws Exception {
        MockHttpSession adminSession = sessionWithRole(UserRole.ADM);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "cidadao." + suffix + "@techalert.com";
        String cpf = "9" + String.format("%010d", Math.abs(suffix.hashCode()) % 1_000_000_000L);

        mockMvc.perform(post("/api/adm/users")
                        .session(adminSession)
                        .contentType("application/json")
                        .content("""
                                {
                                  "nome":"Cidadao Gerenciavel",
                                  "email":"%s",
                                  "senha":"Cidadao123",
                                  "cpf":"%s",
                                  "telefone":"(11) 94444-0000",
                                  "endereco":"Rua dos Testes, 50",
                                  "dataNascimento":"1994-03-15",
                                  "role":"CIDADAO"
                                }
                                """.formatted(email, cpf)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CIDADAO"));

        Long citizenId = usuarioRepository.findByEmailIgnoreCase(email)
                .map(Usuario::getId)
                .orElseThrow();

        mockMvc.perform(put("/api/adm/citizens/" + citizenId)
                        .session(adminSession)
                        .contentType("application/json")
                        .content("""
                                {
                                  "nome":"Cidadao Atualizado",
                                  "email":"%s",
                                  "senha":"",
                                  "cpf":"%s",
                                  "telefone":"(11) 95555-2020",
                                  "endereco":"Rua Atualizada, 99",
                                  "dataNascimento":"1994-03-15"
                                }
                                """.formatted(email, cpf)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Cidadao Atualizado"))
                .andExpect(jsonPath("$.role").value("CIDADAO"));

        mockMvc.perform(post("/api/adm/citizens/" + citizenId + "/promote")
                        .session(adminSession)
                        .contentType("application/json")
                        .content("""
                                {
                                  "confirmacao":true,
                                  "motivo":"Cobertura administrativa regional"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADM"))
                .andExpect(jsonPath("$.message").value("Promocao realizada com sucesso."));

        mockMvc.perform(get("/api/adm/citizens/" + citizenId + "/history")
                        .session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actionType").value("CIDADAO_PROMOVIDO"))
                .andExpect(jsonPath("$[0].adminNome").exists());
    }

    @Test
    void shouldAllowCitizenToReadOwnNotifications() throws Exception {
        MockHttpSession citizenSession = sessionWithRole(UserRole.CIDADAO);
        Long citizenNotificationId = systemNotificationRepository.findAll().stream()
                .filter(notification -> notification.getUsuarioAlvo().getRole() == UserRole.CIDADAO)
                .map(notification -> notification.getId())
                .findFirst()
                .orElseThrow();

        mockMvc.perform(patch("/api/cidadao/notificacoes/" + citizenNotificationId + "/read")
                        .session(citizenSession)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lida").value(true));
    }

    @Test
    void shouldRegisterCitizenAndLoginWithOrmBackedAuth() throws Exception {
        mockMvc.perform(post("/cadastro")
                        .param("nome", "Joao Novo")
                        .param("email", "joao@techalert.com")
                        .param("senha", "Senha123")
                        .param("cpf", "44444444444")
                        .param("telefone", "(11) 95555-0011")
                        .param("endereco", "Rua Nova, 10")
                        .param("dataNascimento", "1998-01-20"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?sucesso=cadastro"));

        mockMvc.perform(post("/login")
                        .param("email", "joao@techalert.com")
                        .param("senha", "Senha123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cidadao/notificacoes"));
    }

    private MockHttpSession sessionWithRole(UserRole role) {
        Usuario user = usuarioRepository.findAllByRole(role).stream()
                .findFirst()
                .orElseThrow();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AdminAccessInterceptor.SESSION_USER_KEY,
                new SessionUser(user.getId(), user.getNome(), user.getEmail(), role));
        return session;
    }
}
