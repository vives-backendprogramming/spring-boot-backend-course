package be.vives.pizzastore.repository;

import be.vives.pizzastore.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({be.vives.pizzastore.config.JpaConfig.class, be.vives.pizzastore.config.AuditorAwareImpl.class})
class OrderRepositoryTest {

    private Customer createTestCustomer(String name, String email) {
        Customer customer = new Customer(name, email);
        customer.setPassword("test123");
        customer.setCreatedAt(java.time.LocalDateTime.now());
        customer.setUpdatedAt(java.time.LocalDateTime.now());
        return customer;
    }

    private Pizza createTestPizza(String name, java.math.BigDecimal price, String description) {
        Pizza pizza = new Pizza(name, price, description);
        pizza.setCreatedAt(java.time.LocalDateTime.now());
        pizza.setUpdatedAt(java.time.LocalDateTime.now());
        return pizza;
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void findByCustomerId_shouldReturnCustomerOrders() {
        // Arrange
        Customer customer1 = createTestCustomer("John Doe", "john@example.com");
        Customer customer2 = createTestCustomer("Jane Smith", "jane@example.com");
        entityManager.persist(customer1);
        entityManager.persist(customer2);

        Order order1 = new Order("ORD-2024-000001", customer1, OrderStatus.PENDING);
        Order order2 = new Order("ORD-2024-000002", customer1, OrderStatus.DELIVERED);
        Order order3 = new Order("ORD-2024-000003", customer2, OrderStatus.PENDING);
        entityManager.persist(order1);
        entityManager.persist(order2);
        entityManager.persist(order3);
        entityManager.flush();

        // Act
        List<Order> orders = orderRepository.findByCustomerId(customer1.getId());

        // Assert
        assertThat(orders).hasSize(2);
        assertThat(orders).extracting(Order::getOrderNumber)
                .containsExactlyInAnyOrder("ORD-2024-000001", "ORD-2024-000002");
    }

    @Test
    void findByStatus_shouldReturnOrdersWithStatus() {
        // Arrange
        Customer customer = createTestCustomer("John Doe", "john@example.com");
        entityManager.persist(customer);

        Order order1 = new Order("ORD-2024-000001", customer, OrderStatus.PENDING);
        Order order2 = new Order("ORD-2024-000002", customer, OrderStatus.PENDING);
        Order order3 = new Order("ORD-2024-000003", customer, OrderStatus.DELIVERED);
        entityManager.persist(order1);
        entityManager.persist(order2);
        entityManager.persist(order3);
        entityManager.flush();

        // Act
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);

        // Assert
        assertThat(pendingOrders).hasSize(2);
        assertThat(pendingOrders).extracting(Order::getStatus)
                .containsOnly(OrderStatus.PENDING);
    }

