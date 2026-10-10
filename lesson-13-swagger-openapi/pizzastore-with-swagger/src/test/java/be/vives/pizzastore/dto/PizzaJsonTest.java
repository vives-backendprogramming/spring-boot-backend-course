package be.vives.pizzastore.dto;

import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.response.NutritionalInfoResponse;
import be.vives.pizzastore.dto.response.PizzaResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @JsonTest} loads nothing but the JSON infrastructure (the Jackson 3 {@code JsonMapper} with the
 * application's {@code spring.jackson.*} settings) and provides {@link JacksonTester}s.
 * No controllers, no services, no database: just "what does this record look like as JSON, and back?".
 */
@JsonTest
class PizzaJsonTest {

    @Autowired
    private JacksonTester<PizzaResponse> responseJson;

    @Autowired
    private JacksonTester<CreatePizzaRequest> requestJson;

    @Test
    void serialize_PizzaResponse_ContainsAllFields() throws Exception {
        PizzaResponse pizza = new PizzaResponse(1L, "Margherita", new BigDecimal("8.50"), "Classic",
                "http://localhost:8080/images/margherita.png", true,
                new NutritionalInfoResponse(266, new BigDecimal("11.0"), new BigDecimal("33.0"), new BigDecimal("10.0")));

        assertThat(responseJson.write(pizza))
                .extractingJsonPathNumberValue("$.id").isEqualTo(1);
        assertThat(responseJson.write(pizza))
                .extractingJsonPathStringValue("$.name").isEqualTo("Margherita");
        assertThat(responseJson.write(pizza))
                .extractingJsonPathNumberValue("$.price").isEqualTo(8.5);
        assertThat(responseJson.write(pizza))
                .extractingJsonPathNumberValue("$.nutritionalInfo.calories").isEqualTo(266);
    }

    @Test
    void serialize_PizzaResponseWithoutNutritionalInfo_WritesNull() throws Exception {
        PizzaResponse pizza = new PizzaResponse(2L, "Marinara", new BigDecimal("7.50"), null, null, true, null);

        assertThat(responseJson.write(pizza)).hasJsonPathValue("$.name");
        assertThat(responseJson.write(pizza)).extractingJsonPathValue("$.nutritionalInfo").isNull();
    }

    @Test
    void serialize_UsesRecordComponentNamesAsJsonProperties() throws Exception {
        PizzaResponse pizza = new PizzaResponse(1L, "Margherita", new BigDecimal("8.50"), "Classic", null, true, null);

        // a record has no getX() methods, yet the JSON properties are plain "imageUrl", "available", ...
        assertThat(responseJson.write(pizza)).hasJsonPathBooleanValue("$.available");
        assertThat(responseJson.write(pizza)).hasJsonPath("$.imageUrl");
    }

    @Test
    void deserialize_CreatePizzaRequest_MapsJsonToRecord() throws Exception {
        String content = """
                {
                  "name": "Diavola",
                  "price": 12.99,
                  "description": "Spicy",
                  "available": true,
                  "nutritionalInfo": { "calories": 310, "protein": 14.0, "carbohydrates": 36.0, "fat": 13.0 }
                }
                """;

        CreatePizzaRequest request = requestJson.parseObject(content);

        assertThat(request.name()).isEqualTo("Diavola");
        assertThat(request.price()).isEqualByComparingTo("12.99");
        assertThat(request.available()).isTrue();
        assertThat(request.nutritionalInfo().calories()).isEqualTo(310);
    }

    @Test
    void deserialize_MissingOptionalFields_LeavesThemNull() throws Exception {
        CreatePizzaRequest request = requestJson.parseObject("""
                { "name": "Plain", "price": 5.00, "available": true }
                """);

        assertThat(request.description()).isNull();
        assertThat(request.nutritionalInfo()).isNull();
    }
}
