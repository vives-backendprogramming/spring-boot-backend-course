package be.vives.pizzastore.dto.response;

public record UserSummaryResponse(
        Long id,
        String name,
        String email
) {
}
