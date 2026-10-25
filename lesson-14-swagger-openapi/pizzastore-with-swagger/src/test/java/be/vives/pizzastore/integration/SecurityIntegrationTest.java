package be.vives.pizzastore.integration;

import be.vives.pizzastore.dto.LoginRequest;
import be.vives.pizzastore.dto.request.CreateCustomerRequest;
import be.vives.pizzastore.dto.request.CreateOrderRequest;
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
import java.util.List;


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

    // ---------- method security: the owner of an account ----------

    @Test
    void me_ReturnsTheAccountOfTheToken() {
        client.get().uri("/api/customers/me")
                .header(HttpHeaders.AUTHORIZATION, customer.bearer())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(customer.id().intValue())
                .jsonPath("$.email").isEqualTo(customer.email());
    }

    @Test
    void customer_CannotReadAnotherCustomer_ButAdminCan() {
        TestAccounts.Account other = new TestAccounts(client, customerRepository, passwordEncoder).registerCustomer();

        client.get().uri("/api/customers/{id}", other.id())
                .header(HttpHeaders.AUTHORIZATION, customer.bearer())
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Access denied. You do not have permission to access this resource.");

        client.get().uri("/api/customers/{id}", other.id())
                .header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void customer_CannotOrderForAnotherCustomer() {
        TestAccounts.Account other = new TestAccounts(client, customerRepository, passwordEncoder).registerCustomer();

        client.post().uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, customer.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateOrderRequest(other.id(), List.of(new CreateOrderRequest.OrderLineRequest(1L, 1))))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void customerCreatedByAdmin_CanLogIn() {
        // POST /api/customers hashes the password, exactly like /api/auth/register
        String email = TestAccounts.uniqueEmail();
        client.post().uri("/api/customers")
                .header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateCustomerRequest("Created By Admin", email, TestAccounts.PASSWORD, null, null))
                .exchange()
                .expectStatus().isCreated();

        client.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(email, TestAccounts.PASSWORD))
                .exchange()
                .expectStatus().isOk();
    }

    // ---------- CORS ----------

    @Test
    void corsPreflight_FromAllowedOrigin_IsAccepted() {
        // what a browser sends before a cross-origin POST with an Authorization header
        client.options().uri("/api/orders")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000");
    }

    @Test
    void corsPreflight_FromUnknownOrigin_IsRejected() {
        client.options().uri("/api/orders")
                .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
    }
}
