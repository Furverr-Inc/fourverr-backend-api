package com.fourverr.api.repository;

import com.fourverr.api.model.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResenaRepository extends JpaRepository<Resena, Long> {

    List<Resena> findByProducto_IdOrderByFechaResenaDesc(Long productoId);

    Optional<Resena> findByPedido_Id(Long pedidoId);

    boolean existsByPedido_Id(Long pedidoId);

    @Query("SELECT AVG(r.calificacion) FROM Resena r WHERE r.producto.id = :productoId")
    Double promedioCalificacion(Long productoId);

    long countByProducto_Id(Long productoId);

    // ── Cascade delete ──
    @Transactional
    void deleteByProducto_Id(Long productoId);

    @Transactional
    void deleteByCliente_Id(Long clienteId);

    @Transactional
    void deleteByPedido_Id(Long pedidoId);
}
