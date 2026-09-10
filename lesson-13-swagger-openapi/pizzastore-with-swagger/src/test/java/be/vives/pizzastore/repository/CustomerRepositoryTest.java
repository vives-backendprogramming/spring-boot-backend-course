package be.vives.pizzastore.repository;

import be.vives.pizzastore.domain.Customer;
import be.vives.pizzastore.domain.Pizza;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({be.vives.pizzastore.config.JpaConfig.class, be.vives.pizzastore.config.AuditorAwareImpl.class})
class CustomerRepositoryTest {

    private Customer createTestCustomer(String name, String email) {
        Customer customer = new Customer(name, email);
        customer.setPassword("test123");
        customer.setCreatedAt(java.time.LocalDateTime.now());
        customer.setUpdatedAt(java.time.LocalDateTime.now());
        return customer;
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void findByEmail_whenExists_shouldReturnCustomer() {
        // Arrange
        Customer customer = createTestCustomer("John Doe", "john@example.com");
        customer.setCreatedAt(java.time.LocalDateTime.now());
        customer.setUpdatedAt(java.time.LocalDateTime.now());
        entityManager.persist(customer);
        entityManager.flush();

        // Act
        Optional<Customer> found = customerRepository.findByEmail("john@example.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("John Doe");
        assertThat(found.get().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void findByEmail_whenNotExists_shouldReturnEmpty() {
        // Act
        Optional<Customer> found = customerRepository.findByEmail("nonexistent@example.com");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void existsByEmail_whenExists_shouldReturnTrue() {
        // Arrange
        Customer customer = createTestCustomer("John Doe", "john@example.com");
        entityManager.persist(customer);
        entityManager.flush();

        // Act
        boolean exists = customerRepository.existsByEmail("john@example.com");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_whenNotExists_shouldReturnFalse() {
        // Act
        boolean exists = customerRepository.existsByEmail("nonexistent@example.com");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void findByIdWithOrders_whenExists_shouldReturnCustomerWithOrders() {
        // Arrange
        Customer customer = createTestCustomer("John Doe", "john@example.com");
        entityManager.persist(customer);
        
        // Create an order for the customer (JOIN FETCH requires at least one order)
        be.vives.pizzastore.domain.Order order = new be.vives.pizzastore.domain.Order("ORD-TEST-001", customer, be.vives.pizzastore.domain.OrderStatus.PENDING);
        order.setCreatedAt(java.time.LocalDateTime.now());
        order.setUpdatedAt(java.time.LocalDateTime.now());
        customer.addOrder(order);
        entityManager.persist(order);
        entityManager.flush();

        // Act
        Optional<Customer> found = customerRepository.findByIdWithOrders(customer.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("John Doe");
        assertThat(found.get().getOrders()).isNotNull();
        assertThat(found.get().getOrders()).hasSize(1);
    }

    @Test
    void findByIdWithOrders_whenNotExists_shouldReturnEmpty() {
        // Act
        Optional<Customer> found = customerRepository.findByIdWithOrders(999L);

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void findByIdWithFavorites_whenExists_shouldReturnCustomerWithFavorites() {
        // Arrange
        Customer customer = createTestCustomer("John Doe", "john@example.com");
        Pizza pizza1 = new Pizza("Margherita", BigDecimal.valueOf(8.50), "Classic pizza with tomato and mozzarella");
        Pizza pizza2 = new Pizza("Pepperoni", BigDecimal.valueOf(10.00), "Spicy pepperoni pizza");
        
        entityManager.persist(pizza1);
        entityManager.persist(pizza2);
        entityManager.persist(customer);
        
        customer.addFavoritePizza(pizza1);
        customer.addFavoritePizza(pizza2);
        entityManager.flush();

        // Act
        Optional<Customer> found = customerRepository.findByIdWithFavorites(customer.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("John Doe");
        assertThat(found.get().getFavoritePizzas()).hasSize(2);
        assertThat(found.get().getFavoritePizzas()).extracting(Pizza::getName)
                .containsExactlyInAnyOrder("Margherita", "Pepperoni");
    }

    @Test
    void findByIdWithFavorites_whenNotExists_shouldReturnEmpty() {
        // Act
        Optional<Customer> found = customerRepository.findByIdWithFavorites(999L);

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void save_shouldPersistCustomer() {
        // Arrange
        Customer customer = createTestCustomer("John Doe", "john@example.com");

        // Act
        Customer saved = customerRepository.save(customer);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("John Doe");
        assertThat(saved.getEmail()).isEqualTo("john@example.com");
        // Note: phone and address are null since we only set name and email in constructor
    }

    @Test
    void deleteById_shouldRemoveCustomer() {
        // Arrange
        Customer customer = createTestCustomer("John Doe", "john@example.com");
        entityManager.persist(customer);
        entityManager.flush();
        Long customerId = customer.getId();

        // Act
        customerRepository.deleteById(customerId);

        // Assert
        Optional<Customer> found = customerRepository.findById(customerId);
        assertThat(found).isEmpty();
    }

    @Test
    void findAll_shouldReturnAllCustomers() {
        // Arrange
        Customer customer1 = createTestCustomer("John Doe", "john@example.com");
        Customer customer2 = createTestCustomer("Jane Smith", "jane@example.com");
        entityManager.persist(customer1);
        entityManager.persist(customer2);
        entityManager.flush();

        // Act
        var customers = customerRepository.findAll();

        // Assert
        assertThat(customers).hasSizeGreaterThanOrEqualTo(2);
        assertThat(customers).extracting(Customer::getName)
                .contains("John Doe", "Jane Smith");
    }
}
