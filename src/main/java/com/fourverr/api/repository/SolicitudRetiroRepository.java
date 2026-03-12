package com.fourverr.api.repository;

import com.fourverr.api.model.SolicitudRetiro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SolicitudRetiroRepository extends JpaRepository<SolicitudRetiro, Long> {
    List<SolicitudRetiro> findByEstadoOrderByFechaSolicitudDesc(String estado);
    List<SolicitudRetiro> findAllByOrderByFechaSolicitudDesc();
    List<SolicitudRetiro> findByVendedor_UsernameOrderByFechaSolicitudDesc(String username);
}