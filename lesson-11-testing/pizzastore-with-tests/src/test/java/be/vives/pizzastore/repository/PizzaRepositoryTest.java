package be.vives.pizzastore.repository;

import be.vives.pizzastore.domain.Pizza;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({be.vives.pizzastore.config.JpaConfig.class, be.vives.pizzastore.config.AuditorAwareImpl.class})  // Enable JPA Auditing and provide auditor
class PizzaRepositoryTest {

    @Autowired
    private PizzaRepository pizzaRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findById_ExistingPizza_ReturnsPizza() {
        // Given
        Pizza pizza = new Pizza("Margherita", new BigDecimal("8.50"), "Classic tomato and mozzarella");
        Pizza saved = entityManager.persistAndFlush(pizza);

        // When
        Optional<Pizza> result = pizzaRepository.findById(saved.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Margherita");
        assertThat(result.get().getPrice()).isEqualByComparingTo(new BigDecimal("8.50"));
    }

    @Test
    void findById_NonExistingPizza_ReturnsEmpty() {
        // When
        Optional<Pizza> result = pizzaRepository.findById(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findByPriceLessThan_MultipleResults_ReturnsFilteredList() {
        // Given
        entityManager.persist(new Pizza("Cheap Pizza", new BigDecimal("5.00"), "Budget option"));
        entityManager.persist(new Pizza("Mid Pizza", new BigDecimal("10.00"), "Medium price"));
        entityManager.persist(new Pizza("Expensive Pizza", new BigDecimal("15.00"), "Premium"));
        entityManager.flush();

        // When
        List<Pizza> result = pizzaRepository.findByPriceLessThan(new BigDecimal("12.00"));

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Pizza::getName)
                .containsExactlyInAnyOrder("Cheap Pizza", "Mid Pizza");
    }

    @Test
    void findByNameContainingIgnoreCase_PartialMatch_ReturnsMatches() {
        // Given
        entityManager.persist(new Pizza("Margherita", new BigDecimal("8.50"), "Classic"));
        entityManager.persist(new Pizza("Marinara", new BigDecimal("7.50"), "Simple"));
        entityManager.persist(new Pizza("Quattro Formaggi", new BigDecimal("11.00"), "Cheese"));
        entityManager.flush();

        // When
        List<Pizza> result = pizzaRepository.findByNameContainingIgnoreCase("mar");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Pizza::getName)
                .containsExactlyInAnyOrder("Margherita", "Marinara");
    }

    @Test
    void save_NewPizza_GeneratesId() {
        // Given
        Pizza pizza = new Pizza("Test Pizza", new BigDecimal("9.99"), "Test description");

        // When
        Pizza saved = pizzaRepository.save(pizza);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isPositive();
    }

    @Test
    void delete_ExistingPizza_RemovesFromDatabase() {
        // Given
        Pizza pizza = entityManager.persistAndFlush(new Pizza("To Delete", new BigDecimal("10.00"), "Will be deleted"));
        Long id = pizza.getId();

        // When
        pizzaRepository.deleteById(id);
        entityManager.flush();

        // Then
        Optional<Pizza> result = pizzaRepository.findById(id);
        assertThat(result).isEmpty();
    }

    @Test
    void findByName_ExistingName_ReturnsPizza() {
        // Given
        entityManager.persist(new Pizza("Margherita", new BigDecimal("8.50"), "Classic"));
        entityManager.flush();

        // When
        Optional<Pizza> result = pizzaRepository.findByName("Margherita");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Margherita");
    }

    @Test
    void findByName_NonExistingName_ReturnsEmpty() {
        // When
        Optional<Pizza> result = pizzaRepository.findByName("NonExistent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findByPriceGreaterThanEqual_MultipleResults_ReturnsFilteredList() {
        // Given
        entityManager.persist(new Pizza("Cheap Pizza", new BigDecimal("5.00"), "Budget"));
        entityManager.persist(new Pizza("Mid Pizza", new BigDecimal("10.00"), "Medium"));
        entityManager.persist(new Pizza("Expensive Pizza", new BigDecimal("15.00"), "Premium"));
        entityManager.flush();

        // When
        List<Pizza> result = pizzaRepository.findByPriceGreaterThanEqual(new BigDecimal("10.00"));

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Pizza::getName)
                .containsExactlyInAnyOrder("Mid Pizza", "Expensive Pizza");
    }

    @Test
    void findByPriceBetween_MultipleResults_ReturnsFilteredList() {
        // Given
        entityManager.persist(new Pizza("Cheap Pizza", new BigDecimal("5.00"), "Budget"));
        entityManager.persist(new Pizza("Mid Pizza", new BigDecimal("10.00"), "Medium"));
        entityManager.persist(new Pizza("Expensive Pizza", new BigDecimal("15.00"), "Premium"));
        entityManager.flush();

        // When
        List<Pizza> result = pizzaRepository.findByPriceBetween(new BigDecimal("8.00"), new BigDecimal("12.00"));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Mid Pizza");
    }

    @Test
    void searchByKeyword_MatchesNameOrDescription_ReturnsMatches() {
        // Given
        entityManager.persist(new Pizza("Margherita", new BigDecimal("8.50"), "Classic tomato"));
        entityManager.persist(new Pizza("Marinara", new BigDecimal("7.50"), "Simple pizza"));
        entityManager.persist(new Pizza("Quattro Formaggi", new BigDecimal("11.00"), "Four cheese special"));
        entityManager.flush();

        // When - search for "cheese"
        List<Pizza> result = pizzaRepository.searchByKeyword("cheese");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Quattro Formaggi");
    }

    @Test
    void searchByKeyword_MatchesName_ReturnsMatches() {
        // Given
        entityManager.persist(new Pizza("Margherita", new BigDecimal("8.50"), "Classic"));
        entityManager.persist(new Pizza("Marinara", new BigDecimal("7.50"), "Simple"));
        entityManager.flush();

        // When - search for part of name
        List<Pizza> result = pizzaRepository.searchByKeyword("Mar");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Pizza::getName)
                .containsExactlyInAnyOrder("Margherita", "Marinara");
    }

    @Test
    void findByIdWithNutritionalInfo_ExistingPizza_ReturnsWithEagerlyLoadedInfo() {
        // Given
        Pizza pizza = new Pizza("Margherita", new BigDecimal("8.50"), "Classic");
        entityManager.persist(pizza);
        entityManager.flush();
        Long id = pizza.getId();

        // When
        Optional<Pizza> result = pizzaRepository.findByIdWithNutritionalInfo(id);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(id);
    }

    @Test
    void findAll_ReturnsAllPizzas() {
        // Given
        entityManager.persist(new Pizza("Pizza 1", new BigDecimal("8.00"), "Desc 1"));
        entityManager.persist(new Pizza("Pizza 2", new BigDecimal("9.00"), "Desc 2"));
        entityManager.persist(new Pizza("Pizza 3", new BigDecimal("10.00"), "Desc 3"));
        entityManager.flush();

        // When
        List<Pizza> result = pizzaRepository.findAll();

        // Then
        assertThat(result).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void existsById_ExistingPizza_ReturnsTrue() {
        // Given
        Pizza pizza = entityManager.persistAndFlush(new Pizza("Test", new BigDecimal("10.00"), "Test"));

        // When
        boolean exists = pizzaRepository.existsById(pizza.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsById_NonExistingPizza_ReturnsFalse() {
        // When
        boolean exists = pizzaRepository.existsById(999L);

        // Then
        assertThat(exists).isFalse();
    }
}
