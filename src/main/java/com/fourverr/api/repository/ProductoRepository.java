package com.fourverr.api.repository;

import com.fourverr.api.model.Producto;
import com.fourverr.api.model.TipoProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

       List<Producto> findByVendedor_Username(String username);
       List<Producto> findByVendedor_Id(Long vendedorId);
       List<Producto> findByTipo(TipoProducto tipo);
       List<Producto> findByActivoTrue();
       List<Producto> findByTipoAndActivoTrue(TipoProducto tipo);

       @Query("SELECT p FROM Producto p WHERE p.activo = true AND (LOWER(p.titulo) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(p.descripcion) LIKE LOWER(CONCAT('%',:q,'%')))")
       List<Producto> buscarPorTextoActivos(@Param("q") String q);

       @Query("SELECT p FROM Producto p WHERE p.activo = true AND p.tipo = :tipo AND (LOWER(p.titulo) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(p.descripcion) LIKE LOWER(CONCAT('%',:q,'%')))")
       List<Producto> buscarPorTextoYTipoActivos(@Param("q") String q, @Param("tipo") TipoProducto tipo);

       @Query("SELECT p FROM Producto p WHERE " +
           "LOWER(p.titulo) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(p.descripcion) LIKE LOWER(CONCAT('%',:q,'%'))")
       List<Producto> buscarPorTexto(@Param("q") String q);

       @Query("SELECT p FROM Producto p WHERE p.tipo = :tipo AND (" +
           "LOWER(p.titulo) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(p.descripcion) LIKE LOWER(CONCAT('%',:q,'%')))")
       List<Producto> buscarPorTextoYTipo(@Param("q") String q, @Param("tipo") TipoProducto tipo);
}
