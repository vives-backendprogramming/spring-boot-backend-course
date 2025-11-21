package be.vives.pizzastore.dto.response;

import java.math.BigDecimal;

public record OrderLineResponse(
        Long id,
        PizzaSummaryResponse pizza,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
