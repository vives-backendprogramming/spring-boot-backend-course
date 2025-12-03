package be.vives.pizzastore.security;

import be.vives.pizzastore.domain.Customer;
import be.vives.pizzastore.domain.Role;
import be.vives.pizzastore.repository.CustomerRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@Component
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter defaultGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    private final CustomerRepository customerRepository;

    public CustomJwtAuthenticationConverter(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // Extract standard authorities
        Collection<GrantedAuthority> authorities = new HashSet<>(defaultGrantedAuthoritiesConverter.convert(jwt));

        // Get claims from JWT
        String sub = jwt.getSubject();  // OIDC subject identifier
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");

        // Find or create customer
        Customer customer = customerRepository.findBySub(sub)
                .orElseGet(() -> createCustomerFromJwt(sub, email, name));

        // Map role to Spring Security authorities
        authorities.addAll(mapRolesToAuthorities(customer.getRole()));

        return new JwtAuthenticationToken(jwt, authorities, email);
    }

    private Customer createCustomerFromJwt(String sub, String email, String name) {
        Customer customer = new Customer();
        customer.setSub(sub);
        customer.setEmail(email != null ? email : "unknown@pizzastore.com");
        customer.setName(name != null ? name : "Unknown User");
        customer.setRole(Role.CUSTOMER);  // Default role for auto-registered users
        return customerRepository.save(customer);
    }

    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Role role) {
        // Map application Role to Spring Security authorities
        // Spring Security expects "ROLE_" prefix
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
