package be.vives.pizzastore.dto.request;

import java.util.List;

public record CreateOrderRequest(
        Long userId,
        List<OrderLineRequest> orderLines
) {
}
