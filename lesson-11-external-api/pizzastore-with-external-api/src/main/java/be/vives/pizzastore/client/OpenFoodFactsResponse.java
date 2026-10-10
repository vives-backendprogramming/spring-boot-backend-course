package be.vives.pizzastore.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * The part of the Open Food Facts response we care about. Everything else in the JSON is ignored.
 * <p>
 * Open Food Facts answers {@code status = 0} (sometimes with HTTP 200!) when the barcode is unknown,
 * so {@code product} can be {@code null}.
 */
public record OpenFoodFactsResponse(
        String code,
        Integer status,
        Product product
) {

    public record Product(
            @JsonProperty("product_name") String productName,
            Nutriments nutriments
    ) {
    }

    /** Nutrition values per 100 g, as published by Open Food Facts. Any of them can be missing. */
    public record Nutriments(
            @JsonProperty("energy-kcal_100g") BigDecimal energyKcal100g,
            @JsonProperty("proteins_100g") BigDecimal proteins100g,
            @JsonProperty("carbohydrates_100g") BigDecimal carbohydrates100g,
            @JsonProperty("fat_100g") BigDecimal fat100g
    ) {
    }
}
