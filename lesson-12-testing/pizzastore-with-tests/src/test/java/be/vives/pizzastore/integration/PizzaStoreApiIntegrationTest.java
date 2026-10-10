package be.vives.pizzastore.integration;

import be.vives.pizzastore.domain.OrderStatus;
import be.vives.pizzastore.dto.request.CreateCustomerRequest;
import be.vives.pizzastore.dto.request.CreateOrderRequest;
import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.request.UpdateOrderStatusRequest;
import be.vives.pizzastore.dto.response.CustomerResponse;
import be.vives.pizzastore.dto.response.OrderResponse;
import be.vives.pizzastore.dto.response.PizzaResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack test: the real application on a real (random) port, a real H2 database, real HTTP requests.
 * Controller, validation, exception handler, service, mapper and repository all run for real.
 * <p>
 * {@code @AutoConfigureRestTestClient} provides a {@link RestTestClient} that is automatically bound to
 * the random port. Because the server handles each request on its own thread, a test method cannot
 * {@code @Transactional}-rollback what the server did (unlike a MockMvc test, see {@link PizzaIntegrationTest}).
 * Instead every test creates its own uniquely named data and never assumes the database is empty.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:api-integration-test")
@AutoConfigureRestTestClient
class PizzaStoreApiIntegrationTest {

    @Autowired
    private RestTestClient client;

    // ---------- helpers: create data through the public API, like a real client would ----------