    @Test
    void findByOrderNumber_whenExists_shouldReturnOrder() {
        // Arrange
        Customer customer = createTestCustomer("John Doe", "john@example.com");
        entityManager.persist(customer);

        Order order = new Order("ORD-2024-000001", customer, OrderStatus.PENDING);
        entityManager.persist(order);
        entityManager.flush();

        // Act
        Optional<Order> found = orderRepository.findByOrderNumber("ORD-2024-000001");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getOrderNumber()).isEqualTo("ORD-2024-000001");
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void findByOrderNumber_whenNotExists_shouldReturnEmpty() {
        // Act
        Optional<Order> found = orderRepository.findByOrderNumber("ORD-NONEXISTENT");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void findByIdWithOrderLines_whenExists_shouldReturnOrderWithLines() {
        // Arrange
        Customer customer = createTestCustomer("John Doe", "john@example.com");
        Pizza pizza1 = createTestPizza("Margherita", BigDecimal.valueOf(8.50), "Classic pizza");
        Pizza pizza2 = createTestPizza("Pepperoni", BigDecimal.valueOf(10.00), "Spicy pizza");
        entityManager.persist(customer);
        entityManager.persist(pizza1);
        entityManager.persist(pizza2);

        Order order = new Order("ORD-2024-000001", customer, OrderStatus.PENDING);
        OrderLine line1 = new OrderLine(pizza1, 2);
        OrderLine line2 = new OrderLine(pizza2, 1);
        order.addOrderLine(line1);
        order.addOrderLine(line2);
        entityManager.persist(order);
        entityManager.flush();

        // Act
        Optional<Order> found = orderRepository.findByIdWithOrderLines(order.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getOrderLines()).hasSize(2);
        assertThat(found.get().getOrderLines()).extracting(ol -> ol.getPizza().getName())
                .containsExactlyInAnyOrder("Margherita", "Pepperoni");
        assertThat(found.get().getOrderLines()).extracting(OrderLine::getQuantity)
                .containsExactlyInAnyOrder(2, 1);
    }

    @Test
    void findByIdWithOrderLines_whenNotExists_shouldReturnEmpty() {
        // Act
        Optional<Order> found = orderRepository.findByIdWithOrderLines(999L);

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void findByCustomerIdWithCustomer_shouldReturnOrdersWithCustomer() {
        // Arrange
        Customer customer = createTestCustomer("John Doe", "john@example.com");
        entityManager.persist(customer);

        Order order1 = new Order("ORD-2024-000001", customer, OrderStatus.PENDING);
        Order order2 = new Order("ORD-2024-000002", customer, OrderStatus.DELIVERED);
        entityManager.persist(order1);
        entityManager.persist(order2);
        entityManager.flush();

        // Act
        List<Order> orders = orderRepository.findByCustomerIdWithCustomer(customer.getId());

        // Assert
        assertThat(orders).hasSize(2);
        assertThat(orders).extracting(o -> o.getCustomer().getName())
                .containsOnly("John Doe");
    }

    @Test
    void save_shouldPersistOrder() {
        // Arrange
        Customer customer = createTestCustomer("John Doe", "john@example.com");
        entityManager.persist(customer);
        entityManager.flush();

        Order order = new Order("ORD-2024-000001", customer, OrderStatus.PENDING);

        // Act
        Order saved = orderRepository.save(order);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrderNumber()).isEqualTo("ORD-2024-000001");
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.getCustomer().getName()).isEqualTo("John Doe");
    }

    @Test
    void save_withOrderLines_shouldPersistOrderAndLines() {
        // Arrange
        Customer customer = createTestCustomer("John Doe", "john@example.com");
        Pizza pizza = createTestPizza("Margherita", BigDecimal.valueOf(8.50), "Classic pizza");
        entityManager.persist(customer);
        entityManager.persist(pizza);
        entityManager.flush();

        Order order = new Order("ORD-2024-000001", customer, OrderStatus.PENDING);
        OrderLine line = new OrderLine(pizza, 2);
        order.addOrderLine(line);

        // Act
        Order saved = orderRepository.save(order);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrderLines()).hasSize(1);
        assertThat(saved.getOrderLines().get(0).getPizza().getName()).isEqualTo("Margherita");
        assertThat(saved.getOrderLines().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void deleteById_shouldRemoveOrder() {
        // Arrange
        Customer customer = createTestCustomer("John Doe", "john@example.com");
        entityManager.persist(customer);

        Order order = new Order("ORD-2024-000001", customer, OrderStatus.PENDING);
        entityManager.persist(order);
        entityManager.flush();
        Long orderId = order.getId();

        // Act
        orderRepository.deleteById(orderId);

        // Assert
        Optional<Order> found = orderRepository.findById(orderId);
        assertThat(found).isEmpty();
    }

    @Test
    void count_shouldReturnNumberOfOrders() {
        // Arrange
        Customer customer = createTestCustomer("John Doe", "john@example.com");
        entityManager.persist(customer);

        long initialCount = orderRepository.count();

        Order order1 = new Order("ORD-2024-000001", customer, OrderStatus.PENDING);
        Order order2 = new Order("ORD-2024-000002", customer, OrderStatus.DELIVERED);
        entityManager.persist(order1);
        entityManager.persist(order2);
        entityManager.flush();

        // Act
        long finalCount = orderRepository.count();

        // Assert
        assertThat(finalCount).isEqualTo(initialCount + 2);
    }
}
