package Techalert.TechAlert.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import Techalert.TechAlert.model.Usuario;
import Techalert.TechAlert.repository.UsuarioRepository;
import Techalert.TechAlert.security.UserRole;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UsuarioRepository usuarioRepository;

    public UserService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
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
        user.setSenha(senha);
        user.setRole(UserRole.CIDADAO);
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

    @Transactional
    public Usuario createUser(String nome,
                              String email,
                              String senha,
                              UserRole role,
                              String cpf,
                              String telefone,
                              String endereco,
                              LocalDate dataNascimento) {
        Usuario user = new Usuario();
        user.setNome(nome);
        user.setEmail(email.trim().toLowerCase(Locale.ROOT));
        user.setSenha(senha);
        user.setRole(role);
        user.setCpf(blankToNull(cpf));
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

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedCpf = cpf == null ? null : cpf.replaceAll("\\D", "");

        usuarioRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ja existe outro usuario com este e-mail.");
                });

        if (normalizedCpf != null && !normalizedCpf.isBlank()) {
            usuarioRepository.findAll().stream()
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
        return usuarioRepository.save(user);
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
