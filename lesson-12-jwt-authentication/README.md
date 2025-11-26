# Lesson 12: Securing Web Applications - JWT Authentication

## 📋 Table of Contents
- [Learning Objectives](#-learning-objectives)
- [Introduction to Spring Security](#-introduction-to-spring-security)
- [What is JWT?](#-what-is-jwt)
- [JWT Structure](#-jwt-structure)
- [Adding Dependencies](#-adding-dependencies)
- [Security Configuration](#-security-configuration)
- [JWT Utility Class](#-jwt-utility-class)
- [JWT Authentication Filter](#-jwt-authentication-filter)
- [UserDetailsService Implementation](#-userdetailsservice-implementation)
- [Authentication Controller](#-authentication-controller)
- [Password Encryption](#-password-encryption)
- [Testing the Authentication](#-testing-the-authentication)
- [Role-Based Access Control](#-role-based-access-control)
- [Best Practices](#-best-practices)
- [Summary](#-summary)
- [Runnable Project](#-runnable-project)

---

## 🎯 Learning Objectives

By the end of this lesson, you will be able to:

- ✅ Understand Spring Security fundamentals
- ✅ Implement JWT-based authentication
- ✅ Generate and validate JWT tokens
- ✅ Secure REST endpoints with role-based access control
- ✅ Handle user registration and login
- ✅ Encrypt passwords with BCrypt
- ✅ Configure stateless session management
- ✅ Test secured endpoints

---

## 🔐 Introduction to Spring Security

**Spring Security** is a powerful and highly customizable authentication and access-control framework. It provides:

- **Authentication**: Verifying the identity of users
- **Authorization**: Determining what authenticated users can access
- **Protection**: Against common vulnerabilities (CSRF, XSS, etc.)

### How Spring Security Works: The Filter Chain Architecture

Spring Security is built around a **chain of servlet filters** that intercept incoming HTTP requests **before** they reach your controllers. This filter-based architecture is the foundation of all Spring Security features.

#### The Security Filter Chain

When a request arrives, it passes through the **Spring Security Filter Chain** (implemented by `FilterChainProxy`), which contains multiple specialized filters that each handle a specific security concern:

```
HTTP Request
    ↓
[Spring Security Filter Chain]
    ↓
┌─────────────────────────────────────┐
│ 1. SecurityContextPersistenceFilter │ ← Loads/saves SecurityContext
├─────────────────────────────────────┤
│ 2. CorsFilter                       │ ← Handles CORS preflight
├─────────────────────────────────────┤
│ 3. CsrfFilter                       │ ← CSRF protection (disabled for stateless APIs)
├─────────────────────────────────────┤
│ 4. LogoutFilter                     │ ← Handles logout requests
├─────────────────────────────────────┤
│ 5. JwtAuthenticationFilter          │ ← 🔑 OUR CUSTOM JWT FILTER (extracts & validates token)
├─────────────────────────────────────┤
│ 6. UsernamePasswordAuthenticationFilter │ ← Login form processing
├─────────────────────────────────────┤
│ 7. AnonymousAuthenticationFilter    │ ← Sets anonymous authentication
├─────────────────────────────────────┤
│ 8. ExceptionTranslationFilter       │ ← Handles security exceptions
├─────────────────────────────────────┤
│ 9. AuthorizationFilter              │ ← Checks access permissions
└─────────────────────────────────────┘
    ↓
Your Controller
```

**Key Concepts:**

1. **Filter Chain Execution**: Filters execute in a specific order. Each filter can:
   - Process the request and pass it to the next filter
   - Short-circuit the chain (reject/redirect the request)
   - Add information to the `SecurityContext`

2. **SecurityContext**: A thread-local storage where authentication information is kept. Once a filter (like our JWT filter) authenticates a user, it stores the `Authentication` object in the `SecurityContext`.

3. **Custom JWT Filter**: For JWT authentication, we add our own filter (`JwtAuthenticationFilter`) to:
   - Extract the JWT token from the `Authorization` header
   - Validate the token (signature, expiration)
   - Load user details
   - Set the authentication in the `SecurityContext`

4. **AuthorizationFilter**: The final filter checks if the authenticated user (from `SecurityContext`) has permission to access the requested endpoint based on roles and authorities.

#### Multiple Security Filter Chains

Spring Security allows multiple filter chains for different URL patterns. For example:

```java
@Bean
@Order(1)
public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
    http
        .securityMatcher("/api/**")  // This chain only applies to /api/**
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/api/pizzas").permitAll()
            .requestMatchers("/api/orders/**").hasRole("CUSTOMER")
            .anyRequest().hasRole("ADMIN")
        )
        .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    return http.build();
}
```

The `@Order` annotation determines which filter chain is evaluated first when multiple chains exist.

#### JWT Authentication Flow in the Filter Chain

For JWT-based authentication, the flow works as follows:

```
1. Request arrives with: Authorization: Bearer <JWT_TOKEN>
                             ↓
2. JwtAuthenticationFilter intercepts:
   - Extracts token from header
   - Validates token signature
   - Checks expiration
   - Extracts username from token
                             ↓
3. Loads user details from database:
   - UserDetailsService.loadUserByUsername(username)
   - Returns user with roles/authorities
                             ↓
4. Creates Authentication object:
   - UsernamePasswordAuthenticationToken
   - Sets principal, credentials, authorities
                             ↓
5. Stores in SecurityContext:
   - SecurityContextHolder.getContext().setAuthentication(auth)
                             ↓
6. Request continues to AuthorizationFilter:
   - Checks if user has required role
   - Allows or denies access
                             ↓
7. Request reaches Controller (if authorized)
```

**Important:** Once the authentication is set in the `SecurityContext`, it's available throughout the entire request lifecycle. Your controllers can access it via:

```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String username = auth.getName();
```

Or using annotations:

```java
@GetMapping("/me")
public CustomerResponse getCurrentUser(@AuthenticationPrincipal UserDetails user) {
    // user is automatically injected by Spring Security
    return customerService.findByEmail(user.getUsername());
}
```

### Why JWT for REST APIs?

Traditional session-based authentication stores session data on the server. For REST APIs, especially those consumed by mobile apps, **stateless authentication** is preferred:

✅ **Scalability**: No server-side session storage  
✅ **Mobile-friendly**: Easy to store and send tokens  
✅ **Microservices-ready**: Tokens can be validated independently  
✅ **Filter-based security**: Integrates seamlessly with Spring Security's filter chain  
✅ **Stateless**: Each request is self-contained with the JWT token  

---

## 🎫 What is JWT?

**JWT (JSON Web Token)** is an open standard (RFC 7519) for securely transmitting information between parties as a JSON object.

### Key Characteristics

- **Self-contained**: Contains all necessary user information
- **Compact**: Small size, easily transmitted via URL, POST parameter, or HTTP header
- **Digitally signed**: Ensures the token hasn't been tampered with

### When to Use JWT?

1. **Authorization**: The most common scenario. Once logged in, each request includes the JWT for accessing protected resources.
2. **Information Exchange**: Securely transmit information between parties.

---

## 🧱 JWT Structure

A JWT consists of three parts separated by dots (`.`):

```
xxxxx.yyyyy.zzzzz
```

### 1. Header

Contains the token type and hashing algorithm:

```json
{
  "alg": "HS384",
  "typ": "JWT"
}
```

### 2. Payload

Contains the claims (user data):

```json
{
  "sub": "emma.johnson@example.com",
  "role": "CUSTOMER",
  "iat": 1764104932,
  "exp": 1764191332
}
```

**Standard Claims:**
- `sub` (subject): User identifier
- `iat` (issued at): Token creation timestamp
- `exp` (expiration): Token expiration timestamp

### 3. Signature

Ensures the token hasn't been altered:

```
HMACSHA384(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret
)
```

### Example JWT

```
eyJhbGciOiJIUzM4NCJ9.eyJyb2xlIjoiQ1VTVE9NRVIiLCJzdWIiOiJlbW1hLmpvaG5zb25AZXhhbXBsZS5jb20iLCJpYXQiOjE3NjQxMDQ5MzIsImV4cCI6MTc2NDE5MTMzMn0.uc52zj3RxRgCnCjZp85lStYFGfyTxtINuaO7YHQrkxJ762RR0c-UkF3KECbFFkZM
```

You can always decode and inspect a JWT token using [jwt\.io](https://jwt.io/) — just paste your token to see its header and payload contents.

**Security of JWT Tokens on Devices**

A JWT token is much more secure to store on a device than a password because:

- **No Sensitive Credentials**: JWT tokens do not contain the user's password or sensitive authentication data. They only encode claims (like user ID, role, etc.) and are signed to prevent tampering.
- **Limited Lifetime**: Tokens have an expiration (`exp` claim), so even if stolen, they are only valid for a short period.
- **Stateless Authentication**: The server does not need to store session data, reducing attack surface.
- **Revocation and Rotation**: You can implement token blacklists or rotate tokens for extra security.
- **Password Risks**: Storing a password on a device risks exposure if the device is compromised. Attackers could use the password to log in anywhere, change account details, or escalate privileges.
- **Token Storage Best Practices**: JWT tokens should be stored in secure device storage (Keychain on iOS, Keystore on Android) or in `httpOnly` cookies for web apps, making them less accessible to malicious apps or scripts.

**Summary**: JWT tokens are designed for secure, temporary authentication. Storing passwords on a device is never recommended, as it exposes users to credential theft and account compromise.

---

## 📦 Adding Dependencies

Update your `pom.xml` to include Spring Security and JWT dependencies:

```xml
<properties>
    <java.version>25</java.version>
    <org.mapstruct.version>1.5.5.Final</org.mapstruct.version>
    <jjwt.version>0.13.0</jjwt.version>
</properties>

<dependencies>
    <!-- Spring Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- JWT Dependencies -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>${jjwt.version}</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>${jjwt.version}</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>${jjwt.version}</version>
        <scope>runtime</scope>
    </dependency>

    <!-- Spring Security Test -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## ⚙️ Security Configuration

Create a `SecurityConfig` class to configure Spring Security:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, 
                          UserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - anyone can access
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/pizzas/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        
                        // Pizza modification endpoints - require ADMIN role only
                        .requestMatchers(HttpMethod.POST, "/api/pizzas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/pizzas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/pizzas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/pizzas/**").hasRole("ADMIN")
                        
                        // Customer endpoints - accessible by CUSTOMER and ADMIN
                        .requestMatchers("/api/customers/**").hasAnyRole("CUSTOMER", "ADMIN")
                        
                        // Order endpoints - require CUSTOMER or ADMIN role
                        .requestMatchers("/api/orders/**").hasAnyRole("CUSTOMER", "ADMIN")
                        
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // For H2 Console
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) 
            throws Exception {
        return config.getAuthenticationManager();
    }
}
```

### 🔍 Understanding @EnableWebSecurity

The `@EnableWebSecurity` annotation is a crucial annotation that **activates Spring Security** for your application. Here's what it does:

**Purpose:**
- Enables Spring Security's web security support
- Activates the `@Configuration` class for Spring Security
- Allows you to define custom security configurations via `SecurityFilterChain` beans

**What happens when you use @EnableWebSecurity:**

1. **Imports Security Configuration**: Automatically imports Spring Security's default configuration classes
2. **Enables WebSecurity**: Activates the `SecurityFilterChain` beans
3. **Filter Chain Registration**: Registers the Spring Security filter chain (`springSecurityFilterChain`) as a servlet filter
4. **Custom Configuration Support**: Allows you to override default security behavior by defining beans like `SecurityFilterChain`, `AuthenticationManager`, etc.

**When to use:**
- ✅ Always use when creating a custom security configuration class
- ✅ Required when you want to override default Spring Security behavior
- ✅ Needed to define custom authentication and authorization rules

**Note:** If you don't use `@EnableWebSecurity`, Spring Boot will still auto-configure basic security (via `SecurityAutoConfiguration`), but you won't be able to customize it.

---

### Key Points:

1. **CSRF Disabled**: Not needed for stateless JWT authentication
2. **Public Endpoints**: 
   - `/api/auth/**`: Registration and login
   - `GET /api/pizzas/**`: Anonymous users can view pizzas (read-only)
   - `/h2-console/**`: H2 database console (development only)
3. **Role-Based Access**:
   - **CUSTOMER**: Can access all `/api/customers/**` endpoints (view, manage favorites) and `/api/orders/**` endpoints (create and view orders)
   - **ADMIN**: Has CUSTOMER privileges + can modify pizzas (POST, PUT, PATCH, DELETE on `/api/pizzas/**`)
4. **Stateless Sessions**: No session storage on the server
5. **JWT Filter**: Added before Spring Security's authentication filter

---

## 🔧 JWT Utility Class

Create a utility class to handle JWT operations:

```java
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
```

### Configuration Properties

Add to `application.properties`:

```properties
# JWT Configuration
jwt.secret=MySecretKeyForJWTTokenGenerationThatShouldBeAtLeast256BitsLong
jwt.expiration=86400000
```

- `jwt.secret`: Secret key for signing tokens (must be at least 256 bits for HS256)
- `jwt.expiration`: Token validity in milliseconds (86400000 = 24 hours)

⚠️ **Security Note**: In production, store the secret in environment variables, not in property files!

---

## 🔒 JWT Authentication Filter

Create a filter to intercept requests and validate JWT tokens:

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            username = jwtUtil.extractUsername(jwt);
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

### How It Works:

1. Extracts the JWT from the `Authorization` header
2. Validates the token
3. If valid, creates an authentication object and stores it in the `SecurityContext`
4. Continues the filter chain

---

## 👤 UserDetailsService Implementation

### Understanding UserDetailsService

**`UserDetailsService`** is a core Spring Security interface responsible for **retrieving user information** during the authentication process. It has a single method:

```java
UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
```

**Key Responsibilities:**
- Load user data from your data source (database, LDAP, external API, etc.)
- Convert your domain user entity into Spring Security's `UserDetails` object
- Throw `UsernameNotFoundException` if the user doesn't exist

**The Flow:**
1. User attempts to log in with credentials (email/password)
2. Spring Security calls `loadUserByUsername(email)` on your `UserDetailsService` implementation
3. Your implementation queries the database and retrieves the user
4. Your implementation converts the user entity to a `UserDetails` object
5. Spring Security compares the provided password with the stored (encrypted) password
6. If valid, authentication succeeds and a JWT token is generated

### Storing User Information in the Database

In our PizzaStore application, **we store user credentials directly in our own database** using the `Customer` entity:

```java
@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;  // Used as username for authentication

    @Column(nullable = false)
    private String password;  // BCrypt-encrypted password

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.CUSTOMER;  // CUSTOMER or ADMIN

    // ... other fields (phone, address, orders, etc.)
}
```

**Why Store User Data Ourselves?**
- Full control over user data and schema
- Can easily extend with business-specific fields (address, phone, orders)
- Passwords are stored **encrypted** using BCrypt (never plain text!)
- Simple to implement for applications with their own user base
- No dependency on external authentication providers (unlike OAuth2/OpenID Connect)

**Security Considerations:**
- **Password encryption**: Always use `BCryptPasswordEncoder` to hash passwords before storing
- **Unique email constraint**: Prevents duplicate accounts
- **Role-based access**: The `Role` enum allows for authorization (CUSTOMER vs ADMIN)
- **Never expose passwords**: DTOs should never include the password field in responses

### Custom UserDetailsService Implementation

Implement Spring Security's `UserDetailsService` to load user data from the database:

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;

    public CustomUserDetailsService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new User(
                customer.getEmail(),
                customer.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + customer.getRole().name()))
        );
    }
}
```

**Implementation Details:**

1. **`@Service`**: Makes this a Spring-managed bean that can be injected into the security configuration

2. **`loadUserByUsername(String email)`**:
   - We use **email as the username** (more user-friendly than a separate username field)
   - Queries the database via `CustomerRepository.findByEmail()`
   - Throws `UsernameNotFoundException` if the user doesn't exist

3. **Return `UserDetails` object**:
   - Uses Spring Security's `User` class (implements `UserDetails`)
   - **Username**: The customer's email
   - **Password**: The BCrypt-encrypted password from the database
   - **Authorities**: A list of granted authorities (roles) for authorization

4. **Role Prefix**: Spring Security expects role names to be prefixed with `ROLE_`. So:
   - `Role.CUSTOMER` → `ROLE_CUSTOMER`
   - `Role.ADMIN` → `ROLE_ADMIN`

**Repository Method:**

The `CustomerRepository` needs a custom query method:

```java
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);
}
```

This method is used by the `UserDetailsService` to retrieve the user by email during authentication.

---

## 🎮 Authentication Controller

Create endpoints for user registration and login:

### DTOs

**LoginRequest:**
```java
public class LoginRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
    
    // Getters and setters
}
```

**RegisterRequest:**
```java
public class RegisterRequest {
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100)
    private String password;

    private String phone;
    private String address;
    
    // Getters and setters
}
```

**AuthResponse:**
```java
public class AuthResponse {
    private String token;
    private String email;
    private String name;
    private String role;
    
    // Constructor, getters and setters
}
```

### Controller Implementation

```java
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
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(), 
                            request.getPassword())
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
```

---

## 🔐 Password Encryption

Always store hashed passwords, never plain text!

### BCrypt Encoding

BCrypt is a password hashing function with a built-in salt:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

### Sample Data with BCrypt

```sql
-- All passwords are "password123" encoded with BCrypt
INSERT INTO customers (name, email, password, phone, address, role, created_at, updated_at) VALUES
('Emma Johnson', 'emma.johnson@example.com', 
 '$2a$10$wvw30spxLOR1gV/NYh86ruw8J1rPa8MvkZwG0ru7VuRECMfARo0ri', 
 '+32 470 12 34 56', 'Rue de la Loi 123, 1000 Brussels', 'CUSTOMER', 
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Admin User', 'admin@pizzastore.be', 
 '$2a$10$wvw30spxLOR1gV/NYh86ruw8J1rPa8MvkZwG0ru7VuRECMfARo0ri', 
 '+32 475 67 89 01', 'Headquarters, 1000 Brussels', 'ADMIN', 
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

---

## 🧪 Testing the Authentication

### 1. Register a New User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john.doe@example.com",
    "password": "password123",
    "phone": "+32 476 12 34 56",
    "address": "Test Street 1, Brussels"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "email": "john.doe@example.com",
  "name": "John Doe",
  "role": "CUSTOMER"
}
```

### 2. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "emma.johnson@example.com",
    "password": "password123"
  }'
```

### 3. Access Public Endpoint (No Auth)

```bash
curl http://localhost:8080/api/pizzas
```

✅ **Works** - Public endpoint

### 4. Try to Create Pizza Without Auth

```bash
curl -X POST http://localhost:8080/api/pizzas \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Unauthorized Pizza",
    "price": 9.99
  }'
```

❌ **403 Forbidden** - Authentication required

### 5. Create Order with Customer Token

```bash
TOKEN="eyJhbGciOiJIUzM4NCJ9..."  # From login

curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "customerId": 1,
    "orderLines": [
      {"pizzaId": 1, "quantity": 2}
    ]
  }'
```

✅ **201 Created** - Customer can create orders

### 6. Try to Create Pizza with Customer Token

```bash
curl -X POST http://localhost:8080/api/pizzas \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{
    "name": "Customer Pizza",
    "price": 9.99
  }'
```

❌ **403 Forbidden** - Requires ADMIN role

### 7. Create Pizza with Admin Token

```bash
# Login as admin first
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@pizzastore.be",
    "password": "password123"
  }' | jq -r '.token')

# Create pizza
curl -X POST http://localhost:8080/api/pizzas \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "Admin Special",
    "description": "Created by admin",
    "price": 14.99,
    "available": true,
    "nutritionalInfo": {
      "calories": 300,
      "protein": 15,
      "carbohydrates": 40,
      "fat": 10
    }
  }'
```

✅ **201 Created** - Admin can create pizzas

---

## 🎭 Role-Based Access Control

### Access Control Summary

| Endpoint | Method | Anonymous | CUSTOMER | ADMIN |
|----------|--------|-----------|----------|-------|
| `/api/auth/**` | ALL | ✅ | ✅ | ✅ |
| `/api/pizzas/**` | GET | ✅ | ✅ | ✅ |
| `/api/pizzas/**` | POST/PUT/PATCH/DELETE | ❌ | ❌ | ✅ |
| `/api/customers/**` | ALL | ❌ | ✅ | ✅ |
| `/api/orders/**` | ALL | ❌ | ✅ | ✅ |

### Implementation Details

**SecurityConfig.java:**
```java
.authorizeHttpRequests(auth -> auth
    // Public endpoints - anyone can access
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/pizzas/**").permitAll()
    .requestMatchers("/h2-console/**").permitAll()
    
    // Pizza modification endpoints - require ADMIN role only
    .requestMatchers(HttpMethod.POST, "/api/pizzas/**").hasRole("ADMIN")
    .requestMatchers(HttpMethod.PUT, "/api/pizzas/**").hasRole("ADMIN")
    .requestMatchers(HttpMethod.PATCH, "/api/pizzas/**").hasRole("ADMIN")
    .requestMatchers(HttpMethod.DELETE, "/api/pizzas/**").hasRole("ADMIN")
    
    // Customer endpoints - accessible by CUSTOMER and ADMIN
    .requestMatchers("/api/customers/**").hasAnyRole("CUSTOMER", "ADMIN")
    
    // Order endpoints - require CUSTOMER or ADMIN role
    .requestMatchers("/api/orders/**").hasAnyRole("CUSTOMER", "ADMIN")
    
    // All other requests require authentication
    .anyRequest().authenticated()
)
```

---

## ✨ Best Practices

### 1. Secure Secret Key Storage

❌ **Don't:**
```properties
jwt.secret=mysecret
```

✅ **Do:**
```bash
export JWT_SECRET="your-very-long-secret-key"
```

```properties
jwt.secret=${JWT_SECRET}
```

### 2. Token Expiration

- **Short-lived tokens**: 15-30 minutes for high-security apps
- **Refresh tokens**: Longer-lived (days/weeks) to get new access tokens
- **Balance security and UX**: 24 hours is reasonable for mobile apps

### 3. HTTPS in Production

Always use HTTPS in production to prevent token interception.

### 4. Token Storage (Client-Side)

**Mobile Apps**: Secure storage (Keychain on iOS, KeyStore on Android)  
**Web Apps**: `httpOnly` cookies or secure local storage

### 5. Logout Implementation

Since JWT is stateless, implement logout by:
- Removing the token from client storage
- Optional: Token blacklist for sensitive applications

### 6. Password Requirements

```java
@NotBlank(message = "Password is required")
@Size(min = 8, message = "Password must be at least 8 characters")
@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", 
         message = "Password must contain uppercase, lowercase, and digit")
private String password;
```

### 7. Keep Controllers Clean - Centralized Security Configuration

**⚠️ Important Architectural Principle**

One of the most important best practices in Spring Security is to **keep security configuration separate from your controllers**. This follows the **Separation of Concerns** principle and makes your application more maintainable.

#### ❌ Don't: Security Annotations in Controllers

```java
@RestController
@RequestMapping("/api/pizzas")
public class PizzaController {
    
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<PizzaResponse> createPizza(@RequestBody PizzaRequest request) {
        // ...
    }
    
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @GetMapping("/{id}")
    public ResponseEntity<PizzaResponse> getPizza(@PathVariable Long id) {
        // ...
    }
}
```

**Problems with this approach:**
- 🔴 Security rules scattered across multiple controller classes
- 🔴 Hard to maintain and audit all security rules
- 🔴 Easy to forget adding security to new endpoints
- 🔴 Controllers become cluttered with security concerns
- 🔴 Difficult to test controllers in isolation

#### ✅ Do: Centralized Security Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/pizzas/**").permitAll()
                
                // Admin-only endpoints
                .requestMatchers(HttpMethod.POST, "/api/pizzas").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/pizzas/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/pizzas/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/pizzas/*/image").hasRole("ADMIN")
                
                // Customer or Admin endpoints
                .requestMatchers("/api/orders/**").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers("/api/customers/me").hasAnyRole("CUSTOMER", "ADMIN")
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

**Benefits of centralized configuration:**
- ✅ **Single source of truth**: All security rules in one place
- ✅ **Easy to audit**: Quick overview of who can access what
- ✅ **Maintainable**: Changes to security rules happen in one location
- ✅ **Clean controllers**: Controllers focus on business logic only
- ✅ **Better testing**: Controllers can be tested without security context
- ✅ **Consistency**: Ensures uniform security policy across the application

#### Clean Controller Example

```java
@RestController
@RequestMapping("/api/pizzas")
@RequiredArgsConstructor
public class PizzaController {
    
    private final PizzaService pizzaService;
    
    // No security annotations needed!
    // Security is configured in SecurityConfig
    
    @PostMapping
    public ResponseEntity<PizzaResponse> createPizza(@Valid @RequestBody PizzaRequest request) {
        PizzaResponse pizza = pizzaService.createPizza(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(pizza);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PizzaResponse> getPizza(@PathVariable Long id) {
        return pizzaService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
```

#### When to Use @PreAuthorize

While centralized configuration is preferred, `@PreAuthorize` can be useful for:

1. **Dynamic, method-level security** based on method parameters:
```java
@PreAuthorize("#userId == authentication.principal.id or hasRole('ADMIN')")
public Order getOrder(Long orderId, Long userId) { ... }
```

2. **Complex SpEL expressions** that can't be expressed in URL patterns:
```java
@PreAuthorize("@orderSecurity.canAccessOrder(#orderId)")
public Order getOrder(Long orderId) { ... }
```

For most REST APIs with standard URL-based security, **centralized configuration in SecurityConfig is the better choice**.

---

## 📝 Summary

In this lesson, you learned:

✅ **Spring Security fundamentals** - Authentication and authorization  
✅ **JWT structure** - Header, payload, and signature  
✅ **Security configuration** - Stateless authentication with JWT  
✅ **JWT generation and validation** - Using jjwt library  
✅ **Password encryption** - BCrypt for secure password storage  
✅ **Role-based access control** - CUSTOMER and ADMIN roles  
✅ **Authentication endpoints** - Register and login  
✅ **Testing secured endpoints** - With and without JWT tokens

### Key Takeaways

1. **JWT is stateless**: No server-side session storage
2. **Always hash passwords**: Never store plain text passwords
3. **Use role-based access**: Restrict endpoints based on user roles
4. **Secure your secret**: Store JWT secret in environment variables
5. **Test thoroughly**: Verify all access control rules work correctly

---

## 🚀 Runnable Project

A complete, production-ready Spring Boot project demonstrating **JWT Authentication** is available in:

**`pizzastore-with-jwt/`**

### Features

✅ **JWT Authentication**: Token-based authentication  
✅ **User Registration & Login**: Complete auth flow  
✅ **Role-Based Access Control**: CUSTOMER and ADMIN roles  
✅ **Password Encryption**: BCrypt password hashing  
✅ **Stateless Sessions**: No server-side session storage  
✅ **Secured Endpoints**: Public, customer, and admin endpoints  
✅ **Sample Users**: Pre-loaded with test accounts

### Test Accounts

**Customer Account:**
- Email: `emma.johnson@example.com`
- Password: `password123`
- Role: `CUSTOMER`

**Admin Account:**
- Email: `admin@pizzastore.be`
- Password: `password123`
- Role: `ADMIN`

### Running the Project

```bash
cd pizzastore-with-jwt
mvn clean install
mvn spring-boot:run
```

### Quick Test

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@pizzastore.be","password":"password123"}'

# Use the returned token
curl -X POST http://localhost:8080/api/pizzas \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{"name":"Secure Pizza","price":12.99,"available":true}'
```

---

🎉 You've successfully implemented JWT authentication in your Spring Boot application!
