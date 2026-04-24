package com.fourverr.api.controller;

import com.fourverr.api.model.ChatMensaje;
import com.fourverr.api.model.MensajeContacto;
import com.fourverr.api.model.Role;
import com.fourverr.api.model.User;
import com.fourverr.api.repository.ChatMensajeRepository;
import com.fourverr.api.repository.MensajeContactoRepository;
import com.fourverr.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/soporte")

public class SoporteController {

    @Autowired private ChatMensajeRepository chatRepo;
    @Autowired private MensajeContactoRepository contactoRepo;
    @Autowired private UserRepository userRepo;

    // ══════════════════════════════════════════════
    //  CONTACTO PÚBLICO (visitantes sin cuenta)
    // ══════════════════════════════════════════════

    @PostMapping("/contacto")
    public ResponseEntity<?> enviarContacto(@RequestBody Map<String, String> body) {
        String nombre  = body.getOrDefault("nombre",  "").trim();
        String email   = body.getOrDefault("email",   "").trim();
        String telefono= body.getOrDefault("telefono","").trim();
        String mensaje = body.getOrDefault("mensaje", "").trim();

        if (nombre.isEmpty() || email.isEmpty() || mensaje.isEmpty())
            return ResponseEntity.badRequest().body("Nombre, email y mensaje son requeridos");

        MensajeContacto mc = new MensajeContacto();
        mc.setNombre(nombre);
        mc.setEmail(email);
        mc.setTelefono(telefono.isEmpty() ? null : telefono);
        mc.setMensaje(mensaje);
        contactoRepo.save(mc);
        return ResponseEntity.ok("Mensaje enviado. Te contactaremos pronto.");
    }

    // ══════════════════════════════════════════════
    //  CHAT USUARIOS AUTENTICADOS ↔ ADMIN
    // ══════════════════════════════════════════════

    /** Enviar mensaje (usuario → admin o admin → usuario) */
    @PostMapping("/chat/enviar")
    public ResponseEntity<?> enviar(@RequestBody Map<String, Object> body) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User remitente = userRepo.findByUsername(username).orElseThrow();

        String texto = body.getOrDefault("texto", "").toString().trim();
        if (texto.isEmpty()) return ResponseEntity.badRequest().body("El mensaje no puede estar vacío");

        User destinatario;
        if (remitente.getRole() == Role.ADMIN) {
            // Admin envía a un usuario específico
            Long destId = Long.valueOf(body.get("destinatarioId").toString());
            destinatario = userRepo.findById(destId).orElseThrow();
        } else {
            // Usuario envía al admin
            destinatario = userRepo.findAll().stream()
                    .filter(u -> u.getRole() == Role.ADMIN)
                    .findFirst()
                    .orElse(null);
            if (destinatario == null)
                return ResponseEntity.status(503).body("No hay administrador disponible");
        }

