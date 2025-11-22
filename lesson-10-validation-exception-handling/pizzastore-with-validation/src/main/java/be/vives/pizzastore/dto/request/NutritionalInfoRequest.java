package be.vives.pizzastore.dto.request;

import java.math.BigDecimal;

public record NutritionalInfoRequest(
        Integer calories,
        BigDecimal protein,
        BigDecimal carbohydrates,
        BigDecimal fat
) {
}
