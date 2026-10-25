package be.vives.pizzastore.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

// springdoc does not translate @PositiveOrZero into the schema, so "minimum" is documented explicitly
@Schema(description = "Nutritional information per 100 g; every value is optional and cannot be negative")
public record NutritionalInfoRequest(
        @PositiveOrZero(message = "Calories cannot be negative")
        @Schema(description = "Energy in kcal", example = "250", minimum = "0")
        Integer calories,

        @PositiveOrZero(message = "Protein cannot be negative")
        @Schema(description = "Protein in grams", example = "11.5", minimum = "0")
        BigDecimal protein,

        @PositiveOrZero(message = "Carbohydrates cannot be negative")
        @Schema(description = "Carbohydrates in grams", example = "30.2", minimum = "0")
        BigDecimal carbohydrates,

        @PositiveOrZero(message = "Fat cannot be negative")
        @Schema(description = "Fat in grams", example = "9.8", minimum = "0")
        BigDecimal fat
) {
}
