package Techalert.TechAlert.controller;

import jakarta.servlet.http.HttpSession;

import Techalert.TechAlert.model.NotificationType;
import Techalert.TechAlert.security.AdminAccessInterceptor;
import Techalert.TechAlert.security.SessionUser;
import Techalert.TechAlert.service.NotificationService;
import Techalert.TechAlert.service.NotificationService.NotificationNotFoundException;
import Techalert.TechAlert.service.NotificationService.NotificationPageResponse;
import Techalert.TechAlert.service.NotificationService.NotificationResponse;
import Techalert.TechAlert.service.NotificationService.NotificationSummaryResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cidadao/notificacoes")
public class CitizenNotificationRestController {

    private final NotificationService notificationService;

    public CitizenNotificationRestController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public NotificationPageResponse list(HttpSession session,
                                         @RequestParam(required = false) String search,
                                         @RequestParam(required = false) String readStatus,
                                         @RequestParam(required = false) NotificationType type,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "8") int size) {
        SessionUser currentUser = currentUser(session);
        return notificationService.listCitizenNotifications(currentUser.id(), search, readStatus, type, page, size);
    }

    @GetMapping("/summary")
    public NotificationSummaryResponse summary(HttpSession session) {
        return notificationService.getCitizenSummary(currentUser(session).id());
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(HttpSession session, @PathVariable long id) {
        return notificationService.markAsReadForCitizen(id, currentUser(session).id());
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotificationNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Nao foi possivel processar a operacao de notificacoes do cidadao."));
    }

    private SessionUser currentUser(HttpSession session) {
        return (SessionUser) session.getAttribute(AdminAccessInterceptor.SESSION_USER_KEY);
    }

    public record ErrorResponse(String message) {
    }
}
