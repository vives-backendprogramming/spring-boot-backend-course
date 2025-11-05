package be.vives.pizzastore.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PizzaResponse(
        Long id,
        String name,
        BigDecimal price,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
