package com.fourverr.api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "pedidos")
public class Pedido {
     
@Column(name = "monto_vendedor", precision = 10, scale = 2)
private BigDecimal montoVendedor;

public BigDecimal getMontoVendedor() { return montoVendedor; }
public void setMontoVendedor(BigDecimal monto) { this.montoVendedor = monto; }

@Column(name = "stripe_payment_intent_id")
private String stripePaymentIntentId;

// Y su getter/setter (o si usas @Data de Lombok ya lo genera automático)
public String getStripePaymentIntentId() { return stripePaymentIntentId; }
public void setStripePaymentIntentId(String id) { this.stripePaymentIntentId = id; }

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