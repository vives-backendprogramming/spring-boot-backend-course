package be.vives.pizzastore.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotNull(message = "Customer ID is required")
        Long customerId,

        @NotEmpty(message = "Order must contain at least one pizza")
        @Size(min = 1, max = 20, message = "Order can contain between 1 and 20 pizzas")
        List<OrderLineRequest> orderLines
) {
    public record OrderLineRequest(
            Long pizzaId,
            Integer quantity
    ) {
    }
}
