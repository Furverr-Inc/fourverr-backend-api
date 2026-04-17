package com.fourverr.api.controller;

import com.fourverr.api.dto.JwtResponse;
import com.fourverr.api.model.Role;
import com.fourverr.api.model.SolicitudRetiro;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired private UserRepository              userRepository;
    @Autowired private ProductoRepository          productoRepository;
    @Autowired private FavoritoRepository          favoritoRepository;
    @Autowired private PreguntaRepository          preguntaRepository;
    @Autowired private PedidoRepository            pedidoRepository;
    @Autowired private ResenaRepository            resenaRepository;
    @Autowired private ChatMensajeRepository       chatMensajeRepository;
    @Autowired private ReporteRepository           reporteRepository;
    @Autowired private MensajeCompraRepository     mensajeCompraRepository;
    @Autowired private SolicitudRetiroRepository   solicitudRetiroRepository;
    @Autowired private JwtUtil                     jwtUtil;
    @Autowired private PasswordEncoder             passwordEncoder;
    @Autowired private S3Service                   s3Service;

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

        String usernameIngresado = datos.get("username");
        String emailIngresado    = datos.get("email");
        String passwordNueva     = datos.get("passwordNueva");

        if (usernameIngresado == null || emailIngresado == null || passwordNueva == null
                || usernameIngresado.isBlank() || emailIngresado.isBlank() || passwordNueva.isBlank())
            return ResponseEntity.badRequest().body("Completa todos los campos");

        if (!user.getUsername().equalsIgnoreCase(usernameIngresado.trim()))
            return ResponseEntity.badRequest().body("El nombre de usuario no coincide con tu cuenta");

        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(emailIngresado.trim()))
            return ResponseEntity.badRequest().body("El correo no coincide con tu cuenta");

        if (!passwordNueva.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,}$"))
            return ResponseEntity.badRequest().body("La nueva contraseña debe tener mínimo 8 caracteres, con al menos 1 letra y 1 número");

        if (passwordEncoder.matches(passwordNueva, user.getPassword()))
            return ResponseEntity.badRequest().body("La nueva contraseña debe ser distinta a la actual");

        user.setPassword(passwordEncoder.encode(passwordNueva));
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

    // ──────────── RETIROS ────────────

    /** Vendedor solicita un retiro de su saldo */
    @PostMapping("/retiros/solicitar")
    public ResponseEntity<?> solicitarRetiro(@RequestBody Map<String, Object> body) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User vendedor = userRepository.findByUsername(username).orElseThrow();

        if (vendedor.getRole() != Role.SELLER && vendedor.getRole() != Role.ADMIN)
            return ResponseEntity.status(403).body("Solo los vendedores pueden solicitar retiros");

        BigDecimal monto;
        try {
            monto = new BigDecimal(body.get("monto").toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Monto inválido");
        }

        if (monto.compareTo(BigDecimal.ZERO) <= 0)
            return ResponseEntity.badRequest().body("El monto debe ser mayor a 0");

        if (monto.compareTo(vendedor.getSaldoDisponible()) > 0)
            return ResponseEntity.badRequest().body("Saldo insuficiente");

        // Descontar saldo inmediatamente para evitar doble solicitud
        vendedor.setSaldoDisponible(vendedor.getSaldoDisponible().subtract(monto));
        userRepository.save(vendedor);

        SolicitudRetiro solicitud = new SolicitudRetiro();
        solicitud.setVendedor(vendedor);
        solicitud.setMonto(monto);
        solicitud.setEstado("PENDIENTE");
        solicitud.setNotas(body.containsKey("notas") ? body.get("notas").toString() : null);

        return ResponseEntity.ok(solicitudRetiroRepository.save(solicitud));
    }

    /** Vendedor consulta su historial de retiros */
    @GetMapping("/retiros/mis-solicitudes")
    public ResponseEntity<?> misSolicitudesRetiro() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(
            solicitudRetiroRepository.findByVendedor_UsernameOrderByFechaSolicitudDesc(username)
        );
    }

    /** Admin lista todos los retiros pendientes */
    @GetMapping("/retiros/pendientes")
    public ResponseEntity<?> retirosPendientes() {
        User admin = getAdmin();
        if (admin == null) return ResponseEntity.status(403).body("No autorizado");
        return ResponseEntity.ok(
            solicitudRetiroRepository.findByEstadoOrderByFechaSolicitudDesc("PENDIENTE")
        );
    }

    /** Admin lista todos los retiros */
    @GetMapping("/retiros/todos")
    public ResponseEntity<?> retirosAdmin() {
        User admin = getAdmin();
        if (admin == null) return ResponseEntity.status(403).body("No autorizado");
        return ResponseEntity.ok(
            solicitudRetiroRepository.findAllByOrderByFechaSolicitudDesc()
        );
    }

    /** Admin aprueba un retiro */
    @PutMapping("/retiros/{id}/aprobar")
    public ResponseEntity<?> aprobarRetiro(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        User admin = getAdmin();
        if (admin == null) return ResponseEntity.status(403).body("No autorizado");

        SolicitudRetiro solicitud = solicitudRetiroRepository.findById(id).orElse(null);
        if (solicitud == null) return ResponseEntity.notFound().build();
        if (!"PENDIENTE".equals(solicitud.getEstado()))
            return ResponseEntity.badRequest().body("Solo se pueden aprobar solicitudes pendientes");

        solicitud.setEstado("COMPLETADO");
        solicitud.setFechaProcesado(LocalDateTime.now());
        if (body != null && body.containsKey("notas")) solicitud.setNotas(body.get("notas"));

        return ResponseEntity.ok(solicitudRetiroRepository.save(solicitud));
    }

    /** Admin rechaza un retiro y devuelve el saldo al vendedor */
    @PutMapping("/retiros/{id}/rechazar")
    public ResponseEntity<?> rechazarRetiro(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        User admin = getAdmin();
        if (admin == null) return ResponseEntity.status(403).body("No autorizado");

        SolicitudRetiro solicitud = solicitudRetiroRepository.findById(id).orElse(null);
        if (solicitud == null) return ResponseEntity.notFound().build();
        if (!"PENDIENTE".equals(solicitud.getEstado()))
            return ResponseEntity.badRequest().body("Solo se pueden rechazar solicitudes pendientes");

        // Devolver saldo al vendedor
        User vendedor = solicitud.getVendedor();
        vendedor.setSaldoDisponible(vendedor.getSaldoDisponible().add(solicitud.getMonto()));
        userRepository.save(vendedor);

        solicitud.setEstado("RECHAZADO");
        solicitud.setFechaProcesado(LocalDateTime.now());
        if (body != null && body.containsKey("notas")) solicitud.setNotas(body.get("notas"));

        return ResponseEntity.ok(solicitudRetiroRepository.save(solicitud));
    }

    // ──────────── ADMIN — USUARIOS ────────────
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

            // 10. Solicitudes de retiro del vendedor
            solicitudRetiroRepository.findByVendedor_UsernameOrderByFechaSolicitudDesc(user.getUsername())
                .forEach(solicitudRetiroRepository::delete);

            // 11. Eliminar usuario
            userRepository.delete(user);

            return ResponseEntity.ok("Usuario eliminado correctamente");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al eliminar usuario: " + e.getMessage());
        }
    }

    // ── ELIMINAR MI PROPIA CUENTA ──
    @Transactional
    @DeleteMapping("/perfil/eliminar-cuenta")
    public ResponseEntity<?> eliminarMiCuenta() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.status(404).body("Usuario no encontrado");

        Long id = user.getId();
        try {
            // 1. Productos del usuario y sus dependencias
            var productos = productoRepository.findByVendedor_Id(id);
            for (var prod : productos) {
                resenaRepository.deleteByProducto_Id(prod.getId());
                reporteRepository.deleteByProducto_Id(prod.getId());
                var pedidosProd = pedidoRepository.findByProducto_Id(prod.getId());
                for (var p : pedidosProd) mensajeCompraRepository.deleteByPedido_Id(p.getId());
                favoritoRepository.deleteByProducto_Id(prod.getId());
                preguntaRepository.deleteByProducto_Id(prod.getId());
                pedidoRepository.deleteByProducto_Id(prod.getId());
            }
            if (!productos.isEmpty()) productoRepository.deleteAll(productos);

            // 2. Compras como cliente
            var pedidosCliente = pedidoRepository.findByCliente_Id(id);
            for (var p : pedidosCliente) mensajeCompraRepository.deleteByPedido_Id(p.getId());
            resenaRepository.deleteByCliente_Id(id);
            favoritoRepository.deleteByUsuario_Id(id);
            preguntaRepository.deleteByUsuario_Id(id);
            pedidoRepository.deleteByCliente_Id(id);

            // 3. Chat soporte y reportes
            chatMensajeRepository.deleteByRemitente_Id(id);
            chatMensajeRepository.deleteByDestinatario_Id(id);
            reporteRepository.deleteByReportante_Id(id);
            reporteRepository.deleteByVendedor_Id(id);
            mensajeCompraRepository.deleteByRemitente_Id(id);

            // 4. Solicitudes de retiro
            solicitudRetiroRepository.findByVendedor_UsernameOrderByFechaSolicitudDesc(username)
                .forEach(solicitudRetiroRepository::delete);

            // 5. Foto de perfil en S3
            if (user.getFotoUrl() != null && !user.getFotoUrl().isBlank()) {
                try { s3Service.eliminarImagen(user.getFotoUrl()); } catch (Exception ignored) {}
            }

            // 6. Eliminar usuario
            userRepository.delete(user);
            return ResponseEntity.ok("Cuenta eliminada correctamente");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al eliminar cuenta: " + e.getMessage());
        }
    }

    private User getAdmin() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .filter(u -> u.getRole() == Role.ADMIN)
                .orElse(null);
    }
}
