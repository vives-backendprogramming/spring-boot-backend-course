package be.vives.pizzastore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ImportNutritionRequest(

        @NotBlank(message = "Barcode is required")
        @Pattern(regexp = "\\d{8}|\\d{12,14}", message = "Barcode must be an EAN-8, UPC-A, EAN-13 or GTIN-14 number (digits only)")
        String barcode
) {
}
