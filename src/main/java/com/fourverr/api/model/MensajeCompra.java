package com.fourverr.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "mensajes_compra")
public class MensajeCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pedido_id", nullable = false)
    @JsonIgnoreProperties({"cliente", "producto", "requisitosCliente",
                           "stripePaymentIntentId", "montoVendedor"})
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "remitente_id", nullable = false)
    @JsonIgnoreProperties({"password", "solicitudVendedor", "habilitado", "descripcion",
                           "telefono", "ciudad", "pais", "sitioWeb",
                           "instagram", "twitter", "linkedin", "email", "saldoDisponible"})
    private User remitente;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;

    @Column(nullable = false)
    private boolean leido = false;

    @Column(name = "fecha_envio", updatable = false)
    private LocalDateTime fechaEnvio;

    @PrePersist
    protected void onCreate() {
        this.fechaEnvio = LocalDateTime.now();
    }
}
