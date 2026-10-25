package be.vives.pizzastore.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// no customerId: the order is always for the logged-in customer, whom OrderController takes from the JWT
public record CreateOrderRequest(
        @NotEmpty(message = "Order must contain at least one pizza")
        @Size(max = 20, message = "Order can contain at most 20 order lines")
        @Valid
        List<OrderLineRequest> orderLines
) {
    public record OrderLineRequest(
            @NotNull(message = "Pizza ID is required")
            Long pizzaId,

            @NotNull(message = "Quantity is required")
            @Min(value = 1, message = "Quantity must be at least 1")
            Integer quantity
    ) {
    }
}
