package com.fourverr.api.model;

import jakarta.persistence.*;
import lombok.Data; // Si usas Lombok, si no, genera Getters/Setters manuales

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
    private Role role; // ADMIN, USER, SELLER

    // NUEVO CAMPO PARA EL SISTEMA DE APROBACIÓN
    @Column(name = "solicitud_vendedor")
    private boolean solicitudVendedor = false;
    
    // OJO: Si no usas Lombok, genera aquí abajo los Getters y Setters de 'solicitudVendedor'
}