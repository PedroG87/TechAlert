package Techalert.TechAlert.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import Techalert.TechAlert.model.AppUser;
import Techalert.TechAlert.repository.AppUserRepository;
import Techalert.TechAlert.security.UserRole;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final AppUserRepository appUserRepository;

    public UserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
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

        if (appUserRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return RegistrationResult.failure("Ja existe uma conta cadastrada com este e-mail.");
        }
        if (!normalizedCpf.isBlank() && appUserRepository.existsByCpf(normalizedCpf)) {
            return RegistrationResult.failure("Ja existe uma conta cadastrada com este CPF.");
        }

        AppUser user = new AppUser();
        user.setNome(nome.trim());
        user.setEmail(normalizedEmail);
        user.setSenha(senha);
        user.setRole(UserRole.CIDADAO);
        user.setCpf(normalizedCpf.isBlank() ? null : normalizedCpf);
        user.setTelefone(blankToNull(telefone));
        user.setEndereco(blankToNull(endereco));
        user.setDataNascimento(toDate(dataNascimento));
        appUserRepository.save(user);

        return RegistrationResult.success(user.getId());
    }

    @Transactional(readOnly = true)
    public Optional<AppUser> findById(Long id) {
        return appUserRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<AppUser> findByRole(UserRole role) {
        return appUserRepository.findAllByRole(role);
    }

    @Transactional(readOnly = true)
    public List<AppUser> listAll() {
        return appUserRepository.findAll();
    }

    @Transactional
    public AppUser createUser(String nome,
                              String email,
                              String senha,
                              UserRole role,
                              String cpf,
                              String telefone,
                              String endereco,
                              LocalDate dataNascimento) {
        AppUser user = new AppUser();
        user.setNome(nome);
        user.setEmail(email.trim().toLowerCase(Locale.ROOT));
        user.setSenha(senha);
        user.setRole(role);
        user.setCpf(blankToNull(cpf));
        user.setTelefone(blankToNull(telefone));
        user.setEndereco(blankToNull(endereco));
        user.setDataNascimento(toDate(dataNascimento));
        return appUserRepository.save(user);
    }

    @Transactional
    public AppUser updateUser(Long id,
                              String nome,
                              String email,
                              String senha,
                              UserRole role,
                              String cpf,
                              String telefone,
                              String endereco,
                              LocalDate dataNascimento) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado."));

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedCpf = cpf == null ? null : cpf.replaceAll("\\D", "");

        appUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ja existe outro usuario com este e-mail.");
                });

        if (normalizedCpf != null && !normalizedCpf.isBlank()) {
            appUserRepository.findAll().stream()
                    .filter(existing -> normalizedCpf.equals(existing.getCpf()))
                    .filter(existing -> !existing.getId().equals(id))
                    .findFirst()
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Ja existe outro usuario com este CPF.");
                    });
        }

        user.setNome(nome.trim());
        user.setEmail(normalizedEmail);
        if (senha != null && !senha.isBlank()) {
            user.setSenha(senha);
        }
        user.setRole(role);
        user.setCpf(normalizedCpf == null || normalizedCpf.isBlank() ? null : normalizedCpf);
        user.setTelefone(blankToNull(telefone));
        user.setEndereco(blankToNull(endereco));
        user.setDataNascimento(toDate(dataNascimento));
        return appUserRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new IllegalArgumentException("O administrador logado nao pode remover a propria conta.");
        }
        if (!appUserRepository.existsById(id)) {
            throw new IllegalArgumentException("Usuario nao encontrado.");
        }
        appUserRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public UserMetricsSummary getMetrics() {
        long total = appUserRepository.count();
        long admins = appUserRepository.countByRole(UserRole.ADM);
        long cidadaos = appUserRepository.countByRole(UserRole.CIDADAO);
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

    private Date toDate(LocalDate value) {
        if (value == null) {
            return null;
        }
        return Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant());
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
}
