package be.vives.pizzastore.controller;

import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.request.UpdatePizzaRequest;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.service.PizzaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/pizzas")
public class PizzaController {

    private static final Logger log = LoggerFactory.getLogger(PizzaController.class);

    private final PizzaService pizzaService;

    public PizzaController(PizzaService pizzaService) {
        this.pizzaService = pizzaService;
    }

    @GetMapping
    public ResponseEntity<?> getPizzas(
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        log.debug("GET /api/pizzas - minPrice: {}, maxPrice: {}, name: {}, page: {}, size: {}",
                minPrice, maxPrice, name, page, size);

        // If pagination parameters are provided
        if (page != null && size != null) {
            Sort sort = direction.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<PizzaResponse> pizzaPage = pizzaService.findAll(pageable);
            return ResponseEntity.ok(pizzaPage);
        }

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

        // Default: return all pizzas
        List<PizzaResponse> pizzas = pizzaService.findAll();
        return ResponseEntity.ok(pizzas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PizzaResponse> getPizza(@PathVariable Long id) {
        log.debug("GET /api/pizzas/{}", id);
        return pizzaService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
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
    public ResponseEntity<PizzaResponse> updatePizza(
            @PathVariable Long id,
            @RequestBody UpdatePizzaRequest request) {

        log.debug("PUT /api/pizzas/{} - {}", id, request);

        return pizzaService.update(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePizza(@PathVariable Long id) {
        log.debug("DELETE /api/pizzas/{}", id);

        if (pizzaService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
