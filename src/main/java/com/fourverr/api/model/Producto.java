package com.fourverr.api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private BigDecimal precio;

    // Aquí va el link del archivo que vendes (video, pdf, imagen final)
    @Column(name = "url_archivo", nullable = false)
    private String urlArchivo;

    // La imagen de portada para mostrar en la tienda
    @Column(name = "url_portada")
    private String urlPortada;

    // Define si es Curso, Gig, etc.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoProducto tipo;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    // Quién lo vende
    @ManyToOne
    @JoinColumn(name = "vendedor_id", referencedColumnName = "nombre_usuario")
    private Usuario vendedor;
}