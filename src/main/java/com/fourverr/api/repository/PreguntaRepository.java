package com.fourverr.api.repository;

import com.fourverr.api.model.Pregunta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface PreguntaRepository extends JpaRepository<Pregunta, Long> {
    List<Pregunta> findByProducto_IdOrderByFechaPreguntaAsc(Long productoId);

    @Transactional
    void deleteByProducto_Id(Long productoId);

    @Transactional
    void deleteByUsuario_Id(Long usuarioId);
}
