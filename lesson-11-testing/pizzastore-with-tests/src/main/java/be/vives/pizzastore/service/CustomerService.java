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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final PizzaRepository pizzaRepository;
    private final CustomerMapper customerMapper;
    private final PizzaMapper pizzaMapper;

    public CustomerService(CustomerRepository customerRepository,
                           PizzaRepository pizzaRepository,
                           CustomerMapper customerMapper,
                           PizzaMapper pizzaMapper) {
        this.customerRepository = customerRepository;
        this.pizzaRepository = pizzaRepository;
        this.customerMapper = customerMapper;
        this.pizzaMapper = pizzaMapper;
    }

    public Page<CustomerResponse> findAll(Pageable pageable) {
        log.debug("Finding all customers with pagination: {}", pageable);
        Page<Customer> customerPage = customerRepository.findAll(pageable);
        return customerPage.map(customerMapper::toResponse);
    }

    public CustomerResponse findById(Long id) {
        log.debug("Finding customer with id: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        return customerMapper.toResponse(customer);
    }

    public List<PizzaResponse> findFavoritePizzas(Long customerId) {
        log.debug("Finding favorite pizzas for customer: {}", customerId);
        Customer customer = customerRepository.findByIdWithFavorites(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
        return customer.getFavoritePizzas().stream()
                .map(pizzaMapper::toResponse)
                .toList();
    }

    public CustomerResponse create(CreateCustomerRequest request) {
        log.debug("Creating new customer: {}", request.name());
        Customer customer = customerMapper.toEntity(request);
        Customer savedCustomer = customerRepository.save(customer);
        log.info("Created customer with id: {}", savedCustomer.getId());
        return customerMapper.toResponse(savedCustomer);
    }

    public CustomerResponse update(Long id, UpdateCustomerRequest request) {
        log.debug("Updating customer with id: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        
        customerMapper.updateEntity(request, customer);
        Customer updatedCustomer = customerRepository.save(customer);
        log.info("Updated customer with id: {}", id);
        return customerMapper.toResponse(updatedCustomer);
    }

    public void delete(Long id) {
        log.debug("Deleting customer with id: {}", id);
        if (!customerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Customer", id);
        }
        customerRepository.deleteById(id);
        log.info("Deleted customer with id: {}", id);
    }

    public void addFavoritePizza(Long customerId, Long pizzaId) {
        log.debug("Adding pizza {} to customer {} favorites", pizzaId, customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
        Pizza pizza = pizzaRepository.findById(pizzaId)
                .orElseThrow(() -> new ResourceNotFoundException("Pizza", pizzaId));
        
        customer.addFavoritePizza(pizza);
        customerRepository.save(customer);
        log.info("Added pizza {} to customer {} favorites", pizzaId, customerId);
    }

    public void removeFavoritePizza(Long customerId, Long pizzaId) {
        log.debug("Removing pizza {} from customer {} favorites", pizzaId, customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
        Pizza pizza = pizzaRepository.findById(pizzaId)
                .orElseThrow(() -> new ResourceNotFoundException("Pizza", pizzaId));
        
        customer.removeFavoritePizza(pizza);
        customerRepository.save(customer);
        log.info("Removed pizza {} from customer {} favorites", pizzaId, customerId);
    }
}
