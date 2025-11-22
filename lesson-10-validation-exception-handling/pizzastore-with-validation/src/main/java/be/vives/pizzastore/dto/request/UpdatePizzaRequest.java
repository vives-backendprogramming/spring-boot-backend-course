package be.vives.pizzastore.dto.request;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record UpdatePizzaRequest(

        String name,

        @DecimalMin(value = "0.01", message = "Price must be positive")
        BigDecimal price,

        String description,

        Boolean available,

        NutritionalInfoRequest nutritionalInfo
) {
}
