package be.vives.pizzastore.service;

import be.vives.pizzastore.domain.Customer;
import be.vives.pizzastore.domain.Pizza;
import be.vives.pizzastore.dto.request.CreateCustomerRequest;
import be.vives.pizzastore.dto.request.UpdateCustomerRequest;
import be.vives.pizzastore.dto.response.CustomerResponse;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.exception.ResourceNotFoundException;
import be.vives.pizzastore.mapper.CustomerMapper;
import be.vives.pizzastore.mapper.PizzaMapper;
import be.vives.pizzastore.repository.CustomerRepository;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PizzaRepository pizzaRepository;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private PizzaMapper pizzaMapper;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void findAll_shouldReturnPageOfCustomers() {
        // Arrange
        Customer customer1 = new Customer("John Doe", "john@example.com");
        customer1.setPhone("1234567890");
        customer1.setAddress("123 Main St");
        Customer customer2 = new Customer("Jane Smith", "jane@example.com");
        customer2.setPhone("0987654321");
        customer2.setAddress("456 Oak Ave");
        Page<Customer> page = new PageImpl<>(Arrays.asList(customer1, customer2));
        Pageable pageable = PageRequest.of(0, 10);

        CustomerResponse response1 = new CustomerResponse(1L, "John Doe", "john@example.com", "1234567890", "123 Main St", "CUSTOMER");
        CustomerResponse response2 = new CustomerResponse(2L, "Jane Smith", "jane@example.com", "0987654321", "456 Oak Ave", "CUSTOMER");

        when(customerRepository.findAll(pageable)).thenReturn(page);
        when(customerMapper.toResponse(customer1)).thenReturn(response1);
        when(customerMapper.toResponse(customer2)).thenReturn(response2);

        // Act
        Page<CustomerResponse> result = customerService.findAll(pageable);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.getContent().get(0).name()).isEqualTo("John Doe");
        assertThat(result.getContent().get(1).name()).isEqualTo("Jane Smith");
        verify(customerRepository).findAll(pageable);
    }

    @Test
    void findById_whenExists_shouldReturnCustomer() {
        // Arrange
        Customer customer = new Customer("John Doe", "john@example.com");
        CustomerResponse response = new CustomerResponse(1L, "John Doe", "john@example.com", "1234567890", "123 Main St", "CUSTOMER");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerMapper.toResponse(customer)).thenReturn(response);

        // Act
        CustomerResponse result = customerService.findById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("John Doe");
        assertThat(result.email()).isEqualTo("john@example.com");
        verify(customerRepository).findById(1L);
    }

    @Test
    void findById_whenNotExists_shouldThrowException() {
        // Arrange
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customerService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer")
                .hasMessageContaining("999");

        verify(customerRepository).findById(999L);
    }

    @Test
    void findFavoritePizzas_shouldReturnListOfPizzas() {
        // Arrange
        Pizza pizza1 = new Pizza("Margherita", BigDecimal.valueOf(8.50), "Classic pizza");
        Pizza pizza2 = new Pizza("Pepperoni", BigDecimal.valueOf(10.00), "Spicy pizza");
        Customer customer = new Customer("John Doe", "john@example.com");
        customer.addFavoritePizza(pizza1);
        customer.addFavoritePizza(pizza2);

        PizzaResponse pizzaResponse1 = new PizzaResponse(1L, "Margherita", BigDecimal.valueOf(8.50), "Classic pizza", null, true, null);
        PizzaResponse pizzaResponse2 = new PizzaResponse(2L, "Pepperoni", BigDecimal.valueOf(10.00), "Spicy pizza", null, true, null);

        when(customerRepository.findByIdWithFavorites(1L)).thenReturn(Optional.of(customer));
        when(pizzaMapper.toResponse(pizza1)).thenReturn(pizzaResponse1);
        when(pizzaMapper.toResponse(pizza2)).thenReturn(pizzaResponse2);

        // Act
        List<PizzaResponse> result = customerService.findFavoritePizzas(1L);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(PizzaResponse::name)
                .containsExactlyInAnyOrder("Margherita", "Pepperoni");
        verify(customerRepository).findByIdWithFavorites(1L);
    }

    @Test
    void create_shouldSaveAndReturnCustomer() {
        // Arrange
        CreateCustomerRequest request = new CreateCustomerRequest("John Doe", "john@example.com", "password123", "1234567890", "123 Main St");
        Customer customer = new Customer("John Doe", "john@example.com");
        Customer savedCustomer = new Customer("John Doe", "john@example.com");
        CustomerResponse response = new CustomerResponse(1L, "John Doe", "john@example.com", "1234567890", "123 Main St", "CUSTOMER");

        when(customerMapper.toEntity(request)).thenReturn(customer);
        when(customerRepository.save(customer)).thenReturn(savedCustomer);
        when(customerMapper.toResponse(savedCustomer)).thenReturn(response);

        // Act
        CustomerResponse result = customerService.create(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("John Doe");
        assertThat(result.email()).isEqualTo("john@example.com");
        verify(customerRepository).save(customer);
    }

    @Test
    void update_whenExists_shouldUpdateAndReturnCustomer() {
        // Arrange
        UpdateCustomerRequest request = new UpdateCustomerRequest("John Doe Updated", "john@example.com", "9998887777", "456 New St");
        Customer customer = new Customer("John Doe", "john@example.com");
        Customer updatedCustomer = new Customer("John Doe Updated", "john@example.com");
        CustomerResponse response = new CustomerResponse(1L, "John Doe Updated", "john@example.com", "9998887777", "456 New St", "CUSTOMER");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(updatedCustomer);
        when(customerMapper.toResponse(updatedCustomer)).thenReturn(response);

        // Act
        CustomerResponse result = customerService.update(1L, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("John Doe Updated");
        assertThat(result.address()).isEqualTo("456 New St");
        assertThat(result.phone()).isEqualTo("9998887777");
        verify(customerRepository).findById(1L);
        verify(customerMapper).updateEntity(request, customer);
        verify(customerRepository).save(customer);
    }

    @Test
    void update_whenNotExists_shouldThrowException() {
        // Arrange
        UpdateCustomerRequest request = new UpdateCustomerRequest("John Doe", "john@example.com", "1234567890", "123 Main St");
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customerService.update(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer")
                .hasMessageContaining("999");

        verify(customerRepository).findById(999L);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void delete_whenExists_shouldDeleteCustomer() {
        // Arrange
        when(customerRepository.existsById(1L)).thenReturn(true);

        // Act
        customerService.delete(1L);

        // Assert
        verify(customerRepository).existsById(1L);
        verify(customerRepository).deleteById(1L);
    }

    @Test
    void delete_whenNotExists_shouldThrowException() {
        // Arrange
        when(customerRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> customerService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer")
                .hasMessageContaining("999");

        verify(customerRepository).existsById(999L);
        verify(customerRepository, never()).deleteById(any());
    }

    @Test
    void addFavoritePizza_shouldAddPizzaToFavorites() {
        // Arrange
        Customer customer = new Customer("John Doe", "john@example.com");
        Pizza pizza = new Pizza("Margherita", BigDecimal.valueOf(8.50), "Classic pizza");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(pizzaRepository.findById(2L)).thenReturn(Optional.of(pizza));
        when(customerRepository.save(customer)).thenReturn(customer);

        // Act
        customerService.addFavoritePizza(1L, 2L);

        // Assert
        verify(customerRepository).findById(1L);
        verify(pizzaRepository).findById(2L);
        verify(customerRepository).save(customer);
    }

    @Test
    void addFavoritePizza_whenCustomerNotExists_shouldThrowException() {
        // Arrange
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customerService.addFavoritePizza(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer")
                .hasMessageContaining("999");

        verify(customerRepository).findById(999L);
        verify(pizzaRepository, never()).findById(any());
    }

    @Test
    void addFavoritePizza_whenPizzaNotExists_shouldThrowException() {
        // Arrange
        Customer customer = new Customer("John Doe", "john@example.com");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(pizzaRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customerService.addFavoritePizza(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Pizza")
                .hasMessageContaining("999");

        verify(customerRepository).findById(1L);
        verify(pizzaRepository).findById(999L);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void removeFavoritePizza_shouldRemovePizzaFromFavorites() {
        // Arrange
        Pizza pizza = new Pizza("Margherita", BigDecimal.valueOf(8.50), "Classic pizza");
        Customer customer = new Customer("John Doe", "john@example.com");
        customer.addFavoritePizza(pizza);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(pizzaRepository.findById(2L)).thenReturn(Optional.of(pizza));
        when(customerRepository.save(customer)).thenReturn(customer);

        // Act
        customerService.removeFavoritePizza(1L, 2L);

        // Assert
        verify(customerRepository).findById(1L);
        verify(pizzaRepository).findById(2L);
        verify(customerRepository).save(customer);
    }

    @Test
    void removeFavoritePizza_whenCustomerNotExists_shouldThrowException() {
        // Arrange
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customerService.removeFavoritePizza(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer")
                .hasMessageContaining("999");

        verify(customerRepository).findById(999L);
        verify(pizzaRepository, never()).findById(any());
    }
}
