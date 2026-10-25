package be.vives.pizzastore.controller;

import be.vives.pizzastore.dto.request.UpdateCustomerRequest;
import be.vives.pizzastore.dto.response.CustomerResponse;
import be.vives.pizzastore.dto.response.OrderResponse;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.security.CurrentUser;
import be.vives.pizzastore.service.CustomerService;
import be.vives.pizzastore.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "My Account", description = "The account, orders and favorite pizzas of the logged-in customer, taken from the JWT token. Requires CUSTOMER or ADMIN role")
@SecurityRequirement(name = "bearerAuth")
public class CurrentCustomerController {

    private static final Logger log = LoggerFactory.getLogger(CurrentCustomerController.class);

    private final CustomerService customerService;
    private final OrderService orderService;

    public CurrentCustomerController(CustomerService customerService, OrderService orderService) {
        this.customerService = customerService;
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(
            summary = "Get my account",
            description = "Retrieves the account that belongs to the JWT token, including its id."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "The logged-in customer",
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid")
    })
    public ResponseEntity<CustomerResponse> getCurrentCustomer(@Parameter(hidden = true) @CurrentUser String email) {
        log.debug("GET /api/customers/me - {}", email);
        return ResponseEntity.ok(customerService.findByEmail(email));
    }

    @PutMapping
    @Operation(
            summary = "Update my account",
            description = "Updates the name, e-mail address, phone and address of the logged-in customer."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Account updated successfully",
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid")
    })
    public ResponseEntity<CustomerResponse> updateCurrentCustomer(
            @Parameter(hidden = true) @CurrentUser String email,
            @Valid @RequestBody UpdateCustomerRequest request) {

        log.debug("PUT /api/customers/me - {} - {}", email, request);
        return ResponseEntity.ok(customerService.update(customerId(email), request));
    }

    @GetMapping("/orders")
    @Operation(
            summary = "Get my orders",
            description = "Retrieves the orders of the logged-in customer, sorted by order date (newest first)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved orders",
                    content = @Content(schema = @Schema(implementation = Page.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid")
    })
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @Parameter(hidden = true) @CurrentUser String email,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        log.debug("GET /api/customers/me/orders - {}", email);
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        return ResponseEntity.ok(orderService.findByCustomerId(customerId(email), pageable));
    }

    @GetMapping("/favorites")
    @Operation(
            summary = "Get my favorite pizzas",
            description = "Retrieves the favorite pizzas of the logged-in customer."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved favorite pizzas"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid")
    })
    public ResponseEntity<List<PizzaResponse>> getMyFavoritePizzas(@Parameter(hidden = true) @CurrentUser String email) {
        log.debug("GET /api/customers/me/favorites - {}", email);
        return ResponseEntity.ok(customerService.findFavoritePizzas(customerId(email)));
    }

    @PostMapping("/favorites/{pizzaId}")
    @Operation(
            summary = "Add pizza to my favorites",
            description = "Adds a pizza to the favorites of the logged-in customer."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pizza added to favorites successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "404", description = "Pizza not found")
    })
    public ResponseEntity<Void> addFavoritePizza(
            @Parameter(hidden = true) @CurrentUser String email,
            @Parameter(description = "Pizza ID", required = true) @PathVariable Long pizzaId) {

        log.debug("POST /api/customers/me/favorites/{} - {}", pizzaId, email);
        customerService.addFavoritePizza(customerId(email), pizzaId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/favorites/{pizzaId}")
    @Operation(
            summary = "Remove pizza from my favorites",
            description = "Removes a pizza from the favorites of the logged-in customer."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Pizza removed from favorites successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "404", description = "Pizza not found")
    })
    public ResponseEntity<Void> removeFavoritePizza(
            @Parameter(hidden = true) @CurrentUser String email,
            @Parameter(description = "Pizza ID", required = true) @PathVariable Long pizzaId) {

        log.debug("DELETE /api/customers/me/favorites/{} - {}", pizzaId, email);
        customerService.removeFavoritePizza(customerId(email), pizzaId);
        return ResponseEntity.noContent().build();
    }

    private Long customerId(String email) {
        return customerService.findByEmail(email).id();
    }
}
