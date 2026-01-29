package com.fourverr.api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data // Genera Getters, Setters, toString, etc. automágicamente
@NoArgsConstructor // Genera constructor vacío (obligatorio para JPA)
@AllArgsConstructor // Genera constructor con todos los argumentos
@Entity // Esto le dice a Spring: "Esta clase es una tabla de BD"
@Table(name = "usuarios") // Nombre real de la tabla en MySQL
public class Usuario {

    @Id // Llave primaria
    @Column(name = "nombre_usuario", length = 50)
    private String nombreUsuario; // En tu BD vieja era varchar(50)

    @Column(nullable = false, unique = true, length = 100)
    private String correo;

    @Column(nullable = false)
    private String password;

    @Column(length = 20)
    private String tipo; // "cliente", "ilustrador", "admin"

    @Column(columnDefinition = "boolean default true")
    private Boolean estatus;

    // --- DATOS DEL PERFIL (Fusionamos la tabla 'ilustradores' aquí) ---
    
    @Column(name = "nombre_mostrado", length = 100)
    private String nombreMostrado;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "ruta_foto_perfil")
    private String rutaFotoPerfil;

    // Campos exclusivos de ilustradores (pueden ser nulos si es un cliente)
    @Column(length = 50)
    private String idioma;

    @Column(name = "nivel_idioma", length = 20)
    private String nivelIdioma;
}