package be.vives.pizzastore.security;

import be.vives.pizzastore.repository.OrderRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Instance-level security: is the logged-in user the customer who placed this order?
 * <p>
 * The URL rules in {@link SecurityConfig} only know the role. Whether customer Emma may read order 12 depends on
 * the data, so {@code @PreAuthorize} expressions call this bean by its name: {@code @orderSecurity.isOwner(#id, authentication)}.
 */
@Component("orderSecurity")
public class OrderSecurity {

    private final OrderRepository orderRepository;

    public OrderSecurity(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public boolean isOwner(Long orderId, Authentication authentication) {
        if (orderId == null || authentication == null) {
            return false;
        }
        // the username of a PizzaStore user is the e-mail address (see CustomUserDetailsService);
        // an unknown order is "not yours" too
        return orderRepository.existsByIdAndCustomerEmail(orderId, authentication.getName());
    }
}
