package be.vives.pizzastore.dto.request;

import java.math.BigDecimal;

public record UpdatePizzaRequest(
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        Boolean available
) {
}
