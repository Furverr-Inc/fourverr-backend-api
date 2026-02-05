package com.fourverr.api.repository;

import com.fourverr.api.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    // CAMBIO: Buscamos por 'cliente.username' en lugar de 'nombreUsuario'
    List<Pedido> findByCliente_Username(String username);
    
    // CAMBIO: Buscamos por 'vendedor.username' dentro de Producto
    List<Pedido> findByProducto_Vendedor_Username(String username);
}