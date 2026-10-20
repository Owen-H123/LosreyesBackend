package com.delosreyes.inventario.modules.movements.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validación del cuerpo de POST /inventory/entry y /inventory/exit.
 *
 * El frontend no envía "type" porque la URL ya indica si es entrada o salida,
 * y el controller lo asigna después de validar. La validación no debe exigirlo.
 */
class MovementCreateRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void sinTipoPasaLaValidacion() {
        MovementDtos.CreateRequest request = new MovementDtos.CreateRequest();
        request.setProductId(1L);
        request.setQuantity(3);

        Set<ConstraintViolation<MovementDtos.CreateRequest>> errores = validator.validate(request);

        assertThat(errores).isEmpty();
    }

    @Test
    void sinProductoNiCantidadFalla() {
        Set<ConstraintViolation<MovementDtos.CreateRequest>> errores =
            validator.validate(new MovementDtos.CreateRequest());

        assertThat(errores).extracting(ConstraintViolation::getMessage)
            .containsExactlyInAnyOrder("El producto es obligatorio", "La cantidad es obligatoria");
    }
}
