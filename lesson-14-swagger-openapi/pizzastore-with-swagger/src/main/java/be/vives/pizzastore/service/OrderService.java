package be.vives.pizzastore.service;

import be.vives.pizzastore.domain.*;
import be.vives.pizzastore.dto.request.CreateOrderRequest;
import be.vives.pizzastore.dto.response.OrderResponse;
import be.vives.pizzastore.exception.BusinessException;
import be.vives.pizzastore.mapper.OrderMapper;
import be.vives.pizzastore.repository.CustomerRepository;
import be.vives.pizzastore.repository.OrderRepository;
import be.vives.pizzastore.repository.PizzaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final PizzaRepository pizzaRepository;
    private final OrderMapper orderMapper;

    public OrderService(OrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        PizzaRepository pizzaRepository,
                        OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.pizzaRepository = pizzaRepository;
        this.orderMapper = orderMapper;
    }

    public Page<OrderResponse> findAll(Pageable pageable) {
        log.debug("Finding all orders with pagination: {}", pageable);
        Page<Order> orderPage = orderRepository.findAll(pageable);
        return orderPage.map(orderMapper::toResponse);
    }

    public Page<OrderResponse> findByCustomerId(Long customerId, Pageable pageable) {
        log.debug("Finding orders for customer: {}", customerId);
        Page<Order> orderPage = orderRepository.findAll(pageable);
        Page<Order> filteredOrders = orderPage
                .map(order -> order.getCustomer().getId().equals(customerId) ? order : null)
                .map(order -> order);
        return filteredOrders.map(orderMapper::toResponse);
    }

    public Page<OrderResponse> findByStatus(OrderStatus status, Pageable pageable) {
        log.debug("Finding orders with status: {}", status);
        List<Order> orders = orderRepository.findByStatus(status);
        return orderMapper.toResponseList(orders)
                .stream()
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(),
                        list -> new org.springframework.data.domain.PageImpl<>(
                                list, pageable, list.size()
                        )
                ));
    }

    public OrderResponse findById(Long id) {
        log.debug("Finding order with id: {}", id);
        Order order = orderRepository.findByIdWithOrderLines(id).orElse(null);
        if (order == null) {
            return null;
        }
        return orderMapper.toResponse(order);
    }

    public OrderResponse create(CreateOrderRequest request) {
        log.debug("Creating new order for customer: {}", request.customerId());

        Customer customer = customerRepository.findById(request.customerId()).orElse(null);
        if (customer == null) {
            throw new BusinessException("Customer with id " + request.customerId() + " not found");
        }

        String orderNumber = generateOrderNumber();
        Order order = new Order(orderNumber, customer, OrderStatus.PENDING);

        for (CreateOrderRequest.OrderLineRequest lineRequest : request.orderLines()) {
            Pizza pizza = pizzaRepository.findById(lineRequest.pizzaId()).orElse(null);
            if (pizza == null) {
                throw new BusinessException("Pizza with id " + lineRequest.pizzaId() + " not found");
            }
            
            // Business rule: Cannot order unavailable pizzas
            if (!pizza.getAvailable()) {
                throw new BusinessException("Pizza '" + pizza.getName() + "' is currently not available");
            }

            OrderLine orderLine = new OrderLine(pizza, lineRequest.quantity());
            order.addOrderLine(orderLine);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Created order with id: {} and number: {}", savedOrder.getId(), savedOrder.getOrderNumber());

        return orderMapper.toResponse(savedOrder);
    }

    public OrderResponse updateStatus(Long id, OrderStatus status) {
        log.debug("Updating order {} status to: {}", id, status);
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            return null;
        }
        
        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);
        log.info("Updated order {} status to: {}", id, status);
        return orderMapper.toResponse(updatedOrder);
    }

    public void cancel(Long id) {
        log.debug("Cancelling order with id: {}", id);
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            throw new BusinessException("Order with id " + id + " not found");
        }
        
        // Business rule: Cannot cancel delivered orders
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessException("Cannot cancel order with status " + order.getStatus());
        }
        
        // Business rule: Cannot cancel already cancelled orders
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Order is already cancelled");
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Cancelled order with id: {}", id);
    }

    private String generateOrderNumber() {
        return "ORD-" + LocalDateTime.now().getYear() + "-" +
                String.format("%06d", orderRepository.count() + 1);
    }
}
