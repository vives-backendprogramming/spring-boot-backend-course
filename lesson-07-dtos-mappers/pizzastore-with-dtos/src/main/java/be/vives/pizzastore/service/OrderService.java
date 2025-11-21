package be.vives.pizzastore.service;

import be.vives.pizzastore.domain.*;
import be.vives.pizzastore.dto.request.CreateOrderRequest;
import be.vives.pizzastore.dto.request.OrderLineRequest;
import be.vives.pizzastore.dto.response.OrderResponse;
import be.vives.pizzastore.mapper.OrderMapper;
import be.vives.pizzastore.repository.OrderRepository;
import be.vives.pizzastore.repository.PizzaRepository;
import be.vives.pizzastore.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PizzaRepository pizzaRepository;
    private final OrderMapper orderMapper;

    public OrderService(OrderRepository orderRepository,
                        UserRepository userRepository,
                        PizzaRepository pizzaRepository,
                        OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.pizzaRepository = pizzaRepository;
        this.orderMapper = orderMapper;
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(orderMapper::toOrderResponse)
                .toList();
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        return orderMapper.toOrderResponse(order);
    }

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(orderMapper::toOrderResponse)
                .toList();
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.userId()));

        Order order = new Order();
        order.setUser(user);
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());

        List<OrderLine> orderLines = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderLineRequest lineRequest : request.orderLines()) {
            Pizza pizza = pizzaRepository.findById(lineRequest.pizzaId())
                    .orElseThrow(() -> new RuntimeException("Pizza not found with id: " + lineRequest.pizzaId()));

            if (!pizza.isAvailable()) {
                throw new RuntimeException("Pizza is not available: " + pizza.getName());
            }

            OrderLine orderLine = new OrderLine();
            orderLine.setOrder(order);
            orderLine.setPizza(pizza);
            orderLine.setQuantity(lineRequest.quantity());
            orderLine.setUnitPrice(pizza.getPrice());
            
            BigDecimal subtotal = pizza.getPrice().multiply(BigDecimal.valueOf(lineRequest.quantity()));
            orderLine.setSubtotal(subtotal);
            totalPrice = totalPrice.add(subtotal);

            orderLines.add(orderLine);
        }

        order.setOrderLines(orderLines);

        Order savedOrder = orderRepository.save(order);
        return orderMapper.toOrderResponse(savedOrder);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        
        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toOrderResponse(updatedOrder);
    }

    @Transactional
    public void cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new RuntimeException("Cannot cancel a delivered order");
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
