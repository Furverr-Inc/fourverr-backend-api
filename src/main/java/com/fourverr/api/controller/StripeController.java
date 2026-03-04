package com.fourverr.api.controller;

import com.fourverr.api.model.Producto;
import com.fourverr.api.repository.ProductoRepository;
import com.fourverr.api.service.StripeService;
import com.stripe.model.PaymentIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
@CrossOrigin(origins = "http://localhost:5173")
public class StripeController {

    @Autowired private StripeService stripeService;
    @Autowired private ProductoRepository productoRepository;

    @PostMapping("/crear-payment-intent")
    public ResponseEntity<?> crearPaymentIntent(@RequestBody Map<String, Object> datos) {
        try {
            Long idProducto = Long.valueOf(datos.get("idProducto").toString());
            String currency = datos.getOrDefault("currency", "mxn").toString();

            // Buscamos el producto para obtener su precio real desde la BD
            // (nunca confíes en el precio que manda el frontend, puede manipularse)
            Producto producto = productoRepository.findById(idProducto)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            PaymentIntent intent = stripeService.crearPaymentIntent(
                producto.getPrecio(), currency
            );

            return ResponseEntity.ok(Map.of(
                "clientSecret", intent.getClientSecret(),
                "paymentIntentId", intent.getId(),
                "monto", producto.getPrecio()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }
}