package com.fourverr.api.dto;

public class JwtResponse {
    private String token;
    private String username;
    private Long id;
    private String role;
    private String nombreMostrado;
    private String fotoUrl;

    // Constructor completo
    public JwtResponse(String token, String username, Long id, String role, String nombreMostrado, String fotoUrl) {
        this.token = token;
        this.username = username;
        this.id = id;
        this.role = role;
        this.nombreMostrado = nombreMostrado;
        this.fotoUrl = fotoUrl;
    }

    // Constructor legacy para no romper otros usos
    public JwtResponse(String token, String username, Long id, String role) {
        this(token, username, id, role, username, "");
    }

    // Getters
    public String getToken() { return token; }
    public String getUsername() { return username; }
    public Long getId() { return id; }
    public String getRole() { return role; }
    public String getNombreMostrado() { return nombreMostrado; }
    public String getFotoUrl() { return fotoUrl; }
}