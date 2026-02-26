package com.fourverr.api.controller;

import com.fourverr.api.dto.JwtResponse;
import com.fourverr.api.dto.LoginRequest;
import com.fourverr.api.dto.RegisterRequest;
import com.fourverr.api.model.Role;
import com.fourverr.api.model.User;
import com.fourverr.api.repository.UserRepository;
import com.fourverr.api.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")

public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        // Verificar si el usuario ya existe (por username)
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("El nombre de usuario ya existe");
        }
        
        // Verificar si el correo ya existe (Validación extra recomendada)
        // if (userRepository.existsByEmail(request.getEmail())) { ... }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNombreMostrado(request.getNombreMostrado());
        user.setRole(Role.USER); // Por defecto todos son USER al registrarse

        userRepository.save(user);
        return ResponseEntity.ok("Usuario registrado exitosamente");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        
        // 1. Autenticar credenciales con Spring Security
        // (Aquí Spring usa tu CustomUserDetailsService para checar user O email)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 2. Obtener el usuario completo de la BD
        User user = userRepository.findByUsernameOrEmail(loginRequest.getUsername(), loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Error crítico: Usuario no encontrado después de autenticar."));

        // 3. Verificar si el usuario está habilitado
        if (!user.isHabilitado()) {
            return ResponseEntity.status(403).body("Tu cuenta ha sido deshabilitada. Contacta al administrador.");
        }

        // 4. Generar el token JWT usando los datos reales del usuario
        String jwt = jwtUtil.generateToken(user);

        // 5. Devolver respuesta al Frontend (con foto y nombre para la Navbar)
        return ResponseEntity.ok(new JwtResponse(
                jwt,
                user.getUsername(),
                user.getId(),
                user.getRole().toString(),
                user.getNombreMostrado() != null ? user.getNombreMostrado() : user.getUsername(),
                user.getFotoUrl() != null ? user.getFotoUrl() : ""
        ));
    }
}