package Techalert.TechAlert.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "alerta_usuario")
public class AlertaUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alerta_id", nullable = false)
    private Alerta alerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "canal_envio", nullable = false, length = 20)
    private String canalEnvio = "SISTEMA";

    @Column(name = "status_entrega", nullable = false, length = 20)
    private String statusEntrega = "PENDENTE";

    @Column(name = "data_envio", nullable = false)
    private LocalDateTime dataEnvio = LocalDateTime.now();

    @Column(name = "data_leitura")
    private LocalDateTime dataLeitura;

    public AlertaUsuario() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Alerta getAlerta() { return alerta; }
    public void setAlerta(Alerta alerta) { this.alerta = alerta; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getCanalEnvio() { return canalEnvio; }
    public void setCanalEnvio(String canalEnvio) { this.canalEnvio = canalEnvio; }

    public String getStatusEntrega() { return statusEntrega; }
    public void setStatusEntrega(String statusEntrega) { this.statusEntrega = statusEntrega; }

    public LocalDateTime getDataEnvio() { return dataEnvio; }
    public void setDataEnvio(LocalDateTime dataEnvio) { this.dataEnvio = dataEnvio; }

    public LocalDateTime getDataLeitura() { return dataLeitura; }
    public void setDataLeitura(LocalDateTime dataLeitura) { this.dataLeitura = dataLeitura; }
}