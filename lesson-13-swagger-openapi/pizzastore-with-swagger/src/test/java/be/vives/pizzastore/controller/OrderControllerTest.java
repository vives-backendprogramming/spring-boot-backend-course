package be.vives.pizzastore.controller;

import be.vives.pizzastore.domain.OrderStatus;
import be.vives.pizzastore.dto.request.CreateOrderRequest;
import be.vives.pizzastore.dto.request.UpdateOrderStatusRequest;
import be.vives.pizzastore.dto.response.OrderLineResponse;
import be.vives.pizzastore.dto.response.OrderResponse;
import be.vives.pizzastore.exception.BusinessException;
import be.vives.pizzastore.exception.GlobalExceptionHandler;
import be.vives.pizzastore.exception.ResourceNotFoundException;
import be.vives.pizzastore.security.JwtUtil;
import be.vives.pizzastore.security.SecurityConfig;
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
 * Web-layer slice for OrderController with RestTestClient and the real SecurityConfig:
 * only a CUSTOMER can place an order, every other order endpoint is for ADMIN.
 */
@WebMvcTest(controllers = OrderController.class)
@AutoConfigureRestTestClient
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class OrderControllerTest {

    @Autowired
    private RestTestClient client;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private static OrderResponse order(long id, long customerId, String customerName, OrderStatus status, double total) {
        return new OrderResponse(id, "ORD-2024-%06d".formatted(id), customerId, customerName,
                List.of(), BigDecimal.valueOf(total), status, LocalDateTime.now());
    }

    private static OrderResponse orderWithLines() {
        OrderLineResponse line1 = new OrderLineResponse(1L, 1L, "Margherita", 2, BigDecimal.valueOf(8.50), BigDecimal.valueOf(17.00));
        OrderLineResponse line2 = new OrderLineResponse(2L, 2L, "Pepperoni", 1, BigDecimal.valueOf(10.00), BigDecimal.valueOf(10.00));
        return new OrderResponse(1L, "ORD-2024-000001", 1L, "John Doe",
                List.of(line1, line2), BigDecimal.valueOf(27.00), OrderStatus.PENDING, LocalDateTime.now());
    }

    private static CreateOrderRequest validOrderRequest() {
        return new CreateOrderRequest(1L, List.of(
                new CreateOrderRequest.OrderLineRequest(1L, 2),
                new CreateOrderRequest.OrderLineRequest(2L, 1)));
    }

    // ---------- reading (ADMIN) ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void getOrders_withAdminRole_shouldReturnAllOrders() {
        Page<OrderResponse> page = new PageImpl<>(List.of(
                order(1, 1, "John Doe", OrderStatus.PENDING, 25.50),
                order(2, 2, "Jane Smith", OrderStatus.DELIVERED, 30.00)));
        when(orderService.findAll(any(Pageable.class))).thenReturn(page);

        client.get().uri("/api/orders?page=0&size=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(2)
                .jsonPath("$.content[0].orderNumber").isEqualTo("ORD-2024-000001")
                .jsonPath("$.content[1].orderNumber").isEqualTo("ORD-2024-000002");

        verify(orderService).findAll(any(Pageable.class));
        verify(orderService, never()).findByCustomerId(any(), any());
        verify(orderService, never()).findByStatus(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getOrders_withCustomerId_shouldReturnCustomerOrders() {
        Page<OrderResponse> page = new PageImpl<>(List.of(order(1, 1, "John Doe", OrderStatus.PENDING, 25.50)));
        when(orderService.findByCustomerId(eq(1L), any(Pageable.class))).thenReturn(page);

        client.get().uri("/api/orders?customerId=1&page=0&size=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(1)
                .jsonPath("$.content[0].customerId").isEqualTo(1)
                .jsonPath("$.content[0].customerName").isEqualTo("John Doe");

        verify(orderService).findByCustomerId(eq(1L), any(Pageable.class));
        verify(orderService, never()).findAll(any());
        verify(orderService, never()).findByStatus(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getOrders_withStatus_shouldReturnOrdersWithStatus() {
        Page<OrderResponse> page = new PageImpl<>(List.of(
                order(1, 1, "John Doe", OrderStatus.PENDING, 25.50),
                order(2, 2, "Jane Smith", OrderStatus.PENDING, 30.00)));
        when(orderService.findByStatus(eq(OrderStatus.PENDING), any(Pageable.class))).thenReturn(page);

        client.get().uri("/api/orders?status=PENDING&page=0&size=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(2);

        verify(orderService).findByStatus(eq(OrderStatus.PENDING), any(Pageable.class));
        verify(orderService, never()).findAll(any());
        verify(orderService, never()).findByCustomerId(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getOrder_whenExists_shouldReturnOrder() {
        when(orderService.findById(1L)).thenReturn(orderWithLines());

        client.get().uri("/api/orders/{id}", 1)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.orderNumber").isEqualTo("ORD-2024-000001")
                .jsonPath("$.totalAmount").isEqualTo(27.00)
                .jsonPath("$.orderLines.length()").isEqualTo(2)
                .jsonPath("$.orderLines[0].pizzaName").isEqualTo("Margherita")
                .jsonPath("$.orderLines[0].quantity").isEqualTo(2)
                .jsonPath("$.orderLines[1].pizzaName").isEqualTo("Pepperoni")
                .jsonPath("$.orderLines[1].quantity").isEqualTo(1);

        verify(orderService).findById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getOrder_whenNotExists_shouldReturnNotFound() {
        when(orderService.findById(999L)).thenThrow(new ResourceNotFoundException("Order", 999L));

        client.get().uri("/api/orders/{id}", 999)
                .exchange()
                .expectStatus().isNotFound();

        verify(orderService).findById(999L);
    }

    // ---------- creating (CUSTOMER) ----------

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createOrder_shouldReturnCreatedOrder() {
        when(orderService.create(any(CreateOrderRequest.class))).thenReturn(orderWithLines());

        client.post().uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(validOrderRequest())
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueMatches("Location", ".*/api/orders/1")
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.orderNumber").isEqualTo("ORD-2024-000001")
                .jsonPath("$.totalAmount").isEqualTo(27.00)
                .jsonPath("$.orderLines.length()").isEqualTo(2);

        verify(orderService).create(any(CreateOrderRequest.class));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createOrder_whenCustomerNotFound_shouldReturnProblemDetail() {
        CreateOrderRequest request = new CreateOrderRequest(999L, List.of(new CreateOrderRequest.OrderLineRequest(1L, 2)));
        when(orderService.create(any(CreateOrderRequest.class)))
                .thenThrow(new BusinessException("Customer with id 999 not found"));

        client.post().uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(422)
                .jsonPath("$.title").isEqualTo("Unprocessable Content")
                .jsonPath("$.detail").isEqualTo("Customer with id 999 not found")
                .jsonPath("$.type").isEqualTo("https://api.pizzastore.example.com/errors/business-rule");

        verify(orderService).create(any(CreateOrderRequest.class));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createOrder_withInvalidOrderLine_shouldReturnBadRequest() {
        // a negative quantity must be rejected before it ever reaches the service
        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(new CreateOrderRequest.OrderLineRequest(1L, -5)));

        client.post().uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isBadRequest();

        verify(orderService, never()).create(any());
    }

    // ---------- status and cancel (ADMIN) ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateOrderStatus_whenExists_shouldReturnUpdatedOrder() {
        when(orderService.updateStatus(eq(1L), eq(OrderStatus.PREPARING)))
                .thenReturn(order(1, 1, "John Doe", OrderStatus.PREPARING, 27.00));

        client.patch().uri("/api/orders/{id}/status", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateOrderStatusRequest(OrderStatus.PREPARING))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.status").isEqualTo("PREPARING");

        verify(orderService).updateStatus(eq(1L), eq(OrderStatus.PREPARING));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateOrderStatus_whenNotExists_shouldReturnNotFound() {
        when(orderService.updateStatus(eq(999L), eq(OrderStatus.PREPARING)))
                .thenThrow(new ResourceNotFoundException("Order", 999L));

        client.patch().uri("/api/orders/{id}/status", 999)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateOrderStatusRequest(OrderStatus.PREPARING))
                .exchange()
                .expectStatus().isNotFound();

        verify(orderService).updateStatus(eq(999L), eq(OrderStatus.PREPARING));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateOrderStatus_withoutStatus_shouldReturnValidationProblemDetail() {
        client.patch().uri("/api/orders/{id}/status", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors[0].field").isEqualTo("status")
                .jsonPath("$.errors[0].message").isEqualTo("Status is required");

        verify(orderService, never()).updateStatus(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateOrderStatus_withInvalidStatusValue_shouldReturnProblemDetail() {
        client.patch().uri("/api/orders/{id}/status", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"status\": \"NOT_A_REAL_STATUS\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").isEqualTo(
                        "Invalid OrderStatus value. Allowed values: PENDING, CONFIRMED, PREPARING, READY, DELIVERED, CANCELLED")
                .jsonPath("$.type").isEqualTo("https://api.pizzastore.example.com/errors/malformed-request");

        verify(orderService, never()).updateStatus(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cancelOrder_shouldReturnNoContent() {
        doNothing().when(orderService).cancel(1L);

        client.delete().uri("/api/orders/{id}", 1)
                .exchange()
                .expectStatus().isNoContent();

        verify(orderService).cancel(1L);
    }

    // ---------- security: who may do what ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void createOrder_withAdminRole_returnsForbidden() {
        // admins cannot place orders, only customers can
        client.post().uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(validOrderRequest())
                .exchange()
                .expectStatus().isForbidden();

        verify(orderService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getOrders_withCustomerRole_returnsForbidden() {
        client.get().uri("/api/orders?page=0&size=10")
                .exchange()
                .expectStatus().isForbidden();

        verify(orderService, never()).findAll(any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getOrder_withCustomerRole_returnsForbidden() {
        client.get().uri("/api/orders/{id}", 1)
                .exchange()
                .expectStatus().isForbidden();

        verify(orderService, never()).findById(any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateOrderStatus_withCustomerRole_returnsForbidden() {
        client.patch().uri("/api/orders/{id}/status", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateOrderStatusRequest(OrderStatus.PREPARING))
                .exchange()
                .expectStatus().isForbidden();

        verify(orderService, never()).updateStatus(any(), any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void cancelOrder_withCustomerRole_returnsForbidden() {
        client.delete().uri("/api/orders/{id}", 1)
                .exchange()
                .expectStatus().isForbidden();

        verify(orderService, never()).cancel(any());
    }
}
