package Techalert.TechAlert.repository;

import java.util.List;
import java.util.Optional;

import Techalert.TechAlert.model.AppUser;
import Techalert.TechAlert.security.UserRole;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByCpf(String cpf);

    List<AppUser> findAllByRole(UserRole role);

    long countByRole(UserRole role);
}
