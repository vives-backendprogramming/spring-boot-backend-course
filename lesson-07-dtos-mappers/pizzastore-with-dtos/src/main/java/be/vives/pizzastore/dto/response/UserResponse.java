package be.vives.pizzastore.dto.response;

import be.vives.pizzastore.domain.Role;

public record UserResponse(
        Long id,
        String name,
        String email,
        String phone,
        String address,
        Role role
) {
}
