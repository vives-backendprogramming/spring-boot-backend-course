package be.vives.pizzastore.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// PUT replaces the customer's profile. The email is the login and cannot be changed:
// CustomerMapper.updateEntity ignores it, so it is only checked for a valid format.
public record UpdateCustomerRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @Email(message = "Email must be valid")
        String email,

        @Size(max = 20, message = "Phone must not exceed 20 characters")
        String phone,

        @Size(max = 200, message = "Address must not exceed 200 characters")
        String address
) {
}
