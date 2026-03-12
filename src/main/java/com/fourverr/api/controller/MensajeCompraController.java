package com.fourverr.api.controller;

import com.fourverr.api.model.MensajeCompra;
import com.fourverr.api.model.Pedido;
import com.fourverr.api.model.User;
import com.fourverr.api.repository.MensajeCompraRepository;
import com.fourverr.api.repository.PedidoRepository;
import com.fourverr.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "http://localhost:5173")
public class MensajeCompraController {

    @Autowired private MensajeCompraRepository mensajeCompraRepository;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private UserRepository userRepository;

    // GET /api/pedidos/{id}/mensajes — comprador O vendedor del pedido
    @GetMapping("/{id}/mensajes")
    public ResponseEntity<?> obtenerMensajes(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        Pedido pedido = pedidoRepository.findById(id).orElse(null);
        if (pedido == null) return ResponseEntity.notFound().build();

        // Solo puede ver mensajes el comprador o el vendedor
        boolean esComprador = pedido.getCliente().getId().equals(user.getId());
        boolean esVendedor  = pedido.getProducto().getVendedor().getId().equals(user.getId());
        if (!esComprador && !esVendedor)
            return ResponseEntity.status(403).body("No tienes acceso a este pedido");

        List<MensajeCompra> mensajes = mensajeCompraRepository
                .findByPedido_IdOrderByFechaEnvioAsc(id);

        // Marcar como leídos los mensajes del otro
        mensajes.stream()
                .filter(m -> !m.getRemitente().getId().equals(user.getId()) && !m.isLeido())
                .forEach(m -> { m.setLeido(true); mensajeCompraRepository.save(m); });

        return ResponseEntity.ok(mensajes);
    }

    // POST /api/pedidos/{id}/mensajes — enviar mensaje
    @PostMapping("/{id}/mensajes")
    public ResponseEntity<?> enviarMensaje(@PathVariable Long id,
                                           @RequestBody Map<String, String> body) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        Pedido pedido = pedidoRepository.findById(id).orElse(null);
        if (pedido == null) return ResponseEntity.notFound().build();

        boolean esComprador = pedido.getCliente().getId().equals(user.getId());
        boolean esVendedor  = pedido.getProducto().getVendedor().getId().equals(user.getId());
        if (!esComprador && !esVendedor)
            return ResponseEntity.status(403).body("No tienes acceso a este pedido");

        String texto = body.get("texto");
        if (texto == null || texto.isBlank())
            return ResponseEntity.badRequest().body("El mensaje no puede estar vacío");

        MensajeCompra mensaje = new MensajeCompra();
        mensaje.setPedido(pedido);
        mensaje.setRemitente(user);
        mensaje.setTexto(texto.trim());

        return ResponseEntity.ok(mensajeCompraRepository.save(mensaje));
    }

    // GET /api/pedidos/{id}/mensajes/no-leidos — contar no leídos
    @GetMapping("/{id}/mensajes/no-leidos")
    public ResponseEntity<?> noLeidos(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        Pedido pedido = pedidoRepository.findById(id).orElse(null);
        if (pedido == null) return ResponseEntity.notFound().build();

        boolean esComprador = pedido.getCliente().getId().equals(user.getId());
        boolean esVendedor  = pedido.getProducto().getVendedor().getId().equals(user.getId());
        if (!esComprador && !esVendedor)
            return ResponseEntity.status(403).body("No tienes acceso");

        long count = mensajeCompraRepository
                .findByPedido_IdOrderByFechaEnvioAsc(id)
                .stream()
                .filter(m -> !m.getRemitente().getId().equals(user.getId()) && !m.isLeido())
                .count();

        return ResponseEntity.ok(Map.of("noLeidos", count));
    }
}
