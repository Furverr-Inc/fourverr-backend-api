package com.fourverr.api.controller;

import com.fourverr.api.model.Pedido;
import com.fourverr.api.model.Producto;
import com.fourverr.api.model.Usuario;
import com.fourverr.api.repository.PedidoRepository;
import com.fourverr.api.repository.ProductoRepository;
import com.fourverr.api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "http://localhost:5173")
public class PedidoController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @PostMapping
    public ResponseEntity<?> crearPedido(@RequestBody Map<String, Object> datos) {
        try {
            String nombreCliente = (String) datos.get("nombreCliente");
            // Ahora recibimos idProducto
            Long idProducto = Long.valueOf(datos.get("idProducto").toString());
            String requisitos = (String) datos.get("requisitos");

            Optional<Usuario> clienteOpt = usuarioRepository.findByNombreUsuario(nombreCliente);
            if (clienteOpt.isEmpty()) return ResponseEntity.badRequest().body("Cliente no existe");

            Optional<Producto> productoOpt = productoRepository.findById(idProducto);
            if (productoOpt.isEmpty()) return ResponseEntity.badRequest().body("Producto no existe");

            Pedido pedido = new Pedido();
            pedido.setCliente(clienteOpt.get());
            pedido.setProducto(productoOpt.get());
            pedido.setRequisitosCliente(requisitos);
            pedido.setEstado("PENDIENTE");

            return ResponseEntity.ok(pedidoRepository.save(pedido));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/mis-compras/{nombreUsuario}")
    public List<Pedido> verMisCompras(@PathVariable String nombreUsuario) {
        return pedidoRepository.findByCliente_NombreUsuario(nombreUsuario);
    }

    @GetMapping("/mis-ventas/{nombreVendedor}")
    public List<Pedido> verMisVentas(@PathVariable String nombreVendedor) {
        return pedidoRepository.findByProducto_Vendedor_NombreUsuario(nombreVendedor);
    }
}