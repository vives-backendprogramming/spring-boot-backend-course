# Lesson 13: API Documentation with Swagger/OpenAPI

**Describing PizzaStore's Secured API So Others Can Use It: OpenAPI 3, springdoc-openapi and Swagger UI**

---

## 📋 Table of Contents

1. [Learning Objectives](#-learning-objectives)
2. [Recap: Where Lesson 12 Left Us](#-recap-where-lesson-12-left-us)
3. [What This Lesson Adds to PizzaStore](#-what-this-lesson-adds-to-pizzastore)
4. [Introduction to OpenAPI](#-introduction-to-openapi)
5. [Integrating Springdoc OpenAPI](#-integrating-springdoc-openapi)
6. [Configuring OpenAPI](#%EF%B8%8F-configuring-openapi)
7. [Documenting Controllers](#-documenting-controllers)
8. [Documenting DTOs](#%EF%B8%8F-documenting-dtos)
9. [Security Configuration](#-security-configuration)
10. [Handling Pageable Parameters](#-handling-pageable-parameters)
11. [Accessing Swagger UI](#-accessing-swagger-ui)
12. [Testing with Swagger UI](#-testing-with-swagger-ui)
13. [Best Practices](#-best-practices)
14. [Summary](#-summary)
15. [Runnable Project](#-runnable-project)

---

## 🎯 Learning Objectives

By the end of this lesson, you will be able to:

- ✅ Understand the OpenAPI Specification (OAS)
- ✅ Integrate Springdoc OpenAPI into a Spring Boot 4 application
- ✅ Configure API metadata and security schemes
- ✅ Document REST API endpoints with annotations, including their error responses
- ✅ Document DTOs with schema descriptions and examples
- ✅ Access and use Swagger UI for API testing
- ✅ Test authenticated endpoints via Swagger UI
- ✅ Generate OpenAPI specifications in JSON/YAML format
- ✅ Apply best practices for API documentation

---

## 🔄 Recap: Where Lesson 12 Left Us

After [Lesson 12](../lesson-12-jwt-authentication/README.md) PizzaStore is complete: a tested REST API with validation, `ProblemDetail` errors, the Open Food Facts import and JWT security. What is missing is a **description for the people who have to use it**. A front-end or mobile developer now has to read our Java code (or ask us) to know which endpoints exist, what to send, what comes back, which role is needed and which errors are possible. This lesson generates that description from the code, as an **OpenAPI 3** document, and makes it browsable and testable with **Swagger UI**.

---

## 🧱 What This Lesson Adds to PizzaStore

The project of this lesson, [`pizzastore-with-swagger`](pizzastore-with-swagger), is **Lesson 12's [`pizzastore-with-jwt`](../lesson-12-jwt-authentication/pizzastore-with-jwt) plus exactly these changes**, and with them it is the final PizzaStore:

| Added / changed | What it does |
|-----------------|--------------|
| [`pom.xml`](pizzastore-with-swagger/pom.xml) | Adds `springdoc-openapi-starter-webmvc-ui` 3.0.3 |
| [`config/OpenApiConfig.java`](pizzastore-with-swagger/src/main/java/be/vives/pizzastore/config/OpenApiConfig.java) (new) | API metadata, servers, the `bearerAuth` security scheme, and `ProblemDetail` as the schema of every error response |
| [`controller/*.java`](pizzastore-with-swagger/src/main/java/be/vives/pizzastore/controller) | `@Tag`, `@Operation`, `@ApiResponses`, `@Parameter`, `@SecurityRequirement` and `@ParameterObject` on every endpoint. **Only annotations**: not one line of logic changed |
| [`dto/LoginRequest.java`](pizzastore-with-swagger/src/main/java/be/vives/pizzastore/dto/LoginRequest.java), [`RegisterRequest.java`](pizzastore-with-swagger/src/main/java/be/vives/pizzastore/dto/RegisterRequest.java), [`dto/request/CreatePizzaRequest.java`](pizzastore-with-swagger/src/main/java/be/vives/pizzastore/dto/request/CreatePizzaRequest.java) | `@Schema` descriptions and examples |
| [`security/SecurityConfig.java`](pizzastore-with-swagger/src/main/java/be/vives/pizzastore/security/SecurityConfig.java) | One extra rule: the documentation endpoints are public |
| [`application.properties`](pizzastore-with-swagger/src/main/resources/application.properties) | `springdoc.*` settings |

Services, repositories, entities, mappers, the Open Food Facts client, `data.sql` and the 236 tests are unchanged. Documentation is metadata *about* the code.

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
    <springdoc.version>3.0.3</springdoc.version>
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

springdoc's version line follows Spring Boot: **springdoc 3.x is for Spring Boot 4** (Spring Framework 7, Jackson 3), springdoc 2.x for Spring Boot 3. Spring Boot does not manage its version, so it is set in `<properties>`.

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

[`OpenApiConfig`](pizzastore-with-swagger/src/main/java/be/vives/pizzastore/config/OpenApiConfig.java) defines the global API metadata:

```java
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "PizzaStore API",
                version = "1.0.0",
                description = """
                        RESTful API for managing a pizza store, including pizzas, customers, and orders.
                        
                        **Authentication**: This API uses JWT Bearer token authentication.
                        
                        **Authorization**:
                        - **Anonymous users**: Can view available pizzas (GET /api/pizzas)
                        - **CUSTOMER role**: Can place orders and manage their profile
                        - **ADMIN role**: Can manage pizzas (create, update, delete) and view all orders
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
        description = "JWT authentication token. Obtain a token by registering or logging in via /api/auth/register or /api/auth/login"
)
public class OpenApiConfig {
    ...   // plus the OpenApiCustomizer of "Error Responses: ProblemDetail" below
}
```

**Key Elements:**

- `@OpenAPIDefinition`: Global API metadata
- `@Info`: API title, version, description and contact (a `license` can be added too)
- `@Server`: Available servers (development, staging, production); Swagger UI lets you pick one
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
2. Paste **only the token** (`eyJhbGciOi...`): because the scheme is `type = HTTP, scheme = "bearer"`, Swagger UI adds the `Bearer ` prefix itself. Typing `Bearer <token>` would send `Bearer Bearer <token>`, which fails
3. Click **"Authorize"**
4. All subsequent requests will include the token in the `Authorization` header

#### OAuth2 / OpenID Connect (IdP Authentication)

For Identity Provider (IdP) based authentication (not used in PizzaStore; it is the approach of the book's Chapter 11, see the note at the end of [Lesson 12](../lesson-12-jwt-authentication/README.md)), you would configure:

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
- **IdP**: External identity provider (like Dex, Keycloak, Google, Azure AD) manages authentication
- **Both** can use the same `@SecurityRequirement` annotations on endpoints
- **Swagger UI** integration is simpler with JWT, more complex with OAuth2/OIDC flows

### Error Responses: `ProblemDetail`

An `@ApiResponse` without `content`, such as `@ApiResponse(responseCode = "404", description = "Pizza not found")`, is documented by springdoc with the **return type of the method**. Swagger UI would then claim that a `404` returns a `PizzaResponse`, which is wrong: since Lesson 10 every error is an RFC 7807 `ProblemDetail`. You could add `content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))` to every error response of every endpoint, but that is a lot of repetition. PizzaStore fixes it once, with an `OpenApiCustomizer` bean in `OpenApiConfig` that edits the generated document:

```java
@Bean
public OpenApiCustomizer problemDetailForErrorResponses() {
    return openApi -> {
        Schema<?> problemDetail = ModelConverters.getInstance().read(ProblemDetail.class).get("ProblemDetail");
        // extension members such as "errors" are written at the top level of the JSON, not inside "properties"
        problemDetail.getProperties().remove("properties");
        problemDetail.setAdditionalProperties(true);
        openApi.getComponents().addSchemas("ProblemDetail", problemDetail);
        Content problemJson = new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail")));

        openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation ->
                operation.getResponses().forEach((code, response) -> {
                    if (code.startsWith("4") || code.startsWith("5")) {
                        response.setContent(problemJson);
                    }
                })));
    };
}
```

An `OpenApiCustomizer` runs after springdoc has built the document from the annotations, and can change anything in it. Here it adds a `ProblemDetail` schema (read from Spring's own `ProblemDetail` class) to the components, and gives every `4xx`/`5xx` response the content type `application/problem+json` with that schema. The annotations on the controllers stay short, and the documentation matches what `GlobalExceptionHandler` really returns.

### Documenting the Open Food Facts Import

The endpoint of [Lesson 10, Part 3](../lesson-10-validation-exception-handling/README.md#-part-3-calling-an-external-api) shows everything together: a description that says what the endpoint does and where the data comes from, the security requirement, and **every** status code it can return, including the two that come from the external service:

```java
@PostMapping("/{id}/nutritional-info/import")
@Operation(
        summary = "Import nutritional info from Open Food Facts",
        description = """
                Fills the nutritional info of a pizza (per 100 g) with the data that the free
                [Open Food Facts](https://world.openfoodfacts.org) database has for a barcode,
                e.g. of a comparable packaged product. An existing nutritional info is overwritten.
                Requires ADMIN role.
                """,
        security = @SecurityRequirement(name = "bearerAuth")
)
@ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Nutritional info imported",
                content = @Content(schema = @Schema(implementation = PizzaResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Barcode is missing or not a valid EAN/UPC number"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
        @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required"),
        @ApiResponse(responseCode = "404", description = "Pizza not found"),
        @ApiResponse(responseCode = "422", description = "Open Food Facts does not know the barcode, or has no complete nutritional data for it"),
        @ApiResponse(responseCode = "502", description = "Open Food Facts is unavailable, too slow or rate-limiting us")
})
public ResponseEntity<PizzaResponse> importNutritionalInfo(
        @Parameter(description = "Pizza ID", required = true) @PathVariable Long id,
        @Valid @RequestBody ImportNutritionRequest request) {
    ...
}
```

The description field accepts **Markdown**, so the link to Open Food Facts is clickable in Swagger UI. A client developer reading this knows, without opening our code, that a `502` is not their fault and can be retried, while a `422` means they should try another barcode. `ImportNutritionRequest` has no `@Schema` at all, yet Swagger UI shows the barcode as required with its regular expression: springdoc reads `@NotBlank` and `@Pattern` (see [Validation Annotations Integration](#validation-annotations-integration)).

> **`401` or `403`?** The two are documented separately because PizzaStore really returns both: `401 Unauthorized` when the token is missing, expired or invalid (the `HttpStatusEntryPoint` of [Lesson 12's `SecurityConfig`](../lesson-12-jwt-authentication/README.md#key-points)), `403 Forbidden` when a logged-in user lacks the role. A client developer needs that difference: after a `401` the app shows the login screen again, after a `403` it doesn't.

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

**Springdoc automatically reflects Bean Validation annotations in the OpenAPI specification!**

When you add Jakarta Bean Validation annotations to your DTOs, they are automatically included in the generated OpenAPI spec, which means:
- **Required fields** are marked as required in Swagger UI (via `@NotNull`, `@NotBlank`)
- **String length constraints** appear in the schema (via `@Size`)
- **Numeric ranges** are documented (via `@Min`, `@Max`, `@DecimalMin`, `@DecimalMax`)
- **Format validations** are shown (via `@Email`, `@Pattern`)
- **Custom error messages** help developers understand validation rules

#### Example: CreatePizzaRequest

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
        String description
) {
}
```

#### Example: RegisterRequest

```java
package be.vives.pizzastore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request object for user registration")
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Schema(description = "User's full name", example = "John Doe", required = true)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(description = "User's email address", example = "john.doe@example.com", required = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    @Schema(description = "User's password (minimum 6 characters)", example = "password123", required = true)
    private String password;

    @Pattern(regexp = "^\\+?[0-9 ]{10,16}$", message = "Phone number should be valid")
    @Schema(description = "User's phone number", example = "+32 456 78 90 12")
    private String phone;

    @Size(max = 200, message = "Address must not exceed 200 characters")
    @Schema(description = "User's address", example = "123 Main Street, Brussels")
    private String address;

    // Constructors, getters, setters...
}
```

#### How Validation Appears in OpenAPI/Swagger UI

When you open the Swagger UI and look at these request schemas, you'll see:

1. **Required fields** are marked with a red asterisk (*) and listed in the "required" array
2. **String length** constraints show `minLength` and `maxLength` properties
3. **Numeric constraints** show `minimum`, `maximum`, and `exclusiveMinimum` properties
4. **Pattern validation** shows the regex pattern
5. **Format validation** shows the format type (e.g., `format: email`)
6. **Example values** help developers understand expected input

**Key Validation Annotations and Their OpenAPI Mappings:**

| Jakarta Validation Annotation | OpenAPI Property | Description |
|------------------------------|------------------|-------------|
| `@NotNull`, `@NotBlank`, `@NotEmpty` | `required: true` | Field is mandatory |
| `@Size(min = x, max = y)` | `minLength: x, maxLength: y` | String/collection size constraints |
| `@Min(value)`, `@Max(value)` | `minimum: value, maximum: value` | Numeric range constraints (inclusive) |
| `@DecimalMin(value)`, `@DecimalMax(value)` | `minimum: value, maximum: value` | Decimal range constraints |
| `@Pattern(regexp)` | `pattern: "regexp"` | Regular expression validation |
| `@Email` | `format: "email"` | Email format validation |
| `@Positive`, `@PositiveOrZero` | `minimum: 0` or `minimum: 1` | Positive number constraint |
| `@Negative`, `@NegativeOrZero` | `maximum: 0` or `maximum: -1` | Negative number constraint |

This automatic integration means **you document validation rules once** in your Java code, and they're automatically reflected in your API documentation—no duplication needed!

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
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                        
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
- `/v3/api-docs`, `/v3/api-docs/**` - OpenAPI specification endpoints (JSON, and per group)
- `/v3/api-docs.yaml` - the YAML version. It needs its own pattern: `/v3/api-docs/**` matches `/v3/api-docs` and everything *below* it, but `/v3/api-docs.yaml` is a different path next to it. Without the pattern the YAML link answers `403`

The snippet shows only the relevant lines; the complete rules are in [`SecurityConfig`](pizzastore-with-swagger/src/main/java/be/vives/pizzastore/security/SecurityConfig.java) (Lesson 12). Do you want the documentation to be private in production? Then do not permit these paths there, or switch springdoc off with `springdoc.api-docs.enabled=false` and `springdoc.swagger-ui.enabled=false` in a production profile.

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

### The Solution: `@ParameterObject`

Tell springdoc to document the fields of the `Pageable` as separate query parameters. That is what PizzaStore's [`PizzaController`](pizzastore-with-swagger/src/main/java/be/vives/pizzastore/controller/PizzaController.java), `CustomerController` and `OrderController` do:

```java
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;

@GetMapping
@Operation(summary = "Get all pizzas", description = "...")
public ResponseEntity<?> getPizzas(
        @Parameter(description = "Minimum price filter") @RequestParam(required = false) BigDecimal minPrice,
        @Parameter(description = "Maximum price filter") @RequestParam(required = false) BigDecimal maxPrice,
        @Parameter(description = "Name filter (case-insensitive partial match)") @RequestParam(required = false) String name,
        @ParameterObject Pageable pageable) {
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

#### Step 1: Log In

1. Navigate to **Authentication** → `POST /api/auth/login`
2. Click **"Try it out"**
3. Fill in the request body. Creating a pizza needs the ADMIN role, so log in as the admin of `data.sql` (registering through `/api/auth/register` always gives you the CUSTOMER role):
   ```json
   {
     "email": "admin@pizzastore.be",
     "password": "password123"
   }
   ```
4. Click **"Execute"**
5. **Copy the JWT token** from the response:
   ```json
   {
     "token": "eyJhbGciOiJIUzM4NCJ9...",
     "email": "admin@pizzastore.be",
     "name": "Admin User",
     "role": "ADMIN"
   }
   ```

#### Step 2: Authorize in Swagger UI

1. Click the **"Authorize"** button (🔒 icon) at the top right
2. In the dialog, paste the token **without** `Bearer ` (Swagger UI adds it)
   - Example: `eyJhbGciOiJIUzM4NCJ9...`
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

The JWT token is automatically included in the `Authorization` header for all subsequent requests! Try the Open Food Facts import the same way: **Pizza Management** → `POST /api/pizzas/{id}/nutritional-info/import`, `id` = `1`, body `{ "barcode": "3017620422003" }`. Then log in as `emma.johnson@example.com`, authorize with her token and repeat: now you get the `403` that the documentation promises.

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

Include the status codes that come from *other* systems: the Open Food Facts import documents its `422` and `502` (see [Documenting the Open Food Facts Import](#documenting-the-open-food-facts-import)).

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

## 📚 Summary

In this lesson, you learned:

- ✅ How to integrate **Springdoc OpenAPI** into Spring Boot 4
- ✅ How to configure **global API metadata** and **security schemes**
- ✅ How to document **controllers** with `@Operation`, `@ApiResponses`, `@Parameter`
- ✅ How to document **error responses** once, as `ProblemDetail`, with an `OpenApiCustomizer`
- ✅ How to document **DTOs** with `@Schema`, and how validation annotations end up in the schema
- ✅ How to configure **Spring Security** to allow Swagger UI access
- ✅ How to access and use **Swagger UI** for interactive API testing
- ✅ How to **test JWT-secured endpoints** via Swagger UI
- ✅ **Best practices** for API documentation

---

**Note on the book**: *Pro Spring Boot 4* does not cover API documentation: the words OpenAPI, Swagger and springdoc do not occur in the book. This lesson is the course's own addition, based on the [springdoc-openapi documentation](https://springdoc.org/) and the [OpenAPI Specification](https://spec.openapis.org/oas/latest.html).

---

## 🚀 Runnable Project

**[`pizzastore-with-swagger/`](pizzastore-with-swagger)** is Lesson 12's [`pizzastore-with-jwt`](../lesson-12-jwt-authentication/pizzastore-with-jwt) plus the changes listed in [What This Lesson Adds to PizzaStore](#-what-this-lesson-adds-to-pizzastore). Its code is the **final PizzaStore** of this course.

The project includes:

✅ **Spring Boot 4.0** on **Java 25**, springdoc-openapi 3.0.3  
✅ **Everything from Lessons 6a-12**: domain, repositories, DTOs, services, REST API, validation, `ProblemDetail` errors, the Open Food Facts import, JWT security  
✅ **All controllers documented** with `@Operation`, `@ApiResponses`, `@Parameter`; error responses as `ProblemDetail`  
✅ **DTOs documented** with `@Schema`  
✅ **JWT Bearer authentication in Swagger UI**  
✅ **Swagger UI** at `/swagger-ui.html`, **OpenAPI spec** at `/v3/api-docs` and `/v3/api-docs.yaml`  
✅ **236 tests** (`mvn test`)

### Running the Project

```bash
cd pizzastore-with-swagger
mvn spring-boot:run
```

Both the application and the tests need JDK 25: if your default `mvn` picks another JDK, point `JAVA_HOME` to JDK 25 first. The H2 console is at http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:pizzastore_swagger`, user `sa`, no password).

### Accessing Documentation

1. **Swagger UI**: http://localhost:8080/swagger-ui.html
2. **OpenAPI JSON**: http://localhost:8080/v3/api-docs
3. **OpenAPI YAML**: http://localhost:8080/v3/api-docs.yaml

### Testing Flow

1. Open Swagger UI
2. Log in via `POST /api/auth/login` as `admin@pizzastore.be` / `password123` (or as the customer `emma.johnson@example.com`)
3. Copy the `token` from the response
4. Click **Authorize** and paste the token (without `Bearer `)
5. Test any secured endpoint, for example the Open Food Facts import with barcode `3017620422003`

---

🎉 Congratulations: you have built, tested, secured and documented a complete Spring Boot 4 REST API!
