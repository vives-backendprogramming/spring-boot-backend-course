# Lesson 8: REST Principles

## 📚 Table of Contents

- [📘 Overview](#-overview)
- [🎯 Learning Objectives](#-learning-objectives)
- [🏛️ What is REST?](#️-what-is-rest)
- [🎯 REST Constraints](#-rest-constraints)
- [🗂️ Resource Naming Conventions](#️-resource-naming-conventions)
- [🔢 API Versioning](#-api-versioning)
- [🎬 HTTP Methods and Their Semantics](#-http-methods-and-their-semantics)
- [📖 GET - Read Resources](#-get---read-resources)
- [✏️ POST - Create Resources](#️-post---create-resources)
- [🔄 PUT - Update/Replace Resources](#-put---updatereplace-resources)
- [🩹 PATCH - Partial Update](#-patch---partial-update)
- [🗑️ DELETE - Remove Resources](#️-delete---remove-resources)
- [🎨 HTTP Status Codes](#-http-status-codes)
- [🌐 CORS (Cross-Origin Resource Sharing)](#-cors-cross-origin-resource-sharing)
- [📋 REST Best Practices Summary](#-rest-best-practices-summary)
- [🎓 Richardson Maturity Model](#-richardson-maturity-model)
- [🎓 Summary](#-summary)
- [📖 Additional Resources](#-additional-resources)

---

## 📘 Overview

In this lesson, we step back from implementation details to understand the **principles and best practices of REST** (Representational State Transfer). While our PizzaStore API works, following REST principles will make it more consistent, predictable, and easier to consume by mobile applications and other clients.

## 🎯 Learning Objectives

By the end of this lesson, you will:
- Understand the **REST architectural style** and its constraints
- Learn **resource naming conventions**
- Apply **URL path API versioning** to evolve an API without breaking existing clients
- Master **HTTP method semantics**
- Know all important **HTTP status codes**
- Configure **CORS** (essential for mobile apps!)
- Apply REST best practices to API design

---

## 🏛️ What is REST?

**REST (Representational State Transfer)** is an architectural style for designing networked applications. It was introduced by Roy Fielding in his 2000 PhD dissertation.

### REST is NOT

❌ A protocol  
❌ A standard  
❌ Just HTTP + JSON  
❌ A specific technology

### REST IS

✅ An architectural style  
✅ A set of constraints and principles  
✅ Guidelines for designing scalable web services  
✅ Technology-agnostic (though commonly used with HTTP)

---

## 🎯 REST Constraints

Roy Fielding defined some **architectural constraints** for REST:

### 1. Client-Server Architecture

- **Separation of concerns**: UI concerns separated from data storage concerns
- Client and server can evolve independently
- Improves portability and scalability

```
Client (Mobile App)  ←→  Server (PizzaStore API)
  UI Logic                 Business Logic
  User State               Data Storage
```

### 2. Stateless

- **No client context** stored on the server between requests
- Each request contains **all information** needed to understand and process it
- Session state kept entirely on the client

```java
// ❌ BAD: Stateful (session-based)
@GetMapping("/cart")
public Cart getCart(HttpSession session) {
    return (Cart) session.getAttribute("cart");
}

// ✅ GOOD: Stateless (token-based)
@GetMapping("/carts/{userId}")
public Cart getCart(@PathVariable Long userId, @RequestHeader("Authorization") String token) {
    // Validate token and get cart
    return cartService.getCart(userId);
}
```

### 3. Cacheable

- Responses must define themselves as **cacheable** or **non-cacheable**
- Improves efficiency and scalability
- Use HTTP cache headers: `Cache-Control`, `ETag`, `Last-Modified`

```java
@GetMapping("/{id}")
public ResponseEntity<PizzaResponse> getPizza(@PathVariable Long id) {
    PizzaResponse pizza = pizzaService.findById(id);
    
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
        .eTag(String.valueOf(pizza.hashCode()))
        .body(pizza);
}
```

### 4. Uniform Interface

- **Consistent** and **predictable** API design
- Resources identified by URIs
- Resources manipulated through representations (JSON, XML)
- Self-descriptive messages
- HATEOAS (Hypermedia as the Engine of Application State)

### 5. Layered System

- Client cannot tell if connected directly to end server or intermediary
- Allows for load balancers, caches, proxies
- Improves scalability

---

## 🗂️ Resource Naming Conventions

Resources are the **nouns** of your API. Good naming is crucial for API usability.

### Golden Rules

1. ✅ **Use nouns, not verbs**
2. ✅ **Use plural nouns** for collections
3. ✅ **Use lowercase** with hyphens (kebab-case)
4. ✅ **Be consistent**
5. ✅ **Keep it simple and intuitive**

### Resource Hierarchy

```
/resources               # Collection
/resources/{id}          # Single resource
/resources/{id}/sub      # Sub-collection
/resources/{id}/sub/{id} # Sub-resource
```

### Examples

```
✅ GOOD
GET    /api/pizzas                    # Get all pizzas
GET    /api/pizzas/5                  # Get pizza with ID 5
GET    /api/orders                    # Get all orders
GET    /api/orders/123/items          # Get items in order 123
GET    /api/customers/42/orders       # Get orders for customer 42

❌ BAD
GET    /api/getPizzas                 # Don't use verbs
GET    /api/pizza                     # Use plural
GET    /api/Pizzas                    # Use lowercase
GET    /api/pizza-management          # Too verbose
```

### Query Parameters for Filtering/Sorting

Use query parameters for:
- **Filtering**: `/pizzas?available=true`
- **Sorting**: `/pizzas?sort=price&order=asc`
- **Pagination**: `/pizzas?page=2&size=20`
- **Search**: `/pizzas?search=margherita`
- **Fields**: `/pizzas?fields=name,price` (sparse fieldsets)

```
GET /api/pizzas?available=true&maxPrice=10&sort=price&order=asc
GET /api/orders?status=pending&customerId=42&page=1&size=20
GET /api/customers?search=john&city=Brussels
```

---

## 🔢 API Versioning

### Why Version an API?

As an API evolves, you will eventually need to make a **breaking change** — tightening validation, renaming a field, changing a response shape — without breaking the clients (mobile apps, other services) that are already calling the existing behavior. **API versioning** lets you ship that breaking change as a *new* version while the old version keeps working exactly as before, for as long as older clients need it.

### Versioning Strategies

Spring Boot 4 and Spring Framework 7 support several ways to version an API:

| Strategy | Example | Notes |
|---|---|---|
| **URL Path** | `/api/v1/pizzas` vs `/api/v2/pizzas` | Most explicit; easiest for clients (and humans) to discover and explore |
| **Request Header** | `X-API-Version: 2` | Keeps the URI stable; version travels as metadata instead |
| **Media Type / Content Negotiation** | `Accept: application/vnd.pizzastore.v2+json` | Follows HTTP content-negotiation semantics most purely |
| **Query Parameter** | `/api/pizzas?version=2` | Simple, but easy to omit by accident — generally discouraged |

The book picks **URL Path Versioning** for its Customer CRM because it is *"the most explicit and easiest for clients to explore"* — you can paste the URL straight into a browser and immediately see which version you're hitting. We'll use the same approach for PizzaStore.

### URL Path Versioning: One Controller per Version

The pattern is straightforward: give each API version its **own package and its own `@RestController`**, mapped under its own version prefix. Don't cram both behaviors into a single controller with `if` statements — that becomes unreadable fast, and it defeats the point of keeping the legacy behavior untouched.

```java
// v1 — legacy, kept exactly as it always behaved
package be.vives.pizzastore.controller.v1;

@RestController
@RequestMapping("/api/v1/pizzas")
public class PizzaControllerV1 {

    private final PizzaService pizzaService;

    public PizzaControllerV1(PizzaService pizzaService) {
        this.pizzaService = pizzaService;
    }

    @PostMapping
    public ResponseEntity<PizzaResponse> create(@RequestBody CreatePizzaRequest request) {
        // Legacy behavior: no @Valid — still accepts the loosely-formed
        // requests that older mobile app builds in the field send
        PizzaResponse created = pizzaService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
```

```java
// v2 — modern, strict
package be.vives.pizzastore.controller.v2;

@RestController
@RequestMapping("/api/v2/pizzas")
public class PizzaControllerV2 {

    private final PizzaService pizzaService;

    public PizzaControllerV2(PizzaService pizzaService) {
        this.pizzaService = pizzaService;
    }

    @PostMapping
    public ResponseEntity<PizzaResponse> create(@Valid @RequestBody CreatePizzaRequest request) {
        // Modern behavior: @Valid triggers Jakarta Validation (Lesson 10).
        // Invalid input never reaches the service layer — GlobalExceptionHandler
        // turns it into a structured ProblemDetail response instead.
        PizzaResponse created = pizzaService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
```

If a client sends an invalid request (e.g. a blank `name`) to `/api/v1/pizzas`, it still succeeds — preserving backward compatibility for whatever still depends on the old, permissive behavior. The exact same payload sent to `/api/v2/pizzas` is rejected with a `400 Bad Request` `ProblemDetail`, because `@Valid` is active there. Both versions delegate to the same `PizzaService`, so business logic isn't duplicated — only the controller-level contract differs between versions.

### Verifying Both Versions Behave Differently

Once the unified `RestTestClient` is introduced in [Lesson 11](../lesson-11-testing/README.md), this dual behavior is easy to assert in a single test class — exactly the way *Pro Spring Boot 4* verifies its own `CustomerControllerV1`/`CustomerControllerV2` pair:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class PizzaVersioningTests {

    @Autowired
    private RestTestClient client;

    @Test
    void v1AcceptsInvalidData_v2RejectsIt() {
        CreatePizzaRequest invalid = new CreatePizzaRequest("", null, null, false, null);

        client.post().uri("/api/v1/pizzas")
            .body(invalid)
            .exchange()
            .expectStatus().isCreated();          // legacy: permissive

        client.post().uri("/api/v2/pizzas")
            .body(invalid)
            .exchange()
            .expectStatus().isBadRequest();        // modern: validated
    }
}
```

## 🎬 HTTP Methods and Their Semantics

### The CRUD Mapping

| HTTP Method | CRUD Operation | Idempotent | Safe |
|-------------|---------------|------------|------|
| **GET** | Read | ✅ Yes | ✅ Yes |
| **POST** | Create | ❌ No | ❌ No |
| **PUT** | Update/Replace | ✅ Yes | ❌ No |
| **PATCH** | Partial Update | ❌ No | ❌ No |
| **DELETE** | Delete | ✅ Yes | ❌ No |

### Idempotent

> Calling the same operation multiple times produces the same result

### Safe

> Does not modify server state (read-only)

---

## 📖 GET - Read Resources

**Purpose**: Retrieve resource(s)

### Characteristics
- ✅ Idempotent
- ✅ Safe (read-only)
- ✅ Cacheable
- ❌ Should NOT modify server state
- ❌ Should NOT have a request body

### Examples

```java
// Get collection
@GetMapping("/api/pizzas")
public List<PizzaResponse> getAllPizzas() {
    return pizzaService.findAll();
}

// Get single resource
@GetMapping("/api/pizzas/{id}")
public PizzaResponse getPizzaById(@PathVariable Long id) {
    return pizzaService.findById(id);
}

// Get with filtering
@GetMapping("/api/pizzas")
public List<PizzaResponse> getPizzas(
    @RequestParam(required = false) String name,
    @RequestParam(required = false) Boolean available
) {
    return pizzaService.search(name, available);
}

// Get nested resource
@GetMapping("/api/orders/{orderId}/lines")
public List<OrderLineResponse> getOrderLines(@PathVariable Long orderId) {
    return orderService.getOrderLines(orderId);
}
```

### Response Codes
- **200 OK**: Success
- **404 Not Found**: Resource doesn't exist
- **400 Bad Request**: Invalid query parameters

---

## ✏️ POST - Create Resources

**Purpose**: Create a new resource

### Characteristics
- ❌ NOT Idempotent (creates new resource each time)
- ❌ NOT Safe
- ❌ NOT Cacheable
- ✅ Has a request body

### Best Practices

```java
@PostMapping("/api/pizzas")
public ResponseEntity<PizzaResponse> createPizza(@Valid @RequestBody CreatePizzaRequest request) {
    PizzaResponse created = pizzaService.create(request);
    
    // Build Location URI
    URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(created.id())
        .toUri();
    
    // Return 201 Created with Location header
    return ResponseEntity
        .created(location)
        .body(created);
}
```

### Request

```json
POST /api/pizzas
Content-Type: application/json

{
  "name": "BBQ Chicken",
  "price": 13.99,
  "description": "BBQ sauce with grilled chicken",
  "available": true,
  "nutritionalInfo": {
    "calories": 850,
    "protein": 45.5,
    "carbs": 78.2,
    "fat": 32.1
  }
}
```

### Response

```
HTTP/1.1 201 Created
Location: http://localhost:8080/api/pizzas/13
Content-Type: application/json

{
  "id": 13,
  "name": "BBQ Chicken",
  "price": 13.99,
  "description": "BBQ sauce with grilled chicken",
  "available": true,
  "nutritionalInfo": {
    "calories": 850,
    "protein": 45.5,
    "carbs": 78.2,
    "fat": 32.1
  }
}
```

### Response Codes
- **201 Created**: Resource created successfully (+ Location header)
- **400 Bad Request**: Invalid request data
- **409 Conflict**: Resource already exists (e.g., duplicate name)

---

## 🔄 PUT - Update/Replace Resources

**Purpose**: Update or replace an **entire** resource

### Characteristics
- ✅ Idempotent (same call multiple times = same result)
- ❌ NOT Safe
- ✅ Has a request body
- ⚠️ Replaces the entire resource

### Best Practices

```java
@PutMapping("/api/pizzas/{id}")
public ResponseEntity<PizzaResponse> updatePizza(
    @PathVariable Long id,
    @Valid @RequestBody UpdatePizzaRequest request
) {
    PizzaResponse updated = pizzaService.update(id, request);
    return ResponseEntity.ok(updated);
}
```

### PUT vs PATCH

```java
// PUT: Replace entire resource
PUT /api/pizzas/1
{
  "name": "Margherita Special",
  "price": 9.99,
  "description": "Classic with extra cheese",
  "available": true,
  "nutritionalInfo": {
    "calories": 720,
    "protein": 28.0,
    "carbs": 85.0,
    "fat": 25.5
  }
}

// PATCH: Update specific fields only
PATCH /api/pizzas/1
{
  "price": 9.99,
  "available": false
}
```

### Response Codes
- **200 OK**: Update successful
- **204 No Content**: Update successful, no response body
- **404 Not Found**: Resource doesn't exist
- **400 Bad Request**: Invalid data

---

## 🩹 PATCH - Partial Update

**Purpose**: Update **specific fields** of a resource

### Characteristics
- ❌ NOT Idempotent (depends on implementation)
- ❌ NOT Safe
- ✅ Has a request body
- ✅ Updates only specified fields

### Implementation

```java
@PatchMapping("/api/pizzas/{id}")
public ResponseEntity<PizzaResponse> patchPizza(
    @PathVariable Long id,
    @RequestBody Map<String, Object> updates
) {
    PizzaResponse patched = pizzaService.partialUpdate(id, updates);
    return ResponseEntity.ok(patched);
}

// Or with specific operations
@PatchMapping("/api/pizzas/{id}/price")
public ResponseEntity<PizzaResponse> updatePrice(
    @PathVariable Long id,
    @RequestBody BigDecimal newPrice
) {
    PizzaResponse updated = pizzaService.updatePrice(id, newPrice);
    return ResponseEntity.ok(updated);
}

@PatchMapping("/api/pizzas/{id}/availability")
public ResponseEntity<PizzaResponse> toggleAvailability(@PathVariable Long id) {
    PizzaResponse updated = pizzaService.toggleAvailability(id);
    return ResponseEntity.ok(updated);
}
```

### Response Codes
- **200 OK**: Update successful
- **404 Not Found**: Resource doesn't exist
- **400 Bad Request**: Invalid patch data

---

## 🗑️ DELETE - Remove Resources

**Purpose**: Delete a resource

### Characteristics
- ✅ Idempotent (deleting same resource multiple times)
- ❌ NOT Safe
- ❌ Usually no request body

### Implementation

```java
@DeleteMapping("/api/pizzas/{id}")
public ResponseEntity<Void> deletePizza(@PathVariable Long id) {
    pizzaService.delete(id);
    return ResponseEntity.noContent().build();
}

// Soft delete (recommended in many cases)
@DeleteMapping("/api/pizzas/{id}")
public ResponseEntity<Void> deletePizza(@PathVariable Long id) {
    pizzaService.softDelete(id);  // Sets active=false instead of deleting
    return ResponseEntity.noContent().build();
}
```

### Response Codes
- **204 No Content**: Delete successful (preferred)
- **200 OK**: Delete successful with response body
- **404 Not Found**: Resource doesn't exist
- **409 Conflict**: Cannot delete (e.g., has dependencies)

---

## 🎨 HTTP Status Codes

### Success (2xx)

| Code | Name | Meaning | Use Case |
|------|------|---------|----------|
| **200** | OK | Success | GET, PUT, PATCH success |
| **201** | Created | Resource created | POST success |
| **204** | No Content | Success, no response body | DELETE success |

### Client Errors (4xx)

| Code | Name | Meaning | Use Case |
|------|------|---------|----------|
| **400** | Bad Request | Invalid request | Validation errors |
| **401** | Unauthorized | Not authenticated | Missing/invalid token |
| **403** | Forbidden | Not authorized | Insufficient permissions |
| **404** | Not Found | Resource not found | Resource doesn't exist |
| **409** | Conflict | Resource conflict | Duplicate, constraint violation |
| **422** | Unprocessable Entity | Semantic errors | Business rule validation |
| **429** | Too Many Requests | Rate limit exceeded | API rate limiting |

### Server Errors (5xx)

| Code | Name | Meaning | Use Case |
|------|------|---------|----------|
| **500** | Internal Server Error | Unexpected error | Unhandled exception |
| **503** | Service Unavailable | Service down | Maintenance, overload |

### Example: Comprehensive Error Responses

```json
// 400 Bad Request
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "name",
      "message": "Pizza name is required"
    },
    {
      "field": "price",
      "message": "Price must be positive"
    }
  ],
  "path": "/api/pizzas"
}

// 404 Not Found
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Pizza not found with id: 999",
  "path": "/api/pizzas/999"
}

// 409 Conflict
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "Pizza with name 'Margherita' already exists",
  "path": "/api/pizzas"
}
```

---

## 🌐 CORS (Cross-Origin Resource Sharing)

**CRITICAL for mobile apps!** Your mobile app runs on a different origin than your API.

### What is CORS?

CORS is a security feature implemented by browsers to prevent malicious websites from accessing your API.

```
Mobile App (localhost:4200)  →  API (localhost:8080)
   Different Origin!            Must allow CORS
```

### Simple CORS Configuration

```java
@Configuration
public class CorsConfig {
    
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:4200", "https://app.pizzastore.com")
                    .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600);
            }
        };
    }
}
```

#### Configuration Details:
- **`addMapping("/api/**")`**: Applies CORS to all endpoints starting with `/api/`.
- **`allowedOrigins("http://localhost:4200", "https://app.pizzastore.com")`**: Permits requests from these origins (e.g., local development and production app).
- **`allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")`**: Allows these HTTP methods.
- **`allowedHeaders("*")`**: Permits all request headers.
- **`allowCredentials(true)`**: Enables sending credentials (cookies, authorization headers) with requests.
- **`maxAge(3600)`**: Caches preflight responses for 1 hour to reduce server load.

### Configuration in application.properties

```properties
# CORS configuration
spring.web.cors.allowed-origins=http://localhost:4200,https://app.pizzastore.com
spring.web.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS
spring.web.cors.allowed-headers=*
spring.web.cors.allow-credentials=true
spring.web.cors.max-age=3600
```

---

## 📋 REST Best Practices Summary

### ✅ DO

1. **Use nouns for resources** (`/pizzas`, not `/getPizzas`)
2. **Use plural names** for collections
3. **Use HTTP methods correctly** (GET for read, POST for create, etc.)
4. **Return appropriate status codes** (201 for created, 404 for not found)
5. **Version your API** (`/api/v1/pizzas`, see [API Versioning](#-api-versioning) above)
6. **Use filtering via query parameters** (`?available=true`)
7. **Include pagination** for large collections
8. **Use consistent naming conventions** (camelCase or snake_case)
9. **Document your API** (Swagger/OpenAPI - covered in Lesson 13)
10. **Configure CORS** for mobile apps

### ❌ DON'T

1. **Don't use verbs in URIs** (`/createPizza` ❌)
2. **Don't use GET for operations that modify state**
3. **Don't ignore HTTP status codes** (don't return 200 for everything)
4. **Don't expose database IDs if not necessary** (use UUIDs for public APIs)
5. **Don't return entire entities** (use DTOs - covered in Lesson 7)
6. **Don't forget to handle errors consistently**

---

## 🎓 Richardson Maturity Model

A model to measure the RESTfulness of your API:

### Level 0: The Swamp of POX (Plain Old XML)

Single URI, single HTTP method (usually POST)

```
POST /api
{ "action": "getPizza", "id": 1 }
```

### Level 1: Resources

Multiple URIs, but still single HTTP method

```
POST /api/pizzas/1
{ "action": "get" }
```

### Level 2: HTTP Verbs

Multiple URIs, multiple HTTP methods ← **Most REST APIs are here**

```
GET    /api/pizzas/1
POST   /api/pizzas
PUT    /api/pizzas/1
DELETE /api/pizzas/1
```

### Level 3: Hypermedia Controls (HATEOAS)

Resources include links to related resources

```json
{
  "id": 1,
  "name": "Margherita",
  "price": 8.99,
  "description": "Classic tomato and mozzarella",
  "imageUrl": "margherita.jpg",
  "available": true,
  "nutritionalInfo": {
    "calories": 720,
    "protein": 28.0,
    "carbs": 85.0,
    "fat": 25.5
  },
  "_links": {
    "self": { "href": "/api/pizzas/1" },
    "orders": { "href": "/api/pizzas/1/orders" }
  }
}
```

**Goal:** Aim for **Level 2** at minimum, **Level 3** if HATEOAS benefits your clients.

---

## 🎓 Summary

### Key Takeaways

1. **REST is an architectural style**, not a protocol or standard
2. **Resources are nouns**, operations are HTTP methods
3. **Idempotence and safety** matter for HTTP methods
4. **Status codes communicate results** clearly
5. **CORS is essential** for web/mobile clients
6. **HATEOAS makes APIs discoverable** (optional but powerful)
7. **Consistency is key** - follow conventions

---

## 📖 Additional Resources

- [Roy Fielding's Dissertation on REST](https://www.ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm)
- [REST API Tutorial](https://restfulapi.net/)
- [HTTP Status Codes](https://httpstatuses.com/)
- [Richardson Maturity Model](https://martinfowler.com/articles/richardsonMaturityModel.html)
- [CORS Explained](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)

**Note on the book**: This lesson corresponds to *Pro Spring Boot 4*, Chapter 3: *Web Development with Spring Boot* — specifically the *RESTful API Design and Native Versioning* section, covering both its *Core REST Principles* subsection (resources as nouns, standard HTTP methods, meaningful status codes) and its *API Versioning Strategy* subsection (URL path vs. header vs. media type versioning, and the book's own `CustomerControllerV1`/`CustomerControllerV2` example, mirrored above as `PizzaControllerV1`/`PizzaControllerV2`). 

Chapter 3 goes on to cover Jakarta Bean Validation and `ProblemDetail`/`@RestControllerAdvice` exception handling ([Lesson 10](../lesson-10-validation-exception-handling/README.md)) and the unified `RestTestClient` ([Lesson 11](../lesson-11-testing/README.md)) — both referenced above, since the book itself demonstrates its versioning example together with `RestTestClient`.

---

**Well done!** 🎉 You now understand the principles that make a truly RESTful API. These principles will guide all your API design decisions going forward.
