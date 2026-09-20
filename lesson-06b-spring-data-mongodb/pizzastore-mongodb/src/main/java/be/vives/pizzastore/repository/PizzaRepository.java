package be.vives.pizzastore.repository;

import be.vives.pizzastore.domain.Pizza;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PizzaRepository extends MongoRepository<Pizza, String> {

    // Derived query methods, same names as the JPA version - Spring Data MongoDB parses
    // them into BSON query filters instead of JPQL/SQL
    Optional<Pizza> findByName(String name);

    List<Pizza> findByPriceLessThan(BigDecimal maxPrice);

    List<Pizza> findByPriceGreaterThanEqual(BigDecimal minPrice);

    List<Pizza> findByNameContainingIgnoreCase(String keyword);

    List<Pizza> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    // @Query here takes a Mongo JSON query document instead of JPQL. This is the equivalent
    // of the JPA version's "name LIKE %:keyword% OR description LIKE %:keyword%".
    @Query("{ '$or': [ { 'name': { '$regex': ?0, '$options': 'i' } }, { 'description': { '$regex': ?0, '$options': 'i' } } ] }")
    List<Pizza> searchByKeyword(String keyword);

    // No findByIdWithNutritionalInfo() here: nutritionalInfo is embedded in the Pizza
    // document, so a plain findById() already returns it - there is no join to fetch.
}
