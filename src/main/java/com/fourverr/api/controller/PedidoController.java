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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

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
            String requisitos = datos.containsKey("requisitos")
                    ? (String) datos.get("requisitos") : null;
            String stripePaymentIntentId = datos.containsKey("stripePaymentIntentId")
                    ? datos.get("stripePaymentIntentId").toString() : null;

            // Cliente sacado del token JWT, nunca del body
            String usernameActual = SecurityContextHolder.getContext().getAuthentication().getName();
            User cliente = userRepository.findByUsername(usernameActual)
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

            Producto producto = productoRepository.findById(idProducto)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            // Comisión: 10% para Fourverr, 90% para el vendedor
            // Cambia el "0.10" cuando quieras ajustar tu comisión
            BigDecimal comision = new BigDecimal("0.10");
            BigDecimal montoVendedor = producto.getPrecio()
                    .multiply(BigDecimal.ONE.subtract(comision))
                    .setScale(2, RoundingMode.HALF_UP);

            Pedido pedido = new Pedido();
            pedido.setCliente(cliente);
            pedido.setProducto(producto);
            pedido.setRequisitosCliente(requisitos);
            pedido.setMontoVendedor(montoVendedor);
            pedido.setStripePaymentIntentId(stripePaymentIntentId);

            // Solo sumamos saldo si el pago de Stripe ya fue confirmado
            if (stripePaymentIntentId != null) {
                pedido.setEstado("PAGADO");

                User vendedor = producto.getVendedor();
                vendedor.setSaldoDisponible(
                    vendedor.getSaldoDisponible().add(montoVendedor)
                );
                userRepository.save(vendedor);

            } else {
                pedido.setEstado("PENDIENTE");
            }

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

    @GetMapping("/mis-ventas")
    public ResponseEntity<?> verMisVentas() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Pedido> ventas = pedidoRepository.findByProducto_Vendedor_Username(username);
        return ResponseEntity.ok(ventas);
    }
}