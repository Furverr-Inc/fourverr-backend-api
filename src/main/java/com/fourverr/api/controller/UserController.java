package com.fourverr.api.controller;

import com.fourverr.api.dto.JwtResponse;
import com.fourverr.api.model.Role;
import com.fourverr.api.model.User;
import com.fourverr.api.repository.UserRepository;
import com.fourverr.api.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtil jwtUtil;

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

        // CORRECCIÓN: Pasamos el objeto 'user', no strings sueltos
        String nuevoToken = jwtUtil.generateToken(user);

        return ResponseEntity.ok(new JwtResponse(
                nuevoToken, 
                user.getUsername(), 
                user.getId(), 
                user.getRole().toString()
        ));
    }
}