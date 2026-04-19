package com.fourverr.api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "solicitudes_retiro")
public class SolicitudRetiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vendedor_id", nullable = false)
    private User vendedor;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private String estado = "PENDIENTE"; // PENDIENTE, COMPLETADO, RECHAZADO

    @Column(name = "fecha_solicitud")
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    @Column(name = "fecha_procesado")
    private LocalDateTime fechaProcesado;

    /** Snapshot de la CLABE del vendedor al momento de solicitar el retiro */
    @Column(name = "clabe_snapshot", length = 18)
    private String clabeSnapshot;

    @Column(length = 500)
    private String notas;
}