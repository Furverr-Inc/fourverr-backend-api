package com.fourverr.api.repository;

import com.fourverr.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    // ¡MAGIA! Spring crea el SQL automáticamente basado en el nombre del método.

    // SELECT * FROM usuarios WHERE nombre_usuario = ?
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    // SELECT * FROM usuarios WHERE correo = ?
    Optional<Usuario> findByCorreo(String correo);

    // SELECT * FROM usuarios WHERE correo = ? AND password = ?
    Optional<Usuario> findByCorreoAndPassword(String correo, String password);
}