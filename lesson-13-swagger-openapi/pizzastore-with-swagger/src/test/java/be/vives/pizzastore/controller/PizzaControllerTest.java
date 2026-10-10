package be.vives.pizzastore.controller;

import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.request.ImportNutritionRequest;
import be.vives.pizzastore.dto.request.UpdatePizzaRequest;
import be.vives.pizzastore.dto.response.NutritionalInfoResponse;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.exception.BusinessException;
import be.vives.pizzastore.exception.ExternalServiceException;
import be.vives.pizzastore.exception.GlobalExceptionHandler;
import be.vives.pizzastore.exception.ResourceNotFoundException;
import be.vives.pizzastore.security.JwtUtil;
import be.vives.pizzastore.security.SecurityConfig;
import be.vives.pizzastore.service.NutritionImportService;
import be.vives.pizzastore.service.PizzaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Web-layer slice for PizzaController with RestTestClient. The real SecurityConfig is part of the slice,
 * so every test runs as a mock user: an admin by default, otherwise as the test says.
 */
@WebMvcTest(controllers = PizzaController.class)
@AutoConfigureRestTestClient
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@WithMockUser(roles = "ADMIN")
class PizzaControllerTest {

    @Autowired
    private RestTestClient client;

    @MockitoBean
    private PizzaService pizzaService;

    @MockitoBean
    private NutritionImportService nutritionImportService;

    // needed by SecurityConfig / JwtAuthenticationFilter, which are part of the slice
    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private static PizzaResponse pizza(long id, String name, String price, String description) {
        return new PizzaResponse(id, name, new BigDecimal(price), description, null, true, null);
    }

    private static CreatePizzaRequest newPizzaRequest() {
        return new CreatePizzaRequest("New Pizza", new BigDecimal("12.00"),
                "Delicious new pizza with amazing toppings", true, null);
    }

    private static UpdatePizzaRequest updateRequest(String name, String price) {
        return new UpdatePizzaRequest(name, new BigDecimal(price), "Updated description for this pizza", true, null);
    }

    // ---------- reading ----------

    @Test
    void getPizzas_NoPizzas_ReturnsEmptyPage() {
        when(pizzaService.findAll(any(Pageable.class))).thenReturn(Page.empty());

        client.get().uri("/api/pizzas")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(0)
                .jsonPath("$.totalElements").isEqualTo(0);

        verify(pizzaService).findAll(any(Pageable.class));
    }

    @Test
    void getPizzas_MultiplePizzas_ReturnsPage() {
        Page<PizzaResponse> page = new PageImpl<>(List.of(
                pizza(1, "Margherita", "8.50", "Classic"),
                pizza(2, "Marinara", "7.50", "Simple")));
        when(pizzaService.findAll(any(Pageable.class))).thenReturn(page);

        client.get().uri("/api/pizzas")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(2)
                .jsonPath("$.content[0].id").isEqualTo(1)
                .jsonPath("$.content[0].name").isEqualTo("Margherita")
                .jsonPath("$.content[0].price").isEqualTo(8.50)
                .jsonPath("$.content[1].id").isEqualTo(2)
                .jsonPath("$.content[1].name").isEqualTo("Marinara");

        verify(pizzaService).findAll(any(Pageable.class));
    }

    @Test
    void getPizza_ExistingId_ReturnsPizza() {
        when(pizzaService.findById(1L)).thenReturn(pizza(1, "Margherita", "8.50", "Classic"));

        client.get().uri("/api/pizzas/{id}", 1)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PizzaResponse.class)
                .value(result -> {
                    assertThat(result.id()).isEqualTo(1L);
                    assertThat(result.name()).isEqualTo("Margherita");
                    assertThat(result.price()).isEqualByComparingTo("8.50");
                    assertThat(result.description()).isEqualTo("Classic");
                });

