package be.vives.pizzastore.dto.response;

import java.math.BigDecimal;

public record PizzaSummaryResponse(
        Long id,
        String name,
        BigDecimal price
) {
}
