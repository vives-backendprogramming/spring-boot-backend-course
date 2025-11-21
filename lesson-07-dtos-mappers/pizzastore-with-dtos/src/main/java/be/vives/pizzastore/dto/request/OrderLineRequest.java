package be.vives.pizzastore.dto.request;

public record OrderLineRequest(
        Long pizzaId,
        int quantity
) {
}
