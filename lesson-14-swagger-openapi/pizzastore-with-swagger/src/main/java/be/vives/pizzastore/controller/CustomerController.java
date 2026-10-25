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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "Customer Management", description = "APIs for managing customers and their favorite pizzas")
@SecurityRequirement(name = "bearerAuth")
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
    @Operation(
            summary = "Get all customers",
            description = """
                    Retrieves a paginated list of all customers. Requires ADMIN role.
                    
                    **Pagination parameters** (query params):
                    - `page`: Page number (0-indexed, default: 0)
                    - `size`: Items per page (default: 20)
                    - `sort`: Sort field and direction (e.g., `id,asc` or `name,desc`)
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved customers",
                    content = @Content(schema = @Schema(implementation = Page.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    public ResponseEntity<Page<CustomerResponse>> getAllCustomers(
            @ParameterObject Pageable pageable) {
        log.debug("GET /api/customers");
        Page<CustomerResponse> customers = customerService.findAll(pageable);
        return ResponseEntity.ok(customers);
    }

    // the account of the logged-in user: how a customer finds out their own id after logging in
    @GetMapping("/me")
    @Operation(
            summary = "Get the logged-in customer",
            description = "Retrieves the account that belongs to the JWT token, including its id. Requires CUSTOMER or ADMIN role."
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

    @GetMapping("/{id}")
    @IsAdminOrOwner
    @Operation(
            summary = "Get customer by ID",
            description = "Retrieves a single customer by their unique identifier. Requires ADMIN role, or CUSTOMER role for your own account."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Customer found",
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not an admin and not your own account"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<CustomerResponse> getCustomer(
            @Parameter(description = "Customer ID", required = true) @PathVariable Long id) {
        log.debug("GET /api/customers/{}", id);
        CustomerResponse customer = customerService.findById(id);
        return ResponseEntity.ok(customer);
    }

    @GetMapping("/{id}/orders")
    @IsAdminOrOwner
    @Operation(
            summary = "Get customer's orders",
            description = "Retrieves all orders for a specific customer, sorted by order date (newest first). Requires ADMIN role, or CUSTOMER role for your own account."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved orders",
                    content = @Content(schema = @Schema(implementation = Page.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not an admin and not your own account"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<Page<OrderResponse>> getCustomerOrders(
            @Parameter(description = "Customer ID", required = true) @PathVariable Long id,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        log.debug("GET /api/customers/{}/orders", id);
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<OrderResponse> orders = orderService.findByCustomerId(id, pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}/favorites")
    @IsAdminOrOwner
    @Operation(
            summary = "Get customer's favorite pizzas",
            description = "Retrieves all favorite pizzas for a specific customer. Requires ADMIN role, or CUSTOMER role for your own account."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved favorite pizzas"
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not an admin and not your own account"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<List<PizzaResponse>> getFavoritePizzas(
            @Parameter(description = "Customer ID", required = true) @PathVariable Long id) {
        log.debug("GET /api/customers/{}/favorites", id);
        List<PizzaResponse> favorites = customerService.findFavoritePizzas(id);
        return ResponseEntity.ok(favorites);
    }

    // customers create their own account with POST /api/auth/register
    @PostMapping
    @IsAdmin
    @Operation(
            summary = "Create a new customer",
            description = "Creates a new customer account (the password is stored as a BCrypt hash). Requires ADMIN role; customers register themselves via /api/auth/register."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Customer created successfully",
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
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
    @Operation(
            summary = "Update a customer",
            description = "Updates an existing customer's information. Requires ADMIN role, or CUSTOMER role for your own account."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Customer updated successfully",
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not an admin and not your own account"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<CustomerResponse> updateCustomer(
            @Parameter(description = "Customer ID", required = true) @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request) {

        log.debug("PUT /api/customers/{} - {}", id, request);

        CustomerResponse updated = customerService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @IsAdminOrOwner
    @Operation(
            summary = "Delete a customer",
            description = "Deletes a customer by their ID. Requires ADMIN role, or CUSTOMER role for your own account."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Customer deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not an admin and not your own account"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<Void> deleteCustomer(
            @Parameter(description = "Customer ID", required = true) @PathVariable Long id) {
        log.debug("DELETE /api/customers/{}", id);

        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/favorites/{pizzaId}")
    @IsAdminOrOwner
    @Operation(
            summary = "Add pizza to favorites",
            description = "Adds a pizza to a customer's favorites list. Requires ADMIN role, or CUSTOMER role for your own account."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pizza added to favorites successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not an admin and not your own account"),
            @ApiResponse(responseCode = "404", description = "Customer or pizza not found")
    })
    public ResponseEntity<Void> addFavoritePizza(
            @Parameter(description = "Customer ID", required = true) @PathVariable Long id,
            @Parameter(description = "Pizza ID", required = true) @PathVariable Long pizzaId) {

        log.debug("POST /api/customers/{}/favorites/{}", id, pizzaId);

        customerService.addFavoritePizza(id, pizzaId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/favorites/{pizzaId}")
    @IsAdminOrOwner
    @Operation(
            summary = "Remove pizza from favorites",
            description = "Removes a pizza from a customer's favorites list. Requires ADMIN role, or CUSTOMER role for your own account."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Pizza removed from favorites successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not an admin and not your own account"),
            @ApiResponse(responseCode = "404", description = "Customer or pizza not found")
    })
    public ResponseEntity<Void> removeFavoritePizza(
            @Parameter(description = "Customer ID", required = true) @PathVariable Long id,
            @Parameter(description = "Pizza ID", required = true) @PathVariable Long pizzaId) {

        log.debug("DELETE /api/customers/{}/favorites/{}", id, pizzaId);

        customerService.removeFavoritePizza(id, pizzaId);
        return ResponseEntity.noContent().build();
    }
}
