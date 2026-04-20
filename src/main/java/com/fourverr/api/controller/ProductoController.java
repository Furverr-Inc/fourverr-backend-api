package com.fourverr.api.controller;

import com.fourverr.api.model.Producto;
import com.fourverr.api.model.Role;
import com.fourverr.api.model.TipoProducto;
import com.fourverr.api.model.User;
import com.fourverr.api.repository.FavoritoRepository;
import com.fourverr.api.repository.PedidoRepository;
import com.fourverr.api.repository.PreguntaRepository;
import com.fourverr.api.repository.ProductoRepository;
import com.fourverr.api.repository.ResenaRepository;
import com.fourverr.api.repository.UserRepository;
import com.fourverr.api.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/productos")

public class ProductoController {

    /** Alineado con el mínimo de cargo en Stripe (MXN) para que todo servicio sea comprable con tarjeta. */
    private static final BigDecimal PRECIO_MINIMO_MXN = new BigDecimal("10.00");

    @Autowired private ProductoRepository productoRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private S3Service s3Service;
    @Autowired private FavoritoRepository favoritoRepository;
    @Autowired private PreguntaRepository preguntaRepository;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private ResenaRepository resenaRepository;

    @GetMapping
    public List<Producto> obtenerTodos(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tipo) {
        boolean tieneQ = q != null && !q.isBlank();
        boolean tieneTipo = tipo != null && !tipo.isBlank();
        if (tieneQ && tieneTipo) {
            try { return productoRepository.buscarPorTextoYTipoActivos(q, TipoProducto.valueOf(tipo)); }
            catch (IllegalArgumentException e) { return productoRepository.buscarPorTextoActivos(q); }
        }
        if (tieneQ) return productoRepository.buscarPorTextoActivos(q);
        if (tieneTipo) {
            try { return productoRepository.findByTipoAndActivoTrue(TipoProducto.valueOf(tipo)); }
            catch (IllegalArgumentException e) { return productoRepository.findByActivoTrue(); }
        }
        return productoRepository.findByActivoTrue();
    }

    @GetMapping("/mis-publicaciones")
    public ResponseEntity<?> misPublicaciones() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User vendedor = userRepository.findByUsername(auth.getName()).orElseThrow();
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
        User vendedor = userRepository.findByUsername(auth.getName()).orElseThrow();
        if (vendedor.getRole() != Role.SELLER && vendedor.getRole() != Role.ADMIN)
            return ResponseEntity.status(403).body("Debes ser Vendedor para publicar");
        if (precio == null || precio.compareTo(PRECIO_MINIMO_MXN) < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("El precio mínimo para publicar un servicio es 10 MXN.");
        }
        try {
            String urlArchivo = s3Service.subirImagenProducto(archivo, auth.getName());
            String urlPortada = (portada != null) ? s3Service.subirImagenProducto(portada, auth.getName()) : null;
            Producto prod = new Producto();
            prod.setTitulo(titulo); prod.setDescripcion(descripcion);
            prod.setPrecio(precio); prod.setUrlArchivo(urlArchivo);
            prod.setUrlPortada(urlPortada); prod.setTipo(TipoProducto.valueOf(tipoStr));
            prod.setVendedor(vendedor);
            return ResponseEntity.ok(productoRepository.save(prod));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<?> editarProducto(
            @PathVariable Long id,
            @RequestParam("titulo") String titulo,
            @RequestParam("descripcion") String descripcion,
            @RequestParam("precio") BigDecimal precio,
            @RequestParam("tipo") String tipoStr,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            @RequestParam(value = "portada", required = false) MultipartFile portada) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return productoRepository.findById(id).map(prod -> {
            if (!prod.getVendedor().getUsername().equals(username)) {
                User req = userRepository.findByUsername(username).orElse(null);
                if (req == null || req.getRole() != Role.ADMIN)
                    return ResponseEntity.status(403).body("No tienes permiso para editar esta publicación");
            }
            if (precio == null || precio.compareTo(PRECIO_MINIMO_MXN) < 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("El precio mínimo para publicar un servicio es 10 MXN.");
            }
            try {
                prod.setTitulo(titulo); prod.setDescripcion(descripcion);
                prod.setPrecio(precio); prod.setTipo(TipoProducto.valueOf(tipoStr));
                if (archivo != null && !archivo.isEmpty()) {
                    s3Service.eliminarImagen(prod.getUrlArchivo());
                    prod.setUrlArchivo(s3Service.subirImagenProducto(archivo, username));
                }
                if (portada != null && !portada.isEmpty()) {
                    if (prod.getUrlPortada() != null) s3Service.eliminarImagen(prod.getUrlPortada());
                    prod.setUrlPortada(s3Service.subirImagenProducto(portada, username));
                }
                return ResponseEntity.ok(productoRepository.save(prod));
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body("Error al editar: " + e.getMessage());
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarProducto(@PathVariable Long id) {
        return productoRepository.findById(id).map(producto -> {
            try {
                // 1. Reseñas PRIMERO (referencian pedidos via FK)
                resenaRepository.deleteByProducto_Id(id);
                // 2. Resto de dependencias
                favoritoRepository.deleteByProducto_Id(id);
                preguntaRepository.deleteByProducto_Id(id);
                pedidoRepository.deleteByProducto_Id(id);
                // 3. S3
                if (producto.getUrlArchivo() != null) s3Service.eliminarImagen(producto.getUrlArchivo());
                if (producto.getUrlPortada() != null) s3Service.eliminarImagen(producto.getUrlPortada());
                // 4. Producto
                productoRepository.delete(producto);
                return ResponseEntity.ok().build();
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body("Error al borrar: " + e.getMessage());
            }
        }).orElse(ResponseEntity.notFound().build());
    }
}
