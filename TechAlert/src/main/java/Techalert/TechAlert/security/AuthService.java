package Techalert.TechAlert.security;

import java.util.Optional;

import Techalert.TechAlert.model.Usuario;
import Techalert.TechAlert.repository.UsuarioRepository;

import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;

    public AuthService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Optional<SessionUser> authenticate(String email, String senha) {
        if (email == null || senha == null) {
            return Optional.empty();
        }

        return usuarioRepository.findByEmailIgnoreCase(email.trim())
                .filter(user -> senha.equals(user.getSenha()))
                .map(this::toSessionUser);
    }

    public SessionUser toSessionUser(Usuario user) {
        return new SessionUser(user.getId(), user.getNome(), user.getEmail(), user.getRole());
    }
}
