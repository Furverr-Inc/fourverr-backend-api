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
    private boolean solicitudVendedor = false;
    
    // CAMPO PARA EL PERFIL (SIN FOTO)
    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    // url s3 para la foto de perfil:
    @Column(name = "foto_url")
    private String fotoUrl;
    
    // Campo para habilitar/deshabilitar usuario
    @Column(name = "habilitado")
    private boolean habilitado = true;
}