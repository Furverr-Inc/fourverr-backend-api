package com.fourverr.api.controller;

import com.fourverr.api.model.Usuario;
import com.fourverr.api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // Indica que esta clase responde a peticiones Web
@RequestMapping("/api/usuarios") // La URL base será: localhost:8080/api/usuarios
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // GET: Obtener todos los usuarios (Para probar conexión)
    @GetMapping
    public List<Usuario> obtenerTodos() {
        return usuarioRepository.findAll();
    }

    // POST: Crear un usuario de prueba rápido
    @PostMapping
    public Usuario crearUsuario(@RequestBody Usuario usuario) {
        return usuarioRepository.save(usuario);
    }
}