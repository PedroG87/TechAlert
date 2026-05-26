package Techalert.TechAlert.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import Techalert.TechAlert.model.AreaRisco;

public interface AreaRiscoRepository extends JpaRepository<AreaRisco, Long> {
    List<AreaRisco> findByAtivoTrue();
    List<AreaRisco> findByBairroIgnoreCase(String bairro);
    List<AreaRisco> findByTipoRisco(String tipoRisco);
    List<AreaRisco> findByNivelCriticidade(String nivelCriticidade);
}