        ChatMensaje msg = new ChatMensaje();
        msg.setRemitente(remitente);
        msg.setDestinatario(destinatario);
        msg.setTexto(texto);
        return ResponseEntity.ok(chatRepo.save(msg));
    }

    /** Obtener conversación del usuario autenticado con el admin */
    @GetMapping("/chat/mi-conversacion")
    public ResponseEntity<?> miConversacion() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User yo = userRepo.findByUsername(username).orElseThrow();

        User admin = userRepo.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN).findFirst().orElse(null);
        if (admin == null) return ResponseEntity.ok(List.of());

        // Marcar como leídos los mensajes del admin hacia mí
        List<ChatMensaje> noLeidos = chatRepo.findNoLeidosParaMi(yo.getId(), admin.getId());
        noLeidos.forEach(m -> m.setLeido(true));
        if (!noLeidos.isEmpty()) chatRepo.saveAll(noLeidos);

        return ResponseEntity.ok(chatRepo.findConversacion(yo.getId(), admin.getId()));
    }

    /** Cuántos mensajes no leídos tiene el usuario */
    @GetMapping("/chat/no-leidos")
    public ResponseEntity<?> noLeidos() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User yo = userRepo.findByUsername(username).orElseThrow();
        long count = chatRepo.countByDestinatario_IdAndLeidoFalse(yo.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    // ══════════════════════════════════════════════
    //  ENDPOINTS EXCLUSIVOS ADMIN
    // ══════════════════════════════════════════════

    /** Lista de usuarios que tienen conversación con el admin */
    @GetMapping("/admin/chats")
    public ResponseEntity<?> listarChats() {
        User admin = getAdmin();
        if (admin == null) return ResponseEntity.status(403).body("No autorizado");

        // Obtener IDs únicos de usuarios con los que el admin habló
        List<ChatMensaje> todos = chatRepo.findAll();
        Set<Long> userIds = new LinkedHashSet<>();
        todos.stream()
            .filter(m -> m.getRemitente().getId().equals(admin.getId()) ||
                         m.getDestinatario().getId().equals(admin.getId()))
            .forEach(m -> {
                Long otherId = m.getRemitente().getId().equals(admin.getId())
                        ? m.getDestinatario().getId()
                        : m.getRemitente().getId();
                userIds.add(otherId);
            });

        // Cargar todos los usuarios en una sola query y conservar el orden original
        Map<Long, User> usuariosPorId = userRepo.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        long noLeidos = chatRepo.countByDestinatario_IdAndLeidoFalse(admin.getId());

        List<Map<String, Object>> resultado = userIds.stream().map(uid -> {
            User u = usuariosPorId.get(uid);
            if (u == null) return null;
            // último mensaje de esta conversación
            List<ChatMensaje> conv = chatRepo.findConversacion(admin.getId(), uid);
            String ultimoMensaje = conv.isEmpty() ? "" : conv.get(conv.size()-1).getTexto();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",             u.getId());
            m.put("username",       u.getUsername());
            m.put("nombreMostrado", u.getNombreMostrado() != null ? u.getNombreMostrado() : u.getUsername());
            m.put("fotoUrl",        u.getFotoUrl() != null ? u.getFotoUrl() : "");
            m.put("role",           u.getRole().toString());
            m.put("noLeidos",       noLeidos);
            m.put("ultimoMensaje",  ultimoMensaje);
            return m;
        }).filter(Objects::nonNull).collect(Collectors.toList());

        return ResponseEntity.ok(resultado);
    }

    /** Conversación del admin con un usuario específico */
    @GetMapping("/admin/chat/{usuarioId}")
    public ResponseEntity<?> conversacionAdmin(@PathVariable Long usuarioId) {
        User admin = getAdmin();
        if (admin == null) return ResponseEntity.status(403).body("No autorizado");

        // Marcar como leídos los mensajes del usuario hacia el admin
        List<ChatMensaje> noLeidos = chatRepo.findNoLeidosParaMi(admin.getId(), usuarioId);
        noLeidos.forEach(m -> m.setLeido(true));
        if (!noLeidos.isEmpty()) chatRepo.saveAll(noLeidos);

        return ResponseEntity.ok(chatRepo.findConversacion(admin.getId(), usuarioId));
    }

    /** Eliminar toda la conversación entre el admin y un usuario */
    @DeleteMapping("/admin/chat/{usuarioId}")
    public ResponseEntity<?> eliminarConversacion(@PathVariable Long usuarioId) {
        User admin = getAdmin();
        if (admin == null) return ResponseEntity.status(403).body("No autorizado");
        if (!userRepo.existsById(usuarioId)) return ResponseEntity.notFound().build();

        int eliminados = chatRepo.deleteConversacion(admin.getId(), usuarioId);
        return ResponseEntity.ok(Map.of("eliminados", eliminados));
    }

    /** Lista de mensajes de contacto (visitantes) */
    @GetMapping("/admin/contactos")
    public ResponseEntity<?> listarContactos() {
        User admin = getAdmin();
        if (admin == null) return ResponseEntity.status(403).body("No autorizado");
        return ResponseEntity.ok(contactoRepo.findAllByOrderByFechaEnvioDesc());
    }

    /** Marcar mensaje de contacto como leído */
    @PutMapping("/admin/contactos/{id}/leido")
    public ResponseEntity<?> marcarLeido(@PathVariable Long id) {
        User admin = getAdmin();
        if (admin == null) return ResponseEntity.status(403).body("No autorizado");
        return contactoRepo.findById(id).map(mc -> {
            mc.setLeido(true);
            return ResponseEntity.ok(contactoRepo.save(mc));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** Eliminar mensaje de contacto */
    @DeleteMapping("/admin/contactos/{id}")
    public ResponseEntity<?> eliminarContacto(@PathVariable Long id) {
        User admin = getAdmin();
        if (admin == null) return ResponseEntity.status(403).body("No autorizado");
        if (!contactoRepo.existsById(id)) return ResponseEntity.notFound().build();
        contactoRepo.deleteById(id);
        return ResponseEntity.ok().build();
    }

    /** Resumen de notificaciones para el admin */
    @GetMapping("/admin/resumen")
    public ResponseEntity<?> resumen() {
        User admin = getAdmin();
        if (admin == null) return ResponseEntity.status(403).body("No autorizado");
        long chatsNoLeidos = chatRepo.countByDestinatario_IdAndLeidoFalse(admin.getId());
        long contactosNoLeidos = contactoRepo.countByLeidoFalse();
        return ResponseEntity.ok(Map.of(
            "chatsNoLeidos",     chatsNoLeidos,
            "contactosNoLeidos", contactosNoLeidos,
            "total",             chatsNoLeidos + contactosNoLeidos
        ));
    }

    private User getAdmin() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByUsername(username)
                .filter(u -> u.getRole() == Role.ADMIN).orElse(null);
    }
}
