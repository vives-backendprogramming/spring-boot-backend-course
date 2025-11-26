package be.vives.pizzastore.controller;

import be.vives.pizzastore.domain.OrderStatus;
import be.vives.pizzastore.dto.request.CreateOrderRequest;
import be.vives.pizzastore.dto.request.UpdateOrderStatusRequest;
import be.vives.pizzastore.dto.response.OrderLineResponse;
import be.vives.pizzastore.dto.response.OrderResponse;
import be.vives.pizzastore.exception.GlobalExceptionHandler;
import be.vives.pizzastore.security.JwtUtil;
import be.vives.pizzastore.security.SecurityConfig;
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

@WebMvcTest(controllers = OrderController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getOrders_withAdminRole_shouldReturnAllOrders() throws Exception {
        // Arrange
        OrderResponse order1 = new OrderResponse(1L, "ORD-2024-000001", 1L, "John Doe", 
                List.of(), BigDecimal.valueOf(25.50), OrderStatus.PENDING, LocalDateTime.now());
        OrderResponse order2 = new OrderResponse(2L, "ORD-2024-000002", 2L, "Jane Smith", 
                List.of(), BigDecimal.valueOf(30.00), OrderStatus.DELIVERED, LocalDateTime.now());
        Page<OrderResponse> page = new PageImpl<>(Arrays.asList(order1, order2));

        when(orderService.findAll(any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].orderNumber", is("ORD-2024-000001")))
                .andExpect(jsonPath("$.content[1].orderNumber", is("ORD-2024-000002")));

        verify(orderService).findAll(any(Pageable.class));
        verify(orderService, never()).findByCustomerId(any(), any());
        verify(orderService, never()).findByStatus(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getOrders_withCustomerId_shouldReturnCustomerOrders() throws Exception {
        // Arrange
        OrderResponse order1 = new OrderResponse(1L, "ORD-2024-000001", 1L, "John Doe", 
                List.of(), BigDecimal.valueOf(25.50), OrderStatus.PENDING, LocalDateTime.now());
        Page<OrderResponse> page = new PageImpl<>(List.of(order1));

        when(orderService.findByCustomerId(eq(1L), any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/orders")
                        .param("customerId", "1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].customerId", is(1)))
                .andExpect(jsonPath("$.content[0].customerName", is("John Doe")));

        verify(orderService).findByCustomerId(eq(1L), any(Pageable.class));
        verify(orderService, never()).findAll(any());
        verify(orderService, never()).findByStatus(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getOrders_withStatus_shouldReturnOrdersWithStatus() throws Exception {
        // Arrange
        OrderResponse order1 = new OrderResponse(1L, "ORD-2024-000001", 1L, "John Doe", 
                List.of(), BigDecimal.valueOf(25.50), OrderStatus.PENDING, LocalDateTime.now());
        OrderResponse order2 = new OrderResponse(2L, "ORD-2024-000002", 2L, "Jane Smith", 
                List.of(), BigDecimal.valueOf(30.00), OrderStatus.PENDING, LocalDateTime.now());
        Page<OrderResponse> page = new PageImpl<>(Arrays.asList(order1, order2));

        when(orderService.findByStatus(eq(OrderStatus.PENDING), any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/orders")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));

        verify(orderService).findByStatus(eq(OrderStatus.PENDING), any(Pageable.class));
        verify(orderService, never()).findAll(any());
        verify(orderService, never()).findByCustomerId(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getOrder_whenExists_shouldReturnOrder() throws Exception {
        // Arrange
        OrderLineResponse line1 = new OrderLineResponse(1L, 1L, "Margherita", 2, BigDecimal.valueOf(8.50), BigDecimal.valueOf(17.00));
        OrderLineResponse line2 = new OrderLineResponse(2L, 2L, "Pepperoni", 1, BigDecimal.valueOf(10.00), BigDecimal.valueOf(10.00));
        OrderResponse order = new OrderResponse(1L, "ORD-2024-000001", 1L, "John Doe", 
                Arrays.asList(line1, line2), BigDecimal.valueOf(27.00), OrderStatus.PENDING, LocalDateTime.now());

        when(orderService.findById(1L)).thenReturn(order);

        // Act & Assert
        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.orderNumber", is("ORD-2024-000001")))
                .andExpect(jsonPath("$.totalAmount", is(27.00)))
                .andExpect(jsonPath("$.orderLines", hasSize(2)))
                .andExpect(jsonPath("$.orderLines[0].pizzaName", is("Margherita")))
                .andExpect(jsonPath("$.orderLines[0].quantity", is(2)))
                .andExpect(jsonPath("$.orderLines[1].pizzaName", is("Pepperoni")))
                .andExpect(jsonPath("$.orderLines[1].quantity", is(1)));

        verify(orderService).findById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getOrder_whenNotExists_shouldReturnNotFound() throws Exception {
        // Arrange
        when(orderService.findById(999L)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound());

        verify(orderService).findById(999L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createOrder_shouldReturnCreatedOrder() throws Exception {
        // Arrange
        CreateOrderRequest.OrderLineRequest lineRequest1 = new CreateOrderRequest.OrderLineRequest(1L, 2);
        CreateOrderRequest.OrderLineRequest lineRequest2 = new CreateOrderRequest.OrderLineRequest(2L, 1);
        CreateOrderRequest request = new CreateOrderRequest(1L, Arrays.asList(lineRequest1, lineRequest2));

        OrderLineResponse line1 = new OrderLineResponse(1L, 1L, "Margherita", 2, BigDecimal.valueOf(8.50), BigDecimal.valueOf(17.00));
        OrderLineResponse line2 = new OrderLineResponse(2L, 2L, "Pepperoni", 1, BigDecimal.valueOf(10.00), BigDecimal.valueOf(10.00));
        OrderResponse response = new OrderResponse(1L, "ORD-2024-000001", 1L, "John Doe", 
                Arrays.asList(line1, line2), BigDecimal.valueOf(27.00), OrderStatus.PENDING, LocalDateTime.now());

        when(orderService.create(any(CreateOrderRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/orders/1")))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.orderNumber", is("ORD-2024-000001")))
                .andExpect(jsonPath("$.totalAmount", is(27.00)))
                .andExpect(jsonPath("$.orderLines", hasSize(2)));

        verify(orderService).create(any(CreateOrderRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateOrderStatus_whenExists_shouldReturnUpdatedOrder() throws Exception {
        // Arrange
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.PREPARING);
        OrderResponse response = new OrderResponse(1L, "ORD-2024-000001", 1L, "John Doe", 
                List.of(), BigDecimal.valueOf(27.00), OrderStatus.PREPARING, LocalDateTime.now());

        when(orderService.updateStatus(eq(1L), eq(OrderStatus.PREPARING))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch("/api/orders/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(orderService).updateStatus(eq(1L), eq(OrderStatus.PREPARING));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateOrderStatus_whenNotExists_shouldReturnNotFound() throws Exception {
        // Arrange
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.PREPARING);

        when(orderService.updateStatus(eq(999L), eq(OrderStatus.PREPARING))).thenReturn(null);

        // Act & Assert
        mockMvc.perform(patch("/api/orders/999/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(orderService).updateStatus(eq(999L), eq(OrderStatus.PREPARING));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cancelOrder_shouldReturnNoContent() throws Exception {
        // Arrange
        doNothing().when(orderService).cancel(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/orders/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(orderService).cancel(1L);
    }

    // Security tests

    @Test
    @WithMockUser(roles = "ADMIN")
    void createOrder_withAdminRole_returnsForbidden() throws Exception {
        // Arrange
        CreateOrderRequest.OrderLineRequest lineRequest = new CreateOrderRequest.OrderLineRequest(1L, 2);
        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(lineRequest));

        // Act & Assert - Admins cannot create orders, only customers can
        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(orderService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getOrders_withCustomerRole_returnsForbidden() throws Exception {
        // Act & Assert - Customers cannot view all orders, only admins can
        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isForbidden());

        verify(orderService, never()).findAll(any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getOrder_withCustomerRole_returnsForbidden() throws Exception {
        // Act & Assert - Customers cannot view specific orders, only admins can
        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isForbidden());

        verify(orderService, never()).findById(any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateOrderStatus_withCustomerRole_returnsForbidden() throws Exception {
        // Arrange
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.PREPARING);

        // Act & Assert - Customers cannot update order status, only admins can
        mockMvc.perform(patch("/api/orders/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(orderService, never()).updateStatus(any(), any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void cancelOrder_withCustomerRole_returnsForbidden() throws Exception {
        // Act & Assert - Customers cannot cancel orders, only admins can
        mockMvc.perform(delete("/api/orders/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(orderService, never()).cancel(any());
    }
}
