package be.vives.pizzastore.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreatePizzaRequest(

        @NotBlank(message = "Pizza name is required")
        String name,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be positive")
        BigDecimal price,

        String description,

        Boolean available,

        NutritionalInfoRequest nutritionalInfo
) {
}
