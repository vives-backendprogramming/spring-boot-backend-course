package be.vives.pizzastore.service;

import be.vives.pizzastore.domain.Pizza;
import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.request.UpdatePizzaRequest;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.mapper.PizzaMapper;
import be.vives.pizzastore.repository.PizzaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PizzaService {

    private static final Logger log = LoggerFactory.getLogger(PizzaService.class);

    private final PizzaRepository pizzaRepository;
    private final PizzaMapper pizzaMapper;
    private final FileStorageService fileStorageService;

    public PizzaService(PizzaRepository pizzaRepository, PizzaMapper pizzaMapper, FileStorageService fileStorageService) {
        this.pizzaRepository = pizzaRepository;
        this.pizzaMapper = pizzaMapper;
        this.fileStorageService = fileStorageService;
    }

    public Page<PizzaResponse> findAll(Pageable pageable) {
        log.debug("Finding pizzas with pagination: {}", pageable);
        Page<Pizza> pizzaPage = pizzaRepository.findAll(pageable);
        return pizzaPage.map(pizzaMapper::toResponse);
    }

    public Optional<PizzaResponse> findById(Long id) {
        log.debug("Finding pizza with id: {}", id);
        return pizzaRepository.findById(id)
                .map(pizzaMapper::toResponse);
    }

    public List<PizzaResponse> findByPriceLessThan(BigDecimal maxPrice) {
        log.debug("Finding pizzas with price less than: {}", maxPrice);
        List<Pizza> pizzas = pizzaRepository.findByPriceLessThan(maxPrice);
        return pizzaMapper.toResponseList(pizzas);
    }

    public List<PizzaResponse> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        log.debug("Finding pizzas with price between {} and {}", minPrice, maxPrice);
        List<Pizza> pizzas = pizzaRepository.findByPriceBetween(minPrice, maxPrice);
        return pizzaMapper.toResponseList(pizzas);
    }

    public List<PizzaResponse> findByNameContaining(String name) {
        log.debug("Finding pizzas with name containing: {}", name);
        List<Pizza> pizzas = pizzaRepository.findByNameContainingIgnoreCase(name);
        return pizzaMapper.toResponseList(pizzas);
    }

    public PizzaResponse create(CreatePizzaRequest request) {
        log.debug("Creating new pizza: {}", request.name());
        Pizza pizza = pizzaMapper.toEntity(request);
        
        // Set bidirectional relationship for NutritionalInfo
        if (pizza.getNutritionalInfo() != null) {
            pizza.getNutritionalInfo().setPizza(pizza);
        }
        
        Pizza savedPizza = pizzaRepository.save(pizza);
        log.info("Created pizza with id: {}", savedPizza.getId());
        return pizzaMapper.toResponse(savedPizza);
    }

    public Optional<PizzaResponse> update(Long id, UpdatePizzaRequest request) {
        log.debug("Updating pizza with id: {}", id);
        return pizzaRepository.findById(id)
                .map(pizza -> {
                    pizzaMapper.updateEntity(request, pizza);
                    
                    // Set bidirectional relationship for NutritionalInfo
                    if (pizza.getNutritionalInfo() != null) {
                        pizza.getNutritionalInfo().setPizza(pizza);
                    }
                    
                    Pizza updatedPizza = pizzaRepository.save(pizza);
                    log.info("Updated pizza with id: {}", id);
                    return pizzaMapper.toResponse(updatedPizza);
                });
    }

    public boolean delete(Long id) {
        log.debug("Deleting pizza with id: {}", id);
        if (pizzaRepository.existsById(id)) {
            pizzaRepository.deleteById(id);
            log.info("Deleted pizza with id: {}", id);
            return true;
        }
        log.warn("Pizza with id {} not found for deletion", id);
        return false;
    }

    public Optional<PizzaResponse> uploadImage(Long id, MultipartFile file) {
        log.debug("Uploading image for pizza with id: {}", id);
        return pizzaRepository.findById(id)
                .map(pizza -> {
                    String imageUrl = fileStorageService.storeFile(file, id);
                    pizza.setImageUrl(imageUrl);
                    Pizza updatedPizza = pizzaRepository.save(pizza);
                    log.info("Updated image URL for pizza with id: {}", id);
                    return pizzaMapper.toResponse(updatedPizza);
                });
    }
}
