package be.vives.pizzastore.dto.request;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record NutritionalInfoRequest(
        @PositiveOrZero(message = "Calories cannot be negative")
        Integer calories,

        @PositiveOrZero(message = "Protein cannot be negative")
        BigDecimal protein,

        @PositiveOrZero(message = "Carbohydrates cannot be negative")
        BigDecimal carbohydrates,

        @PositiveOrZero(message = "Fat cannot be negative")
        BigDecimal fat
) {
}
