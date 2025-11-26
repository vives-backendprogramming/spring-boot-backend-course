package be.vives.pizzastore.controller;

import be.vives.pizzastore.domain.Customer;
import be.vives.pizzastore.domain.Role;
import be.vives.pizzastore.dto.AuthResponse;
import be.vives.pizzastore.dto.LoginRequest;
import be.vives.pizzastore.dto.RegisterRequest;
import be.vives.pizzastore.exception.PizzaStoreException;
import be.vives.pizzastore.repository.CustomerRepository;
import be.vives.pizzastore.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager,
                          CustomerRepository customerRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (customerRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new PizzaStoreException("Email already exists");
        }

        Customer customer = new Customer();
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPassword(passwordEncoder.encode(request.getPassword()));
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        customer.setRole(Role.CUSTOMER);

        customer = customerRepository.save(customer);

        String token = jwtUtil.generateToken(customer.getEmail(), customer.getRole().name());

        AuthResponse response = new AuthResponse(
                token,
                customer.getEmail(),
                customer.getName(),
                customer.getRole().name()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            Customer customer = customerRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new PizzaStoreException("User not found"));

            String token = jwtUtil.generateToken(customer.getEmail(), customer.getRole().name());

            AuthResponse response = new AuthResponse(
                    token,
                    customer.getEmail(),
                    customer.getName(),
                    customer.getRole().name()
            );

            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            throw new PizzaStoreException("Invalid email or password");
        }
    }
}
