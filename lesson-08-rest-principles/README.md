# Lesson 8: REST Principles

## 📚 Table of Contents

- [📘 Overview](#-overview)
- [🎯 Learning Objectives](#-learning-objectives)
- [🏛️ What is REST?](#️-what-is-rest)
- [🎯 REST Constraints](#-rest-constraints)
- [🗂️ Resource Naming Conventions](#️-resource-naming-conventions)
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
public ResponseEntity<Pizza> getPizza(@PathVariable Long id) {
    Pizza pizza = pizzaService.findById(id);
    
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
public List<Pizza> getAllPizzas() {
    return pizzaService.findAll();
}

// Get single resource
@GetMapping("/api/pizzas/{id}")
public Pizza getPizzaById(@PathVariable Long id) {
    return pizzaService.findById(id);
}

// Get with filtering
@GetMapping("/api/pizzas")
public List<Pizza> getPizzas(
    @RequestParam(required = false) String name,
    @RequestParam(required = false) Boolean available
) {
    return pizzaService.search(name, available);
}

// Get nested resource
@GetMapping("/api/orders/{orderId}/items")
public List<OrderItem> getOrderItems(@PathVariable Long orderId) {
    return orderService.getOrderItems(orderId);
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
public ResponseEntity<Pizza> createPizza(@Valid @RequestBody PizzaCreateRequest request) {
    Pizza created = pizzaService.create(request);
    
    // Build Location URI
    URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(created.getId())
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
  "description": "BBQ sauce with grilled chicken",
  "price": 13.99
}
```

### Response

```
HTTP/1.1 201 Created
Location: http://localhost:8080/api/pizzas/8
Content-Type: application/json

{
  "id": 8,
  "name": "BBQ Chicken",
  "description": "BBQ sauce with grilled chicken",
  "price": 13.99,
  "available": true
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
public ResponseEntity<Pizza> updatePizza(
    @PathVariable Long id,
    @Valid @RequestBody PizzaUpdateRequest request
) {
    Pizza updated = pizzaService.update(id, request);
    return ResponseEntity.ok(updated);
}
```

### PUT vs PATCH

```java
// PUT: Replace entire resource
PUT /api/pizzas/1
{
  "name": "Margherita Special",
  "description": "Classic with extra cheese",
  "price": 9.99,
  "available": true,
  "imageUrl": "margherita-special.jpg"
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
public ResponseEntity<Pizza> patchPizza(
    @PathVariable Long id,
    @RequestBody Map<String, Object> updates
) {
    Pizza patched = pizzaService.partialUpdate(id, updates);
    return ResponseEntity.ok(patched);
}

// Or with specific operations
@PatchMapping("/api/pizzas/{id}/price")
public ResponseEntity<Pizza> updatePrice(
    @PathVariable Long id,
    @RequestBody BigDecimal newPrice
) {
    Pizza updated = pizzaService.updatePrice(id, newPrice);
    return ResponseEntity.ok(updated);
}

@PatchMapping("/api/pizzas/{id}/availability")
public ResponseEntity<Pizza> toggleAvailability(@PathVariable Long id) {
    Pizza updated = pizzaService.toggleAvailability(id);
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

```java
// 400 Bad Request
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "name",
      "message": "Name is required"
    },
    {
      "field": "price",
      "message": "Price must be greater than 0"
    }
  ],
  "path": "/api/pizzas"
}

// 404 Not Found
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Pizza not found with id: 999",
  "path": "/api/pizzas/999"
}

// 409 Conflict
{
  "timestamp": "2024-01-15T10:30:00",
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
5. **Version your API** (`/api/v1/pizzas`)
6. **Use filtering via query parameters** (`?available=true`)
7. **Include pagination** for large collections
8. **Use consistent naming conventions** (camelCase or snake_case)
9. **Document your API** (Swagger/OpenAPI - covered in Lesson 16)
10. **Configure CORS** for mobile apps

### ❌ DON'T

1. **Don't use verbs in URIs** (`/createPizza` ❌)
2. **Don't use GET for operations that modify state**
3. **Don't ignore HTTP status codes** (don't return 200 for everything)
4. **Don't expose database IDs if not necessary** (use UUIDs for public APIs)
5. **Don't return entire entities** (use DTOs - covered in Lesson 6)
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

### REST Checklist for PizzaStore API

- ✅ Resources use nouns (`/pizzas`, `/orders`, `/customers`)
- ✅ Plural names for collections
- ✅ HTTP methods used correctly
- ✅ Appropriate status codes returned
- ✅ Query parameters for filtering/sorting
- ✅ CORS configured for mobile app
- 🔲 DTOs instead of entities (next lesson!)
- 🔲 Proper error handling (Lesson 11)
- 🔲 API documentation (Lesson 16)
- 🔲 Security (Lessons 12-15)

---

## 📖 Additional Resources

- [Roy Fielding's Dissertation on REST](https://www.ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm)
- [REST API Tutorial](https://restfulapi.net/)
- [HTTP Status Codes](https://httpstatuses.com/)
- [Richardson Maturity Model](https://martinfowler.com/articles/richardsonMaturityModel.html)
- [CORS Explained](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)

---

**Well done!** 🎉 You now understand the principles that make a truly RESTful API. These principles will guide all your API design decisions going forward.
