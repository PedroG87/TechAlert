package Techalert.TechAlert.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import Techalert.TechAlert.model.AlertaUsuario;

public interface AlertaUsuarioRepository extends JpaRepository<AlertaUsuario, Long> {
    List<AlertaUsuario> findByUsuarioId(Long usuarioId);
    List<AlertaUsuario> findByAlertaId(Long alertaId);
    List<AlertaUsuario> findByUsuarioIdAndStatusEntrega(Long usuarioId, String status);
    long countByAlertaIdAndStatusEntrega(Long alertaId, String status);
}