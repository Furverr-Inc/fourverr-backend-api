package com.fourverr.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reportes")
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reportante_id", nullable = false)
    private User reportante;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vendedor_id", nullable = false)
    private User vendedor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MotivoReporte motivo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReporte estado = EstadoReporte.PENDIENTE;

    @Column(columnDefinition = "TEXT")
    private String respuestaAdmin;

    @Column(nullable = false)
    private LocalDateTime fechaReporte = LocalDateTime.now();

    private LocalDateTime fechaRespuesta;

    public enum MotivoReporte {
        FRAUDE,
        CONTENIDO_INAPROPIADO,
        SPAM,
        PRODUCTO_FALSO,
        MAL_COMPORTAMIENTO,
        PRECIO_ENGAÑOSO,
        OTRO
    }

    public enum EstadoReporte {
        PENDIENTE,
        EN_REVISION,
        RESUELTO,
        RECHAZADO
    }

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getReportante() { return reportante; }
    public void setReportante(User reportante) { this.reportante = reportante; }

    public User getVendedor() { return vendedor; }
    public void setVendedor(User vendedor) { this.vendedor = vendedor; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public MotivoReporte getMotivo() { return motivo; }
    public void setMotivo(MotivoReporte motivo) { this.motivo = motivo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public EstadoReporte getEstado() { return estado; }
    public void setEstado(EstadoReporte estado) { this.estado = estado; }

    public String getRespuestaAdmin() { return respuestaAdmin; }
    public void setRespuestaAdmin(String respuestaAdmin) { this.respuestaAdmin = respuestaAdmin; }

    public LocalDateTime getFechaReporte() { return fechaReporte; }
    public void setFechaReporte(LocalDateTime fechaReporte) { this.fechaReporte = fechaReporte; }

    public LocalDateTime getFechaRespuesta() { return fechaRespuesta; }
    public void setFechaRespuesta(LocalDateTime fechaRespuesta) { this.fechaRespuesta = fechaRespuesta; }
}
