package be.vives.pizzastore.controller;

import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.request.ImportNutritionRequest;
import be.vives.pizzastore.dto.request.UpdatePizzaRequest;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.service.NutritionImportService;
import be.vives.pizzastore.service.PizzaService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class PizzaController {

    private static final Logger log = LoggerFactory.getLogger(PizzaController.class);

    private final PizzaService pizzaService;
    private final NutritionImportService nutritionImportService;

    public PizzaController(PizzaService pizzaService, NutritionImportService nutritionImportService) {
        this.pizzaService = pizzaService;
        this.nutritionImportService = nutritionImportService;
    }

    @GetMapping
    public ResponseEntity<?> getPizzas(
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String name,
            Pageable pageable) {

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
    public ResponseEntity<PizzaResponse> getPizza(@PathVariable Long id) {
        log.debug("GET /api/pizzas/{}", id);
        PizzaResponse pizza = pizzaService.findById(id);
        return ResponseEntity.ok(pizza);
    }

    @PostMapping
    public ResponseEntity<PizzaResponse> createPizza(@Valid @RequestBody CreatePizzaRequest request) {
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
    public ResponseEntity<PizzaResponse> updatePizza(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePizzaRequest request) {

        log.debug("PUT /api/pizzas/{} - {}", id, request);

        PizzaResponse updated = pizzaService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePizza(@PathVariable Long id) {
        log.debug("DELETE /api/pizzas/{}", id);

        pizzaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<PizzaResponse> uploadPizzaImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile file) {
        
        log.debug("POST /api/pizzas/{}/image", id);

        PizzaResponse updated = pizzaService.uploadImage(id, file);
        return ResponseEntity.ok(updated);
    }

    /** Fills the pizza's nutritional info (per 100 g) with the data Open Food Facts has for a barcode. */
    @PostMapping("/{id}/nutritional-info/import")
    public ResponseEntity<PizzaResponse> importNutritionalInfo(
            @PathVariable Long id,
            @Valid @RequestBody ImportNutritionRequest request) {

        log.debug("POST /api/pizzas/{}/nutritional-info/import - {}", id, request);

        PizzaResponse updated = nutritionImportService.importFromBarcode(id, request.barcode());
        return ResponseEntity.ok(updated);
    }
}
