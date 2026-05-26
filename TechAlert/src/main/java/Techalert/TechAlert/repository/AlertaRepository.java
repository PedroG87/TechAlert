package Techalert.TechAlert.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import Techalert.TechAlert.model.Alerta;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {
    List<Alerta> findByStatusAlerta(String status);
    List<Alerta> findByAreaRiscoId(Long areaRiscoId);
    List<Alerta> findByNivelAlerta(String nivel);
    List<Alerta> findByStatusAlertaOrderByDataEmissaoDesc(String status);
    List<Alerta> findByAreaRiscoIdAndStatusAlerta(Long areaRiscoId, String status);
}