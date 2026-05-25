package Techalert.TechAlert.security;

public record SessionUser(
        Long id,
        String nome,
        String email,
        UserRole role
) {
    public boolean isAdmin() {
        return role == UserRole.ADM;
    }

    public boolean isCitizen() {
        return role == UserRole.CIDADAO;
    }

    public boolean isAuthenticated() {
        return id != null && nome != null && email != null && role != null;
    }
}
