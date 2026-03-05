package com.fourverr.api.controller;

import com.fourverr.api.model.Pregunta;
import com.fourverr.api.model.Producto;
import com.fourverr.api.model.User;
import com.fourverr.api.repository.PreguntaRepository;
import com.fourverr.api.repository.ProductoRepository;
import com.fourverr.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/preguntas")
@CrossOrigin(origins = "http://localhost:5173")
public class PreguntaController {

    @Autowired private PreguntaRepository preguntaRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private UserRepository userRepository;

    // GET /api/preguntas/producto/{id} — público
    @GetMapping("/producto/{productoId}")
    public ResponseEntity<?> obtener(@PathVariable Long productoId) {
        return ResponseEntity.ok(preguntaRepository.findByProducto_IdOrderByFechaPreguntaAsc(productoId));
    }

    // POST /api/preguntas/producto/{id} — usuario autenticado
    @PostMapping("/producto/{productoId}")
    public ResponseEntity<?> preguntar(@PathVariable Long productoId,
                                       @RequestBody Map<String, String> body) {
        String texto = body.get("texto");
        if (texto == null || texto.isBlank()) return ResponseEntity.badRequest().body("La pregunta no puede estar vacía");

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User usuario = userRepository.findByUsername(username).orElseThrow();
        Producto producto = productoRepository.findById(productoId).orElseThrow();

        Pregunta p = new Pregunta();
        p.setProducto(producto);
        p.setUsuario(usuario);
        p.setTexto(texto);
        return ResponseEntity.ok(preguntaRepository.save(p));
    }

    // PUT /api/preguntas/{id}/responder — solo el vendedor del producto
    @PutMapping("/{preguntaId}/responder")
    public ResponseEntity<?> responder(@PathVariable Long preguntaId,
                                       @RequestBody Map<String, String> body) {
        String respuesta = body.get("respuesta");
        if (respuesta == null || respuesta.isBlank()) return ResponseEntity.badRequest().body("La respuesta no puede estar vacía");

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Pregunta p = preguntaRepository.findById(preguntaId).orElseThrow();

        if (!p.getProducto().getVendedor().getUsername().equals(username))
            return ResponseEntity.status(403).body("Solo el vendedor puede responder");

        User vendedor = userRepository.findByUsername(username).orElseThrow();
        p.setRespuesta(respuesta);
        p.setRespondidoPor(vendedor);
        p.setFechaRespuesta(LocalDateTime.now());
        return ResponseEntity.ok(preguntaRepository.save(p));
    }
}
