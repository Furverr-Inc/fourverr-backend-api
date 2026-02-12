package com.fourverr.api.security;

import com.fourverr.api.model.User;
import com.fourverr.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired 
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // 🔥 AQUI ESTA EL CAMBIO:
        // Pasamos la variable 'usernameOrEmail' DOS VECES.
        // Significa: "Búscalo en la columna username... y si no está, búscalo en la columna email"
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario o correo no encontrado: " + usernameOrEmail));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), // Usamos el username real de la BD para la sesión
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }
}