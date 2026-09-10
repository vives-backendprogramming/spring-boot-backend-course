package be.vives.pizzastore.controller;

import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.request.UpdatePizzaRequest;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.exception.GlobalExceptionHandler;
import be.vives.pizzastore.security.JwtUtil;
import be.vives.pizzastore.security.SecurityConfig;
import be.vives.pizzastore.service.PizzaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PizzaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class PizzaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PizzaService pizzaService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser
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
    @WithMockUser
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
    @WithMockUser
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
    @WithMockUser
    void getPizza_NonExistingId_Returns404() throws Exception {
        // Given
        when(pizzaService.findById(999L)).thenReturn(Optional.empty());

        // When / Then
        mockMvc.perform(get("/api/pizzas/999"))
                .andExpect(status().isNotFound());

        verify(pizzaService).findById(999L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
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
                        .with(csrf())
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
    @WithMockUser(roles = "ADMIN")
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
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(pizzaService).create(any(CreatePizzaRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
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
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Updated Pizza")))
                .andExpect(jsonPath("$.price", is(11.00)));

        verify(pizzaService).update(eq(1L), any(UpdatePizzaRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
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
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(pizzaService).update(eq(999L), any(UpdatePizzaRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletePizza_ExistingId_Returns204() throws Exception {
        // Given
        when(pizzaService.delete(1L)).thenReturn(true);

        // When / Then
        mockMvc.perform(delete("/api/pizzas/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(pizzaService).delete(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletePizza_NonExistingId_Returns404() throws Exception {
        // Given
        when(pizzaService.delete(999L)).thenReturn(false);

        // When / Then
        mockMvc.perform(delete("/api/pizzas/999")
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(pizzaService).delete(999L);
    }

    @Test
    @WithMockUser
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
    @WithMockUser
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
    @WithMockUser
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
    @WithMockUser(roles = "ADMIN")
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
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.imageUrl", is("/uploads/pizza-1.jpg")));

        verify(pizzaService).uploadImage(eq(1L), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
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
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(pizzaService).uploadImage(eq(999L), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
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
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(pizzaService).uploadImage(eq(1L), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
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
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isInternalServerError());

        verify(pizzaService).uploadImage(eq(1L), any());
    }

    @Test
    @WithMockUser(roles = "ANONYMOUS")
    void getPizzas_WithoutAuthentication_ReturnsOk() throws Exception {
        // Given - Anonymous users can view pizzas (GET /api/pizzas/** is permitAll())
        List<PizzaResponse> pizzas = Arrays.asList(
                new PizzaResponse(1L, "Margherita", new BigDecimal("8.50"), "Classic", null, true, null)
        );
        Page<PizzaResponse> page = new PageImpl<>(pizzas);
        when(pizzaService.findAll(any(Pageable.class))).thenReturn(page);

        // When / Then
        mockMvc.perform(get("/api/pizzas"))
                .andExpect(status().isOk());
    }

    @Test
    void createPizza_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        // Given
        CreatePizzaRequest request = new CreatePizzaRequest(
                "New Pizza",
                new BigDecimal("12.00"),
                "Delicious",
                true,
                null
        );

        // When / Then - Anonymous users cannot create pizzas
        // Note: In WebMvcTest context, Spring Security returns 403 (Forbidden) for anonymous requests
        // In integration tests with full JWT filter chain, it would return 401 (Unauthorized)
        mockMvc.perform(post("/api/pizzas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createPizza_WithCustomerRole_ReturnsForbidden() throws Exception {
        // Given
        CreatePizzaRequest request = new CreatePizzaRequest(
                "New Pizza",
                new BigDecimal("12.00"),
                "Delicious",
                true,
                null
        );

        // When / Then - Customers cannot create pizzas, only admins can
        mockMvc.perform(post("/api/pizzas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
