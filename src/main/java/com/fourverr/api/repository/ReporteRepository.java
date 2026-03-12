package com.fourverr.api.repository;

import com.fourverr.api.model.Reporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReporteRepository extends JpaRepository<Reporte, Long> {
    List<Reporte> findAllByOrderByFechaReporteDesc();
    List<Reporte> findByVendedor_IdOrderByFechaReporteDesc(Long vendedorId);
    List<Reporte> findByEstadoOrderByFechaReporteDesc(Reporte.EstadoReporte estado);
    boolean existsByReportante_IdAndProducto_Id(Long reportanteId, Long productoId);
    void deleteByVendedor_Id(Long vendedorId);
    void deleteByReportante_Id(Long reportanteId);
    void deleteByProducto_Id(Long productoId);
}
