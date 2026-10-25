# Lesson 14: API Documentation with Swagger/OpenAPI

**Describing PizzaStore's Secured API So Others Can Use It: OpenAPI 3, springdoc-openapi and Swagger UI**

---

## 📋 Table of Contents

1. [Learning Objectives](#-learning-objectives)
2. [Recap: Where Lesson 13 Left Us](#-recap-where-lesson-13-left-us)
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
13. [Testing the Documentation](#-testing-the-documentation)
14. [Best Practices](#-best-practices)
15. [Beyond This Lesson](#-beyond-this-lesson)
16. [Summary](#-summary)
17. [Runnable Project](#-runnable-project)

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
- ✅ Check the generated documentation in an automated test
- ✅ Apply best practices for API documentation

---

## 🔄 Recap: Where Lesson 13 Left Us

After [Lesson 13](../lesson-13-jwt-authentication/README.md) PizzaStore is complete: a tested REST API with validation, `ProblemDetail` errors, the Open Food Facts import and JWT security, including the customer's own account under `/api/customers/me` and order ownership. What is missing is a **description for the people who have to use it**. A front-end or mobile developer now has to read our Java code (or ask us) to know which endpoints exist, what to send, what comes back, which role is needed and which errors are possible. This lesson generates that description from the code, as an **OpenAPI 3** document, and makes it browsable and testable with **Swagger UI**.

---

## 🧱 What This Lesson Adds to PizzaStore

The project of this lesson, [`pizzastore-with-swagger`](pizzastore-with-swagger), is **Lesson 13's [`pizzastore-with-jwt`](../lesson-13-jwt-authentication/pizzastore-with-jwt) plus exactly these changes**, and with them it is the final PizzaStore:

| Added / changed | What it does |
|-----------------|--------------|
| [`pom.xml`](pizzastore-with-swagger/pom.xml) | Adds `springdoc-openapi-starter-webmvc-ui` 3.0.3 |
| [`config/OpenApiConfig.java`](pizzastore-with-swagger/src/main/java/be/vives/pizzastore/config/OpenApiConfig.java) (new) | API metadata, the `bearerAuth` security scheme, and `ProblemDetail` as the schema of every error response |
| [`controller/*.java`](pizzastore-with-swagger/src/main/java/be/vives/pizzastore/controller) | `@Tag`, `@Operation`, `@ApiResponses`, `@Parameter`, `@SecurityRequirement` and `@ParameterObject` on every endpoint, and `@Parameter(hidden = true)` on every `@CurrentUser` parameter: the e-mail address comes from the token, so Swagger UI must not ask for it. **Only annotations**: not one line of logic changed |
| [`dto/LoginRequest.java`](pizzastore-with-swagger/src/main/java/be/vives/pizzastore/dto/LoginRequest.java), [`RegisterRequest.java`](pizzastore-with-swagger/src/main/java/be/vives/pizzastore/dto/RegisterRequest.java), [`dto/request/CreatePizzaRequest.java`](pizzastore-with-swagger/src/main/java/be/vives/pizzastore/dto/request/CreatePizzaRequest.java), [`dto/request/NutritionalInfoRequest.java`](pizzastore-with-swagger/src/main/java/be/vives/pizzastore/dto/request/NutritionalInfoRequest.java) | `@Schema` descriptions and examples, and a `minimum` that springdoc cannot derive from `@PositiveOrZero` |
| [`security/SecurityConfig.java`](pizzastore-with-swagger/src/main/java/be/vives/pizzastore/security/SecurityConfig.java) | One extra rule: the documentation endpoints are public |
| [`application.properties`](pizzastore-with-swagger/src/main/resources/application.properties) | `springdoc.*` settings |
| [`OpenApiDocumentationTest.java`](pizzastore-with-swagger/src/test/java/be/vives/pizzastore/integration/OpenApiDocumentationTest.java) (new test) | Checks the generated `/v3/api-docs` (see [Testing the Documentation](#-testing-the-documentation)); the test `application.properties` gets the same `springdoc.default-produces-media-type` |

Services, repositories, entities, mappers, the Open Food Facts client and `data.sql` are unchanged, and so are Lesson 13's 264 tests (272 with the 8 new documentation tests). Documentation is metadata *about* the code.

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

springdoc's version line follows Spring Boot: **springdoc 3.x is for Spring Boot 4** (Spring Framework 7, Jackson 3), springdoc 2.x for Spring Boot 3. Within 3.x, look at the minor version too: **3.0.x is built against Spring Boot 4.0** (the version of this course), 3.1.x and 3.2.x against Spring Boot 4.1. Spring Boot does not manage springdoc's version, so it is set in `<properties>`, and you upgrade it together with Spring Boot.

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

**No additional configuration is required!** The library automatically scans your controllers and generates documentation. springdoc 3 writes an **OpenAPI 3.1** document (`"openapi": "3.1.0"`, the badge next to the title in Swagger UI); set `springdoc.api-docs.version=openapi_3_0` only if a tool you use still needs 3.0.

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
                        - **Anonymous users**: Can view pizzas (GET /api/pizzas), register and log in (/api/auth)
                        - **CUSTOMER role**: Can place orders, read and cancel their own orders, and manage their own account and favorites (/api/customers/me)
                        - **ADMIN role**: Can manage pizzas, customers and all orders
                        
                        Endpoints with a lock need a token: log in, click **Authorize** and paste the token (without "Bearer ").
                        """,
                contact = @Contact(
                        name = "VIVES",
                        email = "yves.seurynck@vives.be",
                        url = "https://www.vives.be"
                )
        )
        // no "servers" and no global "security": see below
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
- `@Info`: API title, version, description (Markdown) and contact (a `license` can be added too)
- `@SecurityScheme`: Authentication mechanism (JWT Bearer token), referenced by its `name` from `@SecurityRequirement`

Two attributes of `@OpenAPIDefinition` are **deliberately not used**:

- **`servers`**: with `servers = @Server(url = "http://localhost:8080")` Swagger UI sends every "Try it out" request to port 8080, also when the application runs on another port or host. Without `servers`, springdoc adds the URL the document was requested on (`"Generated server url"`), so Swagger UI always calls the application that serves it. Declare `@Server`s only for fixed, real environments (and behind a reverse proxy, set `server.forward-headers-strategy=framework` so the generated URL is the public one).
- **`security`**: `security = @SecurityRequirement(name = "bearerAuth")` on `@OpenAPIDefinition` is a *global* requirement: Swagger UI then shows a lock on **every** operation, also on `GET /api/pizzas`, `/api/auth/login` and `/api/auth/register`, which do not need a token. PizzaStore therefore puts `@SecurityRequirement` on the secured controllers and operations only (see [Secured Endpoints](#secured-endpoints)).

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
# success responses are JSON: without this an @ApiResponse without mediaType is documented as */*
springdoc.default-produces-media-type=application/json
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
| `springdoc.swagger-ui.try-it-out-enabled` | Open every operation directly in "Try it out" mode (no extra click on the button) | `false` |
| `springdoc.default-produces-media-type` | Media type of responses whose `@Content` names no `mediaType` | `*/*` |
| `springdoc.swagger-ui.persist-authorization` | Keep the token of **Authorize** after a page reload (in the browser's local storage) | `false` |

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

**Where to put it:**
- **On a controller class**: every operation of the controller gets the lock. PizzaStore does this on `OrderController`, `CustomerController` and `CurrentCustomerController`, whose endpoints all need a token
- **On an operation**: `@Operation(security = @SecurityRequirement(name = "bearerAuth"))` or `@SecurityRequirement` on the method. `PizzaController` does this for `POST`, `PUT`, `DELETE`, the image upload and the import, because its `GET`s are public
- **Nowhere**: the operation is public. `AuthController` and the `GET`s of `PizzaController` have no `@SecurityRequirement`, so Swagger UI shows them without a lock and sends them without a token
- **Globally**, on `@OpenAPIDefinition(security = ...)`: every operation is secured, the public ones included. An operation can switch it off again with an empty `@SecurityRequirements` (plural, from `io.swagger.v3.oas.annotations.security`). With mostly secured endpoints that is a valid choice; PizzaStore has enough public endpoints to prefer the explicit way above

The name, `"bearerAuth"`, must be the `name` of a `@SecurityScheme`. Several `@SecurityRequirement`s on one operation are **alternatives** (the client may use one of them), as in OpenAPI's `security` array.

> The documentation does not secure anything: `SecurityConfig` decides who may call what. `@SecurityRequirement` only tells Swagger UI to send the token and tells the reader that a token is needed. Keep the two in sync; [`OpenApiDocumentationTest`](#-testing-the-documentation) checks that the public endpoints have no lock and the secured ones do.

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

For Identity Provider (IdP) based authentication (not used in PizzaStore; it is the approach of the book's Chapter 11, see the note at the end of [Lesson 13](../lesson-13-jwt-authentication/README.md)), you would configure:

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
- **JWT (Lesson 13)**: Your application manages users, passwords, and token generation
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

The endpoint of [Lesson 11](../lesson-11-external-api/README.md) shows everything together: a description that says what the endpoint does and where the data comes from, the security requirement, and **every** status code it can return, including the two that come from the external service:

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

> **`401` or `403`?** The two are documented separately because PizzaStore really returns both: `401 Unauthorized` when the token is missing, expired or invalid (the authentication entry point of [Lesson 13's `SecurityConfig`](../lesson-13-jwt-authentication/README.md#key-points)), `403 Forbidden` when a logged-in user lacks the role or the order is not theirs. Both come as a `ProblemDetail`, like every other error of the API. A client developer needs that difference: after a `401` the app shows the login screen again, after a `403` it doesn't.

---

## 🏷️ Documenting DTOs

### What springdoc Already Knows

springdoc builds a schema for every DTO from its Java type, **without any annotation**: the record components or getters become the properties, `Long` becomes `integer` with `format: int64`, an `enum` becomes a list of allowed values, a nested record becomes a `$ref` to its own schema. `UpdateOrderStatusRequest` has no `@Schema` at all, and still appears as:

```json
"UpdateOrderStatusRequest": {
  "type": "object",
  "properties": {
    "status": { "type": "string", "enum": ["PENDING", "CONFIRMED", "PREPARING", "READY", "DELIVERED", "CANCELLED"] }
  },
  "required": ["status"]
}
```

`@Schema` adds what the type cannot tell: a **description** and a realistic **example**, which Swagger UI uses to pre-fill the request body of "Try it out".

### Using @Schema on Records

[`CreatePizzaRequest`](pizzastore-with-swagger/src/main/java/be/vives/pizzastore/dto/request/CreatePizzaRequest.java) (excerpt):

```java
@Schema(description = "Request object for creating a new pizza")
public record CreatePizzaRequest(

        @NotBlank(message = "Pizza name is required")
        @Schema(description = "Name of the pizza", example = "Margherita")
        String name,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be positive")
        @Schema(description = "Price of the pizza in EUR", example = "12.50")
        BigDecimal price,

        // description, available ...

        @Valid
        @Schema(description = "Nutritional information for the pizza")
        NutritionalInfoRequest nutritionalInfo
) {
}
```

On a record, `@Schema` goes on the record component; on a class such as [`LoginRequest`](pizzastore-with-swagger/src/main/java/be/vives/pizzastore/dto/LoginRequest.java) and [`RegisterRequest`](pizzastore-with-swagger/src/main/java/be/vives/pizzastore/dto/RegisterRequest.java) it goes on the field:

```java
@Schema(description = "Request object for user login")
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(description = "User's email address", example = "john.doe@example.com")
    private String email;
    // ...
}
```

> **No `required = true`.** Older examples write `@Schema(..., required = true)`. That attribute is deprecated in swagger-annotations 2.2 (its replacement is `requiredMode = Schema.RequiredMode.REQUIRED`), and in PizzaStore it is not needed at all: `@NotBlank` and `@NotNull` already make the field required in the schema, see below. One rule, written once.

The response DTOs (`PizzaResponse`, `OrderResponse`, ...) have no `@Schema`: their names and types are clear enough, and springdoc documents them from the records.

### Validation Annotations Integration

**springdoc reads the Jakarta Bean Validation annotations of Lesson 10** and turns them into schema constraints. You document a rule once, in Java, and it shows up in the documentation. This is the schema that springdoc generates for `RegisterRequest` (from `/v3/api-docs`, descriptions and examples left out):

```json
"RegisterRequest": {
  "type": "object",
  "properties": {
    "name":     { "type": "string", "minLength": 2, "maxLength": 100 },
    "email":    { "type": "string", "format": "email", "minLength": 1 },
    "password": { "type": "string", "minLength": 6, "maxLength": 100 },
    "phone":    { "type": "string", "pattern": "^\\+?[0-9 ]{10,16}$" },
    "address":  { "type": "string", "minLength": 0, "maxLength": 200 }
  },
  "required": ["email", "name", "password"]
}
```

In Swagger UI a required field gets a red asterisk, and the **Schema** tab of the request body shows the lengths, the pattern and the format.

**How PizzaStore's constraints end up in the schema** (checked against the generated `/v3/api-docs`):

| Jakarta Validation annotation | In the OpenAPI schema | Example in PizzaStore |
|-------------------------------|------------------------|-----------------------|
| `@NotNull` | field in `required` | `UpdateOrderStatusRequest.status` |
| `@NotBlank` | field in `required` + `minLength: 1` | `LoginRequest.email` |
| `@NotEmpty` (collection) | field in `required` | `CreateOrderRequest.orderLines` |
| `@Size(min = x, max = y)` | `minLength`/`maxLength` (string), `minItems`/`maxItems` (collection) | `RegisterRequest.name`: 2-100 |
| `@Min(value)` | `minimum` | `OrderLineRequest.quantity`: 1 |
| `@DecimalMin(value)` | `minimum` | `CreatePizzaRequest.price`: 0.01 |
| `@Pattern(regexp)` | `pattern` | `ImportNutritionRequest.barcode` |
| `@Email` | `format: email` | `RegisterRequest.email` |
| `@PositiveOrZero` | **nothing** | `NutritionalInfoRequest` (see below) |

The validation **messages** (`"Email is required"`) are not part of the schema: they only appear in the `ProblemDetail` of a `400`.

### When the Annotations Are Not Enough

The translation is good, not complete, so **look at the generated document** instead of assuming. Two cases in PizzaStore:

- **`@PositiveOrZero` is not translated** by springdoc 3.0. [`NutritionalInfoRequest`](pizzastore-with-swagger/src/main/java/be/vives/pizzastore/dto/request/NutritionalInfoRequest.java) therefore states the minimum itself:

  ```java
  @PositiveOrZero(message = "Calories cannot be negative")
  @Schema(description = "Energy in kcal", example = "250", minimum = "0")
  Integer calories,
  ```

  The validation still comes from `@PositiveOrZero`; `minimum = "0"` only documents it. [`OpenApiDocumentationTest`](#-testing-the-documentation) checks that it is there.
- **`@NotEmpty` together with `@Size(max = 20)`** on `CreateOrderRequest.orderLines` is documented as `minItems: 0, maxItems: 20`: the default `min = 0` of `@Size` wins over the "at least one" of `@NotEmpty`. The API itself rejects an empty list (`400`), only the schema is too lenient. `@Size(min = 1, max = 20)` would document it correctly.

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

The snippet shows only the relevant lines; the complete rules are in [`SecurityConfig`](pizzastore-with-swagger/src/main/java/be/vives/pizzastore/security/SecurityConfig.java) (Lesson 13). Do you want the documentation to be private in production? Then do not permit these paths there, or switch springdoc off with `springdoc.api-docs.enabled=false` and `springdoc.swagger-ui.enabled=false` in a production profile.

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

This ensures Swagger generates the correct query string: `?page=0&size=10&sort=name,asc`. springdoc even documents the defaults and limits of Spring Data: `page` has `default: 0, minimum: 0`, `size` has `default: 20, minimum: 1`.

### When the Return Type Says Nothing: `ResponseEntity<?>`

`getPizzas` returns `ResponseEntity<?>`: a `Page<PizzaResponse>` without filter, a plain `List<PizzaResponse>` with `minPrice`/`maxPrice` or `name`. springdoc cannot derive a schema from `?`, so the `200` response must say it. PizzaStore documents the page as schema and explains both shapes in the description:

```java
@ApiResponse(
        responseCode = "200",
        description = """
                Without filter: a page of pizzas (`content` holds the PizzaResponse objects).
                With minPrice/maxPrice or name: a plain JSON array of PizzaResponse objects, without paging.
                """,
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))
)
```

Documentation that needs this much explanation is often a sign of an API that could be simpler (for instance: always return a page, also when filtering). Writing the documentation is a good moment to notice that.

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
2. Navigate to **Pizza Management** → `GET /api/pizzas` and click it open. Because of `springdoc.swagger-ui.try-it-out-enabled=true` the parameters are editable right away (without that property, click **"Try it out"** first)
3. Click **"Execute"**
4. View the response:
   - Status code (200)
   - Response body (JSON)
   - Response headers

### Testing Secured Endpoints (JWT)

For endpoints requiring authentication:

#### Step 1: Log In

1. Navigate to **Authentication** → `POST /api/auth/login` (no lock: logging in needs no token)
2. Replace the example request body. Creating a pizza needs the ADMIN role, so log in as the admin of `data.sql` (registering through `/api/auth/register` always gives you the CUSTOMER role):
   ```json
   {
     "email": "admin@pizzastore.be",
     "password": "password123"
   }
   ```
3. Click **"Execute"**
4. **Copy the JWT token** from the response:
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

The lock icons are now closed. Swagger UI keeps the token in memory only: after a page reload you have to authorize again (unless `springdoc.swagger-ui.persist-authorization=true`, which stores it in the browser).

#### Step 3: Test Secured Endpoints

Now you can test secured endpoints:

1. Navigate to **Pizza Management** → `POST /api/pizzas`
2. Fill in the request body:
   ```json
   {
     "name": "Quattro Formaggi",
     "price": 14.50,
     "description": "Four cheese pizza",
     "available": true
   }
   ```
3. Click **"Execute"**
4. View the response (201 Created). The **Curl** box shows the request Swagger UI sent, including `-H 'Authorization: Bearer eyJ...'`

The JWT token is automatically included in the `Authorization` header for all subsequent requests! Try the Open Food Facts import the same way: **Pizza Management** → `POST /api/pizzas/{id}/nutritional-info/import`, `id` = `1`, body `{ "barcode": "3017620422003" }`. Then log in as `emma.johnson@example.com`, authorize with her token and repeat: now you get the `403` that the documentation promises. With her token, try **My Account** → `GET /api/customers/me` as well: there is no e-mail parameter to fill in, because the server takes it from the token.

---

## 🔬 Testing the Documentation

Swagger UI is checked by looking at it. But the OpenAPI document is also **input for other programs**: a front-end team may generate its TypeScript client from `/v3/api-docs`. A missing lock, a wrong schema or a parameter that should not be there then breaks their code, not ours. And it breaks silently: all 264 tests of Lesson 13 stay green when the documentation is wrong.

[`OpenApiDocumentationTest`](pizzastore-with-swagger/src/test/java/be/vives/pizzastore/integration/OpenApiDocumentationTest.java) therefore starts the application (like the other full-stack tests of [Lesson 12](../lesson-12-testing/README.md)), fetches `/v3/api-docs` **without a token** and checks it with JSONPath:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:openapi-documentation-test")
@AutoConfigureRestTestClient
class OpenApiDocumentationTest {

    @Autowired
    private RestTestClient client;

    @Test
    void onlySecuredEndpoints_HaveALock() {
        client.get().uri("/v3/api-docs")
                .exchange()
                .expectBody()
                // no global security requirement: it would also put a lock on the public endpoints
                .jsonPath("$.security").doesNotExist()
                .jsonPath("$.paths['/api/pizzas'].get.security").doesNotExist()
                .jsonPath("$.paths['/api/auth/login'].post.security").doesNotExist()
                .jsonPath("$.paths['/api/pizzas'].post.security[0].bearerAuth").exists()
                .jsonPath("$.paths['/api/customers/me'].get.security[0].bearerAuth").exists();
                // ...
    }
    // ...
}
```

Paths contain `/` and `{}`, so they are written in bracket notation: `$.paths['/api/pizzas/{id}']`. The test class checks:

| Test | What it guarantees |
|------|--------------------|
| `apiDocs_ArePublic_AndDescribeTheJwtScheme` | `/v3/api-docs` answers `200` without a token, with the `bearerAuth` scheme (`http`, `bearer`, `JWT`) |
| `server_IsTheUrlTheDocumentWasRequestedOn` | OpenAPI 3.1, and the server is the running application (no hardcoded port) |
| `yamlAndSwaggerUi_ArePublic` | `/v3/api-docs.yaml` and `/swagger-ui/index.html` are reachable (the `SecurityConfig` rule) |
| `onlySecuredEndpoints_HaveALock` | no global requirement; public endpoints without, secured endpoints with `bearerAuth` |
| `errorResponses_AreDocumentedAsProblemDetail` | a `404`/`422` is `application/problem+json` with the `ProblemDetail` schema, a `200` is `application/json` with the real DTO |
| `currentUser_IsNotAParameter` | `@Parameter(hidden = true)` on `@CurrentUser`: `/api/customers/me` and `POST /api/orders` have no e-mail parameter |
| `pageable_IsDocumentedAsSeparateQueryParameters` | `@ParameterObject`: `page` and `size` are query parameters, there is no `pageable` object |
| `validationAnnotations_EndUpInTheSchemas` | `required`, `minLength`, `format: email`, `pattern`, and the `minimum` of `NutritionalInfoRequest` |

Is the test worth it? Put `security = @SecurityRequirement(name = "bearerAuth")` back on `@OpenAPIDefinition` and `onlySecuredEndpoints_HaveALock` fails with `Expected no value at JSON path "$.security"`. Writing the test also revealed that the success responses were documented as `*/*` instead of `application/json`, which led to `springdoc.default-produces-media-type=application/json`.

> **Test properties**: the tests use `src/test/resources/application.properties`, which replaces the main `application.properties`. Every `springdoc.*` setting that changes the document (here `default-produces-media-type`) must therefore be in the test properties as well, or the test checks a different document than the one the application serves.

`mvn test` now runs **272 tests**: the 264 of Lesson 13 and these 8.

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
        description = "Creates a new order for the logged-in customer (taken from the JWT, not from the request body). Requires CUSTOMER role. Order is created with status PENDING."
)
@ApiResponses(value = {
        // 201, 400, 401, 403 ...
        @ApiResponse(responseCode = "422", description = "Business rule violation - unknown pizza or pizza not available")
})
```

This is `createOrder` of [`OrderController`](pizzastore-with-swagger/src/main/java/be/vives/pizzastore/controller/OrderController.java): it says *whose* order it is (a client developer would otherwise look for a `customerId` field), which role is needed and which business rules lead to which status code.

### 2. Realistic Examples

Use realistic example values in `@Schema`:

```java
@Schema(description = "Customer email", example = "john.doe@example.com")
private String email;

@Schema(description = "Pizza price in EUR", example = "12.50")
private BigDecimal price;
```

Swagger UI fills "Try it out" with these examples, so a good example is also one that the API accepts: a valid e-mail address, a price above the `@DecimalMin`.

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

- Update documentation when changing endpoints: a new status code in `GlobalExceptionHandler` or a new role rule in `SecurityConfig` belongs in the `@ApiResponses` as well
- Let a test check what matters most (see [Testing the Documentation](#-testing-the-documentation))
- Look at the generated `/v3/api-docs`, not only at the annotations: it is what clients see

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

Leverage Bean Validation - springdoc documents them (with the exceptions of [When the Annotations Are Not Enough](#when-the-annotations-are-not-enough)):

```java
@NotBlank(message = "Name is required")
@Size(min = 2, max = 100)
private String name;  // required, minLength 2, maxLength 100
```

---

## 🔭 Beyond This Lesson

PizzaStore works **code-first**: the Java code is the source, the OpenAPI document is generated from it. Some topics that build on this lesson:

- **API-first (contract-first)**: the team writes the OpenAPI document (YAML) *before* the code, agrees on it with the front-end team, and generates interfaces and DTOs from it with the [OpenAPI Generator](https://openapi-generator.tech/) Maven plugin. The document is then the contract, and the controllers implement the generated interfaces. Useful when several teams build against an API that does not exist yet.
- **Generated clients**: the same OpenAPI Generator (or tools such as `openapi-typescript`) turns PizzaStore's `/v3/api-docs` into a TypeScript, Kotlin or Swift client. That is the most important reason to keep the document correct and tested.
- **The document as a build artifact**: the `springdoc-openapi-maven-plugin`, together with the `start`/`stop` goals of the `spring-boot-maven-plugin`, runs the application during `mvn verify` and saves `/v3/api-docs` as a file, so it can be published, reviewed in a pull request or compared with the previous version to detect breaking changes.
- **Groups**: a `GroupedOpenApi` bean splits the documentation, for instance a public group (`/api/pizzas/**`, `/api/auth/**`) and an admin group, each with its own document under `/v3/api-docs/{group}` and its own entry in Swagger UI's top bar.
- **Production**: decide whether the documentation is public. Switch it off in a production profile with `springdoc.api-docs.enabled=false` and `springdoc.swagger-ui.enabled=false`, or keep it but remove the permit rule from `SecurityConfig`.
- **Spring REST Docs**: a test-driven alternative from the Spring team: the documentation is generated from snippets that MockMvc, `WebTestClient` or REST Assured tests produce, so it can only document what a test has proven. More work than springdoc, but never out of sync.

---

## 📚 Summary

In this lesson, you learned:

- ✅ How to integrate **Springdoc OpenAPI** into Spring Boot 4
- ✅ How to configure **global API metadata** and **security schemes**, and why PizzaStore declares no global security requirement and no fixed servers
- ✅ How to document **controllers** with `@Operation`, `@ApiResponses`, `@Parameter`
- ✅ How to document **error responses** once, as `ProblemDetail`, with an `OpenApiCustomizer`
- ✅ How to document **DTOs** with `@Schema`, how validation annotations end up in the schema, and where that translation stops
- ✅ How to configure **Spring Security** to allow Swagger UI access
- ✅ How to access and use **Swagger UI** for interactive API testing
- ✅ How to **test JWT-secured endpoints** via Swagger UI
- ✅ How to **test the generated document** with `RestTestClient` and JSONPath
- ✅ **Best practices** for API documentation

---

**Note on the book**: *Pro Spring Boot 4* does not cover API documentation: the words OpenAPI, Swagger and springdoc do not occur in the book. This lesson is the course's own addition, based on the [springdoc-openapi documentation](https://springdoc.org/) and the [OpenAPI Specification](https://spec.openapis.org/oas/latest.html). Related material in the book:

- Chapter 3, *Web Development with Spring Boot*, section *RESTful API Design and Native Versioning* (p. 63-64): the REST principles (resources as nouns, HTTP methods, meaningful status codes) that an OpenAPI document describes, and API versioning, for when the documented contract has to change.
- Chapter 15, *Building Cloud-Native Microservices with Spring Cloud*, section *Consumer-Driven Contract Testing* (p. 440-442): another way to make an API contract explicit, with Spring Cloud Contract. There the contract is a test that the provider must pass, which goes further than describing the API.
- Appendix A, *Spring for GraphQL with Spring Boot*, *Best Practices* (p. 512): "Schema First", the GraphQL counterpart of the API-first approach in [Beyond This Lesson](#-beyond-this-lesson).

---

## 🚀 Runnable Project

**[`pizzastore-with-swagger/`](pizzastore-with-swagger)** is Lesson 13's [`pizzastore-with-jwt`](../lesson-13-jwt-authentication/pizzastore-with-jwt) plus the changes listed in [What This Lesson Adds to PizzaStore](#-what-this-lesson-adds-to-pizzastore). Its code is the **final PizzaStore** of this course.

The project includes:

✅ **Spring Boot 4.0** on **Java 25**, springdoc-openapi 3.0.3 (OpenAPI 3.1)  
✅ **Everything from Lessons 6a-13**: domain, repositories, DTOs, services, REST API, validation, `ProblemDetail` errors, the Open Food Facts import, JWT security  
✅ **All controllers documented** with `@Operation`, `@ApiResponses`, `@Parameter`; error responses as `ProblemDetail`; a lock only on the secured endpoints  
✅ **DTOs documented** with `@Schema`  
✅ **JWT Bearer authentication in Swagger UI**  
✅ **Swagger UI** at `/swagger-ui.html`, **OpenAPI spec** at `/v3/api-docs` and `/v3/api-docs.yaml`  
✅ **272 tests** (`mvn test`): Lesson 13's 264 plus 8 that check the generated OpenAPI document

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
