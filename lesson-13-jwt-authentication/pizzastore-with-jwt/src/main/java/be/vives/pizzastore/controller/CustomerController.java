package be.vives.pizzastore.controller;

import be.vives.pizzastore.dto.request.CreateCustomerRequest;
import be.vives.pizzastore.dto.request.UpdateCustomerRequest;
import be.vives.pizzastore.dto.response.CustomerResponse;
import be.vives.pizzastore.dto.response.OrderResponse;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.security.CurrentUser;
import be.vives.pizzastore.security.IsAdmin;
import be.vives.pizzastore.security.IsAdminOrOwner;
import be.vives.pizzastore.service.CustomerService;
import be.vives.pizzastore.service.OrderService;
import jakarta.validation.Valid;
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
    @IsAdmin
    public ResponseEntity<Page<CustomerResponse>> getAllCustomers(Pageable pageable) {
        log.debug("GET /api/customers");
        Page<CustomerResponse> customers = customerService.findAll(pageable);
        return ResponseEntity.ok(customers);
    }

    // the account of the logged-in user: how a customer finds out their own id after logging in
    @GetMapping("/me")
    public ResponseEntity<CustomerResponse> getCurrentCustomer(@CurrentUser String email) {
        log.debug("GET /api/customers/me - {}", email);
        return ResponseEntity.ok(customerService.findByEmail(email));
    }

    @GetMapping("/{id}")
    @IsAdminOrOwner
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long id) {
        log.debug("GET /api/customers/{}", id);
        CustomerResponse customer = customerService.findById(id);
        return ResponseEntity.ok(customer);
    }

    @GetMapping("/{id}/orders")
    @IsAdminOrOwner
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
    @IsAdminOrOwner
    public ResponseEntity<List<PizzaResponse>> getFavoritePizzas(@PathVariable Long id) {
        log.debug("GET /api/customers/{}/favorites", id);
        List<PizzaResponse> favorites = customerService.findFavoritePizzas(id);
        return ResponseEntity.ok(favorites);
    }

    // customers create their own account with POST /api/auth/register
    @PostMapping
    @IsAdmin
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
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
    @IsAdminOrOwner
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request) {

        log.debug("PUT /api/customers/{} - {}", id, request);

        CustomerResponse updated = customerService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @IsAdminOrOwner
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        log.debug("DELETE /api/customers/{}", id);

        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/favorites/{pizzaId}")
    @IsAdminOrOwner
    public ResponseEntity<Void> addFavoritePizza(
            @PathVariable Long id,
            @PathVariable Long pizzaId) {

        log.debug("POST /api/customers/{}/favorites/{}", id, pizzaId);

        customerService.addFavoritePizza(id, pizzaId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/favorites/{pizzaId}")
    @IsAdminOrOwner
    public ResponseEntity<Void> removeFavoritePizza(
            @PathVariable Long id,
            @PathVariable Long pizzaId) {

        log.debug("DELETE /api/customers/{}/favorites/{}", id, pizzaId);

        customerService.removeFavoritePizza(id, pizzaId);
        return ResponseEntity.noContent().build();
    }
}
