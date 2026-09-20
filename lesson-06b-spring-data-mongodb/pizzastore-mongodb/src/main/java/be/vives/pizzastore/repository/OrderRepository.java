package be.vives.pizzastore.repository;

import be.vives.pizzastore.domain.Order;
import be.vives.pizzastore.domain.OrderStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends MongoRepository<Order, String> {

    List<Order> findByCustomerId(String customerId);

    List<Order> findByStatus(OrderStatus status);

    Optional<Order> findByOrderNumber(String orderNumber);

    // No findByIdWithOrderLines() here: orderLines is embedded in the Order document, so a
    // plain findById() already returns them all - there is nothing left to fetch.
}
