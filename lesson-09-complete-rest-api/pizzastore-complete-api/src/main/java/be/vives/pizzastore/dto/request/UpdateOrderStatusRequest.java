package be.vives.pizzastore.dto.request;

import be.vives.pizzastore.domain.OrderStatus;

public record UpdateOrderStatusRequest(
        OrderStatus status
) {
}
