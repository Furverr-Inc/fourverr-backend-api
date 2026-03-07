package com.fourverr.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "chat_mensajes")
public class ChatMensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quién envía (usuario o admin)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "remitente_id", nullable = false)
    @JsonIgnoreProperties({"password","solicitudVendedor","habilitado","descripcion",
                           "telefono","ciudad","pais","sitioWeb","instagram","twitter","linkedin"})
    private User remitente;

    // A quién va dirigido (admin cuando lo manda el user; el user cuando lo manda el admin)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "destinatario_id", nullable = false)
    @JsonIgnoreProperties({"password","solicitudVendedor","habilitado","descripcion",
                           "telefono","ciudad","pais","sitioWeb","instagram","twitter","linkedin"})
    private User destinatario;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;

    @Column(nullable = false)
    private boolean leido = false;

    @Column(name = "fecha_envio", updatable = false)
    private LocalDateTime fechaEnvio;

    @PrePersist
    protected void onCreate() { this.fechaEnvio = LocalDateTime.now(); }
}
