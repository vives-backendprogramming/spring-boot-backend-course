package be.vives.pizzastore.controller;

import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.exception.GlobalExceptionHandler;
import be.vives.pizzastore.exception.ResourceNotFoundException;
import be.vives.pizzastore.service.NutritionImportService;
import be.vives.pizzastore.service.PizzaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The same web-layer slice as {@link PizzaControllerTest}, but written with Spring Boot 4's
 * {@link RestTestClient} instead of MockMvc.
 * <p>
 * {@code @WebMvcTest} still starts only the web layer (controllers, JSON, {@code @RestControllerAdvice}).
 * {@code @AutoConfigureRestTestClient} adds a RestTestClient that is bound to that mock MVC environment:
 * no real server, no network, but the fluent API you will also use against a running server.
 */
@WebMvcTest(PizzaController.class)
@AutoConfigureRestTestClient
@Import(GlobalExceptionHandler.class)
class PizzaControllerRestTestClientTest {

    @Autowired
    private RestTestClient client;

    @MockitoBean
    private PizzaService pizzaService;

    @MockitoBean
    private NutritionImportService nutritionImportService;

    private final PizzaResponse margherita =
            new PizzaResponse(1L, "Margherita", new BigDecimal("8.50"), "Classic", null, true, null);

    @Test
    void getPizza_ExistingId_ReturnsPizza() {
        when(pizzaService.findById(1L)).thenReturn(margherita);

        client.get().uri("/api/pizzas/{id}", 1)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(PizzaResponse.class)
                .value(pizza -> {
                    assertThat(pizza.id()).isEqualTo(1L);
                    assertThat(pizza.name()).isEqualTo("Margherita");
                    assertThat(pizza.price()).isEqualByComparingTo("8.50");
                });
    }

    @Test
    void getPizza_NonExistingId_ReturnsProblemDetail() {
        when(pizzaService.findById(999L)).thenThrow(new ResourceNotFoundException("Pizza", 999L));

        client.get().uri("/api/pizzas/{id}", 999)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.detail").isEqualTo("Pizza with id 999 not found")
                .jsonPath("$.instance").isEqualTo("/api/pizzas/999");
    }

    @Test
    void getPizzas_ReturnsPage() {
        when(pizzaService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(margherita)));

        client.get().uri("/api/pizzas")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(1)
                .jsonPath("$.content[0].name").isEqualTo("Margherita")
                .jsonPath("$.totalElements").isEqualTo(1);
    }

    @Test
    void createPizza_ValidRequest_Returns201WithLocation() {
        CreatePizzaRequest request = new CreatePizzaRequest("Margherita", new BigDecimal("8.50"), "Classic", true, null);
        when(pizzaService.create(any(CreatePizzaRequest.class))).thenReturn(margherita);

        client.post().uri("/api/pizzas")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueMatches("Location", ".*/api/pizzas/1")
                .expectBody()
                .jsonPath("$.id").isEqualTo(1);
    }

    @Test
    void createPizza_InvalidRequest_Returns400WithFieldErrors() {
        // blank name + negative price: @Valid rejects the request before the service is ever called
        CreatePizzaRequest request = new CreatePizzaRequest("", new BigDecimal("-1"), "Broken", true, null);

        client.post().uri("/api/pizzas")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Validation failed")
                .jsonPath("$.errors[?(@.field == 'name')].message").isEqualTo("Pizza name is required")
                .jsonPath("$.errors[?(@.field == 'price')].message").isEqualTo("Price must be positive");

        verify(pizzaService, never()).create(any());
    }

    @Test
    void deletePizza_ExistingId_Returns204() {
        client.delete().uri("/api/pizzas/{id}", 1)
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        verify(pizzaService).delete(1L);
    }
}
