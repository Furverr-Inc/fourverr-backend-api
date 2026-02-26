package com.fourverr.api.model;

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
    
    // CAMPO PARA EL PERFIL (SIN FOTO)
    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    // url s3 para la foto de perfil:
    @Column(name = "foto_url")
    private String fotoUrl;
    
    // Campo para habilitar/deshabilitar usuario
    @Column(name = "habilitado")
    private Boolean habilitado = true;

    // Métodos seguros contra null (usuarios viejos en BD pueden tener NULL)
    public boolean isHabilitado() {
        return habilitado != null ? habilitado : true;
    }

    public boolean isSolicitudVendedor() {
        return solicitudVendedor != null ? solicitudVendedor : false;
    }
}