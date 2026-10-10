package be.vives.pizzastore.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the Jakarta Bean Validation annotations on the request records WITHOUT Spring, a controller or MockMvc.
 * A {@link Validator} can be built by hand, so every constraint is tested in microseconds.
 * <p>
 * {@code @Nested} groups the tests per request type; {@code @ParameterizedTest} runs one test body
 * for many inputs.
 */
class RequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeFactory() {
        factory.close();
    }

    /** Helper: the "path: message" of every violation, e.g. "price: Price must be positive". */
    private static <T> List<String> violationsOf(T request) {
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        return violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .sorted()
                .toList();
    }

    @Nested
    class CreatePizzaRequestTests {

        private CreatePizzaRequest pizza(String name, String price, Boolean available) {
            return new CreatePizzaRequest(name, price == null ? null : new BigDecimal(price), "desc", available, null);
        }

        @Test
        void validRequest_HasNoViolations() {
            assertThat(violationsOf(pizza("Margherita", "8.50", true))).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   "})
        void blankName_IsRejected(String name) {
            assertThat(violationsOf(pizza(name, "8.50", true))).containsExactly("name: Pizza name is required");
        }

        @Test
        void nullName_IsRejected() {
            assertThat(violationsOf(pizza(null, "8.50", true))).containsExactly("name: Pizza name is required");
        }

        @ParameterizedTest
        @CsvSource({
                "0.00,  false",
                "0.01,  true",
                "-5.00, false",
                "99.99, true"
        })
        void price_MustBeAtLeastOneCent(String price, boolean valid) {
            assertThat(violationsOf(pizza("Margherita", price, true)).isEmpty()).isEqualTo(valid);
        }

        @Test
        void missingPriceAndAvailability_AreBothReported() {
            assertThat(violationsOf(pizza("Margherita", null, null)))
                    .containsExactly("available: Availability is required", "price: Price is required");
        }

        @Test
        void invalidNutritionalInfo_IsValidatedBecauseOfValid() {
            // @Valid on the nested record makes the validator "descend" into it
            CreatePizzaRequest request = new CreatePizzaRequest("Margherita", new BigDecimal("8.50"), "desc", true,
                    new NutritionalInfoRequest(-1, null, null, null));

            assertThat(violationsOf(request)).containsExactly("nutritionalInfo.calories: Calories cannot be negative");
        }
    }

    @Nested
    class ImportNutritionRequestTests {

        @ParameterizedTest
        @ValueSource(strings = {"96385074", "012345678905", "3017620422003", "10012345678902"})   // EAN-8, UPC-A, EAN-13, GTIN-14
        void validBarcode_HasNoViolations(String barcode) {
            assertThat(violationsOf(new ImportNutritionRequest(barcode))).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"abc", "3017-6204-22003", "1234567", "123456789", "123456789012345", "30176204 22003"})
        void malformedBarcode_IsRejected(String barcode) {
            assertThat(violationsOf(new ImportNutritionRequest(barcode)))
                    .containsExactly("barcode: Barcode must be an EAN-8, UPC-A, EAN-13 or GTIN-14 number (digits only)");
        }

        @Test
        void nullBarcode_IsRejected() {
            assertThat(violationsOf(new ImportNutritionRequest(null))).containsExactly("barcode: Barcode is required");
        }
    }

    @Nested
    class CreateCustomerRequestTests {

        private CreateCustomerRequest customer(String name, String email, String password) {
            return new CreateCustomerRequest(name, email, password, null, null);
        }

        @Test
        void validRequest_HasNoViolations() {
            assertThat(violationsOf(customer("Emma Johnson", "emma@example.com", "password123"))).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"not-an-email", "missing-at.example.com", "@example.com"})
        void invalidEmail_IsRejected(String email) {
            assertThat(violationsOf(customer("Emma Johnson", email, "password123")))
                    .containsExactly("email: Email must be valid");
        }

        @Test
        void shortPassword_IsRejected() {
            assertThat(violationsOf(customer("Emma Johnson", "emma@example.com", "short")))
                    .containsExactly("password: Password must be at least 8 characters");
        }

        @Test
        void tooShortName_IsRejected() {
            assertThat(violationsOf(customer("E", "emma@example.com", "password123")))
                    .containsExactly("name: Name must be between 2 and 100 characters");
        }
    }

    @Nested
    class CreateOrderRequestTests {

        @Test
        void validRequest_HasNoViolations() {
            CreateOrderRequest request = new CreateOrderRequest(1L,
                    List.of(new CreateOrderRequest.OrderLineRequest(1L, 2)));

            assertThat(violationsOf(request)).isEmpty();
        }

        @Test
        void emptyOrderLines_AreRejected() {
            assertThat(violationsOf(new CreateOrderRequest(1L, List.of())))
                    .containsExactly("orderLines: Order must contain at least one pizza");
        }

        @Test
        void quantityZero_IsRejectedInsideTheOrderLine() {
            CreateOrderRequest request = new CreateOrderRequest(1L,
                    List.of(new CreateOrderRequest.OrderLineRequest(1L, 0)));

            // the property path tells exactly which element of the list is wrong
            assertThat(violationsOf(request)).containsExactly("orderLines[0].quantity: Quantity must be at least 1");
        }

        @Test
        void moreThanTwentyOrderLines_AreRejected() {
            List<CreateOrderRequest.OrderLineRequest> lines = java.util.stream.IntStream.rangeClosed(1, 21)
                    .mapToObj(i -> new CreateOrderRequest.OrderLineRequest((long) i, 1))
                    .toList();

            assertThat(violationsOf(new CreateOrderRequest(1L, lines)))
                    .containsExactly("orderLines: Order can contain at most 20 order lines");
        }
    }
}
