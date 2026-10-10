# Lesson 12: Securing Web Applications - JWT Authentication

**Who Are You and What May You Do? Stateless JWT Authentication and Role-Based Access for PizzaStore**

---

## 📋 Table of Contents
- [Learning Objectives](#-learning-objectives)
- [Recap: Where Lesson 11 Left Us](#-recap-where-lesson-11-left-us)
- [What This Lesson Adds to PizzaStore](#-what-this-lesson-adds-to-pizzastore)
- [Introduction to Spring Security](#-introduction-to-spring-security)
- [What is JWT?](#-what-is-jwt)
- [JWT Structure](#-jwt-structure)
- [Adding Dependencies](#-adding-dependencies)
- [Security Configuration](#%EF%B8%8F-security-configuration)
- [JWT Utility Class](#-jwt-utility-class)
- [JWT Authentication Filter](#-jwt-authentication-filter)
- [UserDetailsService Implementation](#-userdetailsservice-implementation)
- [Authentication Controller](#-authentication-controller)
- [Password Encryption](#-password-encryption)
- [Testing the Authentication](#-testing-the-authentication)
- [Role-Based Access Control](#-role-based-access-control)
- [Best Practices](#-best-practices)
- [Testing Secured Controllers](#-testing-secured-controllers)
- [Summary](#-summary)
- [Runnable Project](#-runnable-project)

---

## 🎯 Learning Objectives

By the end of this lesson, you will be able to:

- ✅ Understand Spring Security fundamentals: the filter chain and the `SecurityContext`
- ✅ Implement JWT-based authentication with your own filter
- ✅ Generate and validate JWT tokens with JJWT
- ✅ Secure REST endpoints with role-based access control
- ✅ Handle user registration and login
- ✅ Encrypt passwords with BCrypt
- ✅ Configure stateless session management
- ✅ Test secured endpoints, with mock users in a slice and with real tokens end to end

---

## 🔄 Recap: Where Lesson 11 Left Us

After [Lesson 11](../lesson-11-testing/README.md) PizzaStore is a complete, tested REST API: pizzas, customers and orders, validation, `ProblemDetail` errors, the Open Food Facts import of Lesson 10, and 226 automated tests. But **every endpoint is open to everybody**: anyone can delete a pizza, read all customers or change the status of somebody else's order. The `Customer` entity already has a `password` and a `role` column (since Lesson 6a), and `data.sql` already contains an admin account, but nothing uses them yet.

This lesson answers two questions for every request:

1. **Authentication**: *who* is calling? A customer logs in once with e-mail and password and receives a **JWT token**; every following request carries that token.
2. **Authorization**: *may* this caller do this? Reading the menu is public, ordering is for customers, managing the menu and the orders is for admins.

---

## 🧱 What This Lesson Adds to PizzaStore

The project of this lesson, [`pizzastore-with-jwt`](pizzastore-with-jwt), is **Lesson 11's [`pizzastore-with-tests`](../lesson-11-testing/pizzastore-with-tests) plus exactly these changes**:

| Added / changed | What it does |
|-----------------|--------------|
| [`pom.xml`](pizzastore-with-jwt/pom.xml) | Adds `spring-boot-starter-security`, the three JJWT artifacts and `spring-boot-starter-security-test` |
| [`security/SecurityConfig.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/security/SecurityConfig.java) (new) | The `SecurityFilterChain`: public, customer and admin endpoints, stateless sessions, the JWT filter, `PasswordEncoder`, `AuthenticationManager` |
| [`security/JwtUtil.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/security/JwtUtil.java) (new) | Creates and validates tokens |
| [`security/JwtAuthenticationFilter.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/security/JwtAuthenticationFilter.java) (new) | Reads `Authorization: Bearer ...` and fills the `SecurityContext` |
| [`security/CustomUserDetailsService.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/security/CustomUserDetailsService.java) (new) | Loads a `Customer` by e-mail for Spring Security |
| [`controller/AuthController.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/controller/AuthController.java), [`dto/LoginRequest.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/dto/LoginRequest.java), [`RegisterRequest.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/dto/RegisterRequest.java), [`AuthResponse.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/dto/AuthResponse.java) (new) | `POST /api/auth/register` and `POST /api/auth/login` |
| [`config/AuditorAwareImpl.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/config/AuditorAwareImpl.java) (new) | Fills `createdBy`/`updatedBy` (Lesson 6a's auditing) with the logged-in user, `system` otherwise |
| [`exception/GlobalExceptionHandler.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/exception/GlobalExceptionHandler.java) | Two extra handlers: `AuthenticationException` (failed login) → `401`, `AccessDeniedException` → `403`, both as `ProblemDetail` |
| [`application.properties`](pizzastore-with-jwt/src/main/resources/application.properties) | `jwt.secret` and `jwt.expiration` |
| [`src/test`](pizzastore-with-jwt/src/test/java/be/vives/pizzastore) | The test suite adapted to security (see [Testing Secured Controllers](#-testing-secured-controllers)): 236 tests |

The controllers, services, repositories, entities, mappers, request/response DTOs, the Open Food Facts client and `data.sql` are **unchanged**. That is the point of the design: security is added *around* the application, in one configuration class and one filter, not inside every controller (see [Keep Controllers Clean](#7-keep-controllers-clean---centralized-security-configuration)).

The Open Food Facts import of Lesson 10, `POST /api/pizzas/{id}/nutritional-info/import`, needs no extra rule either: it changes the menu, so the existing rule *"`POST /api/pizzas/**` is for admins"* covers it.

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
    <org.mapstruct.version>1.6.3</org.mapstruct.version>
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

    <!-- Spring Security Test: @WithMockUser, @WithAnonymousUser, ... (Spring Boot 4 test starter) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

The versions of `spring-boot-starter-security` (Spring Security 7) and of the test starter come from the Spring Boot 4 parent; JJWT is not managed by Spring Boot, so its version is set in `<properties>`. Only `jjwt-api` is needed at compile time: the implementation and its Jackson binding are `runtime` dependencies, so your code cannot accidentally depend on JJWT internals. As with the other test starters of Lesson 11, `spring-boot-starter-security-test` is the Spring Boot 4 way to get `spring-security-test`.

---

## ⚙️ Security Configuration

Create a `SecurityConfig` class to configure Spring Security:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
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
                        .requestMatchers("/error").permitAll()

                        // Pizza modification endpoints - require ADMIN role only
                        .requestMatchers(HttpMethod.POST, "/api/pizzas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/pizzas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/pizzas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/pizzas/**").hasRole("ADMIN")

                        // Customer endpoints - accessible by CUSTOMER and ADMIN
                        .requestMatchers("/api/customers/**").hasAnyRole("CUSTOMER", "ADMIN")

                        // Order endpoints - POST is for CUSTOMER, all other methods for ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/orders").hasRole("CUSTOMER")
                        .requestMatchers("/api/orders/**").hasRole("ADMIN")

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // No (valid) token on a protected endpoint: 401 Unauthorized. Without this entry point Spring Security
                // answers 403 Forbidden, which is meant for "authenticated, but not allowed" (wrong role)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // For H2 Console
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
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

### ⚙️ Authentication Provider Auto-Configuration

**Important Note about Modern Spring Security (Spring Security 6 and 7, Spring Boot 3 and 4):**

In older versions of Spring Security, you had to manually create a `DaoAuthenticationProvider` bean:

```java
// ❌ OLD WAY - No longer necessary
@Bean
public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
}
```

**In current Spring Security**, Spring **automatically configures** the `DaoAuthenticationProvider` when it detects:
1. A `UserDetailsService` bean (our `CustomUserDetailsService`)
2. A `PasswordEncoder` bean (our `BCryptPasswordEncoder`)

This is done through **auto-configuration**. The framework automatically wires these beans together internally, eliminating boilerplate code.

**Benefits:**
- ✅ **Less code**: No need to explicitly create the authentication provider
- ✅ **Convention over configuration**: Spring Boot handles the wiring
- ✅ **Cleaner configuration**: Focus only on what's unique to your application
- ✅ **More maintainable**: Less custom code means less to maintain

This is why our `SecurityConfig` only needs to:
- Define the `PasswordEncoder` bean
- Define the `AuthenticationManager` bean
- Configure the security filter chain
- Add our custom JWT filter

The authentication provider is created and configured automatically by Spring Boot behind the scenes!

---

### Key Points:

1. **CSRF Disabled**: Not needed for stateless JWT authentication
2. **Public Endpoints**: 
   - `/api/auth/**`: Registration and login
   - `GET /api/pizzas/**`: Anonymous users can view pizzas (read-only)
   - `/h2-console/**`: H2 database console (development only)
   - `/error`: Spring Boot's error page. When Spring Security refuses a logged-in user, it calls `response.sendError(403)`, and the servlet container *forwards* the request to `/error` to render the body. That forward carries no JWT (our filter runs once per request, not again for the forward), so if `/error` were protected the user would be anonymous there and get a `401` instead of the `403`. Permitting `/error` keeps the right status code
3. **Role-Based Access**:
   - **CUSTOMER**: can use `/api/customers/**` (view, manage favorites) and can **place** an order (`POST /api/orders`)
   - **ADMIN**: can use `/api/customers/**`, can modify the menu (`POST`, `PUT`, `PATCH`, `DELETE` on `/api/pizzas/**`, which includes the image upload and the Open Food Facts import) and manages the orders (every other `/api/orders/**` request)
   - The roles do not include each other: an admin cannot place an order and a customer cannot list all orders. `hasRole("X")` checks for exactly that role
4. **Stateless Sessions**: No session storage on the server
5. **401 vs 403**: `HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)` answers `401 Unauthorized` when a protected endpoint is called without a valid token ("who are you?"). A *logged-in* user without the right role gets `403 Forbidden` ("I know you, but you may not"). Without the entry point Spring Security answers `403` in both cases: stateless configurations have no login form or HTTP Basic that would register a `401` entry point
6. **JWT Filter**: Added before Spring Security's authentication filter
7. **Auto-Configuration**: Spring Boot automatically configures the authentication provider using our `UserDetailsService` and `PasswordEncoder` beans

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

- `jwt.secret`: Secret key for signing tokens (must be at least 256 bits for HS256). `Keys.hmacShaKeyFor(...)` picks the strongest HMAC algorithm the key length allows: this 63-character secret (504 bits) gives **HS384**, which is why the example token above has `"alg": "HS384"`
- `jwt.expiration`: Token validity in milliseconds (86400000 = 24 hours)

⚠️ **Security Note**: In production, store the secret in environment variables, not in property files! Spring Boot's relaxed binding maps an environment variable `JWT_SECRET` onto `jwt.secret` automatically. The tests use a secret of their own in `src/test/resources/application.properties`.

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

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String jwt = authorizationHeader.substring(7);
            try {
                String username = jwtUtil.extractUsername(jwt);
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                if (jwtUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authenticationToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } catch (JwtException | UsernameNotFoundException e) {
                // malformed, tampered or expired token, or a user that no longer exists: the request simply stays
                // anonymous, so a protected endpoint answers 401 (see SecurityConfig) instead of failing with a 500
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

### How It Works:

1. Extracts the JWT from the `Authorization` header
2. Reads the e-mail address (the `sub` claim) and loads the user. Reading the claims also **verifies the signature and the expiration**: JJWT throws a `JwtException` (`MalformedJwtException`, `SignatureException`, `ExpiredJwtException`, ...) for a token that is not valid
3. If valid, creates an authentication object and stores it in the `SecurityContext`
4. If not, it catches the exception and does **nothing**: the request continues as anonymous, and the authorization rules decide. A public endpoint still works, a protected one answers `401`. Without the `catch` the exception would escape from the filter and the client would get a `500` for an expired token
5. Continues the filter chain

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

[`AuthController`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/controller/AuthController.java) follows the status-code rules of Lesson 10: an e-mail address that is already registered is a **`409 Conflict`** (`DuplicateResourceException`, like `CustomerService` does for `POST /api/customers`), and a failed login is a **`401 Unauthorized`**. For the login it does not catch anything itself: `authenticationManager.authenticate(...)` throws an `AuthenticationException` (`BadCredentialsException` for a wrong password or an unknown e-mail address), and one new handler in `GlobalExceptionHandler` translates it:

```java
@ExceptionHandler(AuthenticationException.class)
public ProblemDetail handleAuthenticationException(AuthenticationException ex, WebRequest request) {
    // one message for "unknown e-mail" and "wrong password": never reveal which e-mail addresses have an account
    log.warn("Authentication failed: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password", "unauthorized", request);
}
```

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
            throw new DuplicateResourceException("Email already exists");   // 409 Conflict
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
        // wrong e-mail or password: throws an AuthenticationException, which GlobalExceptionHandler turns into a 401
        authenticationManager.authenticate(
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

❌ **401 Unauthorized** - Authentication required

The `401` comes from the `HttpStatusEntryPoint` in `SecurityConfig`. Compare it with step 6: there the caller *is* logged in, but as a customer, and gets `403 Forbidden`. The difference tells a client what to do: after a `401` it should (re)log in, after a `403` logging in again won't help. Sending an expired or tampered token gives the same `401`.

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

### 8. Import Nutrition Data (Admin Only)

The Open Food Facts import of [Lesson 10](../lesson-10-validation-exception-handling/README.md#-part-3-calling-an-external-api) changes the menu, so it follows the same rule as creating a pizza:

```bash
# with the customer token: 403 Forbidden
curl -X POST http://localhost:8080/api/pizzas/1/nutritional-info/import \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{"barcode":"3017620422003"}'

# with the admin token: 200 OK, the pizza with its new nutritionalInfo (needs internet)
curl -X POST http://localhost:8080/api/pizzas/1/nutritional-info/import \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"barcode":"3017620422003"}'
```

Security is checked **before** anything else: a customer gets the `403` without the request ever reaching the controller, so no request is sent to Open Food Facts either.

### 9. Wrong Credentials

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@pizzastore.be","password":"wrong"}'
```

❌ **401 Unauthorized** with a `ProblemDetail` whose `detail` is `"Invalid email or password"` (see [Controller Implementation](#controller-implementation)). The message deliberately does not say *which* of the two was wrong, so nobody can find out which e-mail addresses have an account. A request without e-mail or password is a `400` (validation), and registering an e-mail address that already exists is a `409 Conflict` "Email already exists".

---


## 🎭 Role-Based Access Control

### Access Control Summary

| Endpoint | Method | Anonymous | CUSTOMER | ADMIN |
|----------|--------|-----------|----------|-------|
| `/api/auth/**` | ALL | ✅ | ✅ | ✅ |
| `/api/pizzas/**` | GET | ✅ | ✅ | ✅ |
| `/api/pizzas/**` | POST/PUT/PATCH/DELETE (incl. `/{id}/image` and `/{id}/nutritional-info/import`) | ❌ | ❌ | ✅ |
| `/api/customers/**` | ALL | ❌ | ✅ | ✅ |
| `/api/orders` | POST | ❌ | ✅ | ❌ |
| `/api/orders/**` | GET/PATCH/DELETE | ❌ | ❌ | ✅ |

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
    
    // Order endpoints - POST is for CUSTOMER, all other methods for ADMIN
    .requestMatchers(HttpMethod.POST, "/api/orders").hasRole("CUSTOMER")
    .requestMatchers("/api/orders/**").hasRole("ADMIN")
    
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

**Benefits of centralized configuration:**
- ✅ **Single source of truth**: All security rules in one place
- ✅ **Easy to audit**: Quick overview of who can access what
- ✅ **Maintainable**: Changes to security rules happen in one location
- ✅ **Clean controllers**: Controllers focus on business logic only
- ✅ **Better testing**: Controllers can be tested without security context
- ✅ **Consistency**: Ensures uniform security policy across the application

#### Clean Controller Example

PizzaStore's [`PizzaController`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/controller/PizzaController.java) is **byte-for-byte the same** as in Lesson 11: not a single line changed to secure it.

```java
@RestController
@RequestMapping("/api/pizzas")
public class PizzaController {

    private final PizzaService pizzaService;
    private final NutritionImportService nutritionImportService;
    ...

    // No security annotations needed: SecurityConfig decides who may call this
    @PostMapping
    public ResponseEntity<PizzaResponse> createPizza(@Valid @RequestBody CreatePizzaRequest request) {
        PizzaResponse created = pizzaService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PizzaResponse> getPizza(@PathVariable Long id) {
        return ResponseEntity.ok(pizzaService.findById(id));
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

## 🧪 Testing Secured Controllers

Add Spring Security to a tested application and many of the tests of [Lesson 11](../lesson-11-testing/README.md) turn red: a `POST /api/pizzas` without a user now gets `401`, and full-stack tests that create data need a token. That is the safety net doing its job. This section shows how the test suite of [`pizzastore-with-jwt`](pizzastore-with-jwt/src/test/java/be/vives/pizzastore) deals with security, at two levels:

| Level | How the user is simulated | Proves |
|-------|---------------------------|--------|
| `@WebMvcTest` slice | `@WithMockUser(roles = "...")`, `@WithAnonymousUser` | the URL rules of `SecurityConfig`, per role, in milliseconds |
| `@SpringBootTest` with a real server | real accounts and real JWT tokens, sent as `Authorization: Bearer ...` | the whole chain: login, token, filter, role check, controller |

### Slice Tests: `@WithMockUser`

[`PizzaControllerTest`](pizzastore-with-jwt/src/test/java/be/vives/pizzastore/controller/PizzaControllerTest.java) is a `@WebMvcTest` with `RestTestClient`, as in Lesson 11, plus the real security configuration:

```java
@WebMvcTest(controllers = PizzaController.class)
@AutoConfigureRestTestClient
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@WithMockUser(roles = "ADMIN")                       // default user for every test of the class
class PizzaControllerTest {

    @Autowired
    private RestTestClient client;

    @MockitoBean
    private PizzaService pizzaService;

    @MockitoBean
    private NutritionImportService nutritionImportService;

    // needed by SecurityConfig / JwtAuthenticationFilter, which are part of the slice
    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;
    ...
}
```

Four things to understand:

1. **`@Import(SecurityConfig.class)`**: a slice does not scan `@Configuration` classes (the same lesson as `@Import(JpaConfig.class)` in Lesson 11). Without the import the slice would use Spring Boot's default security (every request needs a login, CSRF protection on), and the tests would check rules PizzaStore does not have.
2. **`JwtAuthenticationFilter` *is* part of the slice**: `@WebMvcTest` includes every `Filter` bean, and our filter is a `@Component`. Its constructor needs a `JwtUtil` and a `UserDetailsService`, which are not in the slice, hence the two `@MockitoBean`s. They are never really called: no request in these tests carries a token.
3. **`@WithMockUser(roles = "ADMIN")`** (from `spring-security-test`) puts an authenticated user with `ROLE_ADMIN` straight into the `SecurityContext`, without passwords or tokens. On the class it is the default; a test overrides it with its own annotation.
4. **The service is a mock**, so a denied request can be proven by `verify(..., never())`: the controller was never reached.

The security tests of the class:

```java
@Test
@WithAnonymousUser
void getPizzas_Anonymous_ReturnsOk() {
    // GET /api/pizzas/** is permitAll()
    when(pizzaService.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(pizza(1, "Margherita", "8.50", "Classic"))));

    client.get().uri("/api/pizzas")
            .exchange()
            .expectStatus().isOk();
}

@Test
@WithMockUser(roles = "CUSTOMER")
void createPizza_WithCustomerRole_ReturnsForbidden() {
    // customers cannot create pizzas, only admins can
    client.post().uri("/api/pizzas")
            .contentType(MediaType.APPLICATION_JSON)
            .body(newPizzaRequest())
            .exchange()
            .expectStatus().isForbidden();

    verify(pizzaService, never()).create(any());
}

@Test
@WithMockUser(roles = "CUSTOMER")
void importNutritionalInfo_WithCustomerRole_ReturnsForbidden() {
    // only admins edit the menu
    client.post().uri("/api/pizzas/{id}/nutritional-info/import", 1)
            .contentType(MediaType.APPLICATION_JSON)
            .body(new ImportNutritionRequest("3017620422003"))
            .exchange()
            .expectStatus().isForbidden();

    verifyNoInteractions(nutritionImportService);
}
```

An anonymous `POST` gets `401` (`createPizza_Anonymous_ReturnsUnauthorized`): `@WithAnonymousUser` plus the real `SecurityConfig` also tests the `HttpStatusEntryPoint` (see [Testing the Authentication](#4-try-to-create-pizza-without-auth)).

[`OrderControllerTest`](pizzastore-with-jwt/src/test/java/be/vives/pizzastore/controller/OrderControllerTest.java) and [`CustomerControllerTest`](pizzastore-with-jwt/src/test/java/be/vives/pizzastore/controller/CustomerControllerTest.java) follow the same pattern, with `@WithMockUser` per test. They check the less obvious rules of the [access table](#access-control-summary): a customer may place an order, but an **admin may not** (`createOrder_withAdminRole_returnsForbidden`), and a customer may not list, read, update or cancel orders. [`AuthControllerTest`](pizzastore-with-jwt/src/test/java/be/vives/pizzastore/controller/AuthControllerTest.java) tests the public `/api/auth` endpoints without any user: a duplicate e-mail address gives a `409` and invalid registration data a `400`, both as `ProblemDetail`.

> **Multipart in a slice.** The four image-upload tests are in a separate class, [`PizzaImageUploadControllerTest`](pizzastore-with-jwt/src/test/java/be/vives/pizzastore/controller/PizzaImageUploadControllerTest.java), that uses MockMvc instead of `RestTestClient`: a `RestTestClient` bound to MockMvc does not turn a multipart body into request parts (Lesson 11, *Which one should I use?*). `@WithMockUser` works with both.

### Full-Stack Tests: Real Tokens

`@WithMockUser` skips everything this lesson built: the login, the token, the filter. [`SecurityIntegrationTest`](pizzastore-with-jwt/src/test/java/be/vives/pizzastore/integration/SecurityIntegrationTest.java) therefore starts the real application on a random port and works with real accounts. The helper [`TestAccounts`](pizzastore-with-jwt/src/test/java/be/vives/pizzastore/integration/TestAccounts.java) creates them:

```java
Account registerCustomer() {
    String email = uniqueEmail();
    AuthResponse response = client.post().uri("/api/auth/register")      // like a real client
            .contentType(MediaType.APPLICATION_JSON)
            .body(new RegisterRequest("Test Customer", email, PASSWORD))
            .exchange()
            .expectStatus().isCreated()
            .expectBody(AuthResponse.class)
            .returnResult().getResponseBody();
    ...
    return new Account(id, email, response.getToken());
}

Account createAdmin() {
    // there is no API to create an admin (that would be a security hole): save it through the repository...
    Customer admin = new Customer("Test Admin", uniqueEmail());
    admin.setPassword(passwordEncoder.encode(PASSWORD));
    admin.setRole(Role.ADMIN);
    admin = customerRepository.save(admin);
    // ...and log in through the API like everybody else
    ...
}
```

A test then sends the token like any client would:

```java
@Test
void customer_CannotListAllOrders_ButAdminCan() {
    client.get().uri("/api/orders")
            .header(HttpHeaders.AUTHORIZATION, customer.bearer())    // "Bearer eyJhbGciOiJIUzM4NCJ9..."
            .exchange()
            .expectStatus().isForbidden();

    client.get().uri("/api/orders")
            .header(HttpHeaders.AUTHORIZATION, admin.bearer())
            .exchange()
            .expectStatus().isOk();
}
```

The other tests of the class: anonymous users can read pizzas but get `401` when they try to create one, a customer gets `403` and an admin `201`, a garbage token gives `401` (not a `500`), a wrong password gives `401`, and the token returned by `register` works immediately.

The full-stack tests of Lesson 11 needed the same change: [`PizzaStoreApiIntegrationTest`](pizzastore-with-jwt/src/test/java/be/vives/pizzastore/integration/PizzaStoreApiIntegrationTest.java) creates an admin and a customer in `@BeforeEach` and sends their tokens with every request that needs one, and the MockMvc-based [`PizzaIntegrationTest`](pizzastore-with-jwt/src/test/java/be/vives/pizzastore/integration/PizzaIntegrationTest.java) uses `@WithMockUser(roles = "ADMIN")`, which works because MockMvc runs on the test thread.

### The Test Suite of This Lesson

`mvn test` runs **236 tests**:

| Kind | Test classes | Tests |
|------|--------------|------:|
| Unit (no Spring) | `PizzaServiceTest`, `CustomerServiceTest`, `OrderServiceTest`, `NutritionImportServiceTest`, `RequestValidationTest`, `PizzaControllerStandaloneTest` | 95 |
| Slice | `PizzaRepositoryTest`, `CustomerRepositoryTest`, `OrderRepositoryTest` (`@DataJpaTest`) | 40 |
| | `PizzaControllerTest`, `CustomerControllerTest`, `OrderControllerTest`, `AuthControllerTest` (`@WebMvcTest` + `RestTestClient` + security), `PizzaImageUploadControllerTest` (MockMvc) | 60 |
| | `PizzaJsonTest`, `PizzaMapperTest`, `OpenFoodFactsClientTest` | 13 |
| Full stack | `ApplicationSmokeTest`, `PizzaIntegrationTest`, `PizzaStoreApiIntegrationTest`, `BeanOverrideIntegrationTest`, `SecurityIntegrationTest` | 28 |

Compared with Lesson 11, this is the test suite of the final PizzaStore: the controller tests are written with `RestTestClient` only (Lesson 11's side-by-side comparison classes `PizzaControllerRestTestClientTest` and `PizzaControllerMockMvcTesterTest` have done their job and are not carried over), and `AuthControllerTest`, `SecurityIntegrationTest` and the role tests are new.

### Best Practices for Security Testing

1. **Test every access level**: anonymous, CUSTOMER and ADMIN, for each kind of endpoint.
2. **Test what is forbidden**, not only what is allowed, and prove with `verify(..., never())` that the controller was never reached.
3. **Import the real `SecurityConfig`** in slice tests, so you test your rules instead of Spring Boot's defaults.
4. **Keep the security tests in the slice** (fast, one rule per test) and add a few full-stack tests with real tokens for the chain itself.
5. **Never weaken security for tests** (no `permitAll()` profile for tests): test the configuration that runs in production.

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

**Note on the book**: *Pro Spring Boot 4* covers security in Chapter 11, *Securing Spring Boot Applications* (p. 281-311). Its *Modern Security Architecture* section describes the security filter chain configured with the Lambda DSL, the modular starters and `requestMatchers` for URL-based rules, which is exactly how `SecurityConfig` works; *Secure Credential Storage* recommends BCrypt (the book wraps it in a `DelegatingPasswordEncoder`, PizzaStore uses `BCryptPasswordEncoder` directly); and *The Identity Contract* implements a custom `UserDetailsService` on top of a repository, like `CustomUserDetailsService`. *Fine-Grained Authorization* shows `@EnableMethodSecurity` and `@PreAuthorize`, discussed under [When to Use @PreAuthorize](#when-to-use-preauthorize). Where the course differs: the book's *Modern Identity: OAuth2 and JWT* section explains stateless JWT security (Table 11-1) but delegates token issuing to an external identity provider (OAuth2/OIDC with Keycloak, or `spring-boot-starter-oauth2-resource-server`), and its case studies use one-time tokens, two-factor authentication and reactive security. The book **does not build its own JWT filter**; this lesson does, because writing `JwtUtil` and `JwtAuthenticationFilter` yourself shows what a resource server does behind the scenes. The book's *Testing Your Security* uses `@SpringBootTest` with a test client and `@WithMockUser`/`mockOidcLogin()`, the same ideas as [Testing Secured Controllers](#-testing-secured-controllers).

---

## 🚀 Runnable Project

**[`pizzastore-with-jwt/`](pizzastore-with-jwt)** is Lesson 11's [`pizzastore-with-tests`](../lesson-11-testing/pizzastore-with-tests) plus the changes listed in [What This Lesson Adds to PizzaStore](#-what-this-lesson-adds-to-pizzastore). Apart from the OpenAPI documentation of Lesson 13, it is the final PizzaStore.

### Features

✅ **Spring Boot 4.0** on **Java 25**, Spring Security 7, JJWT 0.13  
✅ **JWT Authentication**: token-based, stateless  
✅ **User Registration & Login**: `POST /api/auth/register`, `POST /api/auth/login`  
✅ **Role-Based Access Control**: CUSTOMER and ADMIN roles, see the [access table](#access-control-summary)  
✅ **Password Encryption**: BCrypt password hashing  
✅ **Everything from Lessons 6a-11**, including the Open Food Facts import (admin only)  
✅ **236 tests**, including role tests in the web slice and end-to-end tests with real tokens  
❌ No API documentation yet: Lesson 13

### Test Accounts

All accounts in `data.sql` have the password `password123`.

| Email | Role |
|-------|------|
| `emma.johnson@example.com` | `CUSTOMER` |
| `liam.smith@example.com` | `CUSTOMER` |
| `admin@pizzastore.be` | `ADMIN` |

### Running the Project

```bash
cd pizzastore-with-jwt
mvn spring-boot:run
```

Run the tests with `mvn test`. Both need JDK 25: if your default `mvn` picks another JDK, point `JAVA_HOME` to JDK 25 first. The H2 console is at http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:pizzastore_jwt`, user `sa`, no password).

### Quick Test

```bash
# Login as admin and keep the token
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@pizzastore.be","password":"password123"}' | jq -r '.token')

# Use the token
curl -X POST http://localhost:8080/api/pizzas \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"name":"Secure Pizza","price":12.99,"available":true}'
```

---

🎉 You've successfully implemented JWT authentication in your Spring Boot application! Continue to [Lesson 13: Swagger/OpenAPI](../lesson-13-swagger-openapi/README.md) to document the secured API.
