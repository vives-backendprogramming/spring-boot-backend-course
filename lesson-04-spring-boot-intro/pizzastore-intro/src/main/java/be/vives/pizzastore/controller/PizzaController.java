package be.vives.pizzastore.controller;

import be.vives.pizzastore.dto.PizzaRequest;
import be.vives.pizzastore.model.Pizza;
import be.vives.pizzastore.service.PizzaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pizzas")
public class PizzaController {
    
    private final PizzaService pizzaService;
    
    public PizzaController(PizzaService pizzaService) {
        this.pizzaService = pizzaService;
    }
    
    @GetMapping
    public List<Pizza> getAllPizzas() {
        return pizzaService.getAllPizzas();
    }
    
    @GetMapping("/{id}")
    public Pizza getPizza(@PathVariable Long id) {
        return pizzaService.getPizzaById(id);
    }
    
    @PostMapping
    public ResponseEntity<Pizza> createPizza(@Valid @RequestBody PizzaRequest request) {
        Pizza created = pizzaService.createPizza(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
