package com.fourverr.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "preguntas")
public class Pregunta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id", nullable = false)
    @JsonIgnoreProperties({"vendedor","descripcion","urlArchivo","urlPortada","fechaCreacion"})
    private Producto producto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnoreProperties({"password","solicitudVendedor","habilitado","email","role","saldoDisponible"})
    private User usuario;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String texto;

    @Column(columnDefinition = "TEXT")
    private String respuesta;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "respondido_por_id")
    @JsonIgnoreProperties({"password","solicitudVendedor","habilitado","email","role","saldoDisponible"})
    private User respondidoPor;

    @Column(name = "fecha_pregunta", updatable = false)
    private LocalDateTime fechaPregunta;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    @PrePersist
    protected void onCreate() { this.fechaPregunta = LocalDateTime.now(); }
}
