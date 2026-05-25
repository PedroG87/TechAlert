package Techalert.TechAlert.service;


import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import Techalert.TechAlert.model.AppUser;
import Techalert.TechAlert.model.NotificationSeverity;
import Techalert.TechAlert.model.NotificationStatus;
import Techalert.TechAlert.model.NotificationType;
import Techalert.TechAlert.model.SystemNotification;
import Techalert.TechAlert.repository.AppUserRepository;
import Techalert.TechAlert.repository.SystemNotificationRepository;
import Techalert.TechAlert.security.UserRole;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SystemNotificationRepository systemNotificationRepository;
    private final AppUserRepository appUserRepository;

    public NotificationService(SystemNotificationRepository systemNotificationRepository,
                               AppUserRepository appUserRepository) {
        this.systemNotificationRepository = systemNotificationRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse listAdminNotifications(String search,
                                                           String readStatus,
                                                           NotificationType type,
                                                           NotificationSeverity severity,
                                                           NotificationStatus status,
                                                           Long userId,
                                                           int page,
                                                           int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "dataEnvio"));
        Page<SystemNotification> notificationPage = systemNotificationRepository.findAll(
                buildSpecification(search, readStatus, type, severity, status, userId, null), pageable);
        return toPageResponse(notificationPage);
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse listCitizenNotifications(Long currentUserId,
                                                             String search,
                                                             String readStatus,
                                                             NotificationType type,
                                                             int page,
                                                             int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "dataEnvio"));
        Page<SystemNotification> notificationPage = systemNotificationRepository.findAll(
                buildSpecification(search, readStatus, type, null, NotificationStatus.ATIVA, null, currentUserId), pageable);
        return toPageResponse(notificationPage);
    }

    @Transactional(readOnly = true)
    public NotificationSummaryResponse getAdminSummary() {
        long total = systemNotificationRepository.count();
        long ativas = systemNotificationRepository.countByStatus(NotificationStatus.ATIVA);
        long naoLidas = systemNotificationRepository.countByStatusAndLidaFalse(NotificationStatus.ATIVA);
        long criticas = systemNotificationRepository.findAll().stream()
                .filter(notification -> notification.getNivelPericulosidade() == NotificationSeverity.CRITICA)
                .count();
        return new NotificationSummaryResponse(total, ativas, naoLidas, criticas);
    }

    @Transactional(readOnly = true)
    public NotificationSummaryResponse getCitizenSummary(Long currentUserId) {
        List<SystemNotification> notifications = systemNotificationRepository.findAll(buildSpecification(
                null, null, null, null, NotificationStatus.ATIVA, null, currentUserId));
        long total = notifications.size();
        long ativas = total;
        long naoLidas = notifications.stream().filter(notification -> !notification.isLida()).count();
        long criticas = notifications.stream().filter(notification -> notification.getNivelPericulosidade() == NotificationSeverity.CRITICA).count();
        return new NotificationSummaryResponse(total, ativas, naoLidas, criticas);
    }

    @Transactional
    public BulkActionResponse createNotification(String titulo,
                                                 String conteudo,
                                                 NotificationType tipo,
                                                 NotificationSeverity severity,
                                                 Long targetUserId) {
        List<AppUser> targets;
        if (targetUserId != null) {
            AppUser target = appUserRepository.findById(targetUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario alvo nao encontrado."));
            targets = List.of(target);
        } else {
            targets = appUserRepository.findAllByRole(UserRole.CIDADAO);
        }

        if (targets.isEmpty()) {
            return new BulkActionResponse("Nenhum destinatario elegivel foi encontrado.", 0);
        }

        List<SystemNotification> notifications = new ArrayList<>();
        for (AppUser target : targets) {
            SystemNotification notification = new SystemNotification();
            notification.setTitulo(titulo);
            notification.setConteudo(conteudo);
            notification.setTipo(tipo);
            notification.setNivelPericulosidade(severity);
            notification.setStatus(NotificationStatus.ATIVA);
            notification.setLida(false);
            notification.setUsuarioAlvo(target);
            notifications.add(notification);
        }

        systemNotificationRepository.saveAll(notifications);
        return new BulkActionResponse("Notificacao criada com sucesso.", notifications.size());
    }

    @Transactional
    public void createNotificationForUsers(String titulo,
                                           String conteudo,
                                           NotificationType tipo,
                                           NotificationSeverity severity,
                                           List<AppUser> targets) {
        if (targets == null || targets.isEmpty()) {
            return;
        }

        List<SystemNotification> notifications = new ArrayList<>();
        for (AppUser target : targets) {
            SystemNotification notification = new SystemNotification();
            notification.setTitulo(titulo);
            notification.setConteudo(conteudo);
            notification.setTipo(tipo);
            notification.setNivelPericulosidade(severity);
            notification.setStatus(NotificationStatus.ATIVA);
            notification.setLida(false);
            notification.setUsuarioAlvo(target);
            notifications.add(notification);
        }
        systemNotificationRepository.saveAll(notifications);
    }

    @Transactional
    public NotificationResponse markAsReadForCitizen(long id, Long currentUserId) {
        SystemNotification notification = findCitizenNotification(id, currentUserId);
        notification.setLida(true);
        return toResponse(systemNotificationRepository.save(notification));
    }

    @Transactional
    public NotificationResponse updateNotificationStatus(long id, NotificationStatus status) {
        SystemNotification notification = findById(id);
        notification.setStatus(status);
        return toResponse(systemNotificationRepository.save(notification));
    }

    @Transactional
    public void deleteNotification(long id) {
        if (!systemNotificationRepository.existsById(id)) {
            throw new NotificationNotFoundException("Notificacao nao encontrada.");
        }
        systemNotificationRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<SystemNotification> listRecentForModeration() {
        return systemNotificationRepository.findAllByOrderByDataEnvioDesc();
    }

    private Specification<SystemNotification> buildSpecification(String search,
                                                                 String readStatus,
                                                                 NotificationType type,
                                                                 NotificationSeverity severity,
                                                                 NotificationStatus status,
                                                                 Long userId,
                                                                 Long currentUserId) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String normalized = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("titulo")), normalized),
                        cb.like(cb.lower(root.get("conteudo")), normalized),
                        cb.like(cb.lower(root.join("usuarioAlvo").get("nome")), normalized)
                ));
            }
            if ("LIDA".equalsIgnoreCase(readStatus)) {
                predicates.add(cb.isTrue(root.get("lida")));
            } else if ("NAO_LIDA".equalsIgnoreCase(readStatus)) {
                predicates.add(cb.isFalse(root.get("lida")));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("tipo"), type));
            }
            if (severity != null) {
                predicates.add(cb.equal(root.get("nivelPericulosidade"), severity));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.join("usuarioAlvo").get("id"), userId));
            }
            if (currentUserId != null) {
                predicates.add(cb.equal(root.join("usuarioAlvo").get("id"), currentUserId));
            }

            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private NotificationPageResponse toPageResponse(Page<SystemNotification> notificationPage) {
        return new NotificationPageResponse(
                notificationPage.getContent().stream().map(this::toResponse).toList(),
                notificationPage.getNumber(),
                notificationPage.getSize(),
                notificationPage.getTotalElements(),
                notificationPage.hasNext()
        );
    }

    private NotificationResponse toResponse(SystemNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitulo(),
                notification.getConteudo(),
                notification.getDataEnvio().format(DISPLAY_FORMATTER),
                notification.getUsuarioAlvo().getNome(),
                notification.getUsuarioAlvo().getEmail(),
                notification.isLida(),
                notification.isLida() ? "Lida" : "Nao lida",
                notification.getTipo().name(),
                notification.getNivelPericulosidade().name(),
                notification.getStatus().name()
        );
    }

    private SystemNotification findById(long id) {
        return systemNotificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notificacao nao encontrada."));
    }

    private SystemNotification findCitizenNotification(long id, Long currentUserId) {
        return systemNotificationRepository.findById(id)
                .filter(notification -> Objects.equals(notification.getUsuarioAlvo().getId(), currentUserId))
                .orElseThrow(() -> new NotificationNotFoundException("Notificacao nao encontrada."));
    }

    public record NotificationResponse(
            Long id,
            String titulo,
            String conteudo,
            String dataEnvio,
            String usuarioAlvo,
            String emailAlvo,
            boolean lida,
            String statusLeitura,
            String tipo,
            String nivelPericulosidade,
            String status
    ) {
    }

    public record NotificationPageResponse(
            List<NotificationResponse> content,
            int page,
            int size,
            long totalElements,
            boolean hasNext
    ) {
    }

    public record NotificationSummaryResponse(
            long total,
            long ativas,
            long naoLidas,
            long criticas
    ) {
    }

    public record BulkActionResponse(
            String message,
            long affected
    ) {
    }

    public static class NotificationNotFoundException extends RuntimeException {
        public NotificationNotFoundException(String message) {
            super(message);
        }
    }
}
