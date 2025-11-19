package be.vives.pizzastore.service;

import be.vives.pizzastore.domain.Pizza;
import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.request.UpdatePizzaRequest;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.mapper.PizzaMapper;
import be.vives.pizzastore.repository.PizzaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PizzaService {

    private final PizzaRepository pizzaRepository;
    private final PizzaMapper pizzaMapper;

    public PizzaService(PizzaRepository pizzaRepository, PizzaMapper pizzaMapper) {
        this.pizzaRepository = pizzaRepository;
        this.pizzaMapper = pizzaMapper;
    }

    public List<PizzaResponse> getAllPizzas() {
        return pizzaRepository.findAll()
                .stream()
                .map(pizzaMapper::toPizzaResponse)
                .toList();
    }

    public List<PizzaResponse> getAvailablePizzas() {
        return pizzaRepository.findByAvailableTrue()
                .stream()
                .map(pizzaMapper::toPizzaResponse)
                .toList();
    }

    public PizzaResponse getPizzaById(Long id) {
        Pizza pizza = pizzaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pizza not found with id: " + id));
        return pizzaMapper.toPizzaResponse(pizza);
    }

    @Transactional
    public PizzaResponse createPizza(CreatePizzaRequest request) {
        Pizza pizza = pizzaMapper.toEntity(request);
        Pizza savedPizza = pizzaRepository.save(pizza);
        return pizzaMapper.toPizzaResponse(savedPizza);
    }

    @Transactional
    public PizzaResponse updatePizza(Long id, UpdatePizzaRequest request) {
        Pizza pizza = pizzaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pizza not found with id: " + id));
        
        pizzaMapper.updateEntityFromRequest(request, pizza);
        
        Pizza updatedPizza = pizzaRepository.save(pizza);
        return pizzaMapper.toPizzaResponse(updatedPizza);
    }

    @Transactional
    public void deletePizza(Long id) {
        if (!pizzaRepository.existsById(id)) {
            throw new RuntimeException("Pizza not found with id: " + id);
        }
        pizzaRepository.deleteById(id);
    }

    public List<PizzaResponse> searchPizzas(String keyword) {
        return pizzaRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword)
                .stream()
                .map(pizzaMapper::toPizzaResponse)
                .toList();
    }
}
