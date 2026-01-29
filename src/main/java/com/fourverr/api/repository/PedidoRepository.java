package com.fourverr.api.repository;

import com.fourverr.api.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // 1. Para que el Cliente vea sus compras:
    // SELECT * FROM pedidos WHERE cliente_id = (usuario con este nombre)
    List<Pedido> findByCliente_NombreUsuario(String nombreUsuario);

    // 2. Para que el Ilustrador vea sus ventas:
    // SELECT * FROM pedidos JOIN ilustraciones ... WHERE ilustrador = (usuario con este nombre)
    List<Pedido> findByIlustracion_Ilustrador_NombreUsuario(String nombreIlustrador);
}