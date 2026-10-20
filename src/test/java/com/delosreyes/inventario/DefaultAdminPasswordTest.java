package com.delosreyes.inventario;

import com.delosreyes.inventario.modules.users.entity.User;
import com.delosreyes.inventario.modules.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * En una instalación nueva (base vacía + todas las migraciones), el admin debe
 * poder entrar con la contraseña documentada en el README.
 */
@SpringBootTest
@Testcontainers
class DefaultAdminPasswordTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void elAdminDeUnaInstalacionNuevaEntraConLaContrasenaDelReadme() {
        User admin = userRepository.findByEmail("admin@delosreyes.com").orElseThrow();

        assertThat(passwordEncoder.matches("Admin2024!", admin.getPassword())).isTrue();
    }
}
