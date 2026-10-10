package be.vives.pizzastore.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// PUT replaces the whole pizza, so the required fields are the same as for CreatePizzaRequest
public record UpdatePizzaRequest(

        @NotBlank(message = "Pizza name is required")
        String name,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be positive")
        BigDecimal price,

        String description,

        @NotNull(message = "Availability is required")
        Boolean available,

        @Valid
        NutritionalInfoRequest nutritionalInfo
) {
}
