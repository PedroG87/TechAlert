package Techalert.TechAlert.repository;

import java.util.List;

import Techalert.TechAlert.model.AdminActionLog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {

    List<AdminActionLog> findAllByUsuarioAlvoIdOrderByCriadoEmDesc(Long usuarioAlvoId);
}
