package com.fourverr.api.repository;

import com.fourverr.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // El método antiguo (solo buscaba username)
    Optional<User> findByUsername(String username);

    // 🔥 EL NUEVO MÉTODO (Opción Pro)
    // Busca si coincide con el username O si coincide con el email
    Optional<User> findByUsernameOrEmail(String username, String email);
}