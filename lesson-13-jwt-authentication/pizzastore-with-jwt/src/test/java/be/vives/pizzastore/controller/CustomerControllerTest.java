package be.vives.pizzastore.controller;

import be.vives.pizzastore.domain.OrderStatus;
import be.vives.pizzastore.dto.request.CreateCustomerRequest;
import be.vives.pizzastore.dto.request.UpdateCustomerRequest;
import be.vives.pizzastore.dto.response.CustomerResponse;
import be.vives.pizzastore.dto.response.OrderResponse;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.exception.GlobalExceptionHandler;
import be.vives.pizzastore.exception.ResourceNotFoundException;
import be.vives.pizzastore.security.JwtUtil;
import be.vives.pizzastore.security.SecurityConfig;
import be.vives.pizzastore.service.CustomerService;
import be.vives.pizzastore.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Web-layer slice for CustomerController with RestTestClient and the real SecurityConfig.
 * /api/customers/** is open to CUSTOMER and ADMIN, so most tests use the role that the scenario is about.
 */
@WebMvcTest(controllers = CustomerController.class)
@AutoConfigureRestTestClient
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class CustomerControllerTest {

    @Autowired
    private RestTestClient client;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private static CustomerResponse john() {
        return new CustomerResponse(1L, "John Doe", "john@example.com", "1234567890", "123 Main St", "CUSTOMER");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllCustomers_shouldReturnPageOfCustomers() {
        CustomerResponse jane = new CustomerResponse(2L, "Jane Smith", "jane@example.com", "0987654321", "456 Oak Ave", "CUSTOMER");
        when(customerService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(john(), jane)));

        client.get().uri("/api/customers?page=0&size=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(2)
                .jsonPath("$.content[0].name").isEqualTo("John Doe")
                .jsonPath("$.content[0].email").isEqualTo("john@example.com")
                .jsonPath("$.content[1].name").isEqualTo("Jane Smith");

        verify(customerService).findAll(any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getCustomer_whenExists_shouldReturnCustomer() {
        when(customerService.findById(1L)).thenReturn(john());

        client.get().uri("/api/customers/{id}", 1)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("John Doe")
                .jsonPath("$.email").isEqualTo("john@example.com")
                .jsonPath("$.address").isEqualTo("123 Main St")
                .jsonPath("$.phone").isEqualTo("1234567890");

        verify(customerService).findById(1L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getCustomer_whenNotExists_shouldReturnProblemDetail() {
        when(customerService.findById(999L)).thenThrow(new ResourceNotFoundException("Customer", 999L));

        client.get().uri("/api/customers/{id}", 999)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.title").isEqualTo("Not Found")
                .jsonPath("$.detail").isEqualTo("Customer with id 999 not found")
                .jsonPath("$.type").isEqualTo("https://api.pizzastore.example.com/errors/not-found")
                .jsonPath("$.instance").isEqualTo("/api/customers/999");

        verify(customerService).findById(999L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCustomer_withInvalidEmail_shouldReturnValidationProblemDetail() {
        // every field is valid except email, so exactly one violation is produced
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Jane Doe", "not-an-email", "password123", "1234567890", "123 Main St");

        client.post().uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.title").isEqualTo("Bad Request")
                .jsonPath("$.detail").isEqualTo("Validation failed")
                .jsonPath("$.type").isEqualTo("https://api.pizzastore.example.com/errors/validation")
                .jsonPath("$.errors.length()").isEqualTo(1)
                .jsonPath("$.errors[0].field").isEqualTo("email");

        verify(customerService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getCustomer_whenUnexpectedErrorOccurs_shouldReturnProblemDetail() {
        when(customerService.findById(1L)).thenThrow(new IllegalStateException("boom"));

        client.get().uri("/api/customers/{id}", 1)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(500)
                .jsonPath("$.title").isEqualTo("Internal Server Error")
                .jsonPath("$.type").isEqualTo("https://api.pizzastore.example.com/errors/internal-error");

        verify(customerService).findById(1L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getCustomerOrders_shouldReturnPageOfOrders() {
        OrderResponse order1 = new OrderResponse(1L, "ORD-2024-000001", 1L, "John Doe",
                List.of(), BigDecimal.valueOf(25.50), OrderStatus.PENDING, LocalDateTime.now());
        OrderResponse order2 = new OrderResponse(2L, "ORD-2024-000002", 1L, "John Doe",
                List.of(), BigDecimal.valueOf(30.00), OrderStatus.DELIVERED, LocalDateTime.now());
        Page<OrderResponse> page = new PageImpl<>(List.of(order1, order2));
        when(orderService.findByCustomerId(eq(1L), any(Pageable.class))).thenReturn(page);

        client.get().uri("/api/customers/{id}/orders?page=0&size=10", 1)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(2)
                .jsonPath("$.content[0].orderNumber").isEqualTo("ORD-2024-000001")
                .jsonPath("$.content[1].orderNumber").isEqualTo("ORD-2024-000002");

        verify(orderService).findByCustomerId(eq(1L), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getFavoritePizzas_shouldReturnListOfPizzas() {
        PizzaResponse pizza1 = new PizzaResponse(1L, "Margherita", BigDecimal.valueOf(8.50), "Classic pizza", null, true, null);
        PizzaResponse pizza2 = new PizzaResponse(2L, "Pepperoni", BigDecimal.valueOf(10.00), "Spicy pizza", null, true, null);
        when(customerService.findFavoritePizzas(1L)).thenReturn(List.of(pizza1, pizza2));

        client.get().uri("/api/customers/{id}/favorites", 1)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].name").isEqualTo("Margherita")
                .jsonPath("$[0].price").isEqualTo(8.50)
                .jsonPath("$[1].name").isEqualTo("Pepperoni")
                .jsonPath("$[1].price").isEqualTo(10.00);

        verify(customerService).findFavoritePizzas(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCustomer_shouldReturnCreatedCustomer() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "John Doe", "john@example.com", "password123", "1234567890", "123 Main St");
        when(customerService.create(any(CreateCustomerRequest.class))).thenReturn(john());

        client.post().uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueMatches("Location", ".*/api/customers/1")
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("John Doe")
                .jsonPath("$.email").isEqualTo("john@example.com");

        verify(customerService).create(any(CreateCustomerRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCustomer_shouldReturnUpdatedCustomer() {
        UpdateCustomerRequest request = new UpdateCustomerRequest(
                "John Doe Updated", "john@example.com", "9998887777", "456 New St");
        CustomerResponse response = new CustomerResponse(1L, "John Doe Updated", "john@example.com", "9998887777", "456 New St", "CUSTOMER");
        when(customerService.update(eq(1L), any(UpdateCustomerRequest.class))).thenReturn(response);

        client.put().uri("/api/customers/{id}", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("John Doe Updated")
                .jsonPath("$.address").isEqualTo("456 New St")
                .jsonPath("$.phone").isEqualTo("9998887777");

        verify(customerService).update(eq(1L), any(UpdateCustomerRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCustomer_shouldReturnNoContent() {
        doNothing().when(customerService).delete(1L);

        client.delete().uri("/api/customers/{id}", 1)
                .exchange()
                .expectStatus().isNoContent();

        verify(customerService).delete(1L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void addFavoritePizza_shouldReturnOk() {
        doNothing().when(customerService).addFavoritePizza(1L, 2L);

        client.post().uri("/api/customers/{customerId}/favorites/{pizzaId}", 1, 2)
                .exchange()
                .expectStatus().isOk();

        verify(customerService).addFavoritePizza(1L, 2L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void removeFavoritePizza_shouldReturnNoContent() {
        doNothing().when(customerService).removeFavoritePizza(1L, 2L);

        client.delete().uri("/api/customers/{customerId}/favorites/{pizzaId}", 1, 2)
                .exchange()
                .expectStatus().isNoContent();

        verify(customerService).removeFavoritePizza(1L, 2L);
    }
}
