package Techalert.TechAlert.repository;

import java.util.List;
import java.util.Optional;

import Techalert.TechAlert.model.PlatformSetting;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, Long> {

    Optional<PlatformSetting> findByChave(String chave);

    List<PlatformSetting> findAllByOrderByChaveAsc();
}
