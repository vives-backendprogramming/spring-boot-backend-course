package be.vives.pizzastore.service;

import be.vives.pizzastore.dto.PizzaRequest;
import be.vives.pizzastore.model.Pizza;
import be.vives.pizzastore.repository.PizzaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PizzaService {
    
    private static final Logger log = LoggerFactory.getLogger(PizzaService.class);
    private final PizzaRepository pizzaRepository;
    
    public PizzaService(PizzaRepository pizzaRepository) {
        this.pizzaRepository = pizzaRepository;
        log.info("PizzaService initialized");
    }
    
    public List<Pizza> getAllPizzas() {
        log.debug("Fetching all pizzas");
        return pizzaRepository.findAll();
    }
    
    public Pizza getPizzaById(Long id) {
        log.debug("Fetching pizza with id: {}", id);
        return pizzaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Pizza not found with id: " + id));
    }
    
    public Pizza createPizza(PizzaRequest request) {
        log.info("Creating pizza: {}", request.name());
        Pizza pizza = new Pizza(request.name(), request.price());
        return pizzaRepository.save(pizza);
    }
}
