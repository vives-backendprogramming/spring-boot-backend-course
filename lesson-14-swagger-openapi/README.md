# Lesson 14: API Documentation with Swagger/OpenAPI

---

## 📋 Table of Contents

1. [Learning Objectives](#-learning-objectives)
2. [Introduction to OpenAPI](#-introduction-to-openapi)
3. [Integrating Springdoc OpenAPI](#-integrating-springdoc-openapi)
4. [Configuring OpenAPI](#-configuring-openapi)
5. [Documenting Controllers](#-documenting-controllers)
6. [Documenting DTOs](#-documenting-dtos)
7. [Security Configuration](#-security-configuration)
8. [Handling Pageable Parameters](#-handling-pageable-parameters)
9. [Accessing Swagger UI](#-accessing-swagger-ui)
10. [Testing with Swagger UI](#-testing-with-swagger-ui)
11. [Best Practices](#-best-practices)
12. [Runnable Project](#-runnable-project)

---

## 🎯 Learning Objectives

By the end of this lesson, you will be able to:

- ✅ Understand the OpenAPI Specification (OAS)
- ✅ Integrate Springdoc OpenAPI into a Spring Boot application
- ✅ Configure API metadata and security schemes
- ✅ Document REST API endpoints with annotations
- ✅ Document DTOs with schema descriptions and examples
- ✅ Access and use Swagger UI for API testing
- ✅ Test authenticated endpoints via Swagger UI
- ✅ Generate OpenAPI specifications in JSON/YAML format
- ✅ Apply best practices for API documentation

---

## 📖 Introduction to OpenAPI

### What is OpenAPI?

**OpenAPI Specification (OAS)** is a standard, language-agnostic interface description for HTTP APIs. It allows both humans and computers to discover and understand the capabilities of a service without access to source code or additional documentation.

### Why Use OpenAPI?

1. **Interactive Documentation**: Automatically generates browsable, interactive API documentation
2. **Client Generation**: Generate client SDKs in multiple languages from the specification
3. **API Testing**: Test API endpoints directly from the documentation interface
4. **Contract-First Development**: Define the API contract before implementation
5. **Standardization**: Industry-standard format supported by many tools
6. **Mobile Integration**: Frontend and mobile developers can easily understand and integrate with your API

### Swagger vs OpenAPI

- **OpenAPI**: The specification standard (formerly known as Swagger Specification)
- **Swagger**: A set of tools for implementing OpenAPI (Swagger UI, Swagger Editor, etc.)
- **Springdoc OpenAPI**: Java library that generates OpenAPI documentation from Spring Boot code

---

## 🔧 Integrating Springdoc OpenAPI

### Add Maven Dependency

Add the `springdoc-openapi-starter-webmvc-ui` dependency to your `pom.xml`:

```xml
<properties>
    <springdoc.version>2.8.13</springdoc.version>
</properties>

<dependencies>
    <!-- SpringDoc OpenAPI (Swagger) -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>${springdoc.version}</version>
    </dependency>
</dependencies>
```

This single dependency includes:
- OpenAPI specification generation
- Swagger UI interface
- Automatic endpoint discovery

### Default Endpoints

Once added, Springdoc automatically exposes:

| Endpoint | Description |
|----------|-------------|
| `/swagger-ui.html` | Interactive Swagger UI interface (redirects to `/swagger-ui/index.html`) |
| `/swagger-ui/index.html` | Actual Swagger UI page |
| `/v3/api-docs` | OpenAPI specification in JSON format |
| `/v3/api-docs.yaml` | OpenAPI specification in YAML format |

**No additional configuration is required!** The library automatically scans your controllers and generates documentation.

---

## ⚙️ Configuring OpenAPI

### Basic Configuration Class

Create an `OpenApiConfig` class to define global API metadata:

```java
package be.vives.pizzastore.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "PizzaStore API",
                version = "1.0.0",
                description = """
                        RESTful API for managing a pizza store.
                        
                        **Authentication**: JWT Bearer token authentication.
                        
                        **Authorization**:
                        - Anonymous users: Can view available pizzas
                        - CUSTOMER role: Can place orders and manage profile
                        - ADMIN role: Can manage pizzas and view all orders
                        """,
                contact = @Contact(
                        name = "VIVES",
                        email = "yves.seurynck@vives.be",
                        url = "https://www.vives.be"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local Development"),
                @Server(url = "https://api.pizzastore.example.com", description = "Production")
        },
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT authentication token. Obtain via /api/auth/register or /api/auth/login"
)
public class OpenApiConfig {
}
```

**Key Elements:**

- `@OpenAPIDefinition`: Global API metadata
- `@Info`: API title, version, description, contact, and license
- `@Server`: Available servers (development, staging, production)
- `@SecurityScheme`: Authentication mechanism (JWT Bearer token)
- `@SecurityRequirement`: Global security requirement

### Application Properties Configuration

Fine-tune Springdoc behavior in `application.properties`:

```properties
# SpringDoc OpenAPI Configuration
springdoc.api-docs.enabled=true
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.enabled=true
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operations-sorter=method
springdoc.swagger-ui.tags-sorter=alpha
springdoc.swagger-ui.try-it-out-enabled=true
```

**Common Properties:**

| Property | Description | Default |
|----------|-------------|---------|
| `springdoc.api-docs.enabled` | Enable/disable API docs | `true` |
| `springdoc.api-docs.path` | Path for OpenAPI JSON | `/v3/api-docs` |
| `springdoc.swagger-ui.enabled` | Enable/disable Swagger UI | `true` |
| `springdoc.swagger-ui.path` | Path for Swagger UI | `/swagger-ui.html` |
| `springdoc.swagger-ui.operations-sorter` | Sort operations by method or alpha | - |
| `springdoc.swagger-ui.tags-sorter` | Sort tags alphabetically | - |
| `springdoc.swagger-ui.try-it-out-enabled` | Enable "Try it out" button | `true` |

---

## 📝 Documenting Controllers

### Controller-Level Documentation

Use `@Tag` to group related endpoints:

```java
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/pizzas")
@Tag(name = "Pizza Management", description = "APIs for managing pizzas (menu items)")
public class PizzaController {
    // ...
}
```

### Endpoint-Level Documentation

Document individual endpoints with `@Operation`, `@ApiResponses`, and `@Parameter`:

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@GetMapping("/{id}")
@Operation(
        summary = "Get pizza by ID",
        description = "Retrieves a single pizza by its unique identifier"
)
@ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Pizza found",
                content = @Content(schema = @Schema(implementation = PizzaResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Pizza not found")
})
public ResponseEntity<PizzaResponse> getPizza(
        @Parameter(description = "Pizza ID", required = true) @PathVariable Long id) {
    // Implementation
}
```

**OpenAPI Annotations Explained:**

| Annotation | Purpose | Usage |
|------------|---------|-------|
| `@Operation` | Describes the operation (endpoint) | Provides summary and detailed description |
| `@ApiResponses` | Documents all possible HTTP responses | Contains array of `@ApiResponse` annotations |
| `@ApiResponse` | Documents a single HTTP response | Specifies responseCode, description, and content |
| `@Content` | Describes response/request body content | Specifies media type and schema |
| `@Schema` | Links to a Java class/type for the response body | Uses `implementation` to reference DTO classes |
| `@Parameter` | Documents method parameters | Provides description and whether it's required |

### Secured Endpoints

For endpoints requiring authentication, add `@SecurityRequirement`:

```java
@PostMapping
@Operation(
        summary = "Create a new pizza",
        description = "Creates a new pizza. Requires ADMIN role.",
        security = @SecurityRequirement(name = "bearerAuth")
)
@ApiResponses(value = {
        @ApiResponse(
                responseCode = "201",
                description = "Pizza created successfully",
                content = @Content(schema = @Schema(implementation = PizzaResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
        @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required")
})
public ResponseEntity<PizzaResponse> createPizza(@RequestBody CreatePizzaRequest request) {
    // Implementation
}
```

#### Understanding @SecurityRequirement

The `@SecurityRequirement` annotation links an endpoint to a security scheme defined in your OpenAPI configuration. 

**Key Points:**
- **Global Security**: When applied in `@OpenAPIDefinition(security = ...)`, all endpoints are secured by default
- **Endpoint-Level Security**: Use `@SecurityRequirement` at method level to override global settings
- **Public Endpoints**: Use `@SecurityRequirement(name = "")` or omit the annotation to make an endpoint public (when global security is enabled)
- **Multiple Schemes**: You can apply multiple security requirements: `security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "apiKey")}`

#### Available Security Scheme Types

Spring Boot applications can use various authentication methods. Here are the most common types:

| Security Scheme Type | Description | Use Case | Configuration Example |
|---------------------|-------------|----------|----------------------|
| **HTTP Bearer (JWT)** | Bearer token authentication using JWT | Most common for REST APIs. Token in `Authorization: Bearer <token>` header | `type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT"` |
| **HTTP Basic** | Username and password encoded in Base64 | Simple authentication, less secure | `type = SecuritySchemeType.HTTP, scheme = "basic"` |
| **API Key** | Custom API key in header, query, or cookie | Third-party integrations, rate limiting | `type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER, paramName = "X-API-Key"` |
| **OAuth2** | OAuth 2.0 authorization flows | Social login, enterprise SSO | `type = SecuritySchemeType.OAUTH2, flows = @OAuthFlows(...)` |
| **OpenID Connect** | OpenID Connect Discovery | Identity Provider (IdP) authentication like Keycloak, Dex | `type = SecuritySchemeType.OPENIDCONNECT, openIdConnectUrl = "..."` |

#### JWT Bearer Authentication (Current Implementation)

Our PizzaStore API uses JWT Bearer authentication:

```java
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT authentication token. Obtain via /api/auth/login"
)
```

**How it works in Swagger UI:**
1. Click the **"Authorize"** button (lock icon) in Swagger UI
2. Enter your JWT token in the format: `Bearer <your-jwt-token>` or just `<your-jwt-token>`
3. Click **"Authorize"**
4. All subsequent requests will include the token in the `Authorization` header

#### OAuth2 / OpenID Connect (IdP Authentication)

For Identity Provider (IdP) based authentication (covered in Lesson 13), you would configure:

```java
@SecurityScheme(
    name = "oidc",
    type = SecuritySchemeType.OPENIDCONNECT,
    openIdConnectUrl = "http://localhost:5556/.well-known/openid-configuration",
    description = "OpenID Connect authentication via Dex IdP"
)

// OR using OAuth2 with specific flows:
@SecurityScheme(
    name = "oauth2",
    type = SecuritySchemeType.OAUTH2,
    flows = @OAuthFlows(
        authorizationCode = @OAuthFlow(
            authorizationUrl = "http://localhost:5556/auth",
            tokenUrl = "http://localhost:5556/token",
            scopes = {
                @OAuthScope(name = "openid", description = "OpenID Connect scope"),
                @OAuthScope(name = "profile", description = "User profile access"),
                @OAuthScope(name = "email", description = "User email access")
            }
        )
    )
)
```

**Key Difference between JWT and IdP:**
- **JWT (Lesson 12)**: Your application manages users, passwords, and token generation
- **IdP (Lesson 13)**: External identity provider (like Dex, Keycloak, Google, Azure AD) manages authentication
- **Both** can use the same `@SecurityRequirement` annotations on endpoints
- **Swagger UI** integration is simpler with JWT, more complex with OAuth2/OIDC flows

---

## 🏷️ Documenting DTOs

### Using @Schema on Records

For request/response DTOs, use `@Schema` annotations to provide descriptions and examples:

```java
package be.vives.pizzastore.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Request object for creating a new pizza")
public record CreatePizzaRequest(

        @NotBlank(message = "Pizza name is required")
        @Schema(description = "Name of the pizza", example = "Margherita", required = true)
        String name,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be positive")
        @Schema(description = "Price of the pizza in EUR", example = "12.50", required = true)
        BigDecimal price,

        @Schema(description = "Description of the pizza", 
                example = "Classic pizza with tomato sauce, mozzarella, and fresh basil")
        String description,

        @Schema(description = "Whether the pizza is available for ordering", 
                example = "true", 
                defaultValue = "true")
        Boolean available
) {
}
```

### Using @Schema on Classes

For traditional Java classes:

```java
package be.vives.pizzastore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request object for user login")
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(description = "User's email address", 
            example = "john.doe@example.com", 
            required = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(description = "User's password", 
            example = "password123", 
            required = true)
    private String password;

    // Constructors, getters, setters...
}
```

### Validation Annotations Integration

**Springdoc automatically reflects Bean Validation annotations in the OpenAPI spec!**

```java
@Schema(description = "Product creation request")
public record ProductRequest(

        @NotNull(message = "Name is required")
        @Size(min = 3, max = 100)
        @Schema(description = "Product name", example = "Laptop", required = true)
        String name,

        @DecimalMin(value = "0.01")
        @DecimalMax(value = "999999.99")
        @Schema(description = "Product price in EUR", example = "299.99", required = true)
        BigDecimal price,

        @Min(value = 0)
        @Max(value = 10000)
        @Schema(description = "Available quantity", example = "100", required = true)
        Integer quantity
) {
}
```

Validation constraints like `@NotNull`, `@Size`, `@Min`, `@Max`, `@Pattern`, and `@Email` are automatically included in the generated OpenAPI specification!

---

## 🔒 Security Configuration

### Allowing Swagger UI in Security Config

When using Spring Security, you **must allow public access** to Swagger UI endpoints:

```java
package be.vives.pizzastore.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/pizzas/**").permitAll()
                        
                        // Swagger/OpenAPI endpoints - PUBLIC ACCESS!
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                        .permitAll()
                        
                        // Secured endpoints
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
```

**Important paths to allow:**
- `/swagger-ui/**` - Swagger UI static resources (CSS, JS, etc.)
- `/swagger-ui.html` - Swagger UI entry point
- `/v3/api-docs/**` - OpenAPI specification endpoints

---

## 📄 Handling Pageable Parameters

### The Problem with Pageable in Swagger

When using Spring Data's `Pageable` as a controller parameter, Swagger UI may not correctly represent it as individual query parameters (`page`, `size`, `sort`). Instead, it might display a JSON object input, which causes errors when testing.

**Example of the problem:**

```java
@GetMapping
public ResponseEntity<Page<PizzaResponse>> getPizzas(Pageable pageable) {
    // ...
}
```

In Swagger UI, this might show:
```json
{
  "page": 0,
  "size": 20,
  "sort": ["id"]
}
```

When executing, this results in an error:
```
No property '["id"]' found for type 'Pizza'
```

### The Solution: PageableArgumentResolver

Add a custom `PageableArgumentResolver` bean to properly configure how Springdoc interprets `Pageable` parameters:

```java
package be.vives.pizzastore.config;

import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

@Configuration
public class SpringDocConfig {

    @Bean
    public PageableHandlerMethodArgumentResolver pageableResolver() {
        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
        resolver.setMaxPageSize(100);
        resolver.setFallbackPageable(org.springframework.data.domain.PageRequest.of(0, 20));
        return resolver;
    }
}
```

### Alternative: Use @ParameterObject

You can also explicitly tell Springdoc to treat `Pageable` as query parameters:

```java
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;

@GetMapping
@Operation(summary = "Get all pizzas", description = "Retrieves all pizzas with pagination support")
public ResponseEntity<Page<PizzaResponse>> getPizzas(
        @ParameterObject Pageable pageable,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) String name) {
    // Implementation
}
```

**With `@ParameterObject`, Swagger UI correctly displays:**
- `page` (integer, query parameter)
- `size` (integer, query parameter)  
- `sort` (array[string], query parameter)

**Example usage in Swagger UI:**
- `page=0`
- `size=10`
- `sort=name,asc`
- `sort=price,desc`

This ensures Swagger generates the correct query string: `?page=0&size=10&sort=name,asc`

---

## 🌐 Accessing Swagger UI

### Starting the Application

```bash
mvn spring-boot:run
```

### Swagger UI URLs

Once the application is running:

| URL | Purpose |
|-----|---------|
| `http://localhost:8080/swagger-ui.html` | Interactive Swagger UI interface |
| `http://localhost:8080/swagger-ui/index.html` | Actual Swagger UI page (same as above) |
| `http://localhost:8080/v3/api-docs` | OpenAPI spec in JSON format |
| `http://localhost:8080/v3/api-docs.yaml` | OpenAPI spec in YAML format |

### Swagger UI Interface

The Swagger UI interface displays:

1. **API Information**: Title, version, description, contact, license
2. **Servers**: Available server endpoints
3. **Tags**: Grouped API operations (Pizza Management, Customer Management, etc.)
4. **Endpoints**: Each endpoint with:
   - HTTP method and path
   - Summary and description
   - Parameters (path, query, body)
   - Request/response schemas
   - Example values
   - "Try it out" button for live testing

---

## 🧪 Testing with Swagger UI

### Testing Public Endpoints

1. Open `http://localhost:8080/swagger-ui.html`
2. Navigate to **Pizza Management** → `GET /api/pizzas`
3. Click **"Try it out"**
4. Click **"Execute"**
5. View the response:
   - Status code (200)
   - Response body (JSON)
   - Response headers

### Testing Secured Endpoints (JWT)

For endpoints requiring authentication:

#### Step 1: Register or Login

1. Navigate to **Authentication** → `POST /api/auth/register` or `POST /api/auth/login`
2. Click **"Try it out"**
3. Fill in the request body:
   ```json
   {
     "name": "John Doe",
     "email": "john@example.com",
     "password": "password123"
   }
   ```
4. Click **"Execute"**
5. **Copy the JWT token** from the response:
   ```json
   {
     "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     "email": "john@example.com",
     "name": "John Doe",
     "role": "CUSTOMER"
   }
   ```

#### Step 2: Authorize in Swagger UI

1. Click the **"Authorize"** button (🔒 icon) at the top right
2. In the dialog, enter: `Bearer <your-token>`
   - Example: `Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
3. Click **"Authorize"**
4. Click **"Close"**

#### Step 3: Test Secured Endpoints

Now you can test secured endpoints:

1. Navigate to **Pizza Management** → `POST /api/pizzas`
2. Click **"Try it out"**
3. Fill in the request body:
   ```json
   {
     "name": "Quattro Formaggi",
     "price": 14.50,
     "description": "Four cheese pizza",
     "available": true
   }
   ```
4. Click **"Execute"**
5. View the response (201 Created)

The JWT token is automatically included in the `Authorization` header for all subsequent requests!

---

## ✅ Best Practices

### 1. Comprehensive Descriptions

Provide clear, detailed descriptions for:
- API purpose and functionality
- Each endpoint's behavior
- Request/response structures
- Error responses

```java
@Operation(
        summary = "Create a new order",
        description = """
                Creates a new order for a customer.
                
                Business Rules:
                - Customer must exist
                - All pizzas must exist and be available
                - Order total is automatically calculated
                - Order status starts as PENDING
                
                Requires CUSTOMER role.
                """
)
```

### 2. Realistic Examples

Use realistic example values in `@Schema`:

```java
@Schema(description = "Customer email", example = "john.doe@example.com")
private String email;

@Schema(description = "Pizza price in EUR", example = "12.50")
private BigDecimal price;
```

### 3. Document All Status Codes

Document all possible HTTP status codes:

```java
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Resource not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
})
```

### 4. Group Related Endpoints

Use meaningful tag names to group related operations:

```java
@Tag(name = "Pizza Management", description = "APIs for managing pizzas")
@Tag(name = "Order Management", description = "APIs for managing orders")
@Tag(name = "Authentication", description = "APIs for user authentication")
```

### 5. Security Documentation

Clearly document security requirements:

```java
@Operation(
        summary = "Delete a pizza",
        description = "Deletes a pizza. Requires ADMIN role.",
        security = @SecurityRequirement(name = "bearerAuth")
)
@ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Pizza deleted"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
        @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required"),
        @ApiResponse(responseCode = "404", description = "Pizza not found")
})
```

### 6. Keep Documentation in Sync

- Update documentation when changing endpoints
- Review Swagger UI regularly to ensure accuracy
- Use CI/CD to validate OpenAPI spec

### 7. Avoid Over-Documentation

Don't document obvious things:

❌ **Bad**:
```java
@Operation(summary = "Get pizza", description = "This endpoint gets a pizza")
```

✅ **Good**:
```java
@Operation(
        summary = "Get pizza by ID",
        description = "Retrieves a single pizza by its unique identifier"
)
```

### 8. Use Validation Annotations

Leverage Bean Validation - Springdoc automatically documents them:

```java
@NotBlank(message = "Name is required")
@Size(min = 2, max = 100)
private String name;  // Automatically documented!
```

---

## 🚀 Runnable Project

A complete, production-ready Spring Boot project with **OpenAPI/Swagger documentation** is available in:

**`pizzastore-with-swagger/`**

The project includes:

✅ **Springdoc OpenAPI integration**  
✅ **Comprehensive API documentation**  
✅ **JWT Bearer authentication in Swagger UI**  
✅ **All controllers documented with @Operation, @ApiResponses, @Parameter**  
✅ **All DTOs documented with @Schema**  
✅ **Interactive Swagger UI at `/swagger-ui.html`**  
✅ **OpenAPI spec at `/v3/api-docs`**  
✅ **Security configuration allowing Swagger UI access**  
✅ **All tests passing**

### Running the Project

```bash
cd pizzastore-with-swagger
mvn clean install
mvn spring-boot:run
```

### Accessing Documentation

1. **Swagger UI**: http://localhost:8080/swagger-ui.html
2. **OpenAPI JSON**: http://localhost:8080/v3/api-docs
3. **OpenAPI YAML**: http://localhost:8080/v3/api-docs.yaml

### Testing Flow

1. Open Swagger UI
2. Register a user via `/api/auth/register`
3. Copy the JWT token
4. Click "Authorize" and paste: `Bearer <token>`
5. Test any secured endpoint!

---

## 📚 Summary

In this lesson, you learned:

- ✅ How to integrate **Springdoc OpenAPI** into Spring Boot
- ✅ How to configure **global API metadata** and **security schemes**
- ✅ How to document **controllers** with `@Operation`, `@ApiResponses`, `@Parameter`
- ✅ How to document **DTOs** with `@Schema`
- ✅ How to configure **Spring Security** to allow Swagger UI access
- ✅ How to access and use **Swagger UI** for interactive API testing
- ✅ How to **test JWT-secured endpoints** via Swagger UI
- ✅ **Best practices** for API documentation

---

