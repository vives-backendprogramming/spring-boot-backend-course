package be.vives.pizzastore.dto.request;

public record UpdateCustomerRequest(
        String name,
        String email,
        String phone,
        String address
) {
}
