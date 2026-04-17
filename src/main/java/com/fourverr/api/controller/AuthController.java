package com.fourverr.api.controller;

import com.fourverr.api.dto.JwtResponse;
import com.fourverr.api.dto.LoginRequest;
import com.fourverr.api.dto.RegisterRequest;
import com.fourverr.api.model.Role;
import com.fourverr.api.model.User;
import com.fourverr.api.repository.UserRepository;
import com.fourverr.api.security.JwtUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.util.UUID;
import java.util.Collections;
import java.util.Map;

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
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Este correo ya existe");
        }

        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNombreMostrado(request.getNombreMostrado());
        user.setRole(Role.USER); // Por defecto todos son USER al registrarse
        user.setHabilitado(true); // Garantizar habilitado=true al registrarse

        userRepository.save(user);
        return ResponseEntity.ok("Usuario registrado exitosamente");
    }


    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> body) {
        try {
            // 1. Recibimos el token que envía el Frontend
            String idTokenString = body.get("token");
            
            // 2. Validamos el token con la librería de Google
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList("464885428939-7kpa14pougj5jshat56iiq6ak48qulu0.apps.googleusercontent.com"))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            
            if (idToken != null) {
                Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                
                // 3. Buscamos si el usuario ya existe por email
                User user = userRepository.findByUsernameOrEmail(email, email)
                    .orElseGet(() -> {
                        // Si no existe, creamos uno nuevo automáticamente
                        User nuevo = new User();
                        nuevo.setEmail(email);
                        nuevo.setUsername(email); // Usamos el email como username inicial
                        nuevo.setNombreMostrado((String) payload.get("name"));
                        nuevo.setFotoUrl((String) payload.get("picture"));
                        nuevo.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // Password aleatorio
                        nuevo.setRole(Role.USER);
                        nuevo.setHabilitado(true);
                        return userRepository.save(nuevo);
                    });

                // 4. Verificamos si está habilitado (igual que en tu login actual)
                if (!user.isHabilitado()) {
                    return ResponseEntity.status(403).body("Cuenta deshabilitada.");
                }

                // 5. Generamos TU token JWT de Furverr
                String jwt = jwtUtil.generateToken(user);

                return ResponseEntity.ok(new JwtResponse(
                    jwt, user.getUsername(), user.getId(), user.getRole().toString(),
                    user.getNombreMostrado(), user.getFotoUrl()
                ));
            } 
            else {
                return ResponseEntity.status(401).body("Token de Google inválido");
            }
        } 
        catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        
        // 1. Autenticar credenciales con Spring Security
        // (Aquí Spring usa tu CustomUserDetailsService para checar user O email)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 2. Obtener el usuario por su USERNAME REAL (authentication.getName() siempre
        //    devuelve el username, sin importar si el login fue con email o username)
        String realUsername = authentication.getName();
        User user = userRepository.findByUsername(realUsername)
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