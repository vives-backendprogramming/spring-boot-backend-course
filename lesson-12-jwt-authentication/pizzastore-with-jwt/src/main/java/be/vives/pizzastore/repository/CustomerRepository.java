package be.vives.pizzastore.repository;

import be.vives.pizzastore.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmail(String email);

    boolean existsByEmail(String email);

    // JOIN FETCH to avoid N+1 problem
    @Query("SELECT c FROM Customer c JOIN FETCH c.orders WHERE c.id = :id")
    Optional<Customer> findByIdWithOrders(@Param("id") Long id);

    @Query("SELECT c FROM Customer c JOIN FETCH c.favoritePizzas WHERE c.id = :id")
    Optional<Customer> findByIdWithFavorites(@Param("id") Long id);
}
