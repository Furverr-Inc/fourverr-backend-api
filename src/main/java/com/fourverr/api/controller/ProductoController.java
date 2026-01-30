package com.fourverr.api.controller;

import com.fourverr.api.model.Producto;
import com.fourverr.api.model.TipoProducto;
import com.fourverr.api.model.Usuario;
import com.fourverr.api.repository.ProductoRepository;
import com.fourverr.api.repository.UsuarioRepository;
import com.fourverr.api.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "http://localhost:5173")
public class ProductoController {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private S3Service s3Service;

    @GetMapping
    public List<Producto> obtenerTodos() {
        return productoRepository.findAll();
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> publicarProducto(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "portada", required = false) MultipartFile portada,
            @RequestParam("titulo") String titulo,
            @RequestParam("descripcion") String descripcion,
            @RequestParam("precio") BigDecimal precio,
            @RequestParam("tipo") String tipoStr,
            @RequestParam("nombreUsuario") String nombreUsuario) {

        Optional<Usuario> vendedorOpt = usuarioRepository.findByNombreUsuario(nombreUsuario);
        if (vendedorOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("El vendedor no existe");
        }

        try {
            String urlArchivo = s3Service.subirImagen(archivo);
            String urlPortada = (portada != null) ? s3Service.subirImagen(portada) : null;

            Producto prod = new Producto();
            prod.setTitulo(titulo);
            prod.setDescripcion(descripcion);
            prod.setPrecio(precio);
            prod.setUrlArchivo(urlArchivo);
            prod.setUrlPortada(urlPortada);
            prod.setTipo(TipoProducto.valueOf(tipoStr));
            prod.setVendedor(vendedorOpt.get());

            return ResponseEntity.ok(productoRepository.save(prod));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    // Nuevo método para manejar la petición DELETE desde React
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarProducto(@PathVariable Long id) {
        return productoRepository.findById(id).map(producto -> {
            try {
                // Primero limpiamos Amazon S3
                if (producto.getUrlArchivo() != null) {
                    s3Service.eliminarImagen(producto.getUrlArchivo());
                }
                // Luego limpiamos la base de datos MySQL
                productoRepository.delete(producto);
                return ResponseEntity.ok().build();
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body("Fallo al borrar: " + e.getMessage());
            }
        }).orElse(ResponseEntity.notFound().build());
    }
}