package Techalert.TechAlert.service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import Techalert.TechAlert.model.AdminActionLog;
import Techalert.TechAlert.model.AdminActionType;
import Techalert.TechAlert.model.Usuario;
import Techalert.TechAlert.repository.AdminActionLogRepository;
import Techalert.TechAlert.repository.UsuarioRepository;
import Techalert.TechAlert.security.UserRole;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final DateTimeFormatter HISTORY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UsuarioRepository usuarioRepository;
    private final AdminActionLogRepository adminActionLogRepository;

    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public UserService(UsuarioRepository usuarioRepository,
                       AdminActionLogRepository adminActionLogRepository,
                       org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.adminActionLogRepository = adminActionLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegistrationResult registerCitizen(String nome,
                                              String email,
                                              String senha,
                                              String cpf,
                                              String telefone,
                                              String endereco,
                                              LocalDate dataNascimento) {
        if (isBlank(nome) || isBlank(email) || isBlank(senha) || isBlank(cpf)) {
            return RegistrationResult.failure("Preencha os campos obrigatorios para concluir o cadastro.");
        }
        if (senha.length() < 6) {
            return RegistrationResult.failure("A senha precisa ter pelo menos 6 caracteres.");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedCpf = cpf.replaceAll("\\D", "");

        if (usuarioRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return RegistrationResult.failure("Ja existe uma conta cadastrada com este e-mail.");
        }
        if (!normalizedCpf.isBlank() && usuarioRepository.existsByCpf(normalizedCpf)) {
            return RegistrationResult.failure("Ja existe uma conta cadastrada com este CPF.");
        }

        Usuario user = new Usuario();
        user.setNome(nome.trim());
        user.setEmail(normalizedEmail);
        user.setSenha(passwordEncoder.encode(senha));
        applyRoleAndPerfil(user, UserRole.CIDADAO);
        user.setCpf(normalizedCpf.isBlank() ? null : normalizedCpf);
        user.setTelefone(blankToNull(telefone));
        user.setEndereco(blankToNull(endereco));
        user.setDataNascimento(toDate(dataNascimento));
        usuarioRepository.save(user);

        return RegistrationResult.success(user.getId());
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> findById(Long id) {
        return usuarioRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Usuario> findByRole(UserRole role) {
        return usuarioRepository.findAllByRole(role);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listAll() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public CitizenPageResponse listCitizens(String search, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizePageSize(size),
                Sort.by(Sort.Direction.ASC, "nome")
        );

        Page<Usuario> citizenPage = usuarioRepository.findAll(buildCitizenSpecification(search), pageable);
        return new CitizenPageResponse(
                citizenPage.getContent().stream().map(this::toCitizenResponse).toList(),
                citizenPage.getNumber(),
                citizenPage.getSize(),
                citizenPage.getTotalElements(),
                citizenPage.hasNext()
        );
    }

    @Transactional
    public Usuario createUser(String nome,
                              String email,
                              String senha,
                              UserRole role,
                              String cpf,
                              String telefone,
                              String endereco,
                              LocalDate dataNascimento) {
        validateUserInput(nome, email, senha, role, cpf, null);

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedCpf = cpf == null ? null : cpf.replaceAll("\\D", "");
        ensureEmailAvailable(normalizedEmail, null);
        ensureCpfAvailable(normalizedCpf, null);

        Usuario user = new Usuario();
        user.setNome(nome.trim());
        user.setEmail(normalizedEmail);
        user.setSenha(passwordEncoder.encode(senha));
        applyRoleAndPerfil(user, role);
        user.setCpf(blankToNull(normalizedCpf));
        user.setTelefone(blankToNull(telefone));
        user.setEndereco(blankToNull(endereco));
        user.setDataNascimento(toDate(dataNascimento));
        return usuarioRepository.save(user);
    }

    @Transactional
    public Usuario updateUser(Long id,
                              String nome,
                              String email,
                              String senha,
                              UserRole role,
                              String cpf,
                              String telefone,
                              String endereco,
                              LocalDate dataNascimento) {
        Usuario user = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado."));

        validateUserInput(nome, email, senha, role, cpf, id);

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedCpf = cpf == null ? null : cpf.replaceAll("\\D", "");

        ensureEmailAvailable(normalizedEmail, id);
        ensureCpfAvailable(normalizedCpf, id);

        user.setNome(nome.trim());
        user.setEmail(normalizedEmail);
        if (senha != null && !senha.isBlank()) {
            if (senha.length() < 6) {
                throw new IllegalArgumentException("A senha precisa ter pelo menos 6 caracteres.");
            }
            user.setSenha(passwordEncoder.encode(senha));
        }
        applyRoleAndPerfil(user, role);
        user.setCpf(normalizedCpf == null || normalizedCpf.isBlank() ? null : normalizedCpf);
        user.setTelefone(blankToNull(telefone));
        user.setEndereco(blankToNull(endereco));
        user.setDataNascimento(toDate(dataNascimento));
        return usuarioRepository.save(user);
    }

    @Transactional
    public CitizenResponse updateCitizen(Long id,
                                         String nome,
                                         String email,
                                         String senha,
                                         String cpf,
                                         String telefone,
                                         String endereco,
                                         LocalDate dataNascimento,
                                         Long adminUserId) {
        Usuario citizenBeforeUpdate = findCitizenById(id);
        String previousState = describeCitizen(citizenBeforeUpdate);

        Usuario updatedCitizen = updateUser(
                id,
                nome,
                email,
                senha,
                UserRole.CIDADAO,
                cpf,
                telefone,
                endereco,
                dataNascimento
        );

        logAdminAction(
                adminUserId,
                updatedCitizen,
                AdminActionType.CIDADAO_ATUALIZADO,
                previousState,
                describeCitizen(updatedCitizen),
                "Cadastro de cidadao atualizado pelo painel administrativo."
        );

        return toCitizenResponse(updatedCitizen);
    }

    @Transactional
    public PromotionResult promoteCitizenToAdmin(Long citizenId,
                                                 Long adminUserId,
                                                 boolean confirmation,
                                                 String reason) {
        if (!confirmation) {
            throw new IllegalArgumentException("Confirme a promocao antes de concluir a alteracao.");
        }

        Usuario citizen = findCitizenById(citizenId);
        citizen.setRole(UserRole.ADM);
        Usuario promotedUser = usuarioRepository.save(citizen);

        String description = "Cidadao promovido para administrador pelo painel administrativo.";
        if (reason != null && !reason.isBlank()) {
            description = description + " Motivo: " + reason.trim();
        }

        logAdminAction(
                adminUserId,
                promotedUser,
                AdminActionType.CIDADAO_PROMOVIDO,
                UserRole.CIDADAO.name(),
                UserRole.ADM.name(),
                description
        );

        return new PromotionResult(
                promotedUser.getId(),
                promotedUser.getNome(),
                promotedUser.getRole().name(),
                "Promocao realizada com sucesso."
        );
    }

    @Transactional(readOnly = true)
    public List<AdminActionHistoryResponse> listCitizenHistory(Long userId) {
        if (!usuarioRepository.existsById(userId)) {
            throw new IllegalArgumentException("Usuario nao encontrado.");
        }

        return adminActionLogRepository.findAllByUsuarioAlvoIdOrderByCriadoEmDesc(userId).stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    @Transactional
    public void deleteUser(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new IllegalArgumentException("O administrador logado nao pode remover a propria conta.");
        }
        if (!usuarioRepository.existsById(id)) {
            throw new IllegalArgumentException("Usuario nao encontrado.");
        }
        usuarioRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public UserMetricsSummary getMetrics() {
        long total = usuarioRepository.count();
        long admins = usuarioRepository.countByRole(UserRole.ADM);
        long cidadaos = usuarioRepository.countByRole(UserRole.CIDADAO);
        return new UserMetricsSummary(total, admins, cidadaos);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void validateUserInput(String nome,
                                   String email,
                                   String senha,
                                   UserRole role,
                                   String cpf,
                                   Long currentUserId) {
        if (isBlank(nome) || isBlank(email) || isBlank(cpf)) {
            throw new IllegalArgumentException("Nome, e-mail e CPF sao obrigatorios.");
        }
        if (nome.trim().length() < 3) {
            throw new IllegalArgumentException("Informe um nome com pelo menos 3 caracteres.");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Informe um e-mail valido.");
        }
        if (currentUserId == null && isBlank(senha)) {
            throw new IllegalArgumentException("A senha e obrigatoria para novos usuarios.");
        }
        if (senha != null && !senha.isBlank() && senha.length() < 6) {
            throw new IllegalArgumentException("A senha precisa ter pelo menos 6 caracteres.");
        }
        if (role == null) {
            throw new IllegalArgumentException("O papel do usuario e obrigatorio.");
        }
        String normalizedCpf = cpf.replaceAll("\\D", "");
        if (normalizedCpf.length() != 11) {
            throw new IllegalArgumentException("Informe um CPF valido com 11 digitos.");
        }
        Usuario.perfilForRole(role);
    }

    private void applyRoleAndPerfil(Usuario user, UserRole role) {
        user.setRole(role);
        user.setPerfil(Usuario.perfilForRole(role));
    }

    private void ensureEmailAvailable(String normalizedEmail, Long currentUserId) {
        usuarioRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(existing -> currentUserId == null || !existing.getId().equals(currentUserId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ja existe outro usuario com este e-mail.");
                });
    }

    private void ensureCpfAvailable(String normalizedCpf, Long currentUserId) {
        if (normalizedCpf == null || normalizedCpf.isBlank()) {
            return;
        }

        usuarioRepository.findAll().stream()
                .filter(existing -> normalizedCpf.equals(existing.getCpf()))
                .filter(existing -> currentUserId == null || !existing.getId().equals(currentUserId))
                .findFirst()
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ja existe outro usuario com este CPF.");
                });
    }

    private Specification<Usuario> buildCitizenSpecification(String search) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("role"), UserRole.CIDADAO));

            if (search != null && !search.isBlank()) {
                String normalizedSearch = search.trim().toLowerCase(Locale.ROOT);
                String normalizedCpf = search.replaceAll("\\D", "");
                List<jakarta.persistence.criteria.Predicate> searchPredicates = new ArrayList<>();
                searchPredicates.add(cb.like(cb.lower(root.get("nome")), "%" + normalizedSearch + "%"));
                searchPredicates.add(cb.like(cb.lower(root.get("email")), "%" + normalizedSearch + "%"));
                if (!normalizedCpf.isBlank()) {
                    searchPredicates.add(cb.like(root.get("cpf"), "%" + normalizedCpf + "%"));
                }
                predicates.add(cb.or(searchPredicates.toArray(jakarta.persistence.criteria.Predicate[]::new)));
            }

            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private Usuario findCitizenById(Long id) {
        Usuario user = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado."));
        if (user.getRole() != UserRole.CIDADAO) {
            throw new IllegalArgumentException("A operacao so pode ser realizada para cidadaos.");
        }
        return user;
    }

    private void logAdminAction(Long adminUserId,
                                Usuario targetUser,
                                AdminActionType actionType,
                                String previousValue,
                                String newValue,
                                String description) {
        Usuario adminUser = usuarioRepository.findById(adminUserId)
                .filter(Usuario::isAdmin)
                .orElseThrow(() -> new IllegalArgumentException("Administrador responsavel nao encontrado."));

        AdminActionLog log = new AdminActionLog();
        log.setAdminUsuario(adminUser);
        log.setUsuarioAlvo(targetUser);
        log.setActionType(actionType);
        log.setValorAnterior(previousValue);
        log.setValorNovo(newValue);
        log.setDescricao(description);
        adminActionLogRepository.save(log);
    }

    private CitizenResponse toCitizenResponse(Usuario user) {
        return new CitizenResponse(
                user.getId(),
                user.getNome(),
                user.getEmail(),
                user.getCpf(),
                maskCpf(user.getCpf()),
                user.getTelefone(),
                user.getEndereco(),
                toLocalDate(user.getDataNascimento()),
                user.getRole().name()
        );
    }

    private AdminActionHistoryResponse toHistoryResponse(AdminActionLog log) {
        return new AdminActionHistoryResponse(
                log.getId(),
                log.getActionType().name(),
                log.getAdminUsuario().getNome(),
                log.getValorAnterior(),
                log.getValorNovo(),
                log.getDescricao(),
                log.getCriadoEm().format(HISTORY_DATE_FORMATTER)
        );
    }

    private String describeCitizen(Usuario user) {
        return "nome=" + user.getNome()
                + "; email=" + user.getEmail()
                + "; cpf=" + maskCpf(user.getCpf())
                + "; telefone=" + blankToNull(user.getTelefone())
                + "; endereco=" + blankToNull(user.getEndereco());
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private Date toDate(LocalDate value) {
        if (value == null) {
            return null;
        }
        return Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private LocalDate toLocalDate(Date value) {
        if (value == null) {
            return null;
        }
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private String maskCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + ".***.***-" + cpf.substring(9);
    }

    public record RegistrationResult(boolean success, String message, Long userId) {
        public static RegistrationResult success(Long userId) {
            return new RegistrationResult(true, null, userId);
        }

        public static RegistrationResult failure(String message) {
            return new RegistrationResult(false, message, null);
        }
    }

    public record UserMetricsSummary(long total, long admins, long cidadaos) {
    }

    public record CitizenResponse(
            Long id,
            String nome,
            String email,
            String cpf,
            String cpfMascarado,
            String telefone,
            String endereco,
            LocalDate dataNascimento,
            String role
    ) {
    }

    public record CitizenPageResponse(
            List<CitizenResponse> content,
            int page,
            int size,
            long totalElements,
            boolean hasNext
    ) {
    }

    public record PromotionResult(
            Long id,
            String nome,
            String role,
            String message
    ) {
    }

    public record AdminActionHistoryResponse(
            Long id,
            String actionType,
            String adminNome,
            String valorAnterior,
            String valorNovo,
            String descricao,
            String criadoEm
    ) {
    }
}
