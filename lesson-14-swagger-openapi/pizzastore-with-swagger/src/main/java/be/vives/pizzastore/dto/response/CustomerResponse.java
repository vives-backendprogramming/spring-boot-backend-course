package be.vives.pizzastore.dto.response;

public record CustomerResponse(
        Long id,
        String name,
        String email,
        String phone,
        String address,
        String role
) {
}
