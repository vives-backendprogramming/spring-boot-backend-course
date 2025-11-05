package be.vives.pizzastore.dto.request;

public record CreateCustomerRequest(
        String name,
        String email,
        String phone,
        String address
) {
}
