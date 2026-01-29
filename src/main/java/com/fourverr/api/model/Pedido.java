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

    // Relación: Un pedido pertenece a UN Cliente
    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    // Relación: Un pedido es de UNA Ilustración
    @ManyToOne
    @JoinColumn(name = "ilustracion_id", nullable = false)
    private Ilustracion ilustracion;

    @Column(nullable = false)
    private String estado; // Valores: "PENDIENTE", "EN_PROCESO", "ENTREGADO"

    @Column(name = "fecha_pedido")
    private LocalDateTime fechaPedido = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String requisitosCliente; // Ejemplo: "Quiero que el personaje tenga ojos rojos"
}