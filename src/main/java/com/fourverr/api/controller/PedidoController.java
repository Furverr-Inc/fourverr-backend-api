package com.fourverr.api.controller;

import com.fourverr.api.model.Pedido;
import com.fourverr.api.model.Producto;
import com.fourverr.api.model.User;
import com.fourverr.api.repository.PedidoRepository;
import com.fourverr.api.repository.ProductoRepository;
import com.fourverr.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "http://localhost:5173")
public class PedidoController {

    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductoRepository productoRepository;

    @PostMapping
    public ResponseEntity<?> crearPedido(@RequestBody Map<String, Object> datos) {
        try {
            Long idProducto = Long.valueOf(datos.get("idProducto").toString());
            String requisitos = (String) datos.get("requisitos");

            // SEGURIDAD: Obtenemos el cliente del Token
            String usernameActual = SecurityContextHolder.getContext().getAuthentication().getName();
            User cliente = userRepository.findByUsername(usernameActual)
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

            Optional<Producto> productoOpt = productoRepository.findById(idProducto);
            if (productoOpt.isEmpty()) return ResponseEntity.badRequest().body("Producto no existe");

            Pedido pedido = new Pedido();
            pedido.setCliente(cliente);
            pedido.setProducto(productoOpt.get());
            pedido.setRequisitosCliente(requisitos);
            pedido.setEstado("PENDIENTE");

            return ResponseEntity.ok(pedidoRepository.save(pedido));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/mis-compras")
    public List<Pedido> verMisCompras() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return pedidoRepository.findByCliente_Username(username);
    }
}