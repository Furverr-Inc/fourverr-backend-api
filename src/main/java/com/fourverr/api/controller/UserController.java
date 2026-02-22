package com.fourverr.api.controller;

import com.fourverr.api.dto.JwtResponse;
import com.fourverr.api.model.Role;
import com.fourverr.api.model.User;
import com.fourverr.api.repository.UserRepository;
import com.fourverr.api.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.fourverr.api.service.S3Service; // s3
import org.springframework.web.multipart.MultipartFile; // s3


import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private BCryptPasswordEncoder passwordEncoder;
    @Autowired private S3Service s3Service; // s3

    // EL USUARIO PIDE SER VENDEDOR
    @PostMapping("/solicitar-vendedor")
    public ResponseEntity<?> solicitarVendedor() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getRole() == Role.SELLER) {
            return ResponseEntity.badRequest().body("¡Ya eres vendedor!");
        }

        user.setSolicitudVendedor(true);
        userRepository.save(user);

        return ResponseEntity.ok("Solicitud enviada. Esperando aprobación del Admin.");
    }

    // EL ADMIN APRUEBA (Endpoint protegido)
    @PutMapping("/{id}/aprobar-vendedor")
    public ResponseEntity<?> aprobarVendedor(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setRole(Role.SELLER);
        user.setSolicitudVendedor(false);
        userRepository.save(user);

        return ResponseEntity.ok("Usuario " + user.getUsername() + " ahora es VENDEDOR.");
    }

    // REFRESCAR EL TOKEN SIN SALIRSE
    @GetMapping("/refresh-status")
    public ResponseEntity<?> refrescarEstado() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String nuevoToken = jwtUtil.generateToken(user);

        return ResponseEntity.ok(new JwtResponse(
                nuevoToken, 
                user.getUsername(), 
                user.getId(), 
                user.getRole().toString()
        ));
    }

    // OBTENER PEREFIL DE IMAGEN DE PERFIL
    @PostMapping("/perfil/foto")
    public ResponseEntity<?> actualizarFoto(@RequestParam("archivo") MultipartFile archivo) {
        // 1. Validar formato (solo jpg/png)
        String contentType = archivo.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            return ResponseEntity.badRequest().body("Solo se permiten archivos JPG o PNG");
        }

        try {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Borrar foto anterior si existe
        if (user.getFotoUrl() != null && !user.getFotoUrl().isEmpty()) {
            s3Service.eliminarImagen(user.getFotoUrl());
        }

        // LLAMADA ACTUALIZADA: Pasamos el username para que cree la carpeta
        String nuevaUrl = s3Service.subirImagenPerfil(archivo, username);

        user.setFotoUrl(nuevaUrl);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("url", nuevaUrl));
    } catch (Exception e) {
        return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
    }
    }


    // OBTENER PERFIL DEL USUARIO ACTUAL
    @GetMapping("/perfil")
    public ResponseEntity<?> obtenerPerfil() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        return ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "nombreMostrado", user.getNombreMostrado() != null ? user.getNombreMostrado() : "",
            "email", user.getEmail(),
            "role", user.getRole().toString(),
            "descripcion", user.getDescripcion() != null ? user.getDescripcion() : "",
            "fotoUrl", user.getFotoUrl() != null ? user.getFotoUrl() : "" // pal s3
        ));
    }

    // ACTUALIZAR PERFIL (nombre, descripción, email)
    @PutMapping("/perfil")
    public ResponseEntity<?> actualizarPerfil(@RequestBody Map<String, String> datos) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (datos.containsKey("nombreMostrado")) {
            user.setNombreMostrado(datos.get("nombreMostrado"));
        }
        if (datos.containsKey("email")) {
            user.setEmail(datos.get("email"));
        }
        if (datos.containsKey("descripcion")) {
            user.setDescripcion(datos.get("descripcion"));
        }

        userRepository.save(user);
        return ResponseEntity.ok("Perfil actualizado correctamente");
    }

    // CAMBIAR CONTRASEÑA
    @PutMapping("/perfil/password")
    public ResponseEntity<?> cambiarPassword(@RequestBody Map<String, String> datos) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String passwordActual = datos.get("passwordActual");
        String passwordNueva = datos.get("passwordNueva");

        if (!passwordEncoder.matches(passwordActual, user.getPassword())) {
            return ResponseEntity.badRequest().body("La contraseña actual es incorrecta");
        }

        user.setPassword(passwordEncoder.encode(passwordNueva));
        userRepository.save(user);

        return ResponseEntity.ok("Contraseña actualizada correctamente");
    }
}