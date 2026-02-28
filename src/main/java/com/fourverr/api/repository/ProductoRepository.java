package com.fourverr.api.repository;

import com.fourverr.api.model.Producto;
import com.fourverr.api.model.TipoProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    
    // CAMBIO: findByVendedor_Username (Inglés y Entidad User)
    List<Producto> findByVendedor_Username(String username);

    // Para eliminar productos por ID del vendedor
    List<Producto> findByVendedor_Id(Long vendedorId);
    
    // Búsqueda por Tipo
    List<Producto> findByTipo(TipoProducto tipo);
}