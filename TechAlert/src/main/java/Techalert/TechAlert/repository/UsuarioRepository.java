package Techalert.TechAlert.repository;

import java.util.List;
import java.util.Optional;

import Techalert.TechAlert.model.Usuario;
import Techalert.TechAlert.security.UserRole;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByCpf(String cpf);

    List<Usuario> findAllByRole(UserRole role);

    long countByRole(UserRole role);
}
