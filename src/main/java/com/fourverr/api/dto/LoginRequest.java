package com.fourverr.api.dto;

public class LoginRequest {
    private String username; // Ojo: Aunque el usuario escriba correo, Spring suele llamarlo username
    private String password;

    // Getters y Setters obligatorios
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}