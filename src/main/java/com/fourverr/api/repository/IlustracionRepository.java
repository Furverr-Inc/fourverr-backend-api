package com.fourverr.api.repository;

import com.fourverr.api.model.Ilustracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IlustracionRepository extends JpaRepository<Ilustracion, Long> {
    
    // Buscar todas las obras de un ilustrador específico
    // SQL automático: SELECT * FROM ilustraciones WHERE ilustrador_id = ?
    List<Ilustracion> findByIlustrador_NombreUsuario(String nombreUsuario);
}