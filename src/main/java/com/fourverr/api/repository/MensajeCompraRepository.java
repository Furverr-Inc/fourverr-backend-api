package com.fourverr.api.repository;

import com.fourverr.api.model.MensajeCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensajeCompraRepository extends JpaRepository<MensajeCompra, Long> {

    List<MensajeCompra> findByPedido_IdOrderByFechaEnvioAsc(Long pedidoId);

    void deleteByPedido_Id(Long pedidoId);

    void deleteByRemitente_Id(Long remitenteId);
}
