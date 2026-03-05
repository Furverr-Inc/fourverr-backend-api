package com.fourverr.api.model;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    private String nombreMostrado;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "solicitud_vendedor")
    private Boolean solicitudVendedor = false;

    @Column(name = "saldo_disponible", precision = 10, scale = 2)
    private BigDecimal saldoDisponible = BigDecimal.ZERO;

    public BigDecimal getSaldoDisponible() { return saldoDisponible != null ? saldoDisponible : BigDecimal.ZERO; }
    public void setSaldoDisponible(BigDecimal saldo) { this.saldoDisponible = saldo; }

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @Column(name = "foto_url")
    private String fotoUrl;

    @Column(name = "habilitado")
    private Boolean habilitado = true;

    // ===== CAMPOS DE CONTACTO =====
    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "ciudad", length = 100)
    private String ciudad;

    @Column(name = "pais", length = 100)
    private String pais;

    @Column(name = "sitio_web", length = 255)
    private String sitioWeb;

    @Column(name = "instagram", length = 100)
    private String instagram;

    @Column(name = "twitter", length = 100)
    private String twitter;

    @Column(name = "linkedin", length = 255)
    private String linkedin;

    public boolean isHabilitado() { return habilitado != null ? habilitado : true; }
    public boolean isSolicitudVendedor() { return solicitudVendedor != null ? solicitudVendedor : false; }
}
