package com.fourverr.api.controller;

import com.fourverr.api.dto.JwtResponse;
import com.fourverr.api.dto.LoginRequest;
import com.fourverr.api.dto.RegisterRequest;
import com.fourverr.api.dto.ResetPasswordRequest;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${google.client.id:464885428939-7kpa14pougj5jshat56iiq6ak48qulu0.apps.googleusercontent.com}")
    private String googleClientId;

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
        String idTokenString = body == null ? null : body.get("token");
        if (idTokenString == null || idTokenString.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Falta el token de Google");
        }

        GoogleIdToken idToken;
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
            idToken = verifier.verify(idTokenString);
        } catch (Exception e) {
            log.warn("Fallo al verificar token de Google: {}", e.toString());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token de Google inválido");
        }

        if (idToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token de Google inválido");
        }

        try {
            Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            if (email == null || email.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El token de Google no contiene email");
            }

            User user = userRepository.findByEmail(email)
                .or(() -> userRepository.findByUsername(email))
                .orElseGet(() -> crearUsuarioDesdeGoogle(email, payload));

            if (!user.isHabilitado()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cuenta deshabilitada.");
            }

            String jwt = jwtUtil.generateToken(user);
            return ResponseEntity.ok(new JwtResponse(
                jwt, user.getUsername(), user.getId(), user.getRole().toString(),
                user.getNombreMostrado() != null ? user.getNombreMostrado() : user.getUsername(),
                user.getFotoUrl() != null ? user.getFotoUrl() : ""
            ));
        } catch (Exception e) {
            log.error("Error procesando login de Google", e);
            return ResponseEntity.internalServerError().body("No se pudo iniciar sesión con Google");
        }
    }

    private User crearUsuarioDesdeGoogle(String email, Payload payload) {
        User nuevo = new User();
        nuevo.setEmail(email);
        nuevo.setUsername(generarUsernameUnico(email));
        nuevo.setNombreMostrado((String) payload.get("name"));
        nuevo.setFotoUrl((String) payload.get("picture"));
        nuevo.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        nuevo.setRole(Role.USER);
        nuevo.setHabilitado(true);
        return userRepository.save(nuevo);
    }

    private String generarUsernameUnico(String email) {
        String base = email;
        if (!userRepository.existsByUsername(base)) return base;
        for (int i = 1; i < 1000; i++) {
            String candidato = base + "_" + i;
            if (!userRepository.existsByUsername(candidato)) return candidato;
        }
        return base + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        if (request == null || request.getUsername() == null || request.getEmail() == null
                || request.getNewPassword() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Faltan datos para restablecer la contraseña.");
        }

        String username = request.getUsername().trim();
        String email    = request.getEmail().trim().toLowerCase();
        String nueva    = request.getNewPassword();

        if (username.isEmpty() || email.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El usuario y el correo son obligatorios.");
        }
        if (nueva == null || nueva.length() < 8 || !nueva.matches(".*[A-Za-z].*") || !nueva.matches(".*\\d.*")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("La nueva contraseña debe tener mínimo 8 caracteres, con al menos 1 letra y 1 número.");
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getEmail() == null
                || !user.getEmail().trim().equalsIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("El usuario y el correo no coinciden con ninguna cuenta registrada.");
        }

        if (!user.isHabilitado()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Tu cuenta está deshabilitada. Contacta al administrador.");
        }

        user.setPassword(passwordEncoder.encode(nueva));
        userRepository.save(user);
        return ResponseEntity.ok("Contraseña actualizada exitosamente.");
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