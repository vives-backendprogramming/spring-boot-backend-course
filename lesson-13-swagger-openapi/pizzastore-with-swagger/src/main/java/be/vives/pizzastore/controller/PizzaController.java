package be.vives.pizzastore.controller;

import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.request.UpdatePizzaRequest;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.service.PizzaService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/pizzas")
@Tag(name = "Pizza Management", description = "APIs for managing pizzas (menu items)")
public class PizzaController {

    private static final Logger log = LoggerFactory.getLogger(PizzaController.class);

    private final PizzaService pizzaService;

    public PizzaController(PizzaService pizzaService) {
        this.pizzaService = pizzaService;
    }

    @GetMapping
    @Operation(
            summary = "Get all pizzas",
            description = """
                    Retrieves a list of all available pizzas. Supports filtering by price range and name, and pagination.
                    
                    **Pagination parameters** (query params):
                    - `page`: Page number (0-indexed, default: 0)
                    - `size`: Items per page (default: 20)
                    - `sort`: Sort field and direction (e.g., `name,asc` or `price,desc`)
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved pizzas",
                    content = @Content(schema = @Schema(implementation = Page.class))
            )
    })
    public ResponseEntity<?> getPizzas(
            @Parameter(description = "Minimum price filter") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price filter") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Name filter (case-insensitive partial match)") @RequestParam(required = false) String name,
            @ParameterObject Pageable pageable) {

        log.debug("GET /api/pizzas - minPrice: {}, maxPrice: {}, name: {}, pageable: {}",
                minPrice, maxPrice, name, pageable);

        // If filtering by price range
        if (minPrice != null && maxPrice != null) {
            List<PizzaResponse> pizzas = pizzaService.findByPriceBetween(minPrice, maxPrice);
            return ResponseEntity.ok(pizzas);
        }

        // If filtering by max price
        if (maxPrice != null) {
            List<PizzaResponse> pizzas = pizzaService.findByPriceLessThan(maxPrice);
            return ResponseEntity.ok(pizzas);
        }

        // If filtering by name
        if (name != null) {
            List<PizzaResponse> pizzas = pizzaService.findByNameContaining(name);
            return ResponseEntity.ok(pizzas);
        }

        // Default: return paginated pizzas
        Page<PizzaResponse> pizzaPage = pizzaService.findAll(pageable);
        return ResponseEntity.ok(pizzaPage);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get pizza by ID",
            description = "Retrieves a single pizza by its unique identifier"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Pizza found",
                    content = @Content(schema = @Schema(implementation = PizzaResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Pizza not found")
    })
    public ResponseEntity<PizzaResponse> getPizza(
            @Parameter(description = "Pizza ID", required = true) @PathVariable Long id) {
        log.debug("GET /api/pizzas/{}", id);
        return pizzaService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(
            summary = "Create a new pizza",
            description = "Creates a new pizza. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Pizza created successfully",
                    content = @Content(schema = @Schema(implementation = PizzaResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required")
    })
    public ResponseEntity<PizzaResponse> createPizza(@RequestBody CreatePizzaRequest request) {
        log.debug("POST /api/pizzas - {}", request);

        PizzaResponse created = pizzaService.create(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a pizza",
            description = "Updates an existing pizza. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Pizza updated successfully",
                    content = @Content(schema = @Schema(implementation = PizzaResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required"),
            @ApiResponse(responseCode = "404", description = "Pizza not found")
    })
    public ResponseEntity<PizzaResponse> updatePizza(
            @Parameter(description = "Pizza ID", required = true) @PathVariable Long id,
            @RequestBody UpdatePizzaRequest request) {

        log.debug("PUT /api/pizzas/{} - {}", id, request);

        return pizzaService.update(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a pizza",
            description = "Deletes a pizza by its ID. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Pizza deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required"),
            @ApiResponse(responseCode = "404", description = "Pizza not found")
    })
    public ResponseEntity<Void> deletePizza(
            @Parameter(description = "Pizza ID", required = true) @PathVariable Long id) {
        log.debug("DELETE /api/pizzas/{}", id);

        if (pizzaService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/image")
    @Operation(
            summary = "Upload pizza image",
            description = "Uploads an image for a specific pizza. Requires ADMIN role. Accepts JPEG, PNG, or GIF files up to 5MB.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Image uploaded successfully",
                    content = @Content(schema = @Schema(implementation = PizzaResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid file format or size"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required"),
            @ApiResponse(responseCode = "404", description = "Pizza not found"),
            @ApiResponse(responseCode = "500", description = "Error uploading file")
    })
    public ResponseEntity<PizzaResponse> uploadPizzaImage(
            @Parameter(description = "Pizza ID", required = true) @PathVariable Long id,
            @Parameter(description = "Image file (JPEG, PNG, or GIF, max 5MB)", required = true)
            @RequestParam("image") MultipartFile file) {
        
        log.debug("POST /api/pizzas/{}/image", id);

        try {
            return pizzaService.uploadImage(id, file)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid file upload: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Error uploading file: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
