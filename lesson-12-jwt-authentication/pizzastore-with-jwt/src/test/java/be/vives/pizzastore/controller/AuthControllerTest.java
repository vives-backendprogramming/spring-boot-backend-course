package be.vives.pizzastore.controller;

import be.vives.pizzastore.domain.Customer;
import be.vives.pizzastore.dto.RegisterRequest;
import be.vives.pizzastore.exception.GlobalExceptionHandler;
import be.vives.pizzastore.repository.CustomerRepository;
import be.vives.pizzastore.security.JwtUtil;
import be.vives.pizzastore.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Optional;

import static org.mockito.Mockito.when;

/**
 * Web-layer slice for the public /api/auth endpoints, with RestTestClient. No user is logged in:
 * /api/auth/** is permitted for everybody in SecurityConfig.
 */
@WebMvcTest(controllers = AuthController.class)
@AutoConfigureRestTestClient
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private RestTestClient client;

    @MockitoBean
    private CustomerRepository customerRepository;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void register_whenEmailAlreadyExists_shouldReturnProblemDetail() {
        when(customerRepository.findByEmail("jane.doe@example.com"))
                .thenReturn(Optional.of(new Customer()));

        client.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest("Jane Doe", "jane.doe@example.com", "password123"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Email already exists");
    }

    @Test
    void register_withInvalidData_shouldReturnFieldErrors() {
        client.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest("J", "not-an-email", "123"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Validation failed")
                .jsonPath("$.errors.length()").isEqualTo(3);
    }
}
