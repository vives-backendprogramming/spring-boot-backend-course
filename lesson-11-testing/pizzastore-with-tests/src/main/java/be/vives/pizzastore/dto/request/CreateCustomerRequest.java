package be.vives.pizzastore.dto.request;

import jakarta.validation.constraints.*;

public record CreateCustomerRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^\\+32[0-9]{9}$", message = "Phone must be Belgian format (+32xxxxxxxxx)")
        String phone,

        @NotBlank(message = "Address is required")
        @Size(min = 10, max = 200, message = "Address must be between 10 and 200 characters")
        String address
) {
}
