package be.vives.pizzastore.controller;

import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.exception.GlobalExceptionHandler;
import be.vives.pizzastore.exception.ResourceNotFoundException;
import be.vives.pizzastore.service.NutritionImportService;
import be.vives.pizzastore.service.PizzaService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * "Unit test" flavour of a controller test: NO Spring context at all.
 * <p>
 * {@code RestTestClient.bindToController(...)} wires the controller by hand into a minimal Spring MVC setup.
 * The service is a plain Mockito mock, the {@code @RestControllerAdvice} is added explicitly.
 * Starts in milliseconds, but nothing is auto-configured for you (no Spring Data page serialization,
 * no application.properties, no component scanning), so keep it for simple request/response mappings.
 */
class PizzaControllerStandaloneTest {

    private final PizzaService pizzaService = mock(PizzaService.class);
    private final NutritionImportService nutritionImportService = mock(NutritionImportService.class);

    private final RestTestClient client = RestTestClient
            .bindToController(new PizzaController(pizzaService, nutritionImportService))
            .configureServer(server -> server.setControllerAdvice(new GlobalExceptionHandler()))
            .build();

    @Test
    void getPizza_ExistingId_ReturnsPizza() {
        when(pizzaService.findById(1L)).thenReturn(
                new PizzaResponse(1L, "Margherita", new BigDecimal("8.50"), "Classic", null, true, null));

        client.get().uri("/api/pizzas/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PizzaResponse.class)
                .value(pizza -> assertThat(pizza.name()).isEqualTo("Margherita"));
    }

    @Test
    void getPizza_NonExistingId_ReturnsProblemDetail() {
        when(pizzaService.findById(999L)).thenThrow(new ResourceNotFoundException("Pizza", 999L));

        client.get().uri("/api/pizzas/999")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Pizza with id 999 not found");
    }

    @Test
    void getPizza_NonNumericId_Returns400() {
        client.get().uri("/api/pizzas/abc")
                .exchange()
                .expectStatus().isBadRequest();
    }
}
