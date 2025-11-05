package be.vives.pizzastore.service;

import be.vives.pizzastore.domain.Customer;
import be.vives.pizzastore.domain.Pizza;
import be.vives.pizzastore.dto.request.CreateCustomerRequest;
import be.vives.pizzastore.dto.request.UpdateCustomerRequest;
import be.vives.pizzastore.dto.response.CustomerResponse;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.mapper.CustomerMapper;
import be.vives.pizzastore.mapper.PizzaMapper;
import be.vives.pizzastore.repository.CustomerRepository;
import be.vives.pizzastore.repository.PizzaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    public List<CustomerResponse> findAll() {
        log.debug("Finding all customers");
        List<Customer> customers = customerRepository.findAll();
        return customerMapper.toResponseList(customers);
    }

    public Optional<CustomerResponse> findById(Long id) {
        log.debug("Finding customer with id: {}", id);
        return customerRepository.findById(id)
                .map(customerMapper::toResponse);
    }

    public List<PizzaResponse> findFavoritePizzas(Long customerId) {
        log.debug("Finding favorite pizzas for customer: {}", customerId);
        return customerRepository.findByIdWithFavorites(customerId)
                .map(Customer::getFavoritePizzas)
                .map(Set::stream)
                .map(stream -> stream.map(pizzaMapper::toResponse).toList())
                .orElse(List.of());
    }

    public CustomerResponse create(CreateCustomerRequest request) {
        log.debug("Creating new customer: {}", request.name());
        Customer customer = customerMapper.toEntity(request);
        Customer savedCustomer = customerRepository.save(customer);
        log.info("Created customer with id: {}", savedCustomer.getId());
        return customerMapper.toResponse(savedCustomer);
    }

    public Optional<CustomerResponse> update(Long id, UpdateCustomerRequest request) {
        log.debug("Updating customer with id: {}", id);
        return customerRepository.findById(id)
                .map(customer -> {
                    customerMapper.updateEntity(request, customer);
                    Customer updatedCustomer = customerRepository.save(customer);
                    log.info("Updated customer with id: {}", id);
                    return customerMapper.toResponse(updatedCustomer);
                });
    }

    public boolean delete(Long id) {
        log.debug("Deleting customer with id: {}", id);
        if (customerRepository.existsById(id)) {
            customerRepository.deleteById(id);
            log.info("Deleted customer with id: {}", id);
            return true;
        }
        log.warn("Customer with id {} not found for deletion", id);
        return false;
    }

    public boolean addFavoritePizza(Long customerId, Long pizzaId) {
        log.debug("Adding pizza {} to customer {} favorites", pizzaId, customerId);
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        Optional<Pizza> pizzaOpt = pizzaRepository.findById(pizzaId);

        if (customerOpt.isPresent() && pizzaOpt.isPresent()) {
            Customer customer = customerOpt.get();
            Pizza pizza = pizzaOpt.get();
            customer.addFavoritePizza(pizza);
            customerRepository.save(customer);
            log.info("Added pizza {} to customer {} favorites", pizzaId, customerId);
            return true;
        }
        log.warn("Customer {} or Pizza {} not found", customerId, pizzaId);
        return false;
    }

    public boolean removeFavoritePizza(Long customerId, Long pizzaId) {
        log.debug("Removing pizza {} from customer {} favorites", pizzaId, customerId);
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        Optional<Pizza> pizzaOpt = pizzaRepository.findById(pizzaId);

        if (customerOpt.isPresent() && pizzaOpt.isPresent()) {
            Customer customer = customerOpt.get();
            Pizza pizza = pizzaOpt.get();
            customer.removeFavoritePizza(pizza);
            customerRepository.save(customer);
            log.info("Removed pizza {} from customer {} favorites", pizzaId, customerId);
            return true;
        }
        log.warn("Customer {} or Pizza {} not found", customerId, pizzaId);
        return false;
    }
}
