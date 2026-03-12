package com.fourverr.api.controller;

import com.fourverr.api.model.*;
import com.fourverr.api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resenas")

public class ResenaController {

    @Autowired private ResenaRepository resenaRepository;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private UserRepository userRepository;

    // GET /api/resenas/producto/{id} — reseñas públicas de un producto
    @GetMapping("/producto/{productoId}")
    public ResponseEntity<?> getResenas(@PathVariable Long productoId) {
        List<Resena> resenas = resenaRepository.findByProducto_IdOrderByFechaResenaDesc(productoId);
        Double promedio = resenaRepository.promedioCalificacion(productoId);
        long total = resenaRepository.countByProducto_Id(productoId);
        return ResponseEntity.ok(Map.of(
            "resenas", resenas,
            "promedio", promedio != null ? Math.round(promedio * 10.0) / 10.0 : 0.0,
            "total", total
        ));
    }

    // GET /api/resenas/check/{pedidoId} — ¿ya reseñé este pedido?
    @GetMapping("/check/{pedidoId}")
    public ResponseEntity<?> check(@PathVariable Long pedidoId) {
        boolean existe = resenaRepository.existsByPedido_Id(pedidoId);
        return ResponseEntity.ok(Map.of("yaReseno", existe));
    }

    // POST /api/resenas — crear reseña (solo si el pedido es PAGADO y es del usuario)
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User cliente = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            Long pedidoId = Long.valueOf(body.get("pedidoId").toString());
            int calificacion = Integer.parseInt(body.get("calificacion").toString());
            String comentario = body.containsKey("comentario") ? body.get("comentario").toString() : "";

            if (calificacion < 1 || calificacion > 5)
                return ResponseEntity.badRequest().body("La calificación debe ser entre 1 y 5");

            Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

            // Validaciones de seguridad
            if (!pedido.getCliente().getUsername().equals(username))
                return ResponseEntity.status(403).body("Este pedido no te pertenece");
            if (!"PAGADO".equals(pedido.getEstado()))
                return ResponseEntity.badRequest().body("Solo puedes reseñar compras pagadas");
            if (resenaRepository.existsByPedido_Id(pedidoId))
                return ResponseEntity.badRequest().body("Ya reseñaste este pedido");

            Resena resena = new Resena();
            resena.setPedido(pedido);
            resena.setProducto(pedido.getProducto());
            resena.setCliente(cliente);
            resena.setCalificacion(calificacion);
            resena.setComentario(comentario);

            return ResponseEntity.ok(resenaRepository.save(resena));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    // DELETE /api/resenas/{id} — el autor puede eliminar su reseña
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return resenaRepository.findById(id).map(r -> {
            if (!r.getCliente().getUsername().equals(username))
                return ResponseEntity.status(403).body("No autorizado");
            resenaRepository.delete(r);
            return ResponseEntity.ok(Map.of("mensaje", "Reseña eliminada"));
        }).orElse(ResponseEntity.notFound().build());
    }
}
