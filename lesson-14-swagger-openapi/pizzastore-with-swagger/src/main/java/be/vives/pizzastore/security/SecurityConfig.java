package be.vives.pizzastore.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;

@Configuration
@EnableWebSecurity
// @PreAuthorize on controller methods: the rules that depend on *which* order is asked for (see OrderSecurity)
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            // Spring MVC's exception resolver: hands an exception to the @ExceptionHandler methods of GlobalExceptionHandler
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) throws Exception {
        http
                // No CSRF protection needed: the token travels in the Authorization header, never in a cookie,
                // so a browser cannot send it along automatically from another site
                .csrf(AbstractHttpConfigurer::disable)
                // Browser front-ends on another origin (see corsConfigurationSource below)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - anyone can access
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/pizzas/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        // Spring Boot's error page: an error outside Spring MVC is rendered by forwarding to /error.
                        // That forward has no JWT, so a protected /error would turn such an error into a 401
                        .requestMatchers("/error").permitAll()

                        // Swagger/OpenAPI endpoints - public access for API documentation
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()

                        // Pizza modification endpoints - require ADMIN role only
                        .requestMatchers(HttpMethod.POST, "/api/pizzas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/pizzas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/pizzas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/pizzas/**").hasRole("ADMIN")

                        // Customer endpoints - the logged-in user's own account (the customer comes from the token),
                        // every other customer endpoint is for ADMIN
                        .requestMatchers("/api/customers/me/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers("/api/customers/**").hasRole("ADMIN")

                        // Order endpoints - POST is for CUSTOMER; reading and cancelling one order is for CUSTOMER
                        // and ADMIN, and @IsAdminOrOrderOwner in OrderController keeps a customer to their own orders;
                        // everything else (all orders, status changes) is for ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/orders").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/orders/*").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/orders/*").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers("/api/orders/**").hasRole("ADMIN")

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // Refused by the URL rules: the same ProblemDetail responses as everywhere else, made by GlobalExceptionHandler.
                // - no (valid) token on a protected endpoint: the entry point, 401 Unauthorized. Without an entry point
                //   Spring Security answers 403 Forbidden, which is meant for "authenticated, but not allowed"
                // - logged in, but the wrong role: the access-denied handler, 403 Forbidden
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                exceptionResolver.resolveException(request, response, null, authException))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                exceptionResolver.resolveException(request, response, null, accessDeniedException))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // For H2 Console
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt for new passwords, stored as "{bcrypt}$2a$10$...": the prefix tells which algorithm made a hash,
        // so the algorithm can be changed later without invalidating the passwords already stored
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${pizzastore.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // the front-end may read the Location header of a 201 Created
        config.setExposedHeaders(List.of("Location"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
