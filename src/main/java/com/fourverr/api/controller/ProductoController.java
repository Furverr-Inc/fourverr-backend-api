package com.fourverr.api.controller;

import com.fourverr.api.model.Producto;
import com.fourverr.api.model.Role;
import com.fourverr.api.model.TipoProducto;
import com.fourverr.api.model.User;
import com.fourverr.api.repository.ProductoRepository;
import com.fourverr.api.repository.UserRepository;
import com.fourverr.api.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "http://localhost:5173")
public class ProductoController {

    @Autowired private ProductoRepository productoRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private S3Service s3Service;

    // GET /api/productos?q=texto&tipo=SERVICIO_GIG
    @GetMapping
    public List<Producto> obtenerTodos(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tipo) {

        boolean tieneQ = q != null && !q.isBlank();
        boolean tieneTipo = tipo != null && !tipo.isBlank();

        if (tieneQ && tieneTipo) {
            try { return productoRepository.buscarPorTextoYTipo(q, TipoProducto.valueOf(tipo)); }
            catch (IllegalArgumentException e) { return productoRepository.buscarPorTexto(q); }
        }
        if (tieneQ) return productoRepository.buscarPorTexto(q);
        if (tieneTipo) {
            try { return productoRepository.findByTipo(TipoProducto.valueOf(tipo)); }
            catch (IllegalArgumentException e) { return productoRepository.findAll(); }
        }
        return productoRepository.findByActivoTrue();
    }

    @GetMapping("/mis-publicaciones")
    public ResponseEntity<?> misPublicaciones() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User vendedor = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (vendedor.getRole() != Role.SELLER && vendedor.getRole() != Role.ADMIN)
            return ResponseEntity.status(403).body("Solo los vendedores pueden ver sus publicaciones");
        return ResponseEntity.ok(productoRepository.findByVendedor_Username(auth.getName()));
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> publicarProducto(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "portada", required = false) MultipartFile portada,
            @RequestParam("titulo") String titulo,
            @RequestParam("descripcion") String descripcion,
            @RequestParam("precio") BigDecimal precio,
            @RequestParam("tipo") String tipoStr) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User vendedor = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (vendedor.getRole() != Role.SELLER && vendedor.getRole() != Role.ADMIN)
            return ResponseEntity.status(403).body("Debes ser Vendedor para publicar");

        try {
            String urlArchivo = s3Service.subirImagenProducto(archivo, auth.getName());
            String urlPortada = (portada != null) ? s3Service.subirImagenProducto(portada, auth.getName()) : null;

            Producto prod = new Producto();
            prod.setTitulo(titulo);
            prod.setDescripcion(descripcion);
            prod.setPrecio(precio);
            prod.setUrlArchivo(urlArchivo);
            prod.setUrlPortada(urlPortada);
            prod.setTipo(TipoProducto.valueOf(tipoStr));
            prod.setVendedor(vendedor);
            return ResponseEntity.ok(productoRepository.save(prod));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarProducto(@PathVariable Long id) {
    return productoRepository.findById(id).map(producto -> {
        producto.setActivo(false);
        productoRepository.save(producto);
        return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());   
    }
}
