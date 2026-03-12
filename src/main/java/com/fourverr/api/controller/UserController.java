package com.fourverr.api.controller;

import com.fourverr.api.dto.JwtResponse;
import com.fourverr.api.model.Role;
import com.fourverr.api.model.SolicitudRetiro;
import com.fourverr.api.model.User;
import com.fourverr.api.repository.ChatMensajeRepository;
import com.fourverr.api.repository.FavoritoRepository;
import com.fourverr.api.repository.PedidoRepository;
import com.fourverr.api.repository.PreguntaRepository;
import com.fourverr.api.repository.ProductoRepository;
import com.fourverr.api.repository.ResenaRepository;
import com.fourverr.api.repository.SolicitudRetiroRepository;
import com.fourverr.api.repository.UserRepository;
import com.fourverr.api.security.JwtUtil;
import com.fourverr.api.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired private UserRepository userRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private FavoritoRepository favoritoRepository;
    @Autowired private PreguntaRepository preguntaRepository;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private ResenaRepository resenaRepository;
    @Autowired private ChatMensajeRepository chatMensajeRepository;
    @Autowired private SolicitudRetiroRepository solicitudRetiroRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private S3Service s3Service;

    // ──────────── HELPER ────────────
    private Map<String, Object> buildPerfil(User user, boolean incluirPrivados) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id",             user.getId());
        map.put("username",       user.getUsername());
        map.put("nombreMostrado", nvl(user.getNombreMostrado()));
        map.put("role",           user.getRole().toString());
        map.put("descripcion",    nvl(user.getDescripcion()));
        map.put("fotoUrl",        nvl(user.getFotoUrl()));
        map.put("ciudad",         nvl(user.getCiudad()));
        map.put("pais",           nvl(user.getPais()));
        map.put("sitioWeb",       nvl(user.getSitioWeb()));
        map.put("instagram",      nvl(user.getInstagram()));
        map.put("twitter",        nvl(user.getTwitter()));
        map.put("linkedin",       nvl(user.getLinkedin()));
        if (incluirPrivados) {
            map.put("email",             user.getEmail());
            map.put("telefono",          nvl(user.getTelefono()));
            map.put("solicitudVendedor", user.isSolicitudVendedor());
            map.put("saldoDisponible",   user.getSaldoDisponible());
        }
        return map;
    }
    private String nvl(String s) { return s != null ? s : ""; }

    // ──────────── PERFIL ────────────
    @GetMapping("/perfil")
    public ResponseEntity<?> obtenerPerfil() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        return ResponseEntity.ok(buildPerfil(user, true));
    }

    @GetMapping("/perfil/{username}")
    public ResponseEntity<?> perfilPublico(@PathVariable String username) {
        return userRepository.findByUsername(username)
                .map(u -> ResponseEntity.ok(buildPerfil(u, false)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/perfil")
    public ResponseEntity<?> actualizarPerfil(@RequestBody Map<String, String> datos) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        if (datos.containsKey("nombreMostrado")) user.setNombreMostrado(datos.get("nombreMostrado"));
        if (datos.containsKey("email"))          user.setEmail(datos.get("email"));
        if (datos.containsKey("descripcion"))    user.setDescripcion(datos.get("descripcion"));
        if (datos.containsKey("telefono"))       user.setTelefono(datos.get("telefono"));
        if (datos.containsKey("ciudad"))         user.setCiudad(datos.get("ciudad"));
        if (datos.containsKey("pais"))           user.setPais(datos.get("pais"));
        if (datos.containsKey("sitioWeb"))       user.setSitioWeb(datos.get("sitioWeb"));
        if (datos.containsKey("instagram"))      user.setInstagram(datos.get("instagram"));
        if (datos.containsKey("twitter"))        user.setTwitter(datos.get("twitter"));
        if (datos.containsKey("linkedin"))       user.setLinkedin(datos.get("linkedin"));
        userRepository.save(user);
        return ResponseEntity.ok("Perfil actualizado correctamente");
    }

    @PostMapping("/perfil/foto")
    public ResponseEntity<?> actualizarFoto(@RequestParam("archivo") MultipartFile archivo) {
        String ct = archivo.getContentType();
        if (ct == null || (!ct.equals("image/jpeg") && !ct.equals("image/png")))
            return ResponseEntity.badRequest().body("Solo JPG o PNG");
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(username).orElseThrow();
            if (user.getFotoUrl() != null && !user.getFotoUrl().isEmpty())
                s3Service.eliminarImagen(user.getFotoUrl());
            String url = s3Service.subirImagenPerfil(archivo, username);
            user.setFotoUrl(url);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/perfil/password")
    public ResponseEntity<?> cambiarPassword(@RequestBody Map<String, String> datos) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        if (!passwordEncoder.matches(datos.get("passwordActual"), user.getPassword()))
            return ResponseEntity.badRequest().body("La contraseña actual es incorrecta");
        user.setPassword(passwordEncoder.encode(datos.get("passwordNueva")));
        userRepository.save(user);
        return ResponseEntity.ok("Contraseña actualizada");
    }

    @GetMapping("/refresh-status")
    public ResponseEntity<?> refrescarEstado() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        return ResponseEntity.ok(new JwtResponse(
            jwtUtil.generateToken(user), user.getUsername(), user.getId(), user.getRole().toString()));
    }

    // ──────────── VENDEDOR ────────────
    @PostMapping("/solicitar-vendedor")
    public ResponseEntity<?> solicitarVendedor() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        if (user.getRole() == Role.SELLER) return ResponseEntity.badRequest().body("¡Ya eres vendedor!");
        user.setSolicitudVendedor(true);
        userRepository.save(user);
        return ResponseEntity.ok("Solicitud enviada.");
    }

    @PostMapping("/solicitar-retiro")
    public ResponseEntity<?> solicitarRetiro(@RequestBody Map<String, Object> datos) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        if (user.getRole() != Role.SELLER)
            return ResponseEntity.status(403).body("Solo vendedores pueden solicitar retiros");

        BigDecimal monto = new BigDecimal(datos.get("monto").toString());

        if (monto.compareTo(BigDecimal.ZERO) <= 0)
            return ResponseEntity.badRequest().body("El monto debe ser mayor a 0");

        if (monto.compareTo(user.getSaldoDisponible()) > 0)
            return ResponseEntity.badRequest().body("Saldo insuficiente");

        user.setSaldoDisponible(user.getSaldoDisponible().subtract(monto));
        userRepository.save(user);

        SolicitudRetiro solicitud = new SolicitudRetiro();
        solicitud.setVendedor(user);
        solicitud.setMonto(monto);
        solicitudRetiroRepository.save(solicitud);

        return ResponseEntity.ok(Map.of(
            "mensaje", "Solicitud de retiro enviada por $" + monto + " MXN. Te contactaremos en 1-3 días hábiles.",
            "saldoRestante", user.getSaldoDisponible()
        ));
    }

    // ──────────── ADMIN ────────────
    @GetMapping("/debug/mi-rol")
    public ResponseEntity<?> verMiRol() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        return ResponseEntity.ok(Map.of(
            "username", user.getUsername(), "role", user.getRole().toString(),
            "id", user.getId(), "habilitado", user.isHabilitado()));
    }

    @PutMapping("/setup/hacer-admin/{id}")
    public ResponseEntity<?> hacerAdmin(@PathVariable Long id) {
        if (userRepository.findAll().stream().anyMatch(u -> u.getRole() == Role.ADMIN))
            return ResponseEntity.status(403).body("Ya existe un administrador.");
        User user = userRepository.findById(id).orElseThrow();
        user.setRole(Role.ADMIN);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("mensaje", "✅ " + user.getUsername() + " ahora es ADMIN"));
    }

    @GetMapping("/solicitudes-vendedor")
    public ResponseEntity<?> solicitudes() {
        User admin = getAdmin();
        if (admin == null) return ResponseEntity.status(403).body("No tienes permisos");
        return ResponseEntity.ok(userRepository.findAll().stream()
                .filter(u -> u.isSolicitudVendedor() && u.getRole() == Role.USER)
                .map(u -> Map.of("id", u.getId(), "username", u.getUsername(),
                        "nombreMostrado", nvl(u.getNombreMostrado()), "email", u.getEmail()))
                .collect(Collectors.toList()));
    }

    @GetMapping("/vendedores")
    public ResponseEntity<?> vendedores() {
        User admin = getAdmin();
        if (admin == null) return ResponseEntity.status(403).body("No tienes permisos");
        return ResponseEntity.ok(userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.SELLER)
                .map(u -> Map.of("id", u.getId(), "username", u.getUsername(),
                        "nombreMostrado", nvl(u.getNombreMostrado()), "email", u.getEmail(),
                        "habilitado", u.isHabilitado(), "saldoDisponible", u.getSaldoDisponible()))
                .collect(Collectors.toList()));
    }

    @GetMapping("/todos")
    public ResponseEntity<?> todos() {
        User admin = getAdmin();
        if (admin == null) return ResponseEntity.status(403).body("No tienes permisos");
        return ResponseEntity.ok(userRepository.findAll().stream()
                .filter(u -> u.getRole() != Role.ADMIN)
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",             u.getId());
                    m.put("username",       u.getUsername());
                    m.put("nombreMostrado", nvl(u.getNombreMostrado()));
                    m.put("email",          u.getEmail());
                    m.put("role",           u.getRole().toString());
                    m.put("habilitado",     u.isHabilitado());
                    m.put("fotoUrl",        nvl(u.getFotoUrl()));
                    m.put("descripcion",    nvl(u.getDescripcion()));
                    m.put("telefono",       nvl(u.getTelefono()));
                    m.put("ciudad",         nvl(u.getCiudad()));
                    m.put("pais",           nvl(u.getPais()));
                    m.put("sitioWeb",       nvl(u.getSitioWeb()));
                    m.put("instagram",      nvl(u.getInstagram()));
                    m.put("twitter",        nvl(u.getTwitter()));
                    m.put("linkedin",       nvl(u.getLinkedin()));
                    m.put("saldoDisponible",u.getSaldoDisponible());
                    m.put("solicitudVendedor", u.isSolicitudVendedor());
                    return m;
                })
                .collect(Collectors.toList()));
    }

    // ──────────── RETIROS (ADMIN) ────────────
    @GetMapping("/retiros")
    public ResponseEntity<?> verRetiros() {
        User admin = getAdmin();
        if (admin == null) return ResponseEntity.status(403).body("No autorizado");
        return ResponseEntity.ok(solicitudRetiroRepository.findAllByOrderByFechaSolicitudDesc());
    }

    @PutMapping("/retiros/{id}/completar")
    public ResponseEntity<?> completarRetiro(@PathVariable Long id) {
        User admin = getAdmin();
        if (admin == null) return ResponseEntity.status(403).body("No autorizado");
        SolicitudRetiro s = solicitudRetiroRepository.findById(id).orElseThrow();
        s.setEstado("COMPLETADO");
        s.setFechaProcesado(LocalDateTime.now());
        solicitudRetiroRepository.save(s);
        return ResponseEntity.ok("Retiro marcado como completado");
    }

    @PutMapping("/retiros/{id}/rechazar")
    public ResponseEntity<?> rechazarRetiro(@PathVariable Long id) {
        User admin = getAdmin();
        if (admin == null) return ResponseEntity.status(403).body("No autorizado");
        SolicitudRetiro s = solicitudRetiroRepository.findById(id).orElseThrow();
        User vendedor = s.getVendedor();
        vendedor.setSaldoDisponible(vendedor.getSaldoDisponible().add(s.getMonto()));
        userRepository.save(vendedor);
        s.setEstado("RECHAZADO");
        s.setFechaProcesado(LocalDateTime.now());
        solicitudRetiroRepository.save(s);
        return ResponseEntity.ok("Retiro rechazado y saldo devuelto");
    }

    @PutMapping("/{id}/aprobar-vendedor")
    public ResponseEntity<?> aprobar(@PathVariable Long id) {
        User admin = getAdmin();
        if (admin == null) return ResponseEntity.status(403).body("No autorizado");
        User user = userRepository.findById(id).orElseThrow();
        user.setRole(Role.SELLER);
        user.setSolicitudVendedor(false);
        userRepository.save(user);
        return ResponseEntity.ok("Aprobado");
    }

    @PutMapping("/{id}/rechazar-vendedor")
    public ResponseEntity<?> rechazar(@PathVariable Long id) {
        User admin = getAdmin();
        if (admin == null) return ResponseEntity.status(403).body("No autorizado");
        User user = userRepository.findById(id).orElseThrow();
        user.setSolicitudVendedor(false);
        userRepository.save(user);
        return ResponseEntity.ok("Rechazado");
    }

    @PutMapping("/{id}/toggle-habilitado")
    public ResponseEntity<?> toggle(@PathVariable Long id) {
        User admin = getAdmin();
        if (admin == null) return ResponseEntity.status(403).body("No autorizado");
        User user = userRepository.findById(id).orElseThrow();
        user.setHabilitado(!user.isHabilitado());
        userRepository.save(user);
        return ResponseEntity.ok(user.isHabilitado() ? "Habilitado" : "Deshabilitado");
    }

    @PutMapping("/{id}/habilitar")
    public ResponseEntity<?> habilitar(@PathVariable Long id) {
        User admin = getAdmin();
        if (admin == null) return ResponseEntity.status(403).body("No autorizado");
        User user = userRepository.findById(id).orElseThrow();
        user.setHabilitado(true);
        userRepository.save(user);
        return ResponseEntity.ok("Habilitado");
    }

    @PutMapping("/{id}/deshabilitar")
    public ResponseEntity<?> deshabilitar(@PathVariable Long id) {
        User admin = getAdmin();
        if (admin == null) return ResponseEntity.status(403).body("No autorizado");
        User user = userRepository.findById(id).orElseThrow();
        user.setHabilitado(false);
        userRepository.save(user);
        return ResponseEntity.ok("Deshabilitado");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        User admin = getAdmin();
        if (admin == null) return ResponseEntity.status(403).body("No autorizado");

        User user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        try {
            var productos = productoRepository.findByVendedor_Id(id);
            for (var prod : productos) {
                resenaRepository.deleteByProducto_Id(prod.getId());
                favoritoRepository.deleteByProducto_Id(prod.getId());
                preguntaRepository.deleteByProducto_Id(prod.getId());
                pedidoRepository.deleteByProducto_Id(prod.getId());
            }
            if (!productos.isEmpty()) productoRepository.deleteAll(productos);
            resenaRepository.deleteByCliente_Id(id);
            favoritoRepository.deleteByUsuario_Id(id);
            preguntaRepository.deleteByUsuario_Id(id);
            pedidoRepository.deleteByCliente_Id(id);
            chatMensajeRepository.deleteByRemitente_Id(id);
            chatMensajeRepository.deleteByDestinatario_Id(id);
            userRepository.delete(user);
            return ResponseEntity.ok("Usuario eliminado correctamente");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al eliminar usuario: " + e.getMessage());
        }
    }

    private User getAdmin() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .filter(u -> u.getRole() == Role.ADMIN)
                .orElse(null);
    }
}
