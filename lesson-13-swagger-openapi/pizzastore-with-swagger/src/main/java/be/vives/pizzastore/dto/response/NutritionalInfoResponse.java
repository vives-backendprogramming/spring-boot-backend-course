package be.vives.pizzastore.dto.response;

import java.math.BigDecimal;

public record NutritionalInfoResponse(
        Integer calories,
        BigDecimal protein,
        BigDecimal carbohydrates,
        BigDecimal fat
) {
}
