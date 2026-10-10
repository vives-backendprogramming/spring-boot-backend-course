package be.vives.pizzastore.integration;

import be.vives.pizzastore.dto.LoginRequest;
import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;


/**
 * Security end to end: real JWT tokens obtained from /api/auth, sent as "Authorization: Bearer ..."
 * to the real application on a random port. Slice tests with @WithMockUser prove the role rules;
 * this test proves the whole chain (login, token, filter, role check, controller) really works.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:security-integration-test")
@AutoConfigureRestTestClient
class SecurityIntegrationTest {

    @Autowired
    private RestTestClient client;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private TestAccounts.Account admin;
    private TestAccounts.Account customer;

    private final CreatePizzaRequest newPizza =
            new CreatePizzaRequest("Security " + System.nanoTime(), new BigDecimal("9.00"), "desc", true, null);

    @BeforeEach
    void createAccounts() {
        TestAccounts accounts = new TestAccounts(client, customerRepository, passwordEncoder);
        admin = accounts.createAdmin();
        customer = accounts.registerCustomer();
    }

    @Test
    void anonymous_CanReadPizzas() {
        client.get().uri("/api/pizzas")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void anonymous_CannotCreatePizza_Returns401() {
        client.post().uri("/api/pizzas")
                .contentType(MediaType.APPLICATION_JSON)
                .body(newPizza)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void customer_CannotCreatePizza() {
        client.post().uri("/api/pizzas")
                .header(HttpHeaders.AUTHORIZATION, customer.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(newPizza)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void admin_CanCreatePizza() {
        client.post().uri("/api/pizzas")
                .header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(newPizza)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void customer_CannotListAllOrders_ButAdminCan() {
        client.get().uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, customer.bearer())
                .exchange()
                .expectStatus().isForbidden();

        client.get().uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void garbageToken_Returns401() {
        client.get().uri("/api/customers")
                .header(HttpHeaders.AUTHORIZATION, "Bearer this.is.not-a-token")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void login_WithWrongPassword_Returns401() {
        client.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(admin.email(), "wrong-password"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Invalid email or password");
    }

    @Test
    void register_ReturnsTokenThatWorksImmediately() {
        client.get().uri("/api/customers/{id}", customer.id())
                .header(HttpHeaders.AUTHORIZATION, customer.bearer())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.email").isEqualTo(customer.email());
    }
}
