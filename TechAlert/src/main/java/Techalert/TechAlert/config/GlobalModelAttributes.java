package Techalert.TechAlert.config;

import jakarta.servlet.http.HttpSession;

import Techalert.TechAlert.security.AdminAccessInterceptor;
import Techalert.TechAlert.security.SessionUser;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("currentUser")
    public SessionUser currentUser(HttpSession session) {
        Object attribute = session.getAttribute(AdminAccessInterceptor.SESSION_USER_KEY);
        return attribute instanceof SessionUser user ? user : null;
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin(HttpSession session) {
        Object attribute = session.getAttribute(AdminAccessInterceptor.SESSION_USER_KEY);
        return attribute instanceof SessionUser user && user.isAdmin();
    }

    @ModelAttribute("isCitizen")
    public boolean isCitizen(HttpSession session) {
        Object attribute = session.getAttribute(AdminAccessInterceptor.SESSION_USER_KEY);
        return attribute instanceof SessionUser user && user.isCitizen();
    }
}
