package Techalert.TechAlert.model;

import java.util.Date;
import java.util.Locale;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import Techalert.TechAlert.security.UserRole;

@Entity
@Table(name = "usuario")
public class Usuario {

    public static final String PERFIL_ADMINISTRADOR = "ADMINISTRADOR";
    public static final String PERFIL_MORADOR = "MORADOR";
    private static final Set<String> PERFIS_VALIDOS = Set.of(PERFIL_ADMINISTRADOR, PERFIL_MORADOR);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 120)
    private String senha;

    @Column(nullable = false, unique = true, length = 14)
    private String cpf;

    @Column(length = 20)
    private String telefone;

    @Column(length = 220)
    private String endereco;

    @Column(name = "data_nascimento")
    private Date dataNascimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.CIDADAO;

    @NotBlank(message = "O perfil do usuario e obrigatorio.")
    @Pattern(
            regexp = "ADMINISTRADOR|MORADOR",
            message = "Perfil invalido. Valores permitidos: ADMINISTRADOR, MORADOR."
    )
    @Column(nullable = false)
    private String perfil = PERFIL_MORADOR;

    public Usuario() {
    }

    public Usuario(Long id, String nome, String email, String senha, String cpf, String telefone, String endereco, Date dataNascimento) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.cpf = cpf;
        this.telefone = telefone;
        this.endereco = endereco;
        this.dataNascimento = dataNascimento;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public Date getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(Date dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
        if (role != null) {
            this.perfil = perfilForRole(role);
        }
    }

    public boolean isAdmin() {
        return role == UserRole.ADM;
    }

    public boolean isCitizen() {
        return role == UserRole.CIDADAO;
    }

    public String getPerfil() {
        return perfil;
    }

    public void setPerfil(String perfil) {
        this.perfil = normalizePerfil(perfil);
    }

    @PrePersist
    @PreUpdate
    void validateAndSyncPerfil() {
        if (role == null) {
            throw new IllegalArgumentException("O papel do usuario e obrigatorio.");
        }
        this.perfil = perfilForRole(role);
    }

    public static String perfilForRole(UserRole role) {
        if (role == null) {
            throw new IllegalArgumentException("O papel do usuario e obrigatorio.");
        }
        return role == UserRole.ADM ? PERFIL_ADMINISTRADOR : PERFIL_MORADOR;
    }

    public static boolean isPerfilValido(String perfil) {
        return perfil != null && PERFIS_VALIDOS.contains(normalizePerfil(perfil));
    }

    private static String normalizePerfil(String perfil) {
        if (perfil == null || perfil.isBlank()) {
            throw new IllegalArgumentException("O perfil do usuario e obrigatorio.");
        }

        String normalized = perfil.trim().toUpperCase(Locale.ROOT);
        if (!PERFIS_VALIDOS.contains(normalized)) {
            throw new IllegalArgumentException("Perfil invalido. Valores permitidos: ADMINISTRADOR, MORADOR.");
        }
        return normalized;
    }
}
