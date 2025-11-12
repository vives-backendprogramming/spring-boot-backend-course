package be.vives.pizzastore.controller;

import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.request.UpdatePizzaRequest;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.service.PizzaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/pizzas")
public class PizzaController {

    private final PizzaService pizzaService;

    public PizzaController(PizzaService pizzaService) {
        this.pizzaService = pizzaService;
    }

    @GetMapping
    public ResponseEntity<List<PizzaResponse>> getAllPizzas() {
        List<PizzaResponse> pizzas = pizzaService.findAll();
        return ResponseEntity.ok(pizzas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PizzaResponse> getPizza(@PathVariable Long id) {
        return pizzaService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PizzaResponse> createPizza(@Valid @RequestBody CreatePizzaRequest request) {
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

        return pizzaService.update(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePizza(@PathVariable Long id) {
        if (pizzaService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
