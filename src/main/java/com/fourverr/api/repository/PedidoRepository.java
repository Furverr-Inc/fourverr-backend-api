package com.fourverr.api.repository;

import com.fourverr.api.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByCliente_Username(String username);
    List<Pedido> findByProducto_Vendedor_Username(String username);

    @Transactional
    void deleteByProducto_Id(Long productoId);

    @Transactional
    void deleteByCliente_Id(Long clienteId);
}
