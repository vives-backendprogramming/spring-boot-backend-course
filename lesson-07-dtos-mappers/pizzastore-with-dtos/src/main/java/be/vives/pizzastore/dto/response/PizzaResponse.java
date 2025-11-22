package be.vives.pizzastore.dto.response;

import java.math.BigDecimal;

public record PizzaResponse(
        Long id,
        String name,
        BigDecimal price,
        String description,
        String imageUrl,
        Boolean available,
        NutritionalInfoResponse nutritionalInfo
) {
}
