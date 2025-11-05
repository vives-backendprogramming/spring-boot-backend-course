package be.vives.pizzastore.controller;

import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.request.UpdatePizzaRequest;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.exception.ResourceNotFoundException;
import be.vives.pizzastore.service.PizzaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Spring MVC slice test for PizzaController (WITH Spring context).
 * 
 * Uses @WebMvcTest to load only Spring MVC components.
 * Uses @MockBean to mock the service layer (managed by Spring).
 * 
 * @MockBean is NOT deprecated in Spring Boot 3 - it's the standard way to mock beans in Spring tests.
 * 
 * This is DIFFERENT from @Mock which is used for pure unit tests without Spring.
 * - @MockBean = Spring-managed mock (for @WebMvcTest, @SpringBootTest)
 * - @Mock = Mockito mock (for pure unit tests with @ExtendWith(MockitoExtension.class))
 */
@WebMvcTest(PizzaController.class)  // Loads only Spring MVC components
class PizzaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean  // Spring-managed mock (not @Mock!)
    private PizzaService pizzaService;

    @Test
    void getPizzas_NoPizzas_ReturnsEmptyList() throws Exception {
        // Given
        when(pizzaService.findAll()).thenReturn(List.of());

        // When / Then
        mockMvc.perform(get("/api/pizzas"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(pizzaService).findAll();
    }

    @Test
    void getPizzas_MultiplePizzas_ReturnsList() throws Exception {
        // Given
        List<PizzaResponse> pizzas = Arrays.asList(
                new PizzaResponse(1L, "Margherita", new BigDecimal("8.50"), "Classic"),
                new PizzaResponse(2L, "Marinara", new BigDecimal("7.50"), "Simple")
        );
        when(pizzaService.findAll()).thenReturn(pizzas);

        // When / Then
        mockMvc.perform(get("/api/pizzas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Margherita")))
                .andExpect(jsonPath("$[0].price", is(8.50)))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].name", is("Marinara")));

        verify(pizzaService).findAll();
    }

    @Test
    void getPizza_ExistingId_ReturnsPizza() throws Exception {
        // Given
        PizzaResponse pizza = new PizzaResponse(1L, "Margherita", new BigDecimal("8.50"), "Classic");
        when(pizzaService.findById(1L)).thenReturn(Optional.of(pizza));

        // When / Then
        mockMvc.perform(get("/api/pizzas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Margherita")))
                .andExpect(jsonPath("$.price", is(8.50)))
                .andExpect(jsonPath("$.description", is("Classic")));

        verify(pizzaService).findById(1L);
    }

    @Test
    void getPizza_NonExistingId_Returns404() throws Exception {
        // Given
        when(pizzaService.findById(999L)).thenReturn(Optional.empty());

        // When / Then
        mockMvc.perform(get("/api/pizzas/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("Pizza with id 999 not found")));

        verify(pizzaService).findById(999L);
    }

    @Test
    void createPizza_ValidRequest_Returns201() throws Exception {
        // Given
        CreatePizzaRequest request = new CreatePizzaRequest(
                "New Pizza",
                new BigDecimal("12.00"),
                "Delicious new pizza with amazing toppings"
        );

        PizzaResponse response = new PizzaResponse(1L, "New Pizza", new BigDecimal("12.00"), "Delicious new pizza with amazing toppings");
        when(pizzaService.create(any(CreatePizzaRequest.class))).thenReturn(response);

        // When / Then
        mockMvc.perform(post("/api/pizzas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", containsString("/api/pizzas/1")))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("New Pizza")));

        verify(pizzaService).create(any(CreatePizzaRequest.class));
    }

    @Test
    void createPizza_InvalidRequest_Returns400() throws Exception {
        // Given - invalid request (name too short, negative price, description too short)
        CreatePizzaRequest request = new CreatePizzaRequest(
                "A",
                new BigDecimal("-5.00"),
                "Short"
        );

        // When / Then
        mockMvc.perform(post("/api/pizzas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.validationErrors", hasSize(greaterThan(0))));

        verify(pizzaService, never()).create(any());
    }

    @Test
    void updatePizza_ValidRequest_Returns200() throws Exception {
        // Given
        UpdatePizzaRequest request = new UpdatePizzaRequest(
                "Updated Pizza",
                new BigDecimal("11.00"),
                "Updated description for this amazing pizza"
        );

        PizzaResponse response = new PizzaResponse(1L, "Updated Pizza", new BigDecimal("11.00"), "Updated description for this amazing pizza");
        when(pizzaService.update(eq(1L), any(UpdatePizzaRequest.class))).thenReturn(Optional.of(response));

        // When / Then
        mockMvc.perform(put("/api/pizzas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Updated Pizza")))
                .andExpect(jsonPath("$.price", is(11.00)));

        verify(pizzaService).update(eq(1L), any(UpdatePizzaRequest.class));
    }

    @Test
    void updatePizza_NonExistingId_Returns404() throws Exception {
        // Given
        UpdatePizzaRequest request = new UpdatePizzaRequest(
                "Updated Pizza",
                new BigDecimal("11.00"),
                "Updated description for this pizza"
        );

        when(pizzaService.update(eq(999L), any(UpdatePizzaRequest.class)))
                .thenThrow(new ResourceNotFoundException("Pizza", 999L));

        // When / Then
        mockMvc.perform(put("/api/pizzas/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(pizzaService).update(eq(999L), any(UpdatePizzaRequest.class));
    }

    @Test
    void deletePizza_ExistingId_Returns204() throws Exception {
        // Given
        when(pizzaService.delete(1L)).thenReturn(true);

        // When / Then
        mockMvc.perform(delete("/api/pizzas/1"))
                .andExpect(status().isNoContent());

        verify(pizzaService).delete(1L);
    }

    @Test
    void deletePizza_NonExistingId_Returns404() throws Exception {
        // Given
        when(pizzaService.delete(999L)).thenReturn(false);

        // When / Then
        mockMvc.perform(delete("/api/pizzas/999"))
                .andExpect(status().isNotFound());

        verify(pizzaService).delete(999L);
    }
}
