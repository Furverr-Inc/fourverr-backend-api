package com.fourverr.api.dto;

public class JwtResponse {
    private String token;
    private String username;
    private Long id;
    private String role; // Agregamos el rol para saber si es Vendedor

    public JwtResponse(String token, String username, Long id, String role) {
        this.token = token;
        this.username = username;
        this.id = id;
        this.role = role;
    }

    // Getters
    public String getToken() { return token; }
    public String getUsername() { return username; }
    public Long getId() { return id; }
    public String getRole() { return role; }
}