package be.vives.pizzastore.dto.response;

import java.math.BigDecimal;

public record OrderLineResponse(
        Long id,
        Long pizzaId,
        String pizzaName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
