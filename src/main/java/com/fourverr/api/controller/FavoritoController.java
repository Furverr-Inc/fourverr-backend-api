package com.fourverr.api.controller;

import com.fourverr.api.model.Favorito;
import com.fourverr.api.model.Producto;
import com.fourverr.api.model.User;
import com.fourverr.api.repository.FavoritoRepository;
import com.fourverr.api.repository.ProductoRepository;
import com.fourverr.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favoritos")

public class FavoritoController {

    @Autowired private FavoritoRepository favoritoRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private UserRepository userRepository;

    // GET /api/favoritos — wishlist del usuario logueado
    @GetMapping
    public ResponseEntity<?> obtenerFavoritos() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Favorito> favs = favoritoRepository.findByUsuario_Username(username);
        List<Producto> productos = favs.stream().map(Favorito::getProducto).collect(Collectors.toList());
        return ResponseEntity.ok(productos);
    }

    // POST /api/favoritos/{productoId}
    @PostMapping("/{productoId}")
    public ResponseEntity<?> agregar(@PathVariable Long productoId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (favoritoRepository.existsByUsuario_UsernameAndProducto_Id(username, productoId)) {
            return ResponseEntity.badRequest().body("Ya está en tu wishlist");
        }
        User usuario = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        Favorito fav = new Favorito();
        fav.setUsuario(usuario);
        fav.setProducto(producto);
        favoritoRepository.save(fav);
        return ResponseEntity.ok(Map.of("mensaje", "Agregado a wishlist"));
    }

    // DELETE /api/favoritos/{productoId}
    @DeleteMapping("/{productoId}")
    public ResponseEntity<?> quitar(@PathVariable Long productoId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<Favorito> fav = favoritoRepository.findByUsuario_UsernameAndProducto_Id(username, productoId);
        if (fav.isEmpty()) return ResponseEntity.notFound().build();
        favoritoRepository.delete(fav.get());
        return ResponseEntity.ok(Map.of("mensaje", "Eliminado de wishlist"));
    }

    // GET /api/favoritos/check/{productoId}
    @GetMapping("/check/{productoId}")
    public ResponseEntity<?> check(@PathVariable Long productoId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean esFav = favoritoRepository.existsByUsuario_UsernameAndProducto_Id(username, productoId);
        return ResponseEntity.ok(Map.of("esFavorito", esFav));
    }
}
