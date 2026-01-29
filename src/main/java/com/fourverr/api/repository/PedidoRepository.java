package com.fourverr.api.repository;

import com.fourverr.api.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByCliente_NombreUsuario(String nombreUsuario);

    // CAMBIO: Busca por Producto -> Vendedor -> NombreUsuario
    List<Pedido> findByProducto_Vendedor_NombreUsuario(String nombreVendedor);
}