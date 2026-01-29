package com.fourverr.api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ilustraciones")
public class Ilustracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ID Autoincremental (1, 2, 3...)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private BigDecimal precio; // Usamos BigDecimal para dinero (es más preciso que double)

    @Column(name = "url_imagen", nullable = false)
    private String urlImagen; // Aquí guardaremos el link que nos devuelva AWS S3

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    // RELACIÓN: Muchas ilustraciones pertenecen a UN usuario (El ilustrador)
    @ManyToOne
    @JoinColumn(name = "ilustrador_id", referencedColumnName = "nombre_usuario")
    private Usuario ilustrador;
}