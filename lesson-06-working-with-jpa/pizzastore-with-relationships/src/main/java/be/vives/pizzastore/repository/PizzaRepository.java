package be.vives.pizzastore.repository;

import be.vives.pizzastore.domain.Customer;
import be.vives.pizzastore.domain.Pizza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PizzaRepository extends JpaRepository<Pizza, Long> {

    // Derived query methods
    Optional<Pizza> findByName(String name);

    List<Pizza> findByPriceLessThan(BigDecimal maxPrice);

    List<Pizza> findByPriceGreaterThanEqual(BigDecimal minPrice);

    List<Pizza> findByNameContainingIgnoreCase(String keyword);

    List<Pizza> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    // Custom query with JPQL
    @Query("SELECT p FROM Pizza p WHERE p.name LIKE %:keyword% OR p.description LIKE %:keyword%")
    List<Pizza> searchByKeyword(@Param("keyword") String keyword);

    // Query with JOIN FETCH to load nutritional info eagerly
    @Query("SELECT p FROM Pizza p LEFT JOIN FETCH p.nutritionalInfo WHERE p.id = :id")
    Optional<Pizza> findByIdWithNutritionalInfo(@Param("id") Long id);

    @Query("SELECT p FROM Pizza p WHERE p.price < :maxPrice")
    List<Pizza> findCheapPizzas(@Param("maxPrice") BigDecimal maxPrice);

    // JOIN FETCH to avoid N+1 problem (from PizzaStore CustomerRepository)
    @Query("SELECT c FROM Customer c JOIN FETCH c.orders WHERE c.id = :id")
    Optional<Customer> findByIdWithOrders(@Param("id") Long id);

    // Projection (selecting specific fields)
    @Query("SELECT p.name, p.price FROM Pizza p WHERE p.price < :maxPrice")
    List<Object[]> findCheapPizzaNames(@Param("maxPrice") BigDecimal maxPrice);

    // Count query
    @Query("SELECT COUNT(p) FROM Pizza p WHERE p.price > :minPrice")
    long countExpensivePizzas(@Param("minPrice") BigDecimal minPrice);
}
