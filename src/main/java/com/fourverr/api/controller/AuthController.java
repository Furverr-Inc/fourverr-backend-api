package com.fourverr.api.controller;

import com.fourverr.api.dto.LoginRequest;
import com.fourverr.api.model.Usuario;
import com.fourverr.api.repository.UsuarioRepository;
import com.fourverr.api.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        // 1. Buscar usuario por correo
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(loginRequest.getCorreo());

        // 2. Validar existencia
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();

            // 3. Validar contraseña
            if (usuario.getPassword().equals(loginRequest.getPassword())) {
                
                // 4. Generar Token
                String token = jwtUtil.generateToken(usuario.getNombreUsuario());

                // 5. Preparar respuesta limpia
                Map<String, Object> respuesta = new HashMap<>();
                respuesta.put("token", token);
                respuesta.put("mensaje", "Login exitoso");
                respuesta.put("usuarioId", usuario.getNombreUsuario());
                respuesta.put("nombreMostrado", usuario.getNombreMostrado());
                
                return ResponseEntity.ok(respuesta);
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Credenciales incorrectas");
    }
}