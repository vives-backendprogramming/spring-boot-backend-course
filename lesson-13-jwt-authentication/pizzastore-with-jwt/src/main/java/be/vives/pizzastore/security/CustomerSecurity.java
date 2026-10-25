package be.vives.pizzastore.security;

import be.vives.pizzastore.repository.CustomerRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Instance-level security: is the logged-in user the owner of this customer account?
 * <p>
 * The URL rules in {@link SecurityConfig} only know the role. Whether customer 7 may read customer 12 depends on
 * the data, so {@code @PreAuthorize} expressions call this bean by its name: {@code @customerSecurity.isOwner(#id, authentication)}.
 */
@Component("customerSecurity")
public class CustomerSecurity {

    private final CustomerRepository customerRepository;

    public CustomerSecurity(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public boolean isOwner(Long customerId, Authentication authentication) {
        if (customerId == null || authentication == null) {
            return false;
        }
        // the username of a PizzaStore user is the e-mail address (see CustomUserDetailsService)
        return customerRepository.findById(customerId)
                .map(customer -> customer.getEmail().equals(authentication.getName()))
                .orElse(false);
    }
}
