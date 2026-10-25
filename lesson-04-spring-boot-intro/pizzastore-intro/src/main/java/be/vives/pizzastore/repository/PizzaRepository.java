package be.vives.pizzastore.repository;

import be.vives.pizzastore.domain.Pizza;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PizzaRepository extends JpaRepository<Pizza, Long> {
}
