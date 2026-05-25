package Techalert.TechAlert.controller;

import Techalert.TechAlert.model.NotificationSeverity;
import Techalert.TechAlert.model.NotificationStatus;
import Techalert.TechAlert.model.NotificationType;
import Techalert.TechAlert.service.NotificationService;
import Techalert.TechAlert.service.NotificationService.BulkActionResponse;
import Techalert.TechAlert.service.NotificationService.NotificationNotFoundException;
import Techalert.TechAlert.service.NotificationService.NotificationPageResponse;
import Techalert.TechAlert.service.NotificationService.NotificationResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/adm/notificacoes")
public class AdminNotificationRestController {

    private final NotificationService notificationService;

    public AdminNotificationRestController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public NotificationPageResponse list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String readStatus,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) NotificationSeverity severity,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size
    ) {
        return notificationService.listAdminNotifications(search, readStatus, type, severity, status, userId, page, size);
    }

    @PostMapping
    public BulkActionResponse create(@RequestBody CreateNotificationRequest request) {
        return notificationService.createNotification(
                request.titulo(),
                request.conteudo(),
                request.tipo(),
                request.nivelPericulosidade(),
                request.usuarioAlvoId()
        );
    }

    @PatchMapping("/{id}/status")
    public NotificationResponse updateStatus(@PathVariable long id, @RequestParam NotificationStatus status) {
        return notificationService.updateNotificationStatus(id, status);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotificationNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Nao foi possivel processar a operacao de notificacoes."));
    }

    public record ErrorResponse(String message) {
    }

    public record CreateNotificationRequest(
            String titulo,
            String conteudo,
            NotificationType tipo,
            NotificationSeverity nivelPericulosidade,
            Long usuarioAlvoId
    ) {
    }
}
