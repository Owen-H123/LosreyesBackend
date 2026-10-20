package com.delosreyes.inventario.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.delosreyes.inventario.security.filter.JwtAuthenticationFilter;

/**
 * SecurityConfig — Configuración central de Spring Security.
 *
 * - Sesión STATELESS con JWT: no hay sesión en el servidor y CSRF está
 *   desactivado. JwtAuthenticationFilter carga el usuario en cada petición.
 * - Las rutas de requestMatchers son RELATIVAS al context-path: la API vive en
 *   /api/v1 (server.servlet.context-path), pero aquí /api/v1/auth/login se
 *   escribe /auth/login.
 * - /uploads/** es pública: el navegador carga las imágenes con <img src>, sin
 *   el header Authorization.
 * - Sin token o con token vencido responde 401 (el frontend renueva la sesión);
 *   el 403 queda para usuarios autenticados sin permiso.
 * - CORS: ver corsConfigurationSource().
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    private final List<String> corsAllowedOriginPatterns;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            UserDetailsService userDetailsService,
            @Value("${app.cors.allowed-origin-patterns}") String[] corsAllowedOriginPatterns) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
        this.corsAllowedOriginPatterns = List.of(corsAllowedOriginPatterns);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Sin token o con token vencido se responde 401 (no autenticado).
            // Así el frontend sabe que debe renovar la sesión con el refresh
            // token. El 403 queda solo para usuarios autenticados sin permiso.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth

                // Rutas relativas al context-path: POST /api/v1/auth/login se ve aquí como /auth/login
                .requestMatchers("/auth/**").permitAll()

                // Imágenes de productos y avatares, servidas como recurso estático
                .requestMatchers("/uploads/**").permitAll()

                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/api-docs/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()

                // Catálogo de la tienda online: solo lectura y sin login
                .requestMatchers(HttpMethod.GET, "/public/catalog/**").permitAll()
                // Pedido de la tienda online (el servidor valida productos, stock y precios)
                .requestMatchers(HttpMethod.POST, "/public/orders").permitAll()
                .requestMatchers(HttpMethod.GET, "/public/store-settings").permitAll()
                .requestMatchers(HttpMethod.GET, "/public/orders/*/payment").permitAll()
                .requestMatchers(HttpMethod.POST, "/public/orders/*/payment").permitAll()
                .requestMatchers("/web-orders/**", "/web-catalog/**").hasAnyRole("ADMIN", "WORKER")
                // Ajustes de la tienda: cualquiera del equipo los ve, solo el ADMIN los cambia
                .requestMatchers(HttpMethod.GET, "/store-settings").hasAnyRole("ADMIN", "WORKER")
                .requestMatchers(HttpMethod.PUT, "/store-settings").hasRole("ADMIN")

                // ROLES — también relativas al context-path
                .requestMatchers("/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/products/**")
                    .hasAnyAuthority("ROLE_ADMIN", "PERM_DELETE_PRODUCTS")
                .requestMatchers("/reports/**")
                    .hasAnyAuthority("ROLE_ADMIN", "PERM_VIEW_REPORTS")
                .requestMatchers("/inventory/adjustment")
                    .hasAnyAuthority("ROLE_ADMIN", "PERM_ADJUST_INVENTORY")
                .requestMatchers("/dashboard/**").hasAnyRole("ADMIN", "WORKER")

                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // AuthService inyecta el AuthenticationManager para validar el login.
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // Orígenes desde donde el navegador puede llamar a la API. Se configuran
    // en app.cors.allowed-origin-patterns (variable CORS_ALLOWED_ORIGINS).
    // Se usan patrones con comodín de host para que cualquier PC de la red
    // local que abra el frontend (ej. http://192.168.1.50:3000) pueda conectarse.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(corsAllowedOriginPatterns);

        config.setAllowedMethods(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
