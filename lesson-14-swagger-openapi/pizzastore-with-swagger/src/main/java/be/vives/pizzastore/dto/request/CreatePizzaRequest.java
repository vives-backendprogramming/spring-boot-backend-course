package be.vives.pizzastore.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Request object for creating a new pizza")
public record CreatePizzaRequest(

        @NotBlank(message = "Pizza name is required")
        @Schema(description = "Name of the pizza", example = "Margherita", required = true)
        String name,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be positive")
        @Schema(description = "Price of the pizza in EUR", example = "12.50", required = true)
        BigDecimal price,

        @Schema(description = "Description of the pizza", example = "Classic pizza with tomato sauce, mozzarella, and fresh basil")
        String description,

        @Schema(description = "Whether the pizza is available for ordering", example = "true", defaultValue = "true")
        Boolean available,

        @Schema(description = "Nutritional information for the pizza")
        NutritionalInfoRequest nutritionalInfo
) {
}
