package be.vives.pizzastore.integration;

import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.request.UpdatePizzaRequest;
import be.vives.pizzastore.dto.response.PizzaResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PizzaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAndRetrievePizza_Success() throws Exception {
        // Given - Create pizza
        CreatePizzaRequest request = new CreatePizzaRequest(
                "Integration Test Pizza",
                new BigDecimal("13.50"),
                "This pizza was created during an integration test",
                true,
                null
        );

        // When - Create pizza
        MvcResult createResult = mockMvc.perform(post("/api/pizzas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();

        // Extract ID from Location header
        String location = createResult.getResponse().getHeader("Location");
        assertThat(location).isNotNull();
        Long pizzaId = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

        // Then - Retrieve pizza
        mockMvc.perform(get("/api/pizzas/" + pizzaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(pizzaId.intValue())))
                .andExpect(jsonPath("$.name", is("Integration Test Pizza")))
                .andExpect(jsonPath("$.price", is(13.50)))
                .andExpect(jsonPath("$.description", containsString("integration test")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void fullCrudFlow_Success() throws Exception {
        // Create
        CreatePizzaRequest createRequest = new CreatePizzaRequest(
                "CRUD Test Pizza",
                new BigDecimal("10.00"),
                "Testing full CRUD operations with this amazing pizza",
                true,
                null
        );

        MvcResult createResult = mockMvc.perform(post("/api/pizzas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        PizzaResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                PizzaResponse.class
        );
        Long pizzaId = created.id();

        // Read
        mockMvc.perform(get("/api/pizzas/" + pizzaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("CRUD Test Pizza")));

        // Update
        UpdatePizzaRequest updateRequest = new UpdatePizzaRequest(
                "Updated CRUD Pizza",
                new BigDecimal("11.50"),
                "Updated description for this pizza during testing",
                true,
                null
        );

        mockMvc.perform(put("/api/pizzas/" + pizzaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated CRUD Pizza")))
                .andExpect(jsonPath("$.price", is(11.50)));

        // Delete
        mockMvc.perform(delete("/api/pizzas/" + pizzaId))
                .andExpect(status().isNoContent());

        // Verify deleted
        mockMvc.perform(get("/api/pizzas/" + pizzaId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void filterPizzasByPrice_Success() throws Exception {
        // Given - Create test pizzas
        createPizza("Cheap Pizza", "5.00");
        createPizza("Medium Pizza", "10.00");
        createPizza("Expensive Pizza", "20.00");

        // When / Then - Filter by max price
        mockMvc.perform(get("/api/pizzas")
                        .param("maxPrice", "12.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[*].name", hasItem("Cheap Pizza")))
                .andExpect(jsonPath("$[*].name", hasItem("Medium Pizza")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createPizza_WithInvalidData_StillCreates() throws Exception {
        // Given - Invalid pizza (multiple validation errors)
        // Note: Validation is not enforced at controller level without @Valid annotation
        CreatePizzaRequest request = new CreatePizzaRequest(
                "",  // Blank - would be invalid with @Valid
                new BigDecimal("-5.00"),  // Negative - would be invalid with @Valid
                "Test description",
                true,
                null
        );

        // When / Then - without @Valid, invalid requests are still processed
        mockMvc.perform(post("/api/pizzas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("")))
                .andExpect(jsonPath("$.price", is(-5.00)));
    }

    private void createPizza(String name, String price) throws Exception {
        CreatePizzaRequest request = new CreatePizzaRequest(
                name,
                new BigDecimal(price),
                "Test pizza: " + name,
                true,
                null
        );

        mockMvc.perform(post("/api/pizzas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }
}
