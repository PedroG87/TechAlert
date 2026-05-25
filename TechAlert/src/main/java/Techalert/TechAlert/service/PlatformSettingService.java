package Techalert.TechAlert.service;

import java.util.List;
import java.util.Optional;

import Techalert.TechAlert.model.PlatformSetting;
import Techalert.TechAlert.repository.PlatformSettingRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformSettingService {

    private final PlatformSettingRepository platformSettingRepository;

    public PlatformSettingService(PlatformSettingRepository platformSettingRepository) {
        this.platformSettingRepository = platformSettingRepository;
    }

    @Transactional(readOnly = true)
    public List<PlatformSetting> listAll() {
        return platformSettingRepository.findAllByOrderByChaveAsc();
    }

    @Transactional(readOnly = true)
    public String getValue(String chave, String defaultValue) {
        return platformSettingRepository.findByChave(chave)
                .map(PlatformSetting::getValor)
                .orElse(defaultValue);
    }

    @Transactional
    public PlatformSetting save(String chave, String valor, String descricao) {
        PlatformSetting setting = platformSettingRepository.findByChave(chave).orElseGet(PlatformSetting::new);
        setting.setChave(chave);
        setting.setValor(valor);
        setting.setDescricao(descricao);
        return platformSettingRepository.save(setting);
    }

    @Transactional
    public PlatformSetting updateValue(String chave, String valor) {
        PlatformSetting setting = platformSettingRepository.findByChave(chave)
                .orElseThrow(() -> new IllegalArgumentException("Configuracao nao encontrada."));
        setting.setValor(valor);
        return platformSettingRepository.save(setting);
    }

    @Transactional(readOnly = true)
    public long count() {
        return platformSettingRepository.count();
    }

    @Transactional(readOnly = true)
    public Optional<PlatformSetting> findByChave(String chave) {
        return platformSettingRepository.findByChave(chave);
    }
}
