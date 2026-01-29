package com.fourverr.api.controller;

import com.fourverr.api.dto.LoginRequest;
import com.fourverr.api.model.Usuario;
import com.fourverr.api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        // 1. Buscar al usuario por su correo
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(loginRequest.getCorreo());

        // 2. Validar si existe y si la contraseña coincide
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            // NOTA: En producción real, aquí usaríamos BCrypt para comparar contraseñas encriptadas.
            // Por ahora, comparamos texto plano como en tu proyecto anterior.
            if (usuario.getPassword().equals(loginRequest.getPassword())) {
                // ✅ Éxito: Devolvemos al usuario completo (sin la contraseña idealmente, pero para empezar está bien)
                return ResponseEntity.ok(usuario);
            }
        }

        // 🚫 Error: Devolvemos 401 Unauthorized
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales incorrectas (Usuario o Password mal)");
    }
}