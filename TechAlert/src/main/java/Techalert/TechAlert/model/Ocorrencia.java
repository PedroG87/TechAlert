package Techalert.TechAlert.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "ocorrencia")
public class Ocorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_risco_id", nullable = false)
    private AreaRisco areaRisco;

    @Column(name = "tipo_risco", nullable = false, length = 30)
    private String tipoRisco;

    @Column(nullable = false, length = 2000)
    private String descricao;

    @Column(name = "status_ocorrencia", nullable = false, length = 20)
    private String statusOcorrencia = "PENDENTE";

    @Column(name = "data_ocorrencia", nullable = false)
    private LocalDateTime dataOcorrencia = LocalDateTime.now();

    @Column(name = "validada_em")
    private LocalDateTime validadaEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validada_por")
    private Usuario validadaPor;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    public Ocorrencia() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public AreaRisco getAreaRisco() { return areaRisco; }
    public void setAreaRisco(AreaRisco areaRisco) { this.areaRisco = areaRisco; }

    public String getTipoRisco() { return tipoRisco; }
    public void setTipoRisco(String tipoRisco) { this.tipoRisco = tipoRisco; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getStatusOcorrencia() { return statusOcorrencia; }
    public void setStatusOcorrencia(String statusOcorrencia) { this.statusOcorrencia = statusOcorrencia; }

    public LocalDateTime getDataOcorrencia() { return dataOcorrencia; }
    public void setDataOcorrencia(LocalDateTime dataOcorrencia) { this.dataOcorrencia = dataOcorrencia; }

    public LocalDateTime getValidadaEm() { return validadaEm; }
    public void setValidadaEm(LocalDateTime validadaEm) { this.validadaEm = validadaEm; }

    public Usuario getValidadaPor() { return validadaPor; }
    public void setValidadaPor(Usuario validadaPor) { this.validadaPor = validadaPor; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}