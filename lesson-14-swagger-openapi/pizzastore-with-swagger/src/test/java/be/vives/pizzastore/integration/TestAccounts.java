package be.vives.pizzastore.integration;

import be.vives.pizzastore.domain.Customer;
import be.vives.pizzastore.domain.Role;
import be.vives.pizzastore.dto.AuthResponse;
import be.vives.pizzastore.dto.LoginRequest;
import be.vives.pizzastore.dto.RegisterRequest;
import be.vives.pizzastore.repository.CustomerRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.UUID;

/**
 * Creates real accounts and real JWT tokens for full-stack tests against the running application.
 * <p>
 * A customer registers through the public API, like a real client. There is no API to create an
 * admin (that would be a security hole), so the admin is saved directly through the repository
 * and then logs in through the API as well. Every account gets a unique e-mail address, so tests
 * never collide with each other.
 */
class TestAccounts {

    record Account(Long id, String email, String token) {
        /** Value for the Authorization header. */
        String bearer() {
            return "Bearer " + token;
        }
    }

    static final String PASSWORD = "password123";

    private final RestTestClient client;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    TestAccounts(RestTestClient client, CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.client = client;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    static String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }

    Account registerCustomer() {
        String email = uniqueEmail();
        AuthResponse response = client.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest("Test Customer", email, PASSWORD))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthResponse.class)
                .returnResult().getResponseBody();

        Long id = customerRepository.findByEmail(email).orElseThrow().getId();
        return new Account(id, email, response.getToken());
    }

    Account createAdmin() {
        String email = uniqueEmail();
        Customer admin = new Customer("Test Admin", email);
        admin.setPassword(passwordEncoder.encode(PASSWORD));
        admin.setRole(Role.ADMIN);
        admin = customerRepository.save(admin);

        AuthResponse response = client.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(email, PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult().getResponseBody();

        return new Account(admin.getId(), email, response.getToken());
    }

    static String authorization() {
        return HttpHeaders.AUTHORIZATION;
    }
}
