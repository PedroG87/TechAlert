package Techalert.TechAlert.config;

import Techalert.TechAlert.security.AdminAccessInterceptor;
import Techalert.TechAlert.security.CitizenAccessInterceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AdminAccessInterceptor adminAccessInterceptor;
    private final CitizenAccessInterceptor citizenAccessInterceptor;

    public WebConfig(AdminAccessInterceptor adminAccessInterceptor, CitizenAccessInterceptor citizenAccessInterceptor) {
        this.adminAccessInterceptor = adminAccessInterceptor;
        this.citizenAccessInterceptor = citizenAccessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAccessInterceptor)
                .addPathPatterns("/adm/**", "/api/adm/**");
        registry.addInterceptor(citizenAccessInterceptor)
                .addPathPatterns("/cidadao/**", "/api/cidadao/**");
    }
}
