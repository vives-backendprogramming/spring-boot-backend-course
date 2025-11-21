package be.vives.pizzastore.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PizzaRequest(
        @NotBlank(message = "Name is required")
        String name,
        
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Price must be at least 0")
        @DecimalMax(value = "20.0", inclusive = true, message = "Price must not exceed 20")
        BigDecimal price
) {
}
