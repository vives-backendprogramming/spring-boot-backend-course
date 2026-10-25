# Lesson 13: Securing Web Applications - JWT Authentication

**Who Are You and What May You Do? Stateless JWT Authentication and Role-Based Access for PizzaStore**

---

## 📋 Table of Contents
- [Learning Objectives](#-learning-objectives)
- [Recap: Where Lesson 12 Left Us](#-recap-where-lesson-12-left-us)
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
- [Method Security: Who Owns This Customer?](#-method-security-who-owns-this-customer)
- [CORS and CSRF](#-cors-and-csrf)
- [Best Practices](#-best-practices)
- [Testing Secured Controllers](#-testing-secured-controllers)
- [Alternative: Spring Security's OAuth2 Resource Server](#-alternative-spring-securitys-oauth2-resource-server)
- [Beyond This Lesson: More Security in the Book](#-beyond-this-lesson-more-security-in-the-book)
- [Summary](#-summary)
- [Runnable Project](#-runnable-project)

---

## 🎯 Learning Objectives

By the end of this lesson, you will be able to:

- ✅ Understand Spring Security fundamentals: the filter chain and the `SecurityContext`
- ✅ Implement JWT-based authentication with your own filter
- ✅ Generate and validate JWT tokens with JJWT
- ✅ Secure REST endpoints with role-based access control (URL rules in the `SecurityFilterChain`)
- ✅ Add fine-grained, instance-level rules with method security (`@EnableMethodSecurity`, `@PreAuthorize`, custom meta-annotations)
- ✅ Inject the logged-in user into a controller with `@AuthenticationPrincipal`
- ✅ Handle user registration and login
- ✅ Hash passwords with BCrypt behind a `DelegatingPasswordEncoder`
- ✅ Configure stateless session management, CORS, and explain why CSRF protection is off
- ✅ Test secured endpoints, with mock users in a slice and with real tokens end to end
- ✅ Know the alternative: Spring Security's own OAuth2 resource server

---

## 🔄 Recap: Where Lesson 12 Left Us

After [Lesson 12](../lesson-12-testing/README.md) PizzaStore is a complete, tested REST API. But **every endpoint is open to everybody**: anyone can delete a pizza, read all customers or change the status of somebody else's order. The `Customer` entity already has a `password` and a `role` column (since Lesson 6a), and `data.sql` already contains an admin account, but nothing uses them yet.

This lesson answers two questions for every request:

1. **Authentication**: *who* is calling? A customer logs in once with e-mail and password and receives a **JWT token**; every following request carries that token.
2. **Authorization**: *may* this caller do this? Reading the menu is public, ordering is for customers, managing the menu and the orders is for admins. And one level deeper: a customer may read and change **their own** account and order for **themselves**, but not for somebody else.

---

## 🧱 What This Lesson Adds to PizzaStore

The project of this lesson, [`pizzastore-with-jwt`](pizzastore-with-jwt), is **Lesson 12's [`pizzastore-with-tests`](../lesson-12-testing/pizzastore-with-tests) plus exactly these changes**:

| Added / changed | What it does |
|-----------------|--------------|
| [`pom.xml`](pizzastore-with-jwt/pom.xml) | Adds `spring-boot-starter-security`, the three JJWT artifacts and `spring-boot-starter-security-test` |
| [`security/SecurityConfig.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/security/SecurityConfig.java) (new) | The `SecurityFilterChain`: public, customer and admin endpoints, stateless sessions, the JWT filter, CORS; `@EnableMethodSecurity`; the `PasswordEncoder` (`DelegatingPasswordEncoder`), the `AuthenticationManager` and the `CorsConfigurationSource` |
| [`security/JwtUtil.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/security/JwtUtil.java) (new) | Creates and validates tokens |
| [`security/JwtAuthenticationFilter.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/security/JwtAuthenticationFilter.java) (new) | Reads `Authorization: Bearer ...` and fills the `SecurityContext` |
| [`security/CustomUserDetailsService.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/security/CustomUserDetailsService.java) (new) | Loads a `Customer` by e-mail for Spring Security |
| [`security/CustomerSecurity.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/security/CustomerSecurity.java), [`IsAdmin.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/security/IsAdmin.java), [`IsAdminOrOwner.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/security/IsAdminOrOwner.java), [`CurrentUser.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/security/CurrentUser.java) (new) | Method security: "is this my account?" and two meta-annotations around `@PreAuthorize`; `@CurrentUser` injects the e-mail address of the logged-in user (see [Method Security](#-method-security-who-owns-this-customer)) |
| [`controller/AuthController.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/controller/AuthController.java), [`dto/LoginRequest.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/dto/LoginRequest.java), [`RegisterRequest.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/dto/RegisterRequest.java), [`AuthResponse.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/dto/AuthResponse.java) (new) | `POST /api/auth/register` and `POST /api/auth/login` |
| [`controller/CustomerController.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/controller/CustomerController.java) | `@IsAdmin` / `@IsAdminOrOwner` per method, the new `GET /api/customers/me`, and the favorites path variable renamed from `{customerId}` to `{id}` (same URLs) so that one annotation fits all methods |
| [`controller/OrderController.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/controller/OrderController.java) | `createOrder` gets a `@PreAuthorize`: a customer orders only for their own account |
| [`service/CustomerService.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/service/CustomerService.java) | `findByEmail` (for `/me`), and `create` now **hashes the password**: until now `POST /api/customers` stored it as typed, so such a customer could never log in |
| [`config/AuditorAwareImpl.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/config/AuditorAwareImpl.java) (new) | Fills `createdBy`/`updatedBy` (Lesson 6a's auditing) with the logged-in user, `system` otherwise |
| [`exception/GlobalExceptionHandler.java`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/exception/GlobalExceptionHandler.java) | Two extra handlers: `AuthenticationException` (failed login) → `401`, `AccessDeniedException` → `403`, both as `ProblemDetail` |
| [`application.properties`](pizzastore-with-jwt/src/main/resources/application.properties) | `jwt.secret` (overridable with the environment variable `JWT_SECRET`), `jwt.expiration`, `pizzastore.cors.allowed-origins` |
| [`data.sql`](pizzastore-with-jwt/src/main/resources/data.sql) | The existing BCrypt hashes get the `{bcrypt}` prefix that the `DelegatingPasswordEncoder` needs |
| [`src/test`](pizzastore-with-jwt/src/test/java/be/vives/pizzastore) | The test suite adapted to security (see [Testing Secured Controllers](#-testing-secured-controllers)): 252 tests |

The other services, repositories, entities, mappers, request/response DTOs and the Open Food Facts client are **unchanged**, and so is `PizzaController`. Security is added in two layers, and that is the design to remember:

1. **Around the application**: one configuration class and one filter decide per URL and HTTP method which *role* may call it. That covers all of `PizzaController` and most of `OrderController`.
2. **On a few controller methods**: rules that depend on the *data* ("is customer 12 the one who is logged in?") cannot be written as a URL pattern. Those get a `@PreAuthorize`, wrapped in a readable meta-annotation (see [Method Security](#-method-security-who-owns-this-customer)).

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
┌─────────────────────────────────────────┐
│ 1. SecurityContextHolderFilter          │ ← Loads the SecurityContext 
├─────────────────────────────────────────┤
│ 2. CorsFilter                           │ ← Answers CORS preflight requests (see CORS and CSRF)
├─────────────────────────────────────────┤
│ 3. CsrfFilter                           │ ← CSRF protection (disabled for stateless APIs)
├─────────────────────────────────────────┤
│ 4. LogoutFilter                         │ ← Handles logout requests (unused: a JWT is "logged out" by the client)
├─────────────────────────────────────────┤
│ 5. JwtAuthenticationFilter              │ ← 🔑 OUR CUSTOM JWT FILTER (extracts & validates token)
├─────────────────────────────────────────┤
│ 6. UsernamePasswordAuthenticationFilter │ ← Login form processing (no form here)
├─────────────────────────────────────────┤
│ 7. AnonymousAuthenticationFilter        │ ← Sets anonymous authentication
├─────────────────────────────────────────┤
│ 8. ExceptionTranslationFilter           │ ← Handles security exceptions
├─────────────────────────────────────────┤
│ 9. AuthorizationFilter                  │ ← Checks access permissions
└─────────────────────────────────────────┘
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

4. **AuthorizationFilter**: The final filter checks if the authenticated user (from `SecurityContext`) has permission to access the requested endpoint based on roles and authorities. This is the *URL-level* check; [method security](#-method-security-who-owns-this-customer) adds a second check later, when the controller method is called.

The diagram is simplified: the real chain has more filters (`HeaderWriterFilter`, `RequestCacheAwareFilter`, ...). Spring Security logs the actual list at startup; set `logging.level.org.springframework.security=DEBUG` to see it, and to see per request which filter decided what.

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

Or, much cleaner, let Spring inject it into a controller parameter with `@AuthenticationPrincipal`. PizzaStore does exactly that for `GET /api/customers/me`, through its own meta-annotation `@CurrentUser` (see [Method Security](#5-who-am-i-authenticationprincipal-and-currentuser)):

```java
@GetMapping("/me")
public ResponseEntity<CustomerResponse> getCurrentCustomer(@CurrentUser String email) {
    return ResponseEntity.ok(customerService.findByEmail(email));
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

The versions of `spring-boot-starter-security` (Spring Security 7) and of the test starter come from the Spring Boot 4 parent; JJWT is not managed by Spring Boot, so its version is set in `<properties>`. 

---

## ⚙️ Security Configuration

Create a [`SecurityConfig`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/security/SecurityConfig.java) class to configure Spring Security. Like everything in Spring Security 7 it is **component-based**: you declare a `SecurityFilterChain` bean and configure it with the lambda DSL. 

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity                     // @PreAuthorize on controller methods, see "Method Security"
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)      // see "CORS and CSRF"
                .cors(Customizer.withDefaults())            // uses the CorsConfigurationSource bean below
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

                        // Customer endpoints - CUSTOMER and ADMIN; @IsAdmin / @IsAdminOrOwner in CustomerController
                        // decide per method (a customer only sees and changes their own account)
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
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();   // see "Password Encryption"
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) 
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${pizzastore.cors.allowed-origins}") List<String> allowedOrigins) {
        // see "CORS and CSRF"
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

**Note:** strictly speaking Spring Boot does not need it: as soon as `spring-boot-starter-security` is on the classpath, Boot's auto-configuration enables web security itself, and a `SecurityFilterChain` bean of your own replaces Boot's default chain (every request authenticated, a generated password in the log). Putting `@EnableWebSecurity` on your configuration class makes the intention explicit and is what the Spring Security reference does, so PizzaStore keeps it. 

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
2. A `PasswordEncoder` bean (our `DelegatingPasswordEncoder`)

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

1. **CSRF Disabled**: Not needed for stateless JWT authentication; CORS is on (both explained in [CORS and CSRF](#-cors-and-csrf))
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
8. **Order matters**: the rules are checked top to bottom and the **first** match wins. That is why the specific `POST /api/orders` rule comes before the general `/api/orders/**` rule, and why the chain ends with `anyRequest().authenticated()`: an endpoint somebody adds later and forgets to list is protected by default, never public by accident

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

- **Password hashing**: Always hash passwords with the `PasswordEncoder` bean before storing them (see [Password Encryption](#-password-encryption))
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

        // roles("CUSTOMER") grants the authority ROLE_CUSTOMER, which hasRole("CUSTOMER") checks
        return User.withUsername(customer.getEmail())
                .password(customer.getPassword())
                .roles(customer.getRole().name())
                .build();
    }
}
```

**Implementation Details:**

1. **`@Service`**: Makes this a Spring-managed bean that can be injected into the security configuration

2. **`loadUserByUsername(String email)`**:
   - We use **email as the username** (more user-friendly than a separate username field)
   - Queries the database via `CustomerRepository.findByEmail()`
   - Throws `UsernameNotFoundException` if the user doesn't exist

3. **Return `UserDetails` object**, built with the fluent `User.withUsername(...)` builder:
   - Spring Security's `User` class implements `UserDetails`
   - **Username**: The customer's email
   - **Password**: The hashed password from the database, `{bcrypt}$2a$10$...`
   - **Authorities**: the granted authorities (roles) for authorization

4. **Role vs. authority**: an *authority* is just a string. A *role* is an authority with the prefix `ROLE_`: `.roles("ADMIN")` stores the authority `ROLE_ADMIN`, and `hasRole("ADMIN")` checks for exactly that string. So:
   - `Role.CUSTOMER` → `ROLE_CUSTOMER`
   - `Role.ADMIN` → `ROLE_ADMIN`

   `hasAuthority("...")` compares the full string without adding a prefix; OAuth2 scopes, for example, arrive as authorities like `SCOPE_read`.

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

Always store hashed passwords, never plain text! Strictly speaking it is *hashing*, not encryption: a hash cannot be turned back into the password. To check a login, Spring Security hashes the typed password again and compares.

### BCrypt Encoding

**BCrypt** is the recommended algorithm: it is deliberately **slow**, which makes brute-forcing millions of guesses expensive, and it adds a random **salt** to every hash, so two users with the same password get different hashes.

### `DelegatingPasswordEncoder`: BCrypt, and Room to Change

PizzaStore does not create a `BCryptPasswordEncoder` directly, but a **`DelegatingPasswordEncoder`**:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    // BCrypt for new passwords, stored as "{bcrypt}$2a$10$...": the prefix tells which algorithm made a hash,
    // so the algorithm can be changed later without invalidating the passwords already stored
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
}
```

- `encode(...)` uses BCrypt and puts the algorithm id in front: `{bcrypt}$2a$10$wvw30spx...`
- `matches(...)` reads the id and hands the check to the right encoder (`{bcrypt}`, `{argon2}`, `{pbkdf2}`, `{scrypt}`, `{noop}`, ...)

The benefit shows the day you want a stronger algorithm (Argon2, say): new passwords get the new id, the old `{bcrypt}` hashes keep working, and nobody has to reset their password. The price: **every stored hash needs its prefix**. A hash without one makes `matches` throw an `IllegalArgumentException` ("*Given that there is no default password encoder configured, each password must have a password encoding prefix*"), so the existing hashes in `data.sql` got `{bcrypt}` in front.

### Sample Data with BCrypt

```sql
-- All passwords are "password123", hashed with BCrypt; {bcrypt} tells the DelegatingPasswordEncoder the algorithm
INSERT INTO customers (name, email, password, phone, address, role, created_at, updated_at) VALUES
('Emma Johnson', 'emma.johnson@example.com', 
 '{bcrypt}$2a$10$wvw30spxLOR1gV/NYh86ruw8J1rPa8MvkZwG0ru7VuRECMfARo0ri', 
 '+32 470 12 34 56', 'Rue de la Loi 123, 1000 Brussels', 'CUSTOMER', 
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Admin User', 'admin@pizzastore.be', 
 '{bcrypt}$2a$10$wvw30spxLOR1gV/NYh86ruw8J1rPa8MvkZwG0ru7VuRECMfARo0ri', 
 '+32 475 67 89 01', 'Headquarters, 1000 Brussels', 'ADMIN', 
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

### Every Way In Must Hash

`AuthController.register` hashes the password, but it is not the only endpoint that creates an account: since Lesson 9 an admin can also `POST /api/customers`, and `CreateCustomerRequest` has a `password` field too. Until this lesson the mapper copied that password into the entity **as typed**: stored in plain text, and useless for logging in, because the encoder can never match it. With security in place [`CustomerService.create`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/service/CustomerService.java) hashes it as well:

```java
Customer customer = customerMapper.toEntity(request);
// the mapper copies the password as typed: hash it, so that the customer can log in (and nobody can read it)
customer.setPassword(passwordEncoder.encode(request.password()));
```

`SecurityIntegrationTest.customerCreatedByAdmin_CanLogIn` proves it end to end.

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

The response has the same shape as the one of `register`. Keep Emma's token in a shell variable for the next steps (`jq` extracts the `token` field):

```bash
CUSTOMER_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"emma.johnson@example.com","password":"password123"}' | jq -r '.token')
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

Emma is customer `1` in `data.sql` (step 10 shows how a client finds that out):

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{
    "customerId": 1,
    "orderLines": [
      {"pizzaId": 1, "quantity": 2}
    ]
  }'
```

✅ **201 Created** - Customer can create orders, for their own account

The same request with `"customerId": 2` (Liam) gives ❌ **403 Forbidden**: the URL rule lets every customer `POST /api/orders`, but the `@PreAuthorize` on `createOrder` checks that the order is for the logged-in customer (see [Method Security](#-method-security-who-owns-this-customer)).

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

The Open Food Facts import of [Lesson 11](../lesson-11-external-api/README.md) changes the menu, so it follows the same rule as creating a pizza:

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

### 10. Who Am I?

The login response has no customer id. `GET /api/customers/me` returns the account that belongs to the token:

```bash
curl http://localhost:8080/api/customers/me -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

✅ **200 OK**. Without a token: ❌ **401**.

### 11. Somebody Else's Account

```bash
curl http://localhost:8080/api/customers/1 -H "Authorization: Bearer $CUSTOMER_TOKEN"   # Emma herself: 200
curl http://localhost:8080/api/customers/2 -H "Authorization: Bearer $CUSTOMER_TOKEN"   # Liam: 403
curl http://localhost:8080/api/customers   -H "Authorization: Bearer $CUSTOMER_TOKEN"   # all customers: 403
curl http://localhost:8080/api/customers/2 -H "Authorization: Bearer $ADMIN_TOKEN"      # the admin: 200
```

The `403` is a `ProblemDetail` with `"detail" : "Access denied. You do not have permission to access this resource."`, produced by the `AccessDeniedException` handler in `GlobalExceptionHandler`. The same goes for `PUT`/`DELETE /api/customers/2` and for Liam's favorites.

---


## 🎭 Role-Based Access Control

### Access Control Summary

"Own" means: the customer whose id is in the URL (or, for an order, in `customerId`) is the logged-in customer.

| Endpoint | Method | Anonymous | CUSTOMER | ADMIN | Decided by |
|----------|--------|-----------|----------|-------|------------|
| `/api/auth/**` | ALL | ✅ | ✅ | ✅ | URL rule |
| `/api/pizzas/**` | GET | ✅ | ✅ | ✅ | URL rule |
| `/api/pizzas/**` | POST/PUT/PATCH/DELETE (incl. `/{id}/image` and `/{id}/nutritional-info/import`) | ❌ 401 | ❌ 403 | ✅ | URL rule |
| `/api/customers/me` | GET | ❌ 401 | ✅ | ✅ | URL rule |
| `/api/customers` | GET (all), POST | ❌ 401 | ❌ 403 | ✅ | URL rule + `@IsAdmin` |
| `/api/customers/{id}`, `/{id}/orders`, `/{id}/favorites/**` | GET/PUT/DELETE/POST | ❌ 401 | ✅ own only | ✅ | URL rule + `@IsAdminOrOwner` |
| `/api/orders` | POST | ❌ 401 | ✅ own only | ❌ 403 | URL rule + `@PreAuthorize` |
| `/api/orders/**` | GET/PATCH/DELETE | ❌ 401 | ❌ 403 | ✅ | URL rule |

### Two Layers

The rules live in two places, and each layer does what it is good at:

| | URL rules (`SecurityFilterChain`) | Method security (`@PreAuthorize`) |
|---|---|---|
| Where | [`SecurityConfig`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/security/SecurityConfig.java), one place | on the controller method it protects |
| When | in the `AuthorizationFilter`, before Spring MVC is even involved | when the method is called, after the request body is read and validated |
| Knows | URL, HTTP method, roles | all of that **plus the method arguments** (`#id`, `#request.customerId()`) and any bean |
| PizzaStore uses it for | "who may use this kind of endpoint" (role per URL) | "may *this* customer touch *this* customer record" |

The URL rules stay the first, coarse gate: anonymous callers never get past them, and a whole controller (`PizzaController`) is protected without a single annotation. Method security only refines.

---

## 🛂 Method Security: Who Owns This Customer?

The URL rule `/api/customers/** → hasAnyRole("CUSTOMER", "ADMIN")` has a hole: **every** customer can read, change and delete **every** other customer, and could place orders on somebody else's name. No URL pattern can fix that, because `/api/customers/1` and `/api/customers/2` look the same to a pattern; the difference is in the *data*. This is what the book calls **domain-object** or **instance-level security**, and Spring Security solves it with method security.

### 1. Switch It On: `@EnableMethodSecurity`

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig { ... }
```

Without it every `@PreAuthorize` is silently ignored: Spring Boot does **not** enable method security on its own. `@EnableMethodSecurity` (Spring Security 6+) replaces the old `@EnableGlobalMethodSecurity` and activates `@PreAuthorize`, `@PostAuthorize`, `@PreFilter` and `@PostFilter`. 

### 2. `@PreAuthorize` with SpEL

`@PreAuthorize` holds a **SpEL** expression that must be `true` before the method runs; otherwise Spring Security throws an `AccessDeniedException` (to be precise its subclass `AuthorizationDeniedException`), which `GlobalExceptionHandler` turns into a `403` `ProblemDetail`. In the expression you can use:

- `hasRole('ADMIN')`, `hasAnyRole(...)`, `hasAuthority(...)`, `isAuthenticated()`
- `authentication`: the current `Authentication` (`authentication.name` is the e-mail address)
- `#name`: a method parameter by its name (`#id`, `#request`)
- `@beanName`: any Spring bean, e.g. `@customerSecurity.isOwner(...)`

`OrderController.createOrder` uses it directly:

```java
// SecurityConfig: only a CUSTOMER may POST here; this rule adds: only for their own account
@PostMapping
@PreAuthorize("@customerSecurity.isOwner(#request.customerId(), authentication)")
public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) { ... }
```

### 3. The Ownership Check: a Bean

The question "is customer `X` the one who is logged in?" needs the database, so it lives in a small bean, [`CustomerSecurity`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/security/CustomerSecurity.java), registered under the name the expressions use:

```java
@Component("customerSecurity")
public class CustomerSecurity {
    ...
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
```

An unknown id is "not yours": a customer asking for `/api/customers/999` gets `403`, not `404`, so customers cannot probe which ids exist. An admin gets the honest `404`, because `hasRole('ADMIN')` is checked first.

### 4. Meta-Annotations: `@IsAdmin` and `@IsAdminOrOwner`

The same expression on seven methods is seven chances for a typo, and a string the compiler never checks. The book's advice: write it **once**, in a custom annotation that is itself annotated with `@PreAuthorize`. Spring Security finds it on the method's annotations:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ADMIN') or @customerSecurity.isOwner(#id, authentication)")
public @interface IsAdminOrOwner {
}
```

[`IsAdmin`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/security/IsAdmin.java) is the same with `hasRole('ADMIN')`. [`CustomerController`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/controller/CustomerController.java) now reads like a policy:

```java
@GetMapping
@IsAdmin
public ResponseEntity<Page<CustomerResponse>> getAllCustomers(Pageable pageable) { ... }

@GetMapping("/{id}")
@IsAdminOrOwner
public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long id) { ... }

// customers create their own account with POST /api/auth/register
@PostMapping
@IsAdmin
public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) { ... }

@PostMapping("/{id}/favorites/{pizzaId}")
@IsAdminOrOwner
public ResponseEntity<Void> addFavoritePizza(@PathVariable Long id, @PathVariable Long pizzaId) { ... }
```

`#id` refers to the parameter **named** `id`, so every method that carries `@IsAdminOrOwner` must call its customer-id parameter `id`. That is why the favorites endpoints changed from `{customerId}` to `{id}`; the URLs themselves are the same. (Parameter names are available at runtime because the Spring Boot parent compiles with `-parameters`, the same reason `@PathVariable Long id` works without `@PathVariable("id")`.)

### 5. Who Am I? `@AuthenticationPrincipal` and `@CurrentUser`

After logging in, a client knows its token but not its customer id. `GET /api/customers/me` answers that. `@AuthenticationPrincipal` injects the principal that `JwtAuthenticationFilter` stored in the `SecurityContext` (our `UserDetails`), and its `expression` picks one property; the meta-annotation [`@CurrentUser`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/security/CurrentUser.java) gives that a name, like the book's `@CRMUser`:

```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@AuthenticationPrincipal(expression = "username")
public @interface CurrentUser {
}
```

```java
// the account of the logged-in user: how a customer finds out their own id after logging in
@GetMapping("/me")
public ResponseEntity<CustomerResponse> getCurrentCustomer(@CurrentUser String email) {
    return ResponseEntity.ok(customerService.findByEmail(email));
}
```

No `SecurityContextHolder`, no casting in the controller, and in a test `@WithMockUser(username = "john@example.com")` is all it takes. `/me` is a literal path, so Spring MVC prefers it over `/{id}`.

### Controller or Service?

The book puts `@PreAuthorize` on a **service** method; PizzaStore puts it on the **controller**. Both work, the trade-off:

- On the **controller** the rule sits next to the URL and the path variable it checks, and it protects exactly the HTTP entry point. The service unit tests (plain Mockito, no Spring) are unaffected.
- On the **service** the rule also protects other callers of the service (a scheduled job, a second controller), but every caller then needs a logged-in user, and a `@WebMvcTest` with a mocked service no longer tests the rule.

PizzaStore's services are only called from its controllers, so the controller is the simplest correct place. If a service is shared, move the annotation there.

---

## 🌐 CORS and CSRF

Both protect a **browser** user; they matter as soon as a web front-end (React, Angular, Vue) calls PizzaStore.

### CORS: Which Websites May Call the API?

A browser enforces the **same-origin policy**: JavaScript on `http://localhost:5173` may not read responses from `http://localhost:8080` (a different port is a different origin), unless the server says it is allowed. For a "non-simple" request, like a `POST` with JSON and an `Authorization` header, the browser first sends a **preflight** `OPTIONS` request and only sends the real request if the answer contains the right `Access-Control-Allow-*` headers. curl, Postman and other servers do not care about CORS at all: it protects users of a browser, not the API.

PizzaStore configures it in `SecurityConfig`:

```java
.cors(Customizer.withDefaults())          // in the filter chain: use the CorsConfigurationSource bean

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
```

```properties
# CORS: browser front-ends that may call the API from another origin (comma separated)
pizzastore.cors.allowed-origins=http://localhost:3000,http://localhost:5173
```

Configure CORS **in Spring Security**, not only in Spring MVC (`WebMvcConfigurer.addCorsMappings`): the preflight `OPTIONS` request carries no token, so if the security filter chain does not handle CORS itself, it rejects the preflight with a `401` before Spring MVC can answer it. `.cors(Customizer.withDefaults())` puts the `CorsFilter` in the chain, in front of authentication. (Spring Security 7 also does this on its own when it finds a `UrlBasedCorsConfigurationSource` bean, but the explicit line documents the intent and does not depend on the bean's declared type.) Try it:

```bash
curl -i -X OPTIONS http://localhost:8080/api/orders \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: authorization,content-type"
```

✅ `200` with `Access-Control-Allow-Origin: http://localhost:5173`. With `-H "Origin: https://evil.example.com"`: ❌ `403 Invalid CORS request`. Never use `*` as allowed origin for an API that accepts credentials; list the front-ends you trust.

### CSRF: Why It Is Off

**Cross-Site Request Forgery** abuses the fact that a browser automatically sends **cookies** with every request to a site: a malicious page can make your browser `POST` to your bank, and the bank sees your session cookie. Spring Security's CSRF protection (on by default) defends against that with a secret token that the malicious page cannot know.

PizzaStore **disables** it, deliberately:

```java
.csrf(AbstractHttpConfigurer::disable)
```

- The JWT travels in the `Authorization` header, which a browser **never adds automatically**: the client's JavaScript has to set it. A forged request from another site arrives without a token and is anonymous.
- There is no session cookie (`SessionCreationPolicy.STATELESS`), so there is nothing to abuse.

The rule of thumb: **cookie-based authentication needs CSRF protection, header-based tokens do not.** If you ever put the JWT in a cookie (to keep it out of reach of JavaScript, as `httpOnly` cookies do), CSRF protection must come back on. The book's Listing 11-7 shows that variant for single-page apps with a session: `.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))`, a CSRF token in a cookie that the JavaScript copies into a header.

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
# the value after ':' is only a development default; in production JWT_SECRET is set and wins
jwt.secret=${JWT_SECRET:MySecretKeyForJWTTokenGenerationThatShouldBeAtLeast256BitsLong}
```

That is exactly the line in PizzaStore's `application.properties`. Leave out the default (`${JWT_SECRET}`) and the application refuses to start without the variable, which is even safer for production.

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

### 7. Role Rules Centrally, Ownership Rules on the Method

Keep the **role** rules in one place, the `SecurityFilterChain`: one overview of who may call what, `anyRequest().authenticated()` as a safety net, and controllers like [`PizzaController`](pizzastore-with-jwt/src/main/java/be/vives/pizzastore/controller/PizzaController.java) that stay **byte-for-byte the same** as in Lesson 12.

❌ **Don't** repeat what a URL rule already says on every method:

```java
@PreAuthorize("hasRole('ADMIN')")              // SecurityConfig already says: POST /api/pizzas/** is for ADMIN
@PostMapping
public ResponseEntity<PizzaResponse> createPizza(@Valid @RequestBody CreatePizzaRequest request) { ... }
```

Two places for one rule drift apart, and nobody knows which one is the real one.

✅ **Do** use method security for what a URL rule *cannot* express, the rules that depend on the arguments or the data, and give the expression a name with a meta-annotation:

```java
@GetMapping("/{id}")
@IsAdminOrOwner                                // hasRole('ADMIN') or @customerSecurity.isOwner(#id, authentication)
public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long id) { ... }
```

`@IsAdmin` on `getAllCustomers` and `createCustomer` is the one exception to "don't repeat": the URL rule for `/api/customers/**` is deliberately broad (customers need `/me` and their own record), so the admin-only methods inside it say so themselves.

---

## 🧪 Testing Secured Controllers

Add Spring Security to a tested application and many of the tests of [Lesson 12](../lesson-12-testing/README.md) turn red: a `POST /api/pizzas` without a user now gets `401`, and full-stack tests that create data need a token. That is the safety net doing its job. This section shows how the test suite of [`pizzastore-with-jwt`](pizzastore-with-jwt/src/test/java/be/vives/pizzastore) deals with security, at two levels:

| Level | How the user is simulated | Proves |
|-------|---------------------------|--------|
| `@WebMvcTest` slice | `@WithMockUser(roles = "...")`, `@WithAnonymousUser` | the URL rules of `SecurityConfig` and the `@PreAuthorize` rules, per role, in milliseconds |
| `@SpringBootTest` with a real server | real accounts and real JWT tokens, sent as `Authorization: Bearer ...` | the whole chain: login, token, filter, role check, ownership check against the database, controller, CORS |

### Slice Tests: `@WithMockUser`

[`PizzaControllerTest`](pizzastore-with-jwt/src/test/java/be/vives/pizzastore/controller/PizzaControllerTest.java) is a `@WebMvcTest` with `RestTestClient`, as in Lesson 12, plus the real security configuration:

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

1. **`@Import(SecurityConfig.class)`**: a slice does not scan `@Configuration` classes (the same lesson as `@Import(JpaConfig.class)` in Lesson 12). Without the import the slice would use Spring Boot's default security (every request needs a login, CSRF protection on), and the tests would check rules PizzaStore does not have.
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

#### Method Security in the Slice

`@WebMvcTest` does not load `CustomerSecurity` (a plain `@Component`), but the `@IsAdminOrOwner` expression calls it by name. [`CustomerControllerTest`](pizzastore-with-jwt/src/test/java/be/vives/pizzastore/controller/CustomerControllerTest.java) therefore registers a mock **under that name** and decides who owns what:

```java
// the bean that @IsAdminOrOwner calls by name: @customerSecurity.isOwner(#id, authentication)
@MockitoBean(name = "customerSecurity")
private CustomerSecurity customerSecurity;

@BeforeEach
void ownsCustomer1() {
    when(customerSecurity.isOwner(eq(1L), any())).thenReturn(true);
}

@Test
@WithMockUser(roles = "CUSTOMER")
void getCustomer_ofAnotherCustomer_returnsForbidden() {
    client.get().uri("/api/customers/{id}", 2)        // not stubbed: the mock answers false
            .exchange()
            .expectStatus().isForbidden();

    verify(customerService, never()).findById(any());
}

@Test
@WithMockUser(username = "john@example.com", roles = "CUSTOMER")
void getCurrentCustomer_shouldReturnTheLoggedInCustomer() {
    when(customerService.findByEmail("john@example.com")).thenReturn(john());

    client.get().uri("/api/customers/me")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.email").isEqualTo("john@example.com");
}
```

The slice tests the *rule* (`@IsAdminOrOwner` is on the method and is enforced); whether `CustomerSecurity` itself compares the right e-mail addresses is proven by the full-stack tests below, with a real database. `OrderControllerTest` does the same for `createOrder` (`createOrder_forAnotherCustomer_returnsForbidden`). A quick sanity check that these tests really test something: comment out `@EnableMethodSecurity` and eight tests turn red (six in the slices, two full-stack).

> **Multipart in a slice.** The four image-upload tests are in a separate class, [`PizzaImageUploadControllerTest`](pizzastore-with-jwt/src/test/java/be/vives/pizzastore/controller/PizzaImageUploadControllerTest.java), that uses MockMvc instead of `RestTestClient`: a `RestTestClient` bound to MockMvc does not turn a multipart body into request parts (Lesson 12, *Which one should I use?*). `@WithMockUser` works with both.

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

The other tests of the class: anonymous users can read pizzas but get `401` when they try to create one, a customer gets `403` and an admin `201`, a garbage token gives `401` (not a `500`), a wrong password gives `401`, and the token returned by `register` works immediately. And for the method-level rules and CORS: `/me` returns the account of the token, a customer cannot read another customer (the admin can) or order for them, a customer created by an admin can log in (the password was hashed), and a CORS preflight from `http://localhost:3000` is accepted while one from an unknown origin is rejected.

The full-stack tests of Lesson 12 needed the same change: [`PizzaStoreApiIntegrationTest`](pizzastore-with-jwt/src/test/java/be/vives/pizzastore/integration/PizzaStoreApiIntegrationTest.java) creates an admin and a customer in `@BeforeEach` and sends their tokens with every request that needs one; its order scenarios place every order for that customer's own account, because ordering for somebody else is now a `403`. The MockMvc-based [`PizzaIntegrationTest`](pizzastore-with-jwt/src/test/java/be/vives/pizzastore/integration/PizzaIntegrationTest.java) uses `@WithMockUser(roles = "ADMIN")`, which works because MockMvc runs on the test thread.

### The Test Suite of This Lesson

`mvn test` runs **252 tests**:

| Kind | Test classes | Tests |
|------|--------------|------:|
| Unit (no Spring) | `PizzaServiceTest`, `CustomerServiceTest`, `OrderServiceTest`, `NutritionImportServiceTest`, `RequestValidationTest`, `PizzaControllerStandaloneTest` | 97 |
| Slice | `PizzaRepositoryTest`, `CustomerRepositoryTest`, `OrderRepositoryTest` (`@DataJpaTest`) | 40 |
| | `PizzaControllerTest`, `CustomerControllerTest`, `OrderControllerTest`, `AuthControllerTest` (`@WebMvcTest` + `RestTestClient` + security), `PizzaImageUploadControllerTest` (MockMvc) | 68 |
| | `PizzaJsonTest`, `PizzaMapperTest`, `OpenFoodFactsClientTest` | 13 |
| Full stack | `ApplicationSmokeTest`, `PizzaIntegrationTest`, `PizzaStoreApiIntegrationTest`, `BeanOverrideIntegrationTest`, `SecurityIntegrationTest` | 34 |

Compared with Lesson 12, this is the test suite of the final PizzaStore: the controller tests are written with `RestTestClient` only (Lesson 12's side-by-side comparison classes `PizzaControllerRestTestClientTest` and `PizzaControllerMockMvcTesterTest` have done their job and are not carried over), and `AuthControllerTest`, `SecurityIntegrationTest`, the role and ownership tests and the password-hashing tests in `CustomerServiceTest` are new.

### Best Practices for Security Testing

1. **Test every access level**: anonymous, CUSTOMER and ADMIN, for each kind of endpoint, and for ownership rules both "mine" and "somebody else's".
2. **Test what is forbidden**, not only what is allowed, and prove with `verify(..., never())` that the controller was never reached.
3. **Import the real `SecurityConfig`** in slice tests, so you test your rules instead of Spring Boot's defaults.
4. **Keep the security tests in the slice** (fast, one rule per test) and add a few full-stack tests with real tokens for the chain itself.
5. **Never weaken security for tests** (no `permitAll()` profile for tests): test the configuration that runs in production.

---

## 🔁 Alternative: Spring Security's OAuth2 Resource Server

This lesson writes `JwtUtil` and `JwtAuthenticationFilter` by hand, because that shows every step: read the header, verify the signature, check the expiration, fill the `SecurityContext`. In production code you would usually let Spring Security do those steps. The book mentions the starter for it in *Modern Security Architecture*: **`spring-boot-starter-oauth2-resource-server`**, "lightweight support for securing APIs with JWTs". A *resource server* is OAuth2's name for an API that accepts tokens (see [Beyond This Lesson](#-beyond-this-lesson-more-security-in-the-book)).

The same PizzaStore, with a symmetric key like `jwt.secret` and without JJWT:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

```java
// SecurityConfig: no JwtAuthenticationFilter any more
private final SecretKey key;

public SecurityConfig(@Value("${jwt.secret}") String secret) {
    this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
}

@Bean
public JwtEncoder jwtEncoder() {                       // signs the tokens that AuthController hands out
    return new NimbusJwtEncoder(new ImmutableSecret<>(key));
}

@Bean
public JwtDecoder jwtDecoder() {                       // verifies signature and expiration of every request
    return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
}

// the "role" claim becomes the authority ROLE_<role>, so hasRole("ADMIN") keeps working
@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
    authorities.setAuthoritiesClaimName("role");
    authorities.setAuthorityPrefix("ROLE_");
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authorities);
    return converter;
}

// in securityFilterChain(...), instead of .addFilterBefore(jwtAuthenticationFilter, ...):
.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
```

```java
// JwtUtil shrinks to issuing tokens
public String generateToken(String username, String role) {
    Instant now = Instant.now();
    JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(username)
            .claim("role", role)
            .issuedAt(now)
            .expiresAt(now.plus(expiration))
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
}
```

This variant was tried on a copy of `pizzastore-with-jwt`: all full-stack tests (`SecurityIntegrationTest`, `PizzaStoreApiIntegrationTest`, `BeanOverrideIntegrationTest`) pass unchanged. What changes:

| | Own filter (this lesson) | Resource server |
|---|---|---|
| Code | `JwtUtil` (create + parse), `JwtAuthenticationFilter`, JJWT | three beans and one line; JJWT not needed |
| Per request | loads the user from the database (`UserDetailsService`) | **no database access**: the roles come from the token's `role` claim |
| Role changed or account deleted | effective at the next request | only when the token expires (a reason for short lifetimes) |
| Principal | `UserDetails` (`@AuthenticationPrincipal(expression = "username")`) | `Jwt` (`expression = "subject"`); `authentication.name` is the subject in both |
| Invalid token on a **public** endpoint | ignored: anonymous, `GET /api/pizzas` still answers `200` | `401` with `WWW-Authenticate: Bearer error="invalid_token"` |
| Tokens from somebody else | no | yes: point `spring.security.oauth2.resourceserver.jwt.issuer-uri` at Keycloak, Okta, ... and remove the `JwtEncoder` |

The last row is the real reason to know it: as soon as tokens come from a separate **authorization server**, the API only *verifies* tokens, and the resource server does that with one property.

---

## 📚 Beyond This Lesson: More Security in the Book

Chapter 11 of the book covers more than a JWT-secured REST API needs. What PizzaStore does **not** use, and why:

| Topic (book, Chapter 11) | What it is | Why not in PizzaStore |
|---|---|---|
| **One-Time Token login** (p. 295-299), `oneTimeTokenLogin()` | "magic link" login: no password, a single-use token is mailed to the user | a browser flow with redirects and a mail server; PizzaStore's clients are API clients that log in once and get a JWT |
| **Two-factor authentication** (p. 299-302), TOTP with Google Authenticator | a second proof (a 6-digit code) on top of the password, or only for sensitive paths ("step-up") | would need a second login step in the API; worth it for admin accounts in a real shop |
| **Passkeys / WebAuthn** (Pro Tip p. 302), `webAuthn()` | log in with FaceID, TouchID or a hardware key instead of a password | browser API; same reason as OTT |
| **OAuth2 / OpenID Connect** (p. 303-305) | log in at an external identity provider (Keycloak, Okta, GitHub); the API is a resource server | PizzaStore has its own customer accounts; see the [resource server alternative](#-alternative-spring-securitys-oauth2-resource-server) for the API side |
| **Reactive security** (p. 305-308), `SecurityWebFilterChain`, `ReactiveSecurityContextHolder` | the same ideas for WebFlux, where the context is not stored in a `ThreadLocal` | PizzaStore uses Spring MVC (servlet stack) |
| **Security observability** (p. 311) | authentication failures as OpenTelemetry spans | no tracing in this course |
| **mTLS and SSL bundles** (p. 312-318) | services authenticate each other with certificates (zero trust) | PizzaStore calls one public API (Open Food Facts), see Lesson 11 |

The concepts stay the same in all of them: a filter chain that authenticates, a `SecurityContext` that holds the result, and authorization rules on URLs and methods. Chapter 13 (Listing 13-5) applies them to Spring Boot Actuator: `EndpointRequest.to("health", "info")` public, every other actuator endpoint for admins.

Two points where the book does not match Spring Security 7.0 as used here (verified against `spring-security-config` 7.0.7):

- Chapter 18 (p. 495-496) presents `@EnableTwoFactorAuthentication` and a `TwoFactorAuthenticationManager`. These classes do not exist; Spring Security 7's multi-factor support is `@EnableMultiFactorAuthentication` together with `FactorGrantedAuthority`, and `oneTimeTokenLogin()` is an ordinary part of `spring-security-web`/`-config`, included in `spring-boot-starter-security`. It needs no `spring-security-cas` dependency, which Listing 11-10 adds "for OTT": CAS is an unrelated single-sign-on protocol.
- *Fine-Grained Authorization* says method security "is enabled by default in many starters". In Spring Boot it is not: without `@EnableMethodSecurity` a `@PreAuthorize` is ignored, which the mutation check in [Method Security in the Slice](#method-security-in-the-slice) shows.

---

## 📝 Summary

In this lesson, you learned:

✅ **Spring Security fundamentals** - Authentication and authorization  
✅ **JWT structure** - Header, payload, and signature  
✅ **Security configuration** - Stateless authentication with JWT  
✅ **JWT generation and validation** - Using jjwt library  
✅ **Password hashing** - BCrypt behind a `DelegatingPasswordEncoder`, on every path that creates an account  
✅ **Role-based access control** - CUSTOMER and ADMIN roles, as URL rules  
✅ **Method security** - `@EnableMethodSecurity`, `@PreAuthorize`, meta-annotations, an ownership check bean  
✅ **The current user** - `@AuthenticationPrincipal` behind `@CurrentUser`, `GET /api/customers/me`  
✅ **CORS and CSRF** - which one a token API needs, and why  
✅ **Authentication endpoints** - Register and login  
✅ **Testing secured endpoints** - With and without JWT tokens  
✅ **The resource-server alternative** - Spring Security verifying the JWT for you

### Key Takeaways

1. **JWT is stateless**: No server-side session storage
2. **Always hash passwords**: Never store plain text passwords
3. **Use role-based access**: Restrict endpoints based on user roles, centrally, and end with `anyRequest().authenticated()`
4. **Check ownership on the method**: a role says *what kind* of user you are, not *whose* data you may touch
5. **Secure your secret**: Store JWT secret in environment variables
6. **Test thoroughly**: Verify all access control rules work correctly, the allowed and the forbidden cases

---

**Note on the book**: *Pro Spring Boot 4* covers security in Chapter 11, *Securing Spring Boot Applications* (p. 281-320). This lesson follows its first part closely:

- *Modern Security Architecture* (p. 282-286): the security filter chain, the lambda DSL and component-based `SecurityFilterChain` beans, the modular starters, and *Advanced Request Matching* with `requestMatchers` per HTTP method, `hasRole` vs. `hasAuthority` and the catch-all `anyRequest().authenticated()` (Listing 11-1) → [Security Configuration](#%EF%B8%8F-security-configuration).
- *Fine-Grained Authorization: RBAC and Method Security* (p. 287-291): `@EnableMethodSecurity`, `@PreAuthorize` with SpEL, domain-object (instance-level) security, the `@IsAdmin` meta-annotation (Listing 11-4) and `@AuthenticationPrincipal` behind a custom annotation (`@CRMUser`, Listing 11-5) → [Method Security](#-method-security-who-owns-this-customer), with `@IsAdmin`, `@IsAdminOrOwner` and `@CurrentUser`.
- *Exploit Protection: CSRF and CORS* (p. 291-292, Listing 11-7) → [CORS and CSRF](#-cors-and-csrf). The book keeps CSRF on with a cookie-based token for single-page apps; PizzaStore turns it off because its token is a header, not a cookie.
- *Secure Credential Storage: Password Encoding* (p. 292-293): BCrypt behind `PasswordEncoderFactories.createDelegatingPasswordEncoder()` (Listing 11-8), exactly what `SecurityConfig.passwordEncoder()` does → [Password Encryption](#-password-encryption).
- *The Identity Contract* (p. 293-294): a custom `UserDetailsService` on top of the repository with the `User.withUsername(...)` builder (Listing 11-9) → [`CustomUserDetailsService`](#custom-userdetailsservice-implementation).
- *Modern Identity: OAuth2 and JWT* (p. 303-305): stateless vs. session-based security (Table 11-1) and the OAuth2 roles. The book lets an external identity provider issue the tokens and **does not build its own JWT filter**; this lesson does, because writing `JwtUtil` and `JwtAuthenticationFilter` yourself shows what a resource server does behind the scenes. [The resource-server alternative](#-alternative-spring-securitys-oauth2-resource-server) shows the book's starter doing the same job.
- *Testing Your Security* (p. 309-311): a `@SpringBootTest` with a test client, `@WithMockUser` and `mockOidcLogin()` → [Testing Secured Controllers](#-testing-secured-controllers). (Listing 11-19 combines `@AutoConfigureRestTestClient` with a `WebTestClient` field and an outdated package; see Lesson 12 for the working Spring Boot 4 imports.)

The rest of the chapter (one-time tokens, 2FA, passkeys, OIDC login, reactive security, observability, mTLS) is summarised in [Beyond This Lesson](#-beyond-this-lesson-more-security-in-the-book), together with two places where the book does not match Spring Security 7.

---

## 🚀 Runnable Project

**[`pizzastore-with-jwt/`](pizzastore-with-jwt)** is Lesson 12's [`pizzastore-with-tests`](../lesson-12-testing/pizzastore-with-tests) plus the changes listed in [What This Lesson Adds to PizzaStore](#-what-this-lesson-adds-to-pizzastore). Apart from the OpenAPI documentation of Lesson 14, it is the final PizzaStore.

### Features

✅ **Spring Boot 4.0** on **Java 25**, Spring Security 7, JJWT 0.13  
✅ **JWT Authentication**: token-based, stateless  
✅ **User Registration & Login**: `POST /api/auth/register`, `POST /api/auth/login`  
✅ **Role-Based Access Control**: CUSTOMER and ADMIN roles, see the [access table](#access-control-summary)  
✅ **Method Security**: a customer only reads and changes their own account and orders only for themselves; `GET /api/customers/me`  
✅ **Password Hashing**: BCrypt behind a `DelegatingPasswordEncoder`  
✅ **CORS** for browser front-ends on `localhost:3000` and `localhost:5173`  
✅ **Everything from Lessons 6a-12**, including the Open Food Facts import (admin only)  
✅ **252 tests**, including role and ownership tests in the web slice and end-to-end tests with real tokens  
❌ No API documentation yet: Lesson 14

### Test Accounts

All accounts in `data.sql` have the password `password123`.

| Id | Email | Role |
|----|-------|------|
| 1 | `emma.johnson@example.com` | `CUSTOMER` |
| 2 | `liam.smith@example.com` | `CUSTOMER` |
| 3 | `admin@pizzastore.be` | `ADMIN` |

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

# Login as Emma: her own account works, Liam's does not
CUSTOMER_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"emma.johnson@example.com","password":"password123"}' | jq -r '.token')
curl http://localhost:8080/api/customers/me -H "Authorization: Bearer $CUSTOMER_TOKEN"                        # 200
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/customers/2 -H "Authorization: Bearer $CUSTOMER_TOKEN"   # 403
```

Port 8080 taken by another application? Start with `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` and use that port in the examples.

---

🎉 You've successfully implemented JWT authentication in your Spring Boot application! Continue to [Lesson 14: Swagger/OpenAPI](../lesson-14-swagger-openapi/README.md) to document the secured API.
