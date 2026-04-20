package com.fourverr.api.controller;

import com.fourverr.api.model.Producto;
import com.fourverr.api.model.Role;
import com.fourverr.api.model.User;
import com.fourverr.api.repository.ProductoRepository;
import com.fourverr.api.repository.UserRepository;
import com.fourverr.api.service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
public class StripeController {

    private static final Logger log = LoggerFactory.getLogger(StripeController.class);

    /** Stripe no procesa cargos MXN por debajo de este total (documentación de montos mínimos). */
    private static final BigDecimal STRIPE_MIN_TOTAL_MXN = new BigDecimal("10.00");
    private static final BigDecimal STRIPE_MIN_TOTAL_USD = new BigDecimal("0.50");

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

            String cur = currency != null ? currency.trim().toLowerCase() : "mxn";
            if ("mxn".equals(cur) && precioTotal.compareTo(STRIPE_MIN_TOTAL_MXN) < 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error",
                        "El total debe ser de al menos 10 MXN para pagar con tarjeta (límite de Stripe). Aumenta la cantidad o elige otro producto."));
            }
            if ("usd".equals(cur) && precioTotal.compareTo(STRIPE_MIN_TOTAL_USD) < 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error",
                        "El total debe ser de al menos 0.50 USD para pagar con tarjeta."));
            }

            PaymentIntent intent;
            boolean usaConnect = false;

            // Si el vendedor ya tiene cuenta de Connect habilitada, usar destination charge
            User vendedor = producto.getVendedor();
            if (vendedor != null
                    && vendedor.getStripeAccountId() != null
                    && !vendedor.getStripeAccountId().isBlank()
                    && stripeService.cuentaHabilitada(vendedor.getStripeAccountId())) {

                intent = stripeService.crearPaymentIntentConnect(
                        precioTotal, cur, vendedor.getStripeAccountId());
                usaConnect = true;
            } else {
                intent = stripeService.crearPaymentIntent(precioTotal, cur);
            }

            return ResponseEntity.ok(Map.of(
                "clientSecret",    intent.getClientSecret(),
                "paymentIntentId", intent.getId(),
                "monto",           precioTotal,
                "usaConnect",      usaConnect
            ));

        } catch (StripeException e) {
            log.warn("Stripe crear-payment-intent: {}", e.getMessage());
            String msg = e.getUserMessage() != null && !e.getUserMessage().isBlank()
                    ? e.getUserMessage()
                    : e.getMessage();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", msg));
        } catch (Exception e) {
            log.error("crear-payment-intent", e);
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
