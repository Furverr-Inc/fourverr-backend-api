package com.fourverr.api.repository;

import com.fourverr.api.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    
    // Buscar todos los productos de un vendedor específico
    List<Producto> findByVendedor_NombreUsuario(String nombreUsuario);
    
    // Buscar por tipo (Ej: "Dame todos los cursos")
    List<Producto> findByTipo(String tipo);
}