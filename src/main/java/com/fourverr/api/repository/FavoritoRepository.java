package com.fourverr.api.repository;

import com.fourverr.api.model.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FavoritoRepository extends JpaRepository<Favorito, Long> {
    List<Favorito> findByUsuario_Username(String username);
    Optional<Favorito> findByUsuario_UsernameAndProducto_Id(String username, Long productoId);
    boolean existsByUsuario_UsernameAndProducto_Id(String username, Long productoId);
}
