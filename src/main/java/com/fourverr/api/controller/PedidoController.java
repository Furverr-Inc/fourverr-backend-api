package com.fourverr.api.controller;

import com.fourverr.api.model.Ilustracion;
import com.fourverr.api.model.Pedido;
import com.fourverr.api.model.Usuario;
import com.fourverr.api.repository.IlustracionRepository;
import com.fourverr.api.repository.PedidoRepository;
import com.fourverr.api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private IlustracionRepository ilustracionRepository;

    // POST: Crear un nuevo pedido (Comprar)
    @PostMapping
    public ResponseEntity<?> crearPedido(@RequestBody Map<String, Object> datos) {
        // Obtenemos los IDs del JSON
        String nombreCliente = (String) datos.get("nombreCliente");
        Long idIlustracion = Long.valueOf(datos.get("idIlustracion").toString());
        String requisitos = (String) datos.get("requisitos");

        // 1. Validar Cliente
        Optional<Usuario> clienteOpt = usuarioRepository.findByNombreUsuario(nombreCliente);
        if (clienteOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("El cliente no existe");
        }

        // 2. Validar Ilustración
        Optional<Ilustracion> ilustracionOpt = ilustracionRepository.findById(idIlustracion);
        if (ilustracionOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("La ilustración no existe");
        }

        // 3. Crear el Pedido
        Pedido pedido = new Pedido();
        pedido.setCliente(clienteOpt.get());
        pedido.setIlustracion(ilustracionOpt.get());
        pedido.setRequisitosCliente(requisitos);
        pedido.setEstado("PENDIENTE"); // Estado inicial por defecto

        Pedido pedidoGuardado = pedidoRepository.save(pedido);
        return ResponseEntity.ok(pedidoGuardado);
    }

    // GET: Ver mis compras (como Cliente)
    @GetMapping("/mis-compras/{nombreUsuario}")
    public List<Pedido> verMisCompras(@PathVariable String nombreUsuario) {
        return pedidoRepository.findByCliente_NombreUsuario(nombreUsuario);
    }

    // GET: Ver mis ventas (como Ilustrador)
    @GetMapping("/mis-ventas/{nombreIlustrador}")
    public List<Pedido> verMisVentas(@PathVariable String nombreIlustrador) {
        return pedidoRepository.findByIlustracion_Ilustrador_NombreUsuario(nombreIlustrador);
    }
}