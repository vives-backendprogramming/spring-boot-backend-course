package be.vives.pizzastore.dto.request;

import java.math.BigDecimal;

public record UpdatePizzaRequest(
        String name,
        BigDecimal price,
        String description,
        Boolean available,
        NutritionalInfoRequest nutritionalInfo
) {
}
