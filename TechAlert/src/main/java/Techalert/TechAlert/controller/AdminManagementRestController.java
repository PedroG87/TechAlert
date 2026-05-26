package Techalert.TechAlert.controller;

import java.time.LocalDate;
import java.util.List;

import jakarta.servlet.http.HttpSession;

import Techalert.TechAlert.model.Usuario;
import Techalert.TechAlert.model.PlatformSetting;
import Techalert.TechAlert.security.AdminAccessInterceptor;
import Techalert.TechAlert.security.SessionUser;
import Techalert.TechAlert.security.UserRole;
import Techalert.TechAlert.service.NotificationService;
import Techalert.TechAlert.service.PlatformSettingService;
import Techalert.TechAlert.service.UserService;
import Techalert.TechAlert.service.UserService.UserMetricsSummary;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/adm")
public class AdminManagementRestController {

    private final UserService userService;
    private final NotificationService notificationService;
    private final PlatformSettingService platformSettingService;

    public AdminManagementRestController(UserService userService,
                                         NotificationService notificationService,
                                         PlatformSettingService platformSettingService) {
        this.userService = userService;
        this.notificationService = notificationService;
        this.platformSettingService = platformSettingService;
    }

    @GetMapping("/dashboard/summary")
    public DashboardSummaryResponse summary() {
        UserMetricsSummary userMetrics = userService.getMetrics();
        NotificationService.NotificationSummaryResponse notificationMetrics = notificationService.getAdminSummary();
        return new DashboardSummaryResponse(
                userMetrics.total(),
                userMetrics.cidadaos(),
                notificationMetrics.ativas(),
                notificationMetrics.naoLidas(),
                platformSettingService.count()
        );
    }

    @GetMapping("/users")
    public List<UserResponse> listUsers() {
        return userService.listAll().stream().map(this::toUserResponse).toList();
    }

    @GetMapping("/citizens")
    public UserService.CitizenPageResponse listCitizens(@RequestParam(required = false) String search,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return userService.listCitizens(search, page, size);
    }

    @PostMapping("/users")
    public UserResponse createUser(@RequestBody UserRequest request) {
        Usuario user = userService.createUser(
                request.nome(),
                request.email(),
                request.senha(),
                request.role(),
                request.cpf(),
                request.telefone(),
                request.endereco(),
                request.dataNascimento()
        );
        return toUserResponse(user);
    }

    @PutMapping("/users/{id}")
    public UserResponse updateUser(@PathVariable Long id, @RequestBody UserRequest request) {
        Usuario user = userService.updateUser(
                id,
                request.nome(),
                request.email(),
                request.senha(),
                request.role(),
                request.cpf(),
                request.telefone(),
                request.endereco(),
                request.dataNascimento()
        );
        return toUserResponse(user);
    }

    @PutMapping("/citizens/{id}")
    public UserService.CitizenResponse updateCitizen(@PathVariable Long id,
                                                     @RequestBody CitizenRequest request,
                                                     HttpSession session) {
        return userService.updateCitizen(
                id,
                request.nome(),
                request.email(),
                request.senha(),
                request.cpf(),
                request.telefone(),
                request.endereco(),
                request.dataNascimento(),
                currentAdminId(session)
        );
    }

    @PostMapping("/citizens/{id}/promote")
    public UserService.PromotionResult promoteCitizen(@PathVariable Long id,
                                                      @RequestBody PromotionRequest request,
                                                      HttpSession session) {
        return userService.promoteCitizenToAdmin(
                id,
                currentAdminId(session),
                request.confirmacao(),
                request.motivo()
        );
    }

    @GetMapping("/citizens/{id}/history")
    public List<UserService.AdminActionHistoryResponse> listCitizenHistory(@PathVariable Long id) {
        return userService.listCitizenHistory(id);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, HttpSession session) {
        SessionUser currentUser = (SessionUser) session.getAttribute(AdminAccessInterceptor.SESSION_USER_KEY);
        userService.deleteUser(id, currentUser.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/settings")
    public List<SettingResponse> listSettings() {
        return platformSettingService.listAll().stream()
                .map(setting -> new SettingResponse(setting.getChave(), setting.getValor(), setting.getDescricao()))
                .toList();
    }

    @PatchMapping("/settings/{chave}")
    public SettingResponse updateSetting(@PathVariable String chave, @RequestBody SettingRequest request) {
        PlatformSetting setting = platformSettingService.updateValue(chave, request.valor());
        return new SettingResponse(setting.getChave(), setting.getValor(), setting.getDescricao());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(exception.getMessage()));
    }

    private UserResponse toUserResponse(Usuario user) {
        return new UserResponse(
                user.getId(),
                user.getNome(),
                user.getEmail(),
                user.getCpf(),
                user.getTelefone(),
                user.getEndereco(),
                user.getRole().name()
        );
    }

    private Long currentAdminId(HttpSession session) {
        SessionUser currentUser = (SessionUser) session.getAttribute(AdminAccessInterceptor.SESSION_USER_KEY);
        if (currentUser == null || !currentUser.isAdmin()) {
            throw new IllegalArgumentException("Sessao administrativa invalida.");
        }
        return currentUser.id();
    }

    public record DashboardSummaryResponse(
            long totalUsuarios,
            long totalCidadaos,
            long notificacoesAtivas,
            long notificacoesNaoLidas,
            long configuracoesBasicas
    ) {
    }

    public record UserRequest(
            String nome,
            String email,
            String senha,
            String cpf,
            String telefone,
            String endereco,
            LocalDate dataNascimento,
            UserRole role
    ) {
    }

    public record UserResponse(
            Long id,
            String nome,
            String email,
            String cpf,
            String telefone,
            String endereco,
            String role
    ) {
    }

    public record CitizenRequest(
            String nome,
            String email,
            String senha,
            String cpf,
            String telefone,
            String endereco,
            LocalDate dataNascimento
    ) {
    }

    public record PromotionRequest(
            boolean confirmacao,
            String motivo
    ) {
    }

    public record SettingRequest(String valor) {
    }

    public record SettingResponse(String chave, String valor, String descricao) {
    }

    public record ErrorResponse(String message) {
    }
}
