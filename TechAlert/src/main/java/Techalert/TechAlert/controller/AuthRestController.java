package Techalert.TechAlert.controller;

import jakarta.servlet.http.HttpSession;

import Techalert.TechAlert.security.AdminAccessInterceptor;
import Techalert.TechAlert.security.SessionUser;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    @GetMapping("/session")
    public ResponseEntity<SessionResponse> getSession(HttpSession session) {
        Object attribute = session.getAttribute(AdminAccessInterceptor.SESSION_USER_KEY);
        if (!(attribute instanceof SessionUser user) || !user.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new SessionResponse(false, null, null, null, null));
        }

        return ResponseEntity.ok(new SessionResponse(true, user.id(), user.nome(), user.email(), user.role().name()));
    }

    public record SessionResponse(
            boolean authenticated,
            Long id,
            String nome,
            String email,
            String role
    ) {
    }
}
