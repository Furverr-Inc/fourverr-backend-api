package com.fourverr.api.controller;

import com.fourverr.api.model.Usuario;
import com.fourverr.api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:5173")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // GET: Ver todos
    @GetMapping
    public List<Usuario> obtenerUsuarios() {
        return usuarioRepository.findAll();
    }

    // POST: Crear Usuario (Blindado)
    @PostMapping
    public ResponseEntity<?> crearUsuario(@RequestBody Usuario usuario) {
        try {
            // 1. Validar si ya existe el nombre de usuario
            if (usuarioRepository.findByNombreUsuario(usuario.getNombreUsuario()).isPresent()) {
                return ResponseEntity.badRequest().body("Error: El usuario '" + usuario.getNombreUsuario() + "' ya existe.");
            }
            
            // 2. Validar si ya existe el correo (si tu entidad lo requiere único)
            // if (usuarioRepository.findByCorreo(usuario.getCorreo()).isPresent()) ...

            Usuario guardado = usuarioRepository.save(usuario);
            return ResponseEntity.ok(guardado);

        } catch (Exception e) {
            e.printStackTrace(); // Esto imprimirá el error real en tu consola Java
            return ResponseEntity.internalServerError().body("Error crítico al crear usuario: " + e.getMessage());
        }
    }
}