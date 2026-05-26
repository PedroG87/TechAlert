package Techalert.TechAlert.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "alerta")
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_risco_id", nullable = false)
    private AreaRisco areaRisco;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emitido_por", nullable = false)
    private Usuario emitidoPor;

    @Column(name = "nivel_alerta", nullable = false, length = 20)
    private String nivelAlerta;

    @Column(nullable = false, length = 140)
    private String titulo;

    @Column(nullable = false, length = 2000)
    private String mensagem;

    @Column(name = "status_alerta", nullable = false, length = 20)
    private String statusAlerta = "ATIVO";

    @Column(name = "data_emissao", nullable = false)
    private LocalDateTime dataEmissao = LocalDateTime.now();

    @Column(name = "validade_ate")
    private LocalDateTime validadeAte;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    public Alerta() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AreaRisco getAreaRisco() { return areaRisco; }
    public void setAreaRisco(AreaRisco areaRisco) { this.areaRisco = areaRisco; }

    public Usuario getEmitidoPor() { return emitidoPor; }
    public void setEmitidoPor(Usuario emitidoPor) { this.emitidoPor = emitidoPor; }

    public String getNivelAlerta() { return nivelAlerta; }
    public void setNivelAlerta(String nivelAlerta) { this.nivelAlerta = nivelAlerta; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }

    public String getStatusAlerta() { return statusAlerta; }
    public void setStatusAlerta(String statusAlerta) { this.statusAlerta = statusAlerta; }

    public LocalDateTime getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDateTime dataEmissao) { this.dataEmissao = dataEmissao; }

    public LocalDateTime getValidadeAte() { return validadeAte; }
    public void setValidadeAte(LocalDateTime validadeAte) { this.validadeAte = validadeAte; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}