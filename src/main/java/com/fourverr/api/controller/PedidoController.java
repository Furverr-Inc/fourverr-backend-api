package com.fourverr.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourverr.api.model.Pedido;
import com.fourverr.api.model.Producto;
import com.fourverr.api.model.TipoProducto;
import com.fourverr.api.model.User;
import com.fourverr.api.repository.PedidoRepository;
import com.fourverr.api.repository.ProductoRepository;
import com.fourverr.api.repository.UserRepository;
import com.fourverr.api.service.StripeService;
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

public class PedidoController {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private StripeService stripeService;

    @PostMapping
    public ResponseEntity<?> crearPedido(@RequestBody Map<String, Object> datos) {
        try {
            Long idProducto = Long.valueOf(datos.get("idProducto").toString());
            String requisitos = datos.containsKey("requisitos") ? (String) datos.get("requisitos") : null;
            String stripeId = datos.containsKey("stripePaymentIntentId") ? datos.get("stripePaymentIntentId").toString() : null;
            int cantidad = datos.containsKey("cantidad") ? Integer.parseInt(datos.get("cantidad").toString()) : 1;
            if (cantidad < 1) cantidad = 1;

            String usernameActual = SecurityContextHolder.getContext().getAuthentication().getName();
            User cliente = userRepository.findByUsername(usernameActual).orElseThrow();
            Producto producto = productoRepository.findById(idProducto).orElseThrow();

            if (producto.getTipo() == TipoProducto.PRODUCTO_FISICO) {
                String envioErr = validarDatosEnvioFisico(requisitos);
                if (envioErr != null) {
                    return ResponseEntity.badRequest().body(envioErr);
                }
            }

            BigDecimal precioTotal = producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));
            BigDecimal montoVendedor = precioTotal.multiply(new BigDecimal("0.90")).setScale(2, RoundingMode.HALF_UP);

            Pedido pedido = new Pedido();
            pedido.setCliente(cliente);
            pedido.setProducto(producto);
            pedido.setRequisitosCliente(requisitos);
            pedido.setMontoVendedor(montoVendedor);
            pedido.setStripePaymentIntentId(stripeId);
            pedido.setCantidad(cantidad);

            if (stripeId != null) {
                // Verificar con Stripe que el pago realmente se completó
                if (!stripeService.verificarPago(stripeId)) {
                    return ResponseEntity.badRequest().body("El pago no pudo ser verificado con Stripe");
                }
                pedido.setEstado("PAGADO");
                User vendedor = producto.getVendedor();
                vendedor.setSaldoDisponible(vendedor.getSaldoDisponible().add(montoVendedor));
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
        return ResponseEntity.ok(pedidoRepository.findByProducto_Vendedor_Username(username));
    }

    /**
     * {@code requisitos} debe ser JSON con los campos de envío (misma estructura que envía el front).
     * @return mensaje de error o {@code null} si es válido
     */
    private static String validarDatosEnvioFisico(String requisitos) {
        if (requisitos == null || requisitos.isBlank()) {
            return "Los datos de envío son obligatorios para productos físicos.";
        }
        try {
            JsonNode n = JSON.readTree(requisitos);
            if (!n.path("tipoEnvio").asBoolean(false)) {
                return "Formato de datos de envío no reconocido.";
            }
            String[] keys = {"destinatario", "telefono", "pais", "estado", "municipio",
                    "ciudad", "colonia", "codigoPostal", "calleNumero"};
            for (String k : keys) {
                if (!n.has(k) || !n.get(k).isTextual()) {
                    return "Falta el campo de envío: " + k;
                }
                if (n.get(k).asText().trim().isEmpty()) {
                    return "El campo de envío no puede estar vacío: " + k;
                }
            }
            String pais = n.get("pais").asText().trim().toLowerCase();
            String cp = n.get("codigoPostal").asText().trim();
            boolean mexico = pais.contains("méxico") || pais.contains("mexico") || pais.equals("mx");
            if (mexico && !cp.matches("\\d{5}")) {
                return "El código postal para México debe tener 5 dígitos.";
            }
            if (!mexico && (cp.length() < 3 || cp.length() > 16)) {
                return "Código postal inválido.";
            }
            String tel = n.get("telefono").asText().replaceAll("\\D", "");
            if (tel.length() < 10) {
                return "El teléfono de contacto debe tener al menos 10 dígitos.";
            }
            if (n.get("destinatario").asText().trim().length() < 3) {
                return "El nombre del destinatario es demasiado corto.";
            }
            if (n.get("calleNumero").asText().trim().length() < 5) {
                return "La dirección (calle y número) es demasiado corta.";
            }
        } catch (Exception e) {
            return "Datos de envío inválidos. Vuelve a completar el formulario.";
        }
        return null;
    }
}
