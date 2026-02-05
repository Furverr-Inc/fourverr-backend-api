package com.fourverr.api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // CAMBIO: Ahora apunta a 'User'
    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private User cliente; 

    @ManyToOne
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private String estado; // PENDIENTE, PAGADO, etc.

    @Column(name = "fecha_pedido")
    private LocalDateTime fechaPedido = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String requisitosCliente;
}