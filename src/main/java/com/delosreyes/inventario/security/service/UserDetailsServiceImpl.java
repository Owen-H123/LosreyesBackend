package com.delosreyes.inventario.security.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delosreyes.inventario.modules.users.repository.UserRepository;

/**
 * UserDetailsServiceImpl — Implementación requerida por Spring Security.
 *
 * QUÉ HACE:
 *   Carga el usuario desde la base de datos dado su email (username en el sistema).
 *   Spring Security llama a este servicio durante autenticación.
 *
 * POR QUÉ EXISTE:
 *   Spring Security necesita saber cómo cargar usuarios del sistema específico.
 *   Esta clase conecta Spring Security con el UserRepository de la aplicación.
 *
 * FLUJO:
 *   JwtAuthenticationFilter → UserDetailsService.loadUserByUsername(email)
 *   → UserRepository.findByEmail → retorna User (que implementa UserDetails)
 */
@Service
@Transactional(readOnly = true)
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException(
                "Usuario no encontrado con email: " + email));
    }
}