package be.vives.pizzastore.controller;

import be.vives.pizzastore.dto.request.UpdateCustomerRequest;
import be.vives.pizzastore.dto.response.CustomerResponse;
import be.vives.pizzastore.dto.response.OrderResponse;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.security.CurrentUser;
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

import java.util.List;

/**
 * The account of the logged-in user. The customer comes from the JWT, never from the URL or the body,
 * so a customer cannot even ask for somebody else's account. {@link CustomerController} (/api/customers/{id}/...)
 * is for admins only.
 */
@RestController
@RequestMapping("/api/customers/me")
public class CurrentCustomerController {

    private static final Logger log = LoggerFactory.getLogger(CurrentCustomerController.class);

    private final CustomerService customerService;
    private final OrderService orderService;

    public CurrentCustomerController(CustomerService customerService, OrderService orderService) {
        this.customerService = customerService;
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<CustomerResponse> getCurrentCustomer(@CurrentUser String email) {
        log.debug("GET /api/customers/me - {}", email);
        return ResponseEntity.ok(customerService.findByEmail(email));
    }

    @PutMapping
    public ResponseEntity<CustomerResponse> updateCurrentCustomer(
            @CurrentUser String email,
            @Valid @RequestBody UpdateCustomerRequest request) {

        log.debug("PUT /api/customers/me - {} - {}", email, request);
        return ResponseEntity.ok(customerService.update(customerId(email), request));
    }

    @GetMapping("/orders")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @CurrentUser String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.debug("GET /api/customers/me/orders - {}", email);
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        return ResponseEntity.ok(orderService.findByCustomerId(customerId(email), pageable));
    }

    @GetMapping("/favorites")
    public ResponseEntity<List<PizzaResponse>> getMyFavoritePizzas(@CurrentUser String email) {
        log.debug("GET /api/customers/me/favorites - {}", email);
        return ResponseEntity.ok(customerService.findFavoritePizzas(customerId(email)));
    }

    @PostMapping("/favorites/{pizzaId}")
    public ResponseEntity<Void> addFavoritePizza(@CurrentUser String email, @PathVariable Long pizzaId) {
        log.debug("POST /api/customers/me/favorites/{} - {}", pizzaId, email);
        customerService.addFavoritePizza(customerId(email), pizzaId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/favorites/{pizzaId}")
    public ResponseEntity<Void> removeFavoritePizza(@CurrentUser String email, @PathVariable Long pizzaId) {
        log.debug("DELETE /api/customers/me/favorites/{} - {}", pizzaId, email);
        customerService.removeFavoritePizza(customerId(email), pizzaId);
        return ResponseEntity.noContent().build();
    }

    private Long customerId(String email) {
        return customerService.findByEmail(email).id();
    }
}
