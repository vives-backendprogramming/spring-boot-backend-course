package be.vives.pizzastore.controller;

import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.request.UpdatePizzaRequest;
import be.vives.pizzastore.dto.response.NutritionalInfoResponse;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.exception.BusinessException;
import be.vives.pizzastore.exception.ExternalServiceException;
import be.vives.pizzastore.exception.GlobalExceptionHandler;
import be.vives.pizzastore.exception.InvalidFileException;
import be.vives.pizzastore.exception.ResourceNotFoundException;
import be.vives.pizzastore.service.NutritionImportService;
import be.vives.pizzastore.service.PizzaService;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;


import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PizzaController.class)
@Import(GlobalExceptionHandler.class)
class PizzaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private PizzaService pizzaService;

    @MockitoBean
    private NutritionImportService nutritionImportService;
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
        when(pizzaService.findById(1L)).thenReturn(pizza);

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
        when(pizzaService.findById(999L)).thenThrow(new ResourceNotFoundException("Pizza", 999L));

        // When / Then
        mockMvc.perform(get("/api/pizzas/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.detail", is("Pizza with id 999 not found")))
                .andExpect(jsonPath("$.instance", is("/api/pizzas/999")));

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
    void createPizza_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Given - invalid request (blank name, negative price)
        CreatePizzaRequest request = new CreatePizzaRequest(
                "",
                new BigDecimal("-5.00"),
                "Short",
                true,
                null
        );

        // When / Then - @Valid rejects the request before it reaches the service
        mockMvc.perform(post("/api/pizzas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(pizzaService, never()).create(any(CreatePizzaRequest.class));
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
        when(pizzaService.update(eq(1L), any(UpdatePizzaRequest.class))).thenReturn(response);

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
    void updatePizza_MissingRequiredFields_ReturnsValidationProblemDetail() throws Exception {
        // Given - PUT replaces the whole pizza, so only sending the price is not enough
        String partialBody = "{\"price\": 12.00}";

        // When / Then
        mockMvc.perform(put("/api/pizzas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(partialBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", is("Validation failed")))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("name", "available")));

        verify(pizzaService, never()).update(any(), any());
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
        doNothing().when(pizzaService).delete(1L);

        // When / Then
        mockMvc.perform(delete("/api/pizzas/1"))
                .andExpect(status().isNoContent());

        verify(pizzaService).delete(1L);
    }

    @Test
    void deletePizza_StillReferenced_Returns409() throws Exception {
        // Given - e.g. the pizza is still in a customer's favorites
        doThrow(new DataIntegrityViolationException("FK constraint violation")).when(pizzaService).delete(1L);

        // When / Then - the SQL error is not leaked to the client
        mockMvc.perform(delete("/api/pizzas/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", not(containsString("FK"))));
    }

    @Test
    void deletePizza_NonExistingId_Returns404() throws Exception {
        // Given
        doThrow(new ResourceNotFoundException("Pizza", 999L)).when(pizzaService).delete(999L);

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
        when(pizzaService.uploadImage(eq(1L), any())).thenReturn(response);

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

        when(pizzaService.uploadImage(eq(999L), any())).thenThrow(new ResourceNotFoundException("Pizza", 999L));

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
                .thenThrow(new InvalidFileException("Only JPG, JPEG, and PNG files are allowed"));

        // When / Then
        mockMvc.perform(multipart("/api/pizzas/1/image")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", is("Only JPG, JPEG, and PNG files are allowed")));

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

    // --- POST /api/pizzas/{id}/nutritional-info/import (NutritionImportService is a mock: Open Food Facts is never called) ---

    @Test
    void importNutritionalInfo_ValidBarcode_ReturnsUpdatedPizza() throws Exception {
        PizzaResponse updated = new PizzaResponse(1L, "Margherita", new BigDecimal("8.50"), "Classic", null, true,
                new NutritionalInfoResponse(539, new BigDecimal("6.30"), new BigDecimal("57.50"), new BigDecimal("30.90")));
        when(nutritionImportService.importFromBarcode(1L, "3017620422003")).thenReturn(updated);

        mockMvc.perform(post("/api/pizzas/1/nutritional-info/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"barcode\":\"3017620422003\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nutritionalInfo.calories", is(539)))
                .andExpect(jsonPath("$.nutritionalInfo.protein", is(6.30)));
    }

    @Test
    void importNutritionalInfo_InvalidBarcode_Returns400AndNeverCallsService() throws Exception {
        mockMvc.perform(post("/api/pizzas/1/nutritional-info/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"barcode\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field", is("barcode")));

        verifyNoInteractions(nutritionImportService);
    }

    @Test
    void importNutritionalInfo_UnknownPizza_Returns404() throws Exception {
        when(nutritionImportService.importFromBarcode(999L, "3017620422003"))
                .thenThrow(new ResourceNotFoundException("Pizza", 999L));

        mockMvc.perform(post("/api/pizzas/999/nutritional-info/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"barcode\":\"3017620422003\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void importNutritionalInfo_UnknownBarcode_Returns422() throws Exception {
        when(nutritionImportService.importFromBarcode(1L, "0000000000017"))
                .thenThrow(new BusinessException("Open Food Facts does not know a product with barcode 0000000000017"));

        mockMvc.perform(post("/api/pizzas/1/nutritional-info/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"barcode\":\"0000000000017\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail", containsString("0000000000017")));
    }

    @Test
    void importNutritionalInfo_OpenFoodFactsDown_Returns502WithoutLeakingDetails() throws Exception {
        when(nutritionImportService.importFromBarcode(1L, "3017620422003"))
                .thenThrow(new ExternalServiceException("Open Food Facts is currently unavailable, please try again later",
                        new RuntimeException("Connection refused: secret-internal-host:443")));

        mockMvc.perform(post("/api/pizzas/1/nutritional-info/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"barcode\":\"3017620422003\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", is("Open Food Facts is currently unavailable, please try again later")))
                .andExpect(content().string(not(containsString("secret-internal-host"))));
    }
}
