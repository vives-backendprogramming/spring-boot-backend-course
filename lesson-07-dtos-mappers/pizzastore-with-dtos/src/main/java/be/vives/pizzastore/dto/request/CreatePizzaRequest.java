package be.vives.pizzastore.dto.request;

import java.math.BigDecimal;

public record CreatePizzaRequest(
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        boolean available
) {
}
