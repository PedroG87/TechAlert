package Techalert.TechAlert.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import Techalert.TechAlert.model.Ocorrencia;

public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Long> {
    List<Ocorrencia> findByStatusOcorrencia(String status);
    List<Ocorrencia> findByUsuarioId(Long usuarioId);
    List<Ocorrencia> findByAreaRiscoId(Long areaRiscoId);
    List<Ocorrencia> findByStatusOcorrenciaOrderByDataOcorrenciaDesc(String status);
    List<Ocorrencia> findByAreaRiscoIdAndStatusOcorrencia(Long areaRiscoId, String status);
}