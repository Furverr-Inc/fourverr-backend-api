package com.fourverr.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "resenas",
       uniqueConstraints = @UniqueConstraint(columnNames = {"pedido_id"}))
public class Resena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Un pedido solo puede tener UNA reseña
    @OneToOne
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id", nullable = false)
    @JsonIgnoreProperties({"descripcion","urlArchivo","urlPortada","tipo","fechaCreacion","activo","vendedor"})
    private Producto producto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = false)
    @JsonIgnoreProperties({"password","solicitudVendedor","habilitado","descripcion","saldoDisponible","email"})
    private User cliente;

    @Column(nullable = false)
    private Integer calificacion; // 1-5

    @Column(columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "fecha_resena")
    private LocalDateTime fechaResena = LocalDateTime.now();

    @PrePersist
    protected void onCreate() { this.fechaResena = LocalDateTime.now(); }
}
