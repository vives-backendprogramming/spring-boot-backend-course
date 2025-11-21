package be.vives.pizzastore.repository;

import be.vives.pizzastore.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // JOIN FETCH to avoid N+1 problem
    @Query("SELECT u FROM User u JOIN FETCH u.orders WHERE u.id = :id")
    Optional<User> findByIdWithOrders(@Param("id") Long id);

    @Query("SELECT u FROM User u JOIN FETCH u.favoritePizzas WHERE u.id = :id")
    Optional<User> findByIdWithFavorites(@Param("id") Long id);
}
