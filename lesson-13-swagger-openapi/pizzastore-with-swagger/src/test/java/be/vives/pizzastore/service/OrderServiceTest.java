package be.vives.pizzastore.service;

import be.vives.pizzastore.domain.Customer;
import be.vives.pizzastore.domain.Order;
import be.vives.pizzastore.domain.OrderStatus;
import be.vives.pizzastore.domain.Pizza;
import be.vives.pizzastore.dto.request.CreateOrderRequest;
import be.vives.pizzastore.dto.response.OrderResponse;
import be.vives.pizzastore.exception.BusinessException;
import be.vives.pizzastore.mapper.OrderMapper;
import be.vives.pizzastore.repository.CustomerRepository;
import be.vives.pizzastore.repository.OrderRepository;
import be.vives.pizzastore.repository.PizzaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PizzaRepository pizzaRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    @Test
    void findAll_shouldReturnPageOfOrders() {
        // Arrange
        Customer customer = new Customer("John Doe", "john@example.com");
        Order order1 = new Order("ORD-2024-000001", customer, OrderStatus.PENDING);
        Order order2 = new Order("ORD-2024-000002", customer, OrderStatus.DELIVERED);
        Page<Order> page = new PageImpl<>(Arrays.asList(order1, order2));
        Pageable pageable = PageRequest.of(0, 10);

        OrderResponse response1 = new OrderResponse(1L, "ORD-2024-000001", 1L, "John Doe", 
                List.of(), BigDecimal.valueOf(25.50), OrderStatus.PENDING, LocalDateTime.now());
        OrderResponse response2 = new OrderResponse(2L, "ORD-2024-000002", 1L, "John Doe", 
                List.of(), BigDecimal.valueOf(30.00), OrderStatus.PENDING, LocalDateTime.now());

        when(orderRepository.findAll(pageable)).thenReturn(page);
        when(orderMapper.toResponse(order1)).thenReturn(response1);
        when(orderMapper.toResponse(order2)).thenReturn(response2);

        // Act
        Page<OrderResponse> result = orderService.findAll(pageable);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.getContent().get(0).orderNumber()).isEqualTo("ORD-2024-000001");
        assertThat(result.getContent().get(1).orderNumber()).isEqualTo("ORD-2024-000002");
        verify(orderRepository).findAll(pageable);
    }

    @Test
    void findByCustomerId_shouldReturnCustomerOrders() {
        // Arrange
        Customer customer = new Customer("John Doe", "john@example.com");
        customer.setId(1L);
        Order order1 = new Order("ORD-2024-000001", customer, OrderStatus.PENDING);
        Order order2 = new Order("ORD-2024-000002", customer, OrderStatus.DELIVERED);
        Page<Order> page = new PageImpl<>(Arrays.asList(order1, order2));
        Pageable pageable = PageRequest.of(0, 10);

        OrderResponse response1 = new OrderResponse(1L, "ORD-2024-000001", 1L, "John Doe", 
                List.of(), BigDecimal.valueOf(25.50), OrderStatus.PENDING, LocalDateTime.now());
        OrderResponse response2 = new OrderResponse(2L, "ORD-2024-000002", 1L, "John Doe", 
                List.of(), BigDecimal.valueOf(30.00), OrderStatus.PENDING, LocalDateTime.now());

        when(orderRepository.findAll(pageable)).thenReturn(page);
        when(orderMapper.toResponse(order1)).thenReturn(response1);
        when(orderMapper.toResponse(order2)).thenReturn(response2);

        // Act
        Page<OrderResponse> result = orderService.findByCustomerId(1L, pageable);

        // Assert
        assertThat(result).hasSize(2);
        verify(orderRepository).findAll(pageable);
    }

    @Test
    void findByStatus_shouldReturnOrdersWithStatus() {
        // Arrange
        Customer customer = new Customer("John Doe", "john@example.com");
        Order order1 = new Order("ORD-2024-000001", customer, OrderStatus.PENDING);
        Order order2 = new Order("ORD-2024-000002", customer, OrderStatus.PENDING);

        OrderResponse response1 = new OrderResponse(1L, "ORD-2024-000001", 1L, "John Doe", 
                List.of(), BigDecimal.valueOf(25.50), OrderStatus.PENDING, LocalDateTime.now());
        OrderResponse response2 = new OrderResponse(2L, "ORD-2024-000002", 1L, "John Doe", 
                List.of(), BigDecimal.valueOf(30.00), OrderStatus.PENDING, LocalDateTime.now());

        when(orderRepository.findByStatus(OrderStatus.PENDING)).thenReturn(Arrays.asList(order1, order2));
        when(orderMapper.toResponseList(Arrays.asList(order1, order2))).thenReturn(Arrays.asList(response1, response2));

        // Act
        Page<OrderResponse> result = orderService.findByStatus(OrderStatus.PENDING, PageRequest.of(0, 10));

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.getContent().get(0).status()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.getContent().get(1).status()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository).findByStatus(OrderStatus.PENDING);
    }

    @Test
    void findById_whenExists_shouldReturnOrder() {
        // Arrange
        Customer customer = new Customer("John Doe", "john@example.com");
        Order order = new Order("ORD-2024-000001", customer, OrderStatus.PENDING);
        OrderResponse response = new OrderResponse(1L, "ORD-2024-000001", 1L, "John Doe", 
                List.of(), BigDecimal.valueOf(25.50), OrderStatus.PENDING, LocalDateTime.now());

        when(orderRepository.findByIdWithOrderLines(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(response);

        // Act
        OrderResponse result = orderService.findById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.orderNumber()).isEqualTo("ORD-2024-000001");
        verify(orderRepository).findByIdWithOrderLines(1L);
    }

    @Test
    void findById_whenNotExists_shouldReturnNull() {
        // Arrange
        when(orderRepository.findByIdWithOrderLines(999L)).thenReturn(Optional.empty());

        // Act
        OrderResponse result = orderService.findById(999L);

        // Assert
        assertThat(result).isNull();
        verify(orderRepository).findByIdWithOrderLines(999L);
    }

    @Test
    void create_shouldCreateAndReturnOrder() {
        // Arrange
        Customer customer = new Customer("John Doe", "john@example.com");
        Pizza pizza1 = new Pizza("Margherita", BigDecimal.valueOf(8.50), "Classic pizza");
        pizza1.setAvailable(true);
        Pizza pizza2 = new Pizza("Pepperoni", BigDecimal.valueOf(10.00), "Spicy pizza");
        pizza2.setAvailable(true);

        CreateOrderRequest.OrderLineRequest lineRequest1 = new CreateOrderRequest.OrderLineRequest(1L, 2);
        CreateOrderRequest.OrderLineRequest lineRequest2 = new CreateOrderRequest.OrderLineRequest(2L, 1);
        CreateOrderRequest request = new CreateOrderRequest(1L, Arrays.asList(lineRequest1, lineRequest2));

        Order order = new Order("ORD-2024-000001", customer, OrderStatus.PENDING);
        OrderResponse response = new OrderResponse(1L, "ORD-2024-000001", 1L, "John Doe", 
                List.of(), BigDecimal.valueOf(27.00), OrderStatus.PENDING, LocalDateTime.now());

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(pizzaRepository.findById(1L)).thenReturn(Optional.of(pizza1));
        when(pizzaRepository.findById(2L)).thenReturn(Optional.of(pizza2));
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(response);

        // Act
        OrderResponse result = orderService.create(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.orderNumber()).isEqualTo("ORD-2024-000001");
        verify(customerRepository).findById(1L);
        verify(pizzaRepository).findById(1L);
        verify(pizzaRepository).findById(2L);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void create_whenCustomerNotFound_shouldThrowException() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(999L, List.of());
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Customer")
                .hasMessageContaining("999");

        verify(customerRepository).findById(999L);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void create_whenPizzaNotFound_shouldThrowException() {
        // Arrange
        Customer customer = new Customer("John Doe", "john@example.com");
        CreateOrderRequest.OrderLineRequest lineRequest = new CreateOrderRequest.OrderLineRequest(999L, 1);
        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(lineRequest));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(pizzaRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Pizza")
                .hasMessageContaining("999");

        verify(customerRepository).findById(1L);
        verify(pizzaRepository).findById(999L);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void create_whenPizzaNotAvailable_shouldThrowException() {
        // Arrange
        Customer customer = new Customer("John Doe", "john@example.com");
        Pizza pizza = new Pizza("Margherita", BigDecimal.valueOf(8.50), "Classic pizza");
        pizza.setAvailable(false);

        CreateOrderRequest.OrderLineRequest lineRequest = new CreateOrderRequest.OrderLineRequest(1L, 1);
        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(lineRequest));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(pizzaRepository.findById(1L)).thenReturn(Optional.of(pizza));

        // Act & Assert
        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not available");

        verify(customerRepository).findById(1L);
        verify(pizzaRepository).findById(1L);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateStatus_whenExists_shouldUpdateAndReturnOrder() {
        // Arrange
        Customer customer = new Customer("John Doe", "john@example.com");
        Order order = new Order("ORD-2024-000001", customer, OrderStatus.PENDING);
        OrderResponse response = new OrderResponse(1L, "ORD-2024-000001", 1L, "John Doe", 
                List.of(), BigDecimal.valueOf(25.50), OrderStatus.PREPARING, LocalDateTime.now());

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(response);

        // Act
        OrderResponse result = orderService.updateStatus(1L, OrderStatus.PREPARING);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(OrderStatus.PREPARING);
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(order);
    }

    @Test
    void updateStatus_whenNotExists_shouldReturnNull() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        OrderResponse result = orderService.updateStatus(999L, OrderStatus.PREPARING);

        // Assert
        assertThat(result).isNull();
        verify(orderRepository).findById(999L);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancel_whenExists_shouldCancelOrder() {
        // Arrange
        Customer customer = new Customer("John Doe", "john@example.com");
        Order order = new Order("ORD-2024-000001", customer, OrderStatus.PENDING);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        // Act
        orderService.cancel(1L);

        // Assert
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(order);
    }

    @Test
    void cancel_whenNotExists_shouldThrowException() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.cancel(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Order")
                .hasMessageContaining("999");

        verify(orderRepository).findById(999L);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancel_whenAlreadyDelivered_shouldThrowException() {
        // Arrange
        Customer customer = new Customer("John Doe", "john@example.com");
        Order order = new Order("ORD-2024-000001", customer, OrderStatus.DELIVERED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThatThrownBy(() -> orderService.cancel(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot cancel");

        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancel_whenAlreadyCancelled_shouldThrowException() {
        // Arrange
        Customer customer = new Customer("John Doe", "john@example.com");
        Order order = new Order("ORD-2024-000001", customer, OrderStatus.CANCELLED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThatThrownBy(() -> orderService.cancel(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already cancelled");

        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any());
    }
}
