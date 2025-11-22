package be.vives.pizzastore.dto.request;

import java.util.List;

public record CreateOrderRequest(
        Long customerId,
        List<OrderLineRequest> orderLines
) {
    public record OrderLineRequest(
            Long pizzaId,
            Integer quantity
    ) {
    }
}
