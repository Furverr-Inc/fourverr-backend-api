package com.fourverr.api.dto;

import lombok.Data;

@Data // Genera Getters y Setters automáticos
public class LoginRequest {
    private String correo;
    private String password;
}