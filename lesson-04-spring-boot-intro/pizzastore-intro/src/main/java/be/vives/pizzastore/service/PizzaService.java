package be.vives.pizzastore.service;

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
    
    public List<Pizza> getAvailablePizzas() {
        log.debug("Fetching available pizzas");
        return pizzaRepository.findByAvailable(true);
    }
    
    public Pizza createPizza(Pizza pizza) {
        log.info("Creating pizza: {}", pizza.getName());
        return pizzaRepository.save(pizza);
    }
    
    public Pizza updatePizza(Long id, Pizza pizza) {
        log.info("Updating pizza with id: {}", id);
        Pizza existing = getPizzaById(id);
        pizza.setId(id);
        return pizzaRepository.save(pizza);
    }
    
    public void deletePizza(Long id) {
        log.info("Deleting pizza with id: {}", id);
        pizzaRepository.deleteById(id);
    }
}
