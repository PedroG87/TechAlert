package Techalert.TechAlert.config;

import java.time.LocalDate;
import java.util.List;

import Techalert.TechAlert.model.Usuario;
import Techalert.TechAlert.model.NotificationSeverity;
import Techalert.TechAlert.model.NotificationType;
import Techalert.TechAlert.repository.UsuarioRepository;
import Techalert.TechAlert.repository.SystemNotificationRepository;
import Techalert.TechAlert.security.UserRole;
import Techalert.TechAlert.service.NotificationService;
import Techalert.TechAlert.service.PlatformSettingService;
import Techalert.TechAlert.service.UserService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedApplicationData(UserService userService,
                                          UsuarioRepository usuarioRepository,
                                          SystemNotificationRepository systemNotificationRepository,
                                          NotificationService notificationService,
                                          PlatformSettingService platformSettingService) {
        return args -> {
            platformSettingService.save("platform.name", "TechAlert", "Nome principal da plataforma.");
            platformSettingService.save("home.hero.message", "Receba alertas essenciais e acompanhe a plataforma com foco no que realmente importa.", "Mensagem principal exibida na home.");
            platformSettingService.save("platform.support.email", "suporte@techalert.com", "Contato basico de suporte do MVP.");

            Usuario admin = usuarioRepository.findByEmailIgnoreCase("admin@techalert.com")
                    .orElseGet(() -> userService.createUser(
                            "Administrador TechAlert",
                            "admin@techalert.com",
                            "Adm123!",
                            UserRole.ADM,
                            "11111111111",
                            "(11) 98888-0001",
                            "Centro de Operacoes",
                            LocalDate.of(1988, 5, 10)
                    ));

            if (!Usuario.perfilForRole(admin.getRole()).equals(admin.getPerfil())) {
                admin.setPerfil(Usuario.perfilForRole(admin.getRole()));
                usuarioRepository.save(admin);
            }

            Usuario citizenOne = usuarioRepository.findByEmailIgnoreCase("cidadao@techalert.com")
                    .orElseGet(() -> userService.createUser(
                            "Cidadao TechAlert",
                            "cidadao@techalert.com",
                            "Cid123!",
                            UserRole.CIDADAO,
                            "22222222222",
                            "(11) 97777-0002",
                            "Rua das Flores, 120",
                            LocalDate.of(1995, 7, 22)
                    ));

            if (!Usuario.perfilForRole(citizenOne.getRole()).equals(citizenOne.getPerfil())) {
                citizenOne.setPerfil(Usuario.perfilForRole(citizenOne.getRole()));
                usuarioRepository.save(citizenOne);
            }

            if (systemNotificationRepository.count() == 0) {
                notificationService.createNotificationForUsers(
                        "Alerta de chuva intensa",
                        "Risco elevado de alagamento para a Zona Norte nas proximas horas. Evite deslocamentos desnecessarios.",
                        NotificationType.ALERTA,
                        NotificationSeverity.CRITICA,
                        List.of(admin, citizenOne)
                );
                notificationService.createNotificationForUsers(
                        "Comunicado operacional",
                        "O sistema MVP esta disponivel para consulta de alertas, moderacao administrativa e configuracoes basicas.",
                        NotificationType.SISTEMA,
                        NotificationSeverity.MEDIA,
                        List.of(admin)
                );
            }
        };
    }
}
