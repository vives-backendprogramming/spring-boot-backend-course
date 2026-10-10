package be.vives.pizzastore.controller;

import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.exception.GlobalExceptionHandler;
import be.vives.pizzastore.exception.ResourceNotFoundException;
import be.vives.pizzastore.service.NutritionImportService;
import be.vives.pizzastore.service.PizzaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * The same slice once more, now with {@link MockMvcTester}: MockMvc with AssertJ assertions
 * instead of Hamcrest matchers. {@code @WebMvcTest} auto-configures it next to MockMvc.
 * <p>
 * Compare with {@link PizzaControllerTest}: no {@code throws Exception}, no static imports of
 * {@code status()}/{@code jsonPath()}, and everything reads like any other AssertJ assertion.
 */
@WebMvcTest(PizzaController.class)
@Import(GlobalExceptionHandler.class)
class PizzaControllerMockMvcTesterTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private PizzaService pizzaService;

    @MockitoBean
    private NutritionImportService nutritionImportService;

    @Test
    void getPizza_ExistingId_ReturnsPizza() {
        when(pizzaService.findById(1L)).thenReturn(
                new PizzaResponse(1L, "Margherita", new BigDecimal("8.50"), "Classic", null, true, null));

        assertThat(mvc.get().uri("/api/pizzas/{id}", 1))
                .hasStatusOk()
                .hasContentType(MediaType.APPLICATION_JSON)
                .bodyJson()
                .extractingPath("$.name").isEqualTo("Margherita");
    }

    @Test
    void getPizza_NonExistingId_ReturnsProblemDetail() {
        when(pizzaService.findById(999L)).thenThrow(new ResourceNotFoundException("Pizza", 999L));

        assertThat(mvc.get().uri("/api/pizzas/{id}", 999))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("Pizza with id 999 not found");
    }

    @Test
    void createPizza_BlankName_Returns400() {
        String json = """
                {"name": "", "price": 8.50, "description": "x", "available": true}
                """;

        assertThat(mvc.post().uri("/api/pizzas").contentType(MediaType.APPLICATION_JSON).content(json))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors[0].field").isEqualTo("name");
    }
}
