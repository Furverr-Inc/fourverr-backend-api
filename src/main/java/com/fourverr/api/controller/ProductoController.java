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

    @GetMapping
    public List<Producto> obtenerTodos() {
        return productoRepository.findAll();
    }

    // metodo para publicar un producto (solo vendedores)
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> publicarProducto(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "portada", required = false) MultipartFile portada,
            @RequestParam("titulo") String titulo,
            @RequestParam("descripcion") String descripcion,
            @RequestParam("precio") BigDecimal precio,
            @RequestParam("tipo") String tipoStr
    ) {
        // SEGURIDAD: Sacamos el usuario del Token, no del parámetro
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        User vendedor = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (vendedor.getRole() != Role.SELLER && vendedor.getRole() != Role.ADMIN) {
             return ResponseEntity.status(403).body("Debes ser Vendedor para publicar");
        }

        try {
        // subida de productos a S3
        String urlArchivo = s3Service.subirImagenProducto(archivo, username);
        
        // Si hay portada, also la subimos a la carpeta de productos
        String urlPortada = (portada != null) ? s3Service.subirImagenProducto(portada, username) : null;

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
    
    // Método para eliminar
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarProducto(@PathVariable Long id) {
        return productoRepository.findById(id).map(producto -> {
            try {
                if (producto.getUrlArchivo() != null) s3Service.eliminarImagen(producto.getUrlArchivo());
                productoRepository.delete(producto);
                return ResponseEntity.ok().build();
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body("Error al borrar: " + e.getMessage());
            }
        }).orElse(ResponseEntity.notFound().build());
    }
}