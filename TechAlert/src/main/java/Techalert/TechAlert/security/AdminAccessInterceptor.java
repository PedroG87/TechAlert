package Techalert.TechAlert.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAccessInterceptor implements HandlerInterceptor {

    public static final String SESSION_USER_KEY = "currentUser";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        SessionUser currentUser = session == null ? null : (SessionUser) session.getAttribute(SESSION_USER_KEY);

        if (currentUser != null && currentUser.isAdmin()) {
            return true;
        }

        boolean apiRequest = request.getRequestURI().startsWith("/api/");
        if (apiRequest) {
            writeApiError(response, currentUser == null ? HttpServletResponse.SC_UNAUTHORIZED : HttpServletResponse.SC_FORBIDDEN,
                    currentUser == null ? "Sessao expirada ou inexistente. Faca login para continuar." :
                            "Acesso negado. Apenas administradores podem gerenciar notificacoes.");
            return false;
        }

        response.sendRedirect(currentUser == null ? "/login?erro=autenticacao" : "/home?erro=acesso");
        return false;
    }

    private void writeApiError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
