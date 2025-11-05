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
import java.util.Optional;

@Service
@Transactional
public class PizzaService {

    private final PizzaRepository pizzaRepository;
    private final PizzaMapper pizzaMapper;

    public PizzaService(PizzaRepository pizzaRepository, PizzaMapper pizzaMapper) {
        this.pizzaRepository = pizzaRepository;
        this.pizzaMapper = pizzaMapper;
    }

    public List<PizzaResponse> findAll() {
        List<Pizza> pizzas = pizzaRepository.findAll();
        return pizzaMapper.toResponseList(pizzas);
    }

    public Optional<PizzaResponse> findById(Long id) {
        return pizzaRepository.findById(id)
                .map(pizzaMapper::toResponse);
    }

    public PizzaResponse create(CreatePizzaRequest request) {
        Pizza pizza = pizzaMapper.toEntity(request);
        Pizza savedPizza = pizzaRepository.save(pizza);
        return pizzaMapper.toResponse(savedPizza);
    }

    public Optional<PizzaResponse> update(Long id, UpdatePizzaRequest request) {
        return pizzaRepository.findById(id)
                .map(pizza -> {
                    pizzaMapper.updateEntity(request, pizza);
                    Pizza updatedPizza = pizzaRepository.save(pizza);
                    return pizzaMapper.toResponse(updatedPizza);
                });
    }

    public boolean delete(Long id) {
        if (pizzaRepository.existsById(id)) {
            pizzaRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
