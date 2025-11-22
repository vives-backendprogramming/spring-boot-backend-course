package be.vives.pizzastore.controller;

import be.vives.pizzastore.dto.request.CreateCustomerRequest;
import be.vives.pizzastore.dto.request.UpdateCustomerRequest;
import be.vives.pizzastore.dto.response.CustomerResponse;
import be.vives.pizzastore.dto.response.OrderResponse;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.service.CustomerService;
import be.vives.pizzastore.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService customerService;
    private final OrderService orderService;

    public CustomerController(CustomerService customerService, OrderService orderService) {
        this.customerService = customerService;
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<Page<CustomerResponse>> getAllCustomers(Pageable pageable) {
        log.debug("GET /api/customers");
        Page<CustomerResponse> customers = customerService.findAll(pageable);
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long id) {
        log.debug("GET /api/customers/{}", id);
        return customerService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/orders")
    public ResponseEntity<Page<OrderResponse>> getCustomerOrders(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.debug("GET /api/customers/{}/orders", id);
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<OrderResponse> orders = orderService.findByCustomerId(id, pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}/favorites")
    public ResponseEntity<List<PizzaResponse>> getFavoritePizzas(@PathVariable Long id) {
        log.debug("GET /api/customers/{}/favorites", id);
        List<PizzaResponse> favorites = customerService.findFavoritePizzas(id);
        return ResponseEntity.ok(favorites);
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@RequestBody CreateCustomerRequest request) {
        log.debug("POST /api/customers - {}", request);

        CustomerResponse created = customerService.create(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @RequestBody UpdateCustomerRequest request) {

        log.debug("PUT /api/customers/{} - {}", id, request);

        return customerService.update(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        log.debug("DELETE /api/customers/{}", id);

        if (customerService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{customerId}/favorites/{pizzaId}")
    public ResponseEntity<Void> addFavoritePizza(
            @PathVariable Long customerId,
            @PathVariable Long pizzaId) {

        log.debug("POST /api/customers/{}/favorites/{}", customerId, pizzaId);

        if (customerService.addFavoritePizza(customerId, pizzaId)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{customerId}/favorites/{pizzaId}")
    public ResponseEntity<Void> removeFavoritePizza(
            @PathVariable Long customerId,
            @PathVariable Long pizzaId) {

        log.debug("DELETE /api/customers/{}/favorites/{}", customerId, pizzaId);

        if (customerService.removeFavoritePizza(customerId, pizzaId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
