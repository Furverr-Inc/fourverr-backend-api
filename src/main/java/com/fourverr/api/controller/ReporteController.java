package com.fourverr.api.controller;

import com.fourverr.api.model.Reporte;
import com.fourverr.api.model.User;
import com.fourverr.api.repository.ProductoRepository;
import com.fourverr.api.repository.ReporteRepository;
import com.fourverr.api.repository.UserRepository;
import com.fourverr.api.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/reportes")

public class ReporteController {

    @Autowired private ReporteRepository reporteRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<?> crearReporte(@RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User reportante = userRepository.findByUsernameOrEmail(auth.getName(), auth.getName()).orElseThrow();
        Long vendedorId  = Long.valueOf(body.get("vendedorId").toString());
        Long productoId  = body.get("productoId") != null ? Long.valueOf(body.get("productoId").toString()) : null;
        String motivoStr = body.get("motivo").toString();
        String descripcion = body.containsKey("descripcion") ? body.get("descripcion").toString() : null;
        Optional<User> vendedorOpt = userRepository.findById(vendedorId);
        if (vendedorOpt.isEmpty()) return ResponseEntity.badRequest().body("Vendedor no encontrado");
        if (reportante.getId().equals(vendedorId)) return ResponseEntity.badRequest().body("No puedes reportarte a ti mismo");
        Reporte reporte = new Reporte();
        reporte.setReportante(reportante);
        reporte.setVendedor(vendedorOpt.get());
        reporte.setMotivo(Reporte.MotivoReporte.valueOf(motivoStr));
        reporte.setDescripcion(descripcion);
        if (productoId != null) productoRepository.findById(productoId).ifPresent(reporte::setProducto);
        return ResponseEntity.ok(reporteRepository.save(reporte));
    }

    /** Listado de reportes enviados por el usuario autenticado (estado y respuesta del admin). */
    @GetMapping("/mis-reportes")
    public ResponseEntity<List<Reporte>> misReportes(HttpServletRequest request) {
        User reportante = resolveUsuarioAutenticado(request);
        return ResponseEntity.ok(reporteRepository.findByReportante_IdOrderByFechaReporteDesc(reportante.getId()));
    }

    /**
     * Usuario actual: preferimos {@code userId} del JWT (mismo criterio que el front en localStorage)
     * para que coincida con el {@code reportante_id} guardado al crear el reporte.
     */
    private User resolveUsuarioAutenticado(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String jwt = header.substring(7);
            try {
                Long id = jwtUtil.extractUserId(jwt);
                if (id != null) {
                    return userRepository.findById(id).orElseThrow();
                }
            } catch (Exception ignored) {
                // continuar con nombre del contexto
            }
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Usuario no autenticado");
        }
        return userRepository.findByUsernameOrEmail(auth.getName(), auth.getName()).orElseThrow();
    }

    @GetMapping("/admin")
    public ResponseEntity<List<Reporte>> listarTodos() {
        return ResponseEntity.ok(reporteRepository.findAllByOrderByFechaReporteDesc());
    }

    @PutMapping("/{id}/revisar")
    public ResponseEntity<?> marcarEnRevision(@PathVariable Long id) {
        Optional<Reporte> opt = reporteRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Reporte r = opt.get();
        r.setEstado(Reporte.EstadoReporte.EN_REVISION);
        return ResponseEntity.ok(reporteRepository.save(r));
    }

    @PutMapping("/{id}/responder")
    public ResponseEntity<?> responder(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Optional<Reporte> opt = reporteRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Reporte r = opt.get();
        r.setRespuestaAdmin(body.get("respuesta"));
        r.setFechaRespuesta(LocalDateTime.now());
        r.setEstado(Reporte.EstadoReporte.RESUELTO);
        return ResponseEntity.ok(reporteRepository.save(r));
    }

    @PutMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazar(@PathVariable Long id,
                                      @RequestBody(required = false) Map<String, String> body) {
        Optional<Reporte> opt = reporteRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Reporte r = opt.get();
        r.setEstado(Reporte.EstadoReporte.RECHAZADO);
        if (body != null) {
            String mensaje = body.get("mensaje");
            if (mensaje != null && !mensaje.isBlank()) {
                r.setRespuestaAdmin(mensaje.trim());
                r.setFechaRespuesta(LocalDateTime.now());
            }
        }
        return ResponseEntity.ok(reporteRepository.save(r));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        Optional<Reporte> opt = reporteRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        reporteRepository.delete(opt.get());
        return ResponseEntity.ok().build();
    }
}
