package be.vives.pizzastore.repository;

import be.vives.pizzastore.model.Pizza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PizzaRepository extends JpaRepository<Pizza, Long> {
    
    List<Pizza> findByAvailable(boolean available);
    
    List<Pizza> findByNameContainingIgnoreCase(String name);
}
