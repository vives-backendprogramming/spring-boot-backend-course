package be.vives.pizzastore.repository;

import be.vives.pizzastore.domain.Order;
import be.vives.pizzastore.domain.OrderStatus;
import be.vives.pizzastore.repository.projection.PizzaSalesStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerId(Long customerId);

    List<Order> findByStatus(OrderStatus status);

    Optional<Order> findByOrderNumber(String orderNumber);

    // JOIN FETCH to avoid N+1 problem
    @Query("SELECT o FROM Order o JOIN FETCH o.orderLines ol JOIN FETCH ol.pizza WHERE o.id = :id")
    Optional<Order> findByIdWithOrderLines(@Param("id") Long id);

    @Query("SELECT o FROM Order o JOIN FETCH o.customer WHERE o.customer.id = :customerId")
    List<Order> findByCustomerIdWithCustomer(@Param("customerId") Long customerId);

    // DTO projection: the result is a PizzaSalesStatistics record, not an Order/OrderLine entity
    @Query("""
            SELECT new be.vives.pizzastore.repository.projection.PizzaSalesStatistics(
                ol.pizza.name, COUNT(ol), SUM(ol.quantity), SUM(ol.subtotal))
            FROM Order o JOIN o.orderLines ol
            GROUP BY ol.pizza.name
            ORDER BY SUM(ol.quantity) DESC
            """)
    List<PizzaSalesStatistics> findPizzaSalesStatistics();
}
