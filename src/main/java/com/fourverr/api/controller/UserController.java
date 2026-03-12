package com.fourverr.api.controller;

import com.fourverr.api.dto.JwtResponse;
import com.fourverr.api.model.Role;
import com.fourverr.api.model.User;
import com.fourverr.api.repository.*;
import com.fourverr.api.security.JwtUtil;
import com.fourverr.api.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired private UserRepository           userRepository;
    @Autowired private ProductoRepository       productoRepository;
    @Autowired private FavoritoRepository       favoritoRepository;
    @Autowired private PreguntaRepository       preguntaRepository;
    @Autowired private PedidoRepository         pedidoRepository;
    @Autowired private ResenaRepository         resenaRepository;
    @Autowired private ChatMensajeRepository    chatMensajeRepository;
    @Autowired private ReporteRepository        reporteRepository;
    @Autowired private MensajeCompraRepository  mensajeCompraRepository;
    @Autowired private JwtUtil                  jwtUtil;
    @Autowired private PasswordEncoder          passwordEncoder;
    @Autowired private S3Service                s3Service;

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

    // ── ELIMINAR USUARIO — cascada COMPLETA ──
    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        User admin = getAdmin();
        if (admin == null) return ResponseEntity.status(403).body("No autorizado");

        User user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        try {
            // 1. Productos del vendedor y sus dependencias
            var productos = productoRepository.findByVendedor_Id(id);
            for (var prod : productos) {
                Long prodId = prod.getId();
                // Mensajes de compra de pedidos de este producto
                pedidoRepository.findByProducto_Id(prodId)
                    .forEach(p -> mensajeCompraRepository.deleteByPedido_Id(p.getId()));
                // Reseñas del producto
                resenaRepository.deleteByProducto_Id(prodId);
                // Reportes que mencionan este producto
                reporteRepository.deleteByProducto_Id(prodId);
                // Resto de dependencias
                favoritoRepository.deleteByProducto_Id(prodId);
                preguntaRepository.deleteByProducto_Id(prodId);
                pedidoRepository.deleteByProducto_Id(prodId);
            }
            if (!productos.isEmpty()) productoRepository.deleteAll(productos);

            // 2. Compras del usuario como comprador
            var comprasUsuario = pedidoRepository.findByCliente_Id(id);
            for (var pedido : comprasUsuario) {
                mensajeCompraRepository.deleteByPedido_Id(pedido.getId());
            }

            // 3. Reseñas que el usuario escribió como cliente
            resenaRepository.deleteByCliente_Id(id);

            // 4. Favoritos del usuario
            favoritoRepository.deleteByUsuario_Id(id);

            // 5. Preguntas del usuario en productos de otros
            preguntaRepository.deleteByUsuario_Id(id);

            // 6. Pedidos donde el usuario fue comprador
            pedidoRepository.deleteByCliente_Id(id);

            // 7. Mensajes de chat de soporte
            chatMensajeRepository.deleteByRemitente_Id(id);
            chatMensajeRepository.deleteByDestinatario_Id(id);

            // 8. Reportes hechos por o contra el usuario
            reporteRepository.deleteByReportante_Id(id);
            reporteRepository.deleteByVendedor_Id(id);

            // 9. Mensajes de compra enviados por el usuario
            mensajeCompraRepository.deleteByRemitente_Id(id);

            // 10. Eliminar usuario
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