    private PizzaResponse createPizza(String price) {
        CreatePizzaRequest request = new CreatePizzaRequest(
                "Pizza " + UUID.randomUUID(), new BigDecimal(price), "Created by an integration test", true, null);
        return client.post().uri("/api/pizzas")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PizzaResponse.class)
                .returnResult().getResponseBody();
    }

    private CustomerResponse createCustomer(String email) {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Test Customer", email, "password123", "+32 470 00 00 00", "Teststraat 1, Kortrijk");
        return client.post().uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CustomerResponse.class)
                .returnResult().getResponseBody();
    }

    private String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }

    // ---------- pizzas ----------

    @Nested
    class Pizzas {

        @Test
        void createReadUpdateDelete_FullCrudFlow() {
            PizzaResponse created = createPizza("10.00");
            assertThat(created.id()).isNotNull();

            client.get().uri("/api/pizzas/{id}", created.id())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.name").isEqualTo(created.name())
                    .jsonPath("$.price").isEqualTo(10.0);

            client.put().uri("/api/pizzas/{id}", created.id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""
                            {"name": "Renamed pizza", "price": 11.50, "description": "Updated", "available": false}
                            """)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.name").isEqualTo("Renamed pizza")
                    .jsonPath("$.price").isEqualTo(11.5)
                    .jsonPath("$.available").isEqualTo(false);

            client.delete().uri("/api/pizzas/{id}", created.id())
                    .exchange()
                    .expectStatus().isNoContent();

            client.get().uri("/api/pizzas/{id}", created.id())
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        void create_Returns201WithLocationHeaderPointingToTheNewPizza() {
            CreatePizzaRequest request = new CreatePizzaRequest(
                    "Location " + UUID.randomUUID(), new BigDecimal("9.00"), "desc", true, null);

            client.post().uri("/api/pizzas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectHeader().valueMatches("Location", "http://localhost:\\d+/api/pizzas/\\d+");
        }

        @Test
        void list_ContainsCreatedPizzaAndSupportsMaxPriceFilter() {
            PizzaResponse cheap = createPizza("1.23");
            createPizza("98.76");

            client.get().uri("/api/pizzas?maxPrice=2.00")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$[?(@.id == %d)].name".formatted(cheap.id())).isEqualTo(cheap.name())
                    .jsonPath("$[?(@.price > 2.00)]").isEmpty();
        }

        @Test
        void list_IsPagedByDefault() {
            createPizza("5.00");

            client.get().uri("/api/pizzas?page=0&size=1")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.content.length()").isEqualTo(1)
                    .jsonPath("$.size").isEqualTo(1)
                    .jsonPath("$.totalElements").value(total -> assertThat((Integer) total).isGreaterThanOrEqualTo(1));
        }
    }

    // ---------- errors: the ProblemDetail contract, end to end ----------

    @Nested
    class Errors {

        @Test
        void unknownPizza_Returns404ProblemDetail() {
            client.get().uri("/api/pizzas/{id}", 987_654)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(404)
                    .jsonPath("$.detail").isEqualTo("Pizza with id 987654 not found")
                    .jsonPath("$.instance").isEqualTo("/api/pizzas/987654");
        }

        @Test
        void invalidBody_Returns400WithAllFieldErrors() {
            client.post().uri("/api/pizzas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""
                            {"name": "", "price": -5, "available": true}
                            """)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .expectBody()
                    .jsonPath("$.errors.length()").isEqualTo(2)
                    .jsonPath("$.errors[?(@.field == 'name')].message").isEqualTo("Pizza name is required")
                    .jsonPath("$.errors[?(@.field == 'price')].message").isEqualTo("Price must be positive");
        }

        @Test
        void malformedJson_Returns400() {
            client.post().uri("/api/pizzas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{ this is not json")
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo("Malformed JSON request");
        }

        @Test
        void unknownUrl_Returns404ProblemDetail() {
            // not thrown by our code but by Spring MVC itself: still a ProblemDetail thanks to ResponseEntityExceptionHandler
            client.get().uri("/api/does-not-exist")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
        }

        @Test
        void wrongHttpMethod_Returns405() {
            client.patch().uri("/api/pizzas/1")
                    .exchange()
                    .expectStatus().isEqualTo(405)
                    .expectHeader().exists("Allow");
        }

        @Test
        void duplicateCustomerEmail_Returns409() {
            String email = uniqueEmail();
            createCustomer(email);

            client.post().uri("/api/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CreateCustomerRequest("Other Name", email, "password123", null, null))
                    .exchange()
                    .expectStatus().isEqualTo(409)
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(409);
        }
    }

    // ---------- orders: a business scenario over several endpoints ----------

    @Nested
    class Orders {

        @Test
        void orderLifecycle_CreateUpdateStatusAndCancel() {
            PizzaResponse margherita = createPizza("8.50");
            PizzaResponse diavola = createPizza("12.00");
            CustomerResponse customer = createCustomer(uniqueEmail());

            // 1. place an order: 2 x 8.50 + 1 x 12.00 = 29.00
            OrderResponse order = client.post().uri("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CreateOrderRequest(customer.id(), List.of(
                            new CreateOrderRequest.OrderLineRequest(margherita.id(), 2),
                            new CreateOrderRequest.OrderLineRequest(diavola.id(), 1))))
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(OrderResponse.class)
                    .returnResult().getResponseBody();

            assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
            assertThat(order.totalAmount()).isEqualByComparingTo("29.00");
            assertThat(order.orderLines()).hasSize(2);
            assertThat(order.customerId()).isEqualTo(customer.id());

            // 2. the order is visible under the customer
            client.get().uri("/api/customers/{id}/orders", customer.id())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.content.length()").isEqualTo(1)
                    .jsonPath("$.content[0].id").isEqualTo(order.id().intValue());

            // 3. move it through the workflow
            client.patch().uri("/api/orders/{id}/status", order.id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new UpdateOrderStatusRequest(OrderStatus.PREPARING))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo("PREPARING");

            // 4. cancel it: 204; cancelling again violates a business rule: 422
            client.delete().uri("/api/orders/{id}", order.id())
                    .exchange()
                    .expectStatus().isNoContent();

            client.delete().uri("/api/orders/{id}", order.id())
                    .exchange()
                    .expectStatus().isEqualTo(422)
                    .expectBody()
                    .jsonPath("$.detail").isEqualTo("Order is already cancelled");
        }

        @Test
        void orderForUnknownPizza_Returns422AndCreatesNothing() {
            CustomerResponse customer = createCustomer(uniqueEmail());

            client.post().uri("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CreateOrderRequest(customer.id(),
                            List.of(new CreateOrderRequest.OrderLineRequest(987_654L, 1))))
                    .exchange()
                    .expectStatus().isEqualTo(422);

            client.get().uri("/api/customers/{id}/orders", customer.id())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.content").isEmpty();
        }

        @Test
        void invalidStatusValue_Returns400() {
            PizzaResponse pizza = createPizza("8.50");
            CustomerResponse customer = createCustomer(uniqueEmail());
            OrderResponse order = client.post().uri("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CreateOrderRequest(customer.id(),
                            List.of(new CreateOrderRequest.OrderLineRequest(pizza.id(), 1))))
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(OrderResponse.class)
                    .returnResult().getResponseBody();

            client.patch().uri("/api/orders/{id}/status", order.id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"status\": \"TELEPORTED\"}")
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }
}
