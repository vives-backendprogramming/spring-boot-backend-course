package be.vives.pizzastore.controller;

import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.request.UpdatePizzaRequest;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.service.PizzaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
@WebMvcTest(controllers = PizzaController.class)  // Loads only Spring MVC components
@Import(be.vives.pizzastore.exception.GlobalExceptionHandler.class)  // Import exception handler
class PizzaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean  // Spring-managed mock (not @Mock!)
    private PizzaService pizzaService;

    @Test
    void getPizzas_NoPizzas_ReturnsEmptyPage() throws Exception {
        // Given
        Page<PizzaResponse> emptyPage = Page.empty();
        when(pizzaService.findAll(any(Pageable.class))).thenReturn(emptyPage);

        // When / Then
        mockMvc.perform(get("/api/pizzas"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));

        verify(pizzaService).findAll(any(Pageable.class));
    }

    @Test
    void getPizzas_MultiplePizzas_ReturnsPage() throws Exception {
        // Given
        List<PizzaResponse> pizzas = Arrays.asList(
                new PizzaResponse(1L, "Margherita", new BigDecimal("8.50"), "Classic", null, true, null),
                new PizzaResponse(2L, "Marinara", new BigDecimal("7.50"), "Simple", null, true, null)
        );
        Page<PizzaResponse> page = new PageImpl<>(pizzas);
        when(pizzaService.findAll(any(Pageable.class))).thenReturn(page);

        // When / Then
        mockMvc.perform(get("/api/pizzas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("Margherita")))
                .andExpect(jsonPath("$.content[0].price", is(8.50)))
                .andExpect(jsonPath("$.content[1].id", is(2)))
                .andExpect(jsonPath("$.content[1].name", is("Marinara")));

        verify(pizzaService).findAll(any(Pageable.class));
    }

    @Test
    void getPizza_ExistingId_ReturnsPizza() throws Exception {
        // Given
        PizzaResponse pizza = new PizzaResponse(1L, "Margherita", new BigDecimal("8.50"), "Classic", null, true, null);
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
                .andExpect(status().isNotFound());

        verify(pizzaService).findById(999L);
    }

    @Test
    void createPizza_ValidRequest_Returns201() throws Exception {
        // Given
        CreatePizzaRequest request = new CreatePizzaRequest(
                "New Pizza",
                new BigDecimal("12.00"),
                "Delicious new pizza with amazing toppings",
                true,
                null
        );

        PizzaResponse response = new PizzaResponse(1L, "New Pizza", new BigDecimal("12.00"), 
                "Delicious new pizza with amazing toppings", null, true, null);
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
    void createPizza_InvalidRequest_StillCreates() throws Exception {
        // Given - invalid request (blank name, negative price)
        // Note: Validation is not enforced at controller level without @Valid annotation
        CreatePizzaRequest request = new CreatePizzaRequest(
                "",
                new BigDecimal("-5.00"),
                "Short",
                true,
                null
        );

        PizzaResponse response = new PizzaResponse(1L, "", new BigDecimal("-5.00"), "Short", null, true, null);
        when(pizzaService.create(any(CreatePizzaRequest.class))).thenReturn(response);

        // When / Then - without @Valid, invalid requests are still processed
        mockMvc.perform(post("/api/pizzas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(pizzaService).create(any(CreatePizzaRequest.class));
    }

    @Test
    void updatePizza_ValidRequest_Returns200() throws Exception {
        // Given
        UpdatePizzaRequest request = new UpdatePizzaRequest(
                "Updated Pizza",
                new BigDecimal("11.00"),
                "Updated description for this amazing pizza",
                true,
                null
        );

        PizzaResponse response = new PizzaResponse(1L, "Updated Pizza", new BigDecimal("11.00"), 
                "Updated description for this amazing pizza", null, true, null);
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
                "Updated description for this pizza",
                true,
                null
        );

        when(pizzaService.update(eq(999L), any(UpdatePizzaRequest.class)))
                .thenReturn(Optional.empty());

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

    @Test
    void getPizzas_WithPriceRangeFilter_ReturnsFilteredList() throws Exception {
        // Given
        List<PizzaResponse> pizzas = Arrays.asList(
                new PizzaResponse(1L, "Margherita", new BigDecimal("8.50"), "Classic", null, true, null),
                new PizzaResponse(2L, "Marinara", new BigDecimal("9.00"), "Simple", null, true, null)
        );
        when(pizzaService.findByPriceBetween(new BigDecimal("8.00"), new BigDecimal("10.00"))).thenReturn(pizzas);

        // When / Then
        mockMvc.perform(get("/api/pizzas")
                        .param("minPrice", "8.00")
                        .param("maxPrice", "10.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Margherita")))
                .andExpect(jsonPath("$[1].name", is("Marinara")));

        verify(pizzaService).findByPriceBetween(new BigDecimal("8.00"), new BigDecimal("10.00"));
    }

    @Test
    void getPizzas_WithMaxPriceFilter_ReturnsFilteredList() throws Exception {
        // Given
        List<PizzaResponse> pizzas = Arrays.asList(
                new PizzaResponse(1L, "Margherita", new BigDecimal("8.50"), "Classic", null, true, null)
        );
        when(pizzaService.findByPriceLessThan(new BigDecimal("10.00"))).thenReturn(pizzas);

        // When / Then
        mockMvc.perform(get("/api/pizzas")
                        .param("maxPrice", "10.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Margherita")));

        verify(pizzaService).findByPriceLessThan(new BigDecimal("10.00"));
    }

    @Test
    void getPizzas_WithNameFilter_ReturnsFilteredList() throws Exception {
        // Given
        List<PizzaResponse> pizzas = Arrays.asList(
                new PizzaResponse(1L, "Margherita", new BigDecimal("8.50"), "Classic", null, true, null),
                new PizzaResponse(2L, "Marinara", new BigDecimal("7.50"), "Simple", null, true, null)
        );
        when(pizzaService.findByNameContaining("mar")).thenReturn(pizzas);

        // When / Then
        mockMvc.perform(get("/api/pizzas")
                        .param("name", "mar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Margherita")))
                .andExpect(jsonPath("$[1].name", is("Marinara")));

        verify(pizzaService).findByNameContaining("mar");
    }

    @Test
    void uploadPizzaImage_ExistingPizza_ReturnsUpdatedPizza() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        PizzaResponse response = new PizzaResponse(1L, "Margherita", new BigDecimal("8.50"), 
                "Classic", "/uploads/pizza-1.jpg", true, null);
        when(pizzaService.uploadImage(eq(1L), any())).thenReturn(Optional.of(response));

        // When / Then
        mockMvc.perform(multipart("/api/pizzas/1/image")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.imageUrl", is("/uploads/pizza-1.jpg")));

        verify(pizzaService).uploadImage(eq(1L), any());
    }

    @Test
    void uploadPizzaImage_NonExistingPizza_Returns404() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        when(pizzaService.uploadImage(eq(999L), any())).thenReturn(Optional.empty());

        // When / Then
        mockMvc.perform(multipart("/api/pizzas/999/image")
                        .file(file))
                .andExpect(status().isNotFound());

        verify(pizzaService).uploadImage(eq(999L), any());
    }

    @Test
    void uploadPizzaImage_InvalidFile_Returns400() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "not an image".getBytes()
        );

        when(pizzaService.uploadImage(eq(1L), any()))
                .thenThrow(new IllegalArgumentException("Invalid file type"));

        // When / Then
        mockMvc.perform(multipart("/api/pizzas/1/image")
                        .file(file))
                .andExpect(status().isBadRequest());

        verify(pizzaService).uploadImage(eq(1L), any());
    }

    @Test
    void uploadPizzaImage_StorageError_Returns500() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        when(pizzaService.uploadImage(eq(1L), any()))
                .thenThrow(new RuntimeException("Storage error"));

        // When / Then
        mockMvc.perform(multipart("/api/pizzas/1/image")
                        .file(file))
                .andExpect(status().isInternalServerError());

        verify(pizzaService).uploadImage(eq(1L), any());
    }
}
