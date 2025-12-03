package be.vives.pizzastore.controller;

import be.vives.pizzastore.domain.OrderStatus;
import be.vives.pizzastore.dto.request.CreateCustomerRequest;
import be.vives.pizzastore.dto.request.UpdateCustomerRequest;
import be.vives.pizzastore.dto.response.CustomerResponse;
import be.vives.pizzastore.dto.response.OrderResponse;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.exception.GlobalExceptionHandler;
import be.vives.pizzastore.security.JwtUtil;
import be.vives.pizzastore.security.SecurityConfig;
import be.vives.pizzastore.service.CustomerService;
import be.vives.pizzastore.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CustomerController.class)
@Import({SecurityConfig.class,GlobalExceptionHandler.class})
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllCustomers_shouldReturnPageOfCustomers() throws Exception {
        // Arrange
        CustomerResponse customer1 = new CustomerResponse(1L, "John Doe", "john@example.com", "1234567890", "123 Main St", "CUSTOMER");
        CustomerResponse customer2 = new CustomerResponse(2L, "Jane Smith", "jane@example.com", "0987654321", "456 Oak Ave", "CUSTOMER");
        Page<CustomerResponse> page = new PageImpl<>(Arrays.asList(customer1, customer2));

        when(customerService.findAll(any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/customers")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name", is("John Doe")))
                .andExpect(jsonPath("$.content[0].email", is("john@example.com")))
                .andExpect(jsonPath("$.content[1].name", is("Jane Smith")));

        verify(customerService).findAll(any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getCustomer_whenExists_shouldReturnCustomer() throws Exception {
        // Arrange
        CustomerResponse customer = new CustomerResponse(1L, "John Doe", "john@example.com", "1234567890", "123 Main St", "CUSTOMER");

        when(customerService.findById(1L)).thenReturn(customer);

        // Act & Assert
        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("John Doe")))
                .andExpect(jsonPath("$.email", is("john@example.com")))
                .andExpect(jsonPath("$.address", is("123 Main St")))
                .andExpect(jsonPath("$.phone", is("1234567890")));

        verify(customerService).findById(1L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getCustomerOrders_shouldReturnPageOfOrders() throws Exception {
        // Arrange
        OrderResponse order1 = new OrderResponse(1L, "ORD-2024-000001", 1L, "John Doe", 
                List.of(), BigDecimal.valueOf(25.50), OrderStatus.PENDING, LocalDateTime.now());
        OrderResponse order2 = new OrderResponse(2L, "ORD-2024-000002", 1L, "John Doe", 
                List.of(), BigDecimal.valueOf(30.00), OrderStatus.DELIVERED, LocalDateTime.now());
        Page<OrderResponse> page = new PageImpl<>(Arrays.asList(order1, order2));

        when(orderService.findByCustomerId(eq(1L), any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/customers/1/orders")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].orderNumber", is("ORD-2024-000001")))
                .andExpect(jsonPath("$.content[1].orderNumber", is("ORD-2024-000002")));

        verify(orderService).findByCustomerId(eq(1L), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getFavoritePizzas_shouldReturnListOfPizzas() throws Exception {
        // Arrange
        PizzaResponse pizza1 = new PizzaResponse(1L, "Margherita", BigDecimal.valueOf(8.50), "Classic pizza", null, true, null);
        PizzaResponse pizza2 = new PizzaResponse(2L, "Pepperoni", BigDecimal.valueOf(10.00), "Spicy pizza", null, true, null);
        List<PizzaResponse> favorites = Arrays.asList(pizza1, pizza2);

        when(customerService.findFavoritePizzas(1L)).thenReturn(favorites);

        // Act & Assert
        mockMvc.perform(get("/api/customers/1/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Margherita")))
                .andExpect(jsonPath("$[0].price", is(8.50)))
                .andExpect(jsonPath("$[1].name", is("Pepperoni")))
                .andExpect(jsonPath("$[1].price", is(10.00)));

        verify(customerService).findFavoritePizzas(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCustomer_shouldReturnCreatedCustomer() throws Exception {
        // Arrange
        CreateCustomerRequest request = new CreateCustomerRequest(
                "John Doe", "john@example.com", "password123", "1234567890", "123 Main St");
        CustomerResponse response = new CustomerResponse(1L, "John Doe", "john@example.com", "1234567890", "123 Main St", "CUSTOMER");

        when(customerService.create(any(CreateCustomerRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/customers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/customers/1")))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("John Doe")))
                .andExpect(jsonPath("$.email", is("john@example.com")));

        verify(customerService).create(any(CreateCustomerRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCustomer_shouldReturnUpdatedCustomer() throws Exception {
        // Arrange
        UpdateCustomerRequest request = new UpdateCustomerRequest(
                "John Doe Updated", "john@example.com", "9998887777", "456 New St");
        CustomerResponse response = new CustomerResponse(1L, "John Doe Updated", "john@example.com", "9998887777", "456 New St", "CUSTOMER");

        when(customerService.update(eq(1L), any(UpdateCustomerRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/customers/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("John Doe Updated")))
                .andExpect(jsonPath("$.address", is("456 New St")))
                .andExpect(jsonPath("$.phone", is("9998887777")));

        verify(customerService).update(eq(1L), any(UpdateCustomerRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCustomer_shouldReturnNoContent() throws Exception {
        // Arrange
        doNothing().when(customerService).delete(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/customers/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(customerService).delete(1L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void addFavoritePizza_shouldReturnOk() throws Exception {
        // Arrange
        doNothing().when(customerService).addFavoritePizza(1L, 2L);

        // Act & Assert
        mockMvc.perform(post("/api/customers/1/favorites/2")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(customerService).addFavoritePizza(1L, 2L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void removeFavoritePizza_shouldReturnNoContent() throws Exception {
        // Arrange
        doNothing().when(customerService).removeFavoritePizza(1L, 2L);

        // Act & Assert
        mockMvc.perform(delete("/api/customers/1/favorites/2")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(customerService).removeFavoritePizza(1L, 2L);
    }
}