        verify(pizzaService).findById(1L);
    }

    @Test
    void getPizza_NonExistingId_Returns404() {
        when(pizzaService.findById(999L)).thenThrow(new ResourceNotFoundException("Pizza", 999L));

        client.get().uri("/api/pizzas/{id}", 999)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.detail").isEqualTo("Pizza with id 999 not found")
                .jsonPath("$.instance").isEqualTo("/api/pizzas/999");

        verify(pizzaService).findById(999L);
    }

    // ---------- filtering ----------

    @Test
    void getPizzas_WithPriceRangeFilter_ReturnsFilteredList() {
        when(pizzaService.findByPriceBetween(new BigDecimal("8.00"), new BigDecimal("10.00"))).thenReturn(List.of(
                pizza(1, "Margherita", "8.50", "Classic"),
                pizza(2, "Marinara", "9.00", "Simple")));

        client.get().uri("/api/pizzas?minPrice=8.00&maxPrice=10.00")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].name").isEqualTo("Margherita")
                .jsonPath("$[1].name").isEqualTo("Marinara");

        verify(pizzaService).findByPriceBetween(new BigDecimal("8.00"), new BigDecimal("10.00"));
    }

    @Test
    void getPizzas_WithMaxPriceFilter_ReturnsFilteredList() {
        when(pizzaService.findByPriceLessThan(new BigDecimal("10.00")))
                .thenReturn(List.of(pizza(1, "Margherita", "8.50", "Classic")));

        client.get().uri("/api/pizzas?maxPrice=10.00")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].name").isEqualTo("Margherita");

        verify(pizzaService).findByPriceLessThan(new BigDecimal("10.00"));
    }

    @Test
    void getPizzas_WithNameFilter_ReturnsFilteredList() {
        when(pizzaService.findByNameContaining("mar")).thenReturn(List.of(
                pizza(1, "Margherita", "8.50", "Classic"),
                pizza(2, "Marinara", "7.50", "Simple")));

        client.get().uri("/api/pizzas?name=mar")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].name").isEqualTo("Margherita")
                .jsonPath("$[1].name").isEqualTo("Marinara");

        verify(pizzaService).findByNameContaining("mar");
    }

    // ---------- creating ----------

    @Test
    void createPizza_ValidRequest_Returns201() {
        when(pizzaService.create(any(CreatePizzaRequest.class)))
                .thenReturn(pizza(1, "New Pizza", "12.00", "Delicious new pizza with amazing toppings"));

        client.post().uri("/api/pizzas")
                .contentType(MediaType.APPLICATION_JSON)
                .body(newPizzaRequest())
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueMatches("Location", ".*/api/pizzas/1")
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("New Pizza");

        verify(pizzaService).create(any(CreatePizzaRequest.class));
    }

    @Test
    void createPizza_InvalidRequest_ReturnsBadRequest() {
        // blank name, negative price: @Valid rejects the request before it reaches the service
        CreatePizzaRequest request = new CreatePizzaRequest("", new BigDecimal("-5.00"), "Short", true, null);

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

        verify(pizzaService, never()).create(any(CreatePizzaRequest.class));
    }

    // ---------- updating ----------

    @Test
    void updatePizza_ValidRequest_Returns200() {
        when(pizzaService.update(eq(1L), any(UpdatePizzaRequest.class)))
                .thenReturn(pizza(1, "Updated Pizza", "11.00", "Updated description for this pizza"));

        client.put().uri("/api/pizzas/{id}", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateRequest("Updated Pizza", "11.00"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("Updated Pizza")
                .jsonPath("$.price").isEqualTo(11.00);

        verify(pizzaService).update(eq(1L), any(UpdatePizzaRequest.class));
    }

    @Test
    void updatePizza_MissingRequiredFields_ReturnsValidationProblemDetail() {
        // PUT replaces the whole pizza, so only sending the price is not enough
        client.put().uri("/api/pizzas/{id}", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"price\": 12.00}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Validation failed")
                .jsonPath("$.errors[*].field").value(fields ->
                        assertThat((List<Object>) fields).containsExactlyInAnyOrder("name", "available"));

        verify(pizzaService, never()).update(any(), any());
    }

    @Test
    void updatePizza_NonExistingId_Returns404() {
        when(pizzaService.update(eq(999L), any(UpdatePizzaRequest.class)))
                .thenThrow(new ResourceNotFoundException("Pizza", 999L));

        client.put().uri("/api/pizzas/{id}", 999)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateRequest("Updated Pizza", "11.00"))
                .exchange()
                .expectStatus().isNotFound();

        verify(pizzaService).update(eq(999L), any(UpdatePizzaRequest.class));
    }

    // ---------- deleting ----------

    @Test
    void deletePizza_ExistingId_Returns204() {
        doNothing().when(pizzaService).delete(1L);

        client.delete().uri("/api/pizzas/{id}", 1)
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        verify(pizzaService).delete(1L);
    }

    @Test
    void deletePizza_StillReferenced_Returns409() {
        // e.g. the pizza is still in a customer's favorites
        doThrow(new DataIntegrityViolationException("FK constraint violation")).when(pizzaService).delete(1L);

        // the SQL error is not leaked to the client
        client.delete().uri("/api/pizzas/{id}", 1)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.detail").value(detail -> assertThat((String) detail).doesNotContain("FK"));
    }

    @Test
    void deletePizza_NonExistingId_Returns404() {
        doThrow(new ResourceNotFoundException("Pizza", 999L)).when(pizzaService).delete(999L);

        client.delete().uri("/api/pizzas/{id}", 999)
                .exchange()
                .expectStatus().isNotFound();

        verify(pizzaService).delete(999L);
    }

    // ---------- security: the same rules as in production ----------

    @Test
    @WithAnonymousUser
    void getPizzas_Anonymous_ReturnsOk() {
        // GET /api/pizzas/** is permitAll()
        when(pizzaService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pizza(1, "Margherita", "8.50", "Classic"))));

        client.get().uri("/api/pizzas")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @WithAnonymousUser
    void createPizza_Anonymous_ReturnsUnauthorized() {
        // no user: 401 Unauthorized (the HttpStatusEntryPoint of SecurityConfig), not 403
        client.post().uri("/api/pizzas")
                .contentType(MediaType.APPLICATION_JSON)
                .body(newPizzaRequest())
                .exchange()
                .expectStatus().isUnauthorized();

        verify(pizzaService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createPizza_WithCustomerRole_ReturnsForbidden() {
        // customers cannot create pizzas, only admins can
        client.post().uri("/api/pizzas")
                .contentType(MediaType.APPLICATION_JSON)
                .body(newPizzaRequest())
                .exchange()
                .expectStatus().isForbidden();

        verify(pizzaService, never()).create(any());
    }

    // --- POST /api/pizzas/{id}/nutritional-info/import (NutritionImportService is a mock: Open Food Facts is never called) ---

    @Test
    void importNutritionalInfo_ValidBarcode_ReturnsUpdatedPizza() {
        PizzaResponse updated = new PizzaResponse(1L, "Margherita", new BigDecimal("8.50"), "Classic", null, true,
                new NutritionalInfoResponse(539, new BigDecimal("6.30"), new BigDecimal("57.50"), new BigDecimal("30.90")));
        when(nutritionImportService.importFromBarcode(1L, "3017620422003")).thenReturn(updated);

        client.post().uri("/api/pizzas/{id}/nutritional-info/import", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ImportNutritionRequest("3017620422003"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.nutritionalInfo.calories").isEqualTo(539)
                .jsonPath("$.nutritionalInfo.protein").isEqualTo(6.30);
    }

    @Test
    void importNutritionalInfo_InvalidBarcode_Returns400AndNeverCallsService() {
        client.post().uri("/api/pizzas/{id}/nutritional-info/import", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ImportNutritionRequest("abc"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors[0].field").isEqualTo("barcode");

        verifyNoInteractions(nutritionImportService);
    }

    @Test
    void importNutritionalInfo_UnknownPizza_Returns404() {
        when(nutritionImportService.importFromBarcode(999L, "3017620422003"))
                .thenThrow(new ResourceNotFoundException("Pizza", 999L));

        client.post().uri("/api/pizzas/{id}/nutritional-info/import", 999)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ImportNutritionRequest("3017620422003"))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void importNutritionalInfo_UnknownBarcode_Returns422() {
        when(nutritionImportService.importFromBarcode(1L, "0000000000017"))
                .thenThrow(new BusinessException("Open Food Facts does not know a product with barcode 0000000000017"));

        client.post().uri("/api/pizzas/{id}/nutritional-info/import", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ImportNutritionRequest("0000000000017"))
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Open Food Facts does not know a product with barcode 0000000000017");
    }

    @Test
    void importNutritionalInfo_OpenFoodFactsDown_Returns502WithoutLeakingDetails() {
        when(nutritionImportService.importFromBarcode(1L, "3017620422003"))
                .thenThrow(new ExternalServiceException("Open Food Facts is currently unavailable, please try again later",
                        new RuntimeException("Connection refused: secret-internal-host:443")));

        client.post().uri("/api/pizzas/{id}/nutritional-info/import", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ImportNutritionRequest("3017620422003"))
                .exchange()
                .expectStatus().isEqualTo(502)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody(String.class)
                .value(body -> assertThat(body)
                        .contains("Open Food Facts is currently unavailable")
                        .doesNotContain("secret-internal-host"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void importNutritionalInfo_WithCustomerRole_ReturnsForbidden() {
        // only admins edit the menu
        client.post().uri("/api/pizzas/{id}/nutritional-info/import", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ImportNutritionRequest("3017620422003"))
                .exchange()
                .expectStatus().isForbidden();

        verifyNoInteractions(nutritionImportService);
    }
}
