package be.vives.pizzastore.controller;

import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.exception.GlobalExceptionHandler;
import be.vives.pizzastore.exception.InvalidFileException;
import be.vives.pizzastore.exception.ResourceNotFoundException;
import be.vives.pizzastore.security.JwtUtil;
import be.vives.pizzastore.security.SecurityConfig;
import be.vives.pizzastore.service.NutritionImportService;
import be.vives.pizzastore.service.PizzaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The one controller test that still uses MockMvc, on purpose: a RestTestClient that is bound to MockMvc
 * (what @AutoConfigureRestTestClient gives you in a @WebMvcTest) does not turn a multipart body into
 * request parts, so the controller answers "Required part 'image' is not present". MockMvc builds the
 * multipart request itself ({@code multipart(...).file(...)}) and has no such limit.
 * Against a real server RestTestClient does send multipart, see BeanOverrideIntegrationTest.
 */
@WebMvcTest(controllers = PizzaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@WithMockUser(roles = "ADMIN")
class PizzaImageUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PizzaService pizzaService;

    @MockitoBean
    private NutritionImportService nutritionImportService;   // second dependency of PizzaController

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private static MockMultipartFile image(String filename, String contentType) {
        return new MockMultipartFile("image", filename, contentType, "test image content".getBytes());
    }

    @Test
    void uploadPizzaImage_ExistingPizza_ReturnsUpdatedPizza() throws Exception {
        PizzaResponse response = new PizzaResponse(1L, "Margherita", new BigDecimal("8.50"),
                "Classic", "/uploads/pizza-1.jpg", true, null);
        when(pizzaService.uploadImage(eq(1L), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/pizzas/1/image").file(image("test.jpg", MediaType.IMAGE_JPEG_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.imageUrl", is("/uploads/pizza-1.jpg")));

        verify(pizzaService).uploadImage(eq(1L), any());
    }

    @Test
    void uploadPizzaImage_NonExistingPizza_Returns404() throws Exception {
        when(pizzaService.uploadImage(eq(999L), any())).thenThrow(new ResourceNotFoundException("Pizza", 999L));

        mockMvc.perform(multipart("/api/pizzas/999/image").file(image("test.jpg", MediaType.IMAGE_JPEG_VALUE)))
                .andExpect(status().isNotFound());

        verify(pizzaService).uploadImage(eq(999L), any());
    }

    @Test
    void uploadPizzaImage_InvalidFile_Returns400() throws Exception {
        when(pizzaService.uploadImage(eq(1L), any()))
                .thenThrow(new InvalidFileException("Only JPG, JPEG, and PNG files are allowed"));

        mockMvc.perform(multipart("/api/pizzas/1/image").file(image("test.txt", MediaType.TEXT_PLAIN_VALUE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", is("Only JPG, JPEG, and PNG files are allowed")));

        verify(pizzaService).uploadImage(eq(1L), any());
    }

    @Test
    void uploadPizzaImage_StorageError_Returns500() throws Exception {
        when(pizzaService.uploadImage(eq(1L), any())).thenThrow(new RuntimeException("Storage error"));

        mockMvc.perform(multipart("/api/pizzas/1/image").file(image("test.jpg", MediaType.IMAGE_JPEG_VALUE)))
                .andExpect(status().isInternalServerError());

        verify(pizzaService).uploadImage(eq(1L), any());
    }
}
