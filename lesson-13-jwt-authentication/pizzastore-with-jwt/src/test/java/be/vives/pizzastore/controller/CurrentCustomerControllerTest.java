package be.vives.pizzastore.controller;

import be.vives.pizzastore.domain.OrderStatus;
import be.vives.pizzastore.dto.request.UpdateCustomerRequest;
import be.vives.pizzastore.dto.response.CustomerResponse;
import be.vives.pizzastore.dto.response.OrderResponse;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.exception.GlobalExceptionHandler;
import be.vives.pizzastore.security.JwtUtil;
import be.vives.pizzastore.security.SecurityConfig;
import be.vives.pizzastore.service.CustomerService;
import be.vives.pizzastore.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
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
 * Web-layer slice for CurrentCustomerController (/api/customers/me) with RestTestClient and the real SecurityConfig.
 * The logged-in user is john@example.com, customer 1: every test checks that the controller works on the account of
 * the token, without any id in the request.
 */
@WebMvcTest(controllers = CurrentCustomerController.class)
@AutoConfigureRestTestClient
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class CurrentCustomerControllerTest {

    private static final String JOHN = "john@example.com";

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
        return new CustomerResponse(1L, "John Doe", JOHN, "1234567890", "123 Main St", "CUSTOMER");
    }

    @BeforeEach
    void johnIsCustomer1() {
        when(customerService.findByEmail(JOHN)).thenReturn(john());
    }

    @Test
    @WithMockUser(username = JOHN, roles = "CUSTOMER")
    void getCurrentCustomer_shouldReturnTheLoggedInCustomer() {
        client.get().uri("/api/customers/me")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.email").isEqualTo(JOHN);
    }

    @Test
    @WithMockUser(username = JOHN, roles = "CUSTOMER")
    void updateCurrentCustomer_updatesTheLoggedInCustomer() {
        UpdateCustomerRequest request = new UpdateCustomerRequest("John Doe Updated", JOHN, "9998887777", "456 New St");
        CustomerResponse updated = new CustomerResponse(1L, "John Doe Updated", JOHN, "9998887777", "456 New St", "CUSTOMER");
        when(customerService.update(eq(1L), any(UpdateCustomerRequest.class))).thenReturn(updated);

        client.put().uri("/api/customers/me")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("John Doe Updated");

        verify(customerService).update(eq(1L), any(UpdateCustomerRequest.class));
    }

    @Test
    @WithMockUser(username = JOHN, roles = "CUSTOMER")
    void updateCurrentCustomer_withInvalidData_returnsBadRequest() {
        client.put().uri("/api/customers/me")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateCustomerRequest("", "not-an-email", null, null))
                .exchange()
                .expectStatus().isBadRequest();

        verify(customerService, never()).update(any(), any());
    }

    @Test
    @WithMockUser(username = JOHN, roles = "CUSTOMER")
    void getMyOrders_returnsTheOrdersOfTheLoggedInCustomer() {
        OrderResponse order = new OrderResponse(1L, "ORD-2024-000001", 1L, "John Doe",
                List.of(), BigDecimal.valueOf(25.50), OrderStatus.PENDING, LocalDateTime.now());
        when(orderService.findByCustomerId(eq(1L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(order)));

        client.get().uri("/api/customers/me/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(1)
                .jsonPath("$.content[0].customerId").isEqualTo(1);

        verify(orderService).findByCustomerId(eq(1L), any(Pageable.class));
    }

    @Test
    @WithMockUser(username = JOHN, roles = "CUSTOMER")
    void getMyFavoritePizzas_returnsTheFavoritesOfTheLoggedInCustomer() {
        PizzaResponse margherita = new PizzaResponse(1L, "Margherita", BigDecimal.valueOf(8.50), "Classic pizza", null, true, null);
        when(customerService.findFavoritePizzas(1L)).thenReturn(List.of(margherita));

        client.get().uri("/api/customers/me/favorites")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].name").isEqualTo("Margherita");

        verify(customerService).findFavoritePizzas(1L);
    }

    @Test
    @WithMockUser(username = JOHN, roles = "CUSTOMER")
    void addFavoritePizza_addsToTheLoggedInCustomer() {
        doNothing().when(customerService).addFavoritePizza(1L, 2L);

        client.post().uri("/api/customers/me/favorites/{pizzaId}", 2)
                .exchange()
                .expectStatus().isOk();

        verify(customerService).addFavoritePizza(1L, 2L);
    }

    @Test
    @WithMockUser(username = JOHN, roles = "CUSTOMER")
    void removeFavoritePizza_removesFromTheLoggedInCustomer() {
        doNothing().when(customerService).removeFavoritePizza(1L, 2L);

        client.delete().uri("/api/customers/me/favorites/{pizzaId}", 2)
                .exchange()
                .expectStatus().isNoContent();

        verify(customerService).removeFavoritePizza(1L, 2L);
    }

    @Test
    @WithAnonymousUser
    void getCurrentCustomer_Anonymous_ReturnsUnauthorized() {
        client.get().uri("/api/customers/me")
                .exchange()
                .expectStatus().isUnauthorized();

        verify(customerService, never()).findByEmail(any());
    }
}
