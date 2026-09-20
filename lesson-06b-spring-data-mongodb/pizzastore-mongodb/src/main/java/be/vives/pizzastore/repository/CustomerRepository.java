package be.vives.pizzastore.repository;

import be.vives.pizzastore.domain.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CustomerRepository extends MongoRepository<Customer, String> {

    Optional<Customer> findByEmail(String email);

    boolean existsByEmail(String email);

    // No findByIdWithOrders()/findByIdWithFavorites() here: MongoDB has no JOIN FETCH.
    // favoritePizzaIds is already embedded and comes back with every Customer for free;
    // a customer's orders live in their own collection and are looked up separately with
    // OrderRepository.findByCustomerId(customer.getId()) - see the lesson README.
}
