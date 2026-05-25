package Techalert.TechAlert.security;

import java.util.Optional;

import Techalert.TechAlert.model.AppUser;
import Techalert.TechAlert.repository.AppUserRepository;

import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;

    public AuthService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public Optional<SessionUser> authenticate(String email, String senha) {
        if (email == null || senha == null) {
            return Optional.empty();
        }

        return appUserRepository.findByEmailIgnoreCase(email.trim())
                .filter(user -> senha.equals(user.getSenha()))
                .map(this::toSessionUser);
    }

    public SessionUser toSessionUser(AppUser user) {
        return new SessionUser(user.getId(), user.getNome(), user.getEmail(), user.getRole());
    }
}
