package com.fourverr.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "favoritos", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"usuario_id", "producto_id"})
})
public class Favorito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnoreProperties({"password","solicitudVendedor","habilitado","descripcion","fotoUrl","email","saldoDisponible"})
    private User usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "fecha_agregado", updatable = false)
    private LocalDateTime fechaAgregado;

    @PrePersist
    protected void onCreate() { this.fechaAgregado = LocalDateTime.now(); }
}
