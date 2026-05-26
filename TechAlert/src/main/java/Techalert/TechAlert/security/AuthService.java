package Techalert.TechAlert.security;

import java.util.Optional;

import Techalert.TechAlert.model.Usuario;
import Techalert.TechAlert.repository.UsuarioRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<SessionUser> authenticate(String email, String senha) {
        if (email == null || senha == null) {
            return Optional.empty();
        }

        return usuarioRepository.findByEmailIgnoreCase(email.trim())
                .filter(user -> passwordEncoder.matches(senha, user.getSenha()))
                .map(this::toSessionUser);
    }

    public SessionUser toSessionUser(Usuario user) {
        return new SessionUser(user.getId(), user.getNome(), user.getEmail(), user.getRole());
    }
}
