package com.fourverr.api.controller;

import com.fourverr.api.model.Producto;
import com.fourverr.api.model.Role;
import com.fourverr.api.model.User;
import com.fourverr.api.repository.ProductoRepository;
import com.fourverr.api.repository.UserRepository;
import com.fourverr.api.service.StripeService;
import com.stripe.model.PaymentIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
public class StripeController {

    @Autowired private StripeService stripeService;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private UserRepository userRepository;

    // ─────────── PAYMENT INTENT ───────────

    /**
     * Crea un PaymentIntent para el producto dado.
     * Si el vendedor tiene cuenta de Stripe Connect activa → usa destination charge (90% al vendedor).
     * Si no → PaymentIntent estándar (el saldo se acumula en la plataforma como antes).
     */
    @PostMapping("/crear-payment-intent")
    public ResponseEntity<?> crearPaymentIntent(@RequestBody Map<String, Object> datos) {
        try {
            Long idProducto = Long.valueOf(datos.get("idProducto").toString());
            String currency = datos.getOrDefault("currency", "mxn").toString();
            int cantidad = datos.containsKey("cantidad")
                    ? Integer.parseInt(datos.get("cantidad").toString()) : 1;
            if (cantidad < 1) cantidad = 1;

            Producto producto = productoRepository.findById(idProducto)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            BigDecimal precioTotal = producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));

            PaymentIntent intent;
            boolean usaConnect = false;

            // Si el vendedor ya tiene cuenta de Connect habilitada, usar destination charge
            User vendedor = producto.getVendedor();
            if (vendedor != null
                    && vendedor.getStripeAccountId() != null
                    && !vendedor.getStripeAccountId().isBlank()
                    && stripeService.cuentaHabilitada(vendedor.getStripeAccountId())) {

                intent = stripeService.crearPaymentIntentConnect(
                        precioTotal, currency, vendedor.getStripeAccountId());
                usaConnect = true;
            } else {
                intent = stripeService.crearPaymentIntent(precioTotal, currency);
            }

            return ResponseEntity.ok(Map.of(
                "clientSecret",    intent.getClientSecret(),
                "paymentIntentId", intent.getId(),
                "monto",           precioTotal,
                "usaConnect",      usaConnect
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────── STRIPE CONNECT — ONBOARDING ───────────

    /**
     * El vendedor inicia el onboarding de Stripe Connect.
     * Crea la cuenta Express si no existe, y devuelve la URL de onboarding.
     */
    @PostMapping("/connect/onboarding")
    public ResponseEntity<?> iniciarOnboarding() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User vendedor = userRepository.findByUsername(username).orElseThrow();

            if (vendedor.getRole() != Role.SELLER && vendedor.getRole() != Role.ADMIN)
                return ResponseEntity.status(403).body("Solo los vendedores pueden conectar su cuenta de Stripe");

            // Crear cuenta Express si aún no tiene una
            if (vendedor.getStripeAccountId() == null || vendedor.getStripeAccountId().isBlank()) {
                String accountId = stripeService.crearCuentaConnect(vendedor.getEmail());
                vendedor.setStripeAccountId(accountId);
                userRepository.save(vendedor);
            }

            String url = stripeService.generarLinkOnboarding(vendedor.getStripeAccountId());
            return ResponseEntity.ok(Map.of("url", url));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Consulta el estado de la cuenta de Stripe Connect del vendedor autenticado.
     * El frontend puede llamar este endpoint al volver del onboarding de Stripe.
     */
    @GetMapping("/connect/estado")
    public ResponseEntity<?> estadoConnect() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User vendedor = userRepository.findByUsername(username).orElseThrow();

            boolean tieneCuenta = vendedor.getStripeAccountId() != null
                    && !vendedor.getStripeAccountId().isBlank();

            boolean habilitada = tieneCuenta
                    && stripeService.cuentaHabilitada(vendedor.getStripeAccountId());

            return ResponseEntity.ok(Map.of(
                "stripeAccountId", tieneCuenta ? vendedor.getStripeAccountId() : "",
                "conectado",       tieneCuenta,
                "habilitado",      habilitada
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
