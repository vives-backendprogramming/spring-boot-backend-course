# Lesson 9: Building a Complete REST API

**Implementing Full CRUD Operations with REST Best Practices**

---

## 📋 Learning Objectives

By the end of this lesson, you will be able to:
- Implement complete CRUD operations for a REST API
- Apply proper HTTP methods (GET, POST, PUT, PATCH, DELETE)
- Return appropriate HTTP status codes
- Handle query parameters for filtering and sorting
- Use pagination for large datasets
- Implement proper Location headers for created resources
- Handle soft deletes vs hard deletes
- Upload and serve files (images) in a Spring Boot application
- Apply all concepts from previous lessons in one complete API

---

## 📚 Table of Contents

1. [Recap: What We've Learned So Far](#recap-what-weve-learned-so-far)
2. [CRUD Operations Overview](#crud-operations-overview)
3. [CREATE: POST Operations](#create-post-operations)
4. [READ: GET Operations](#read-get-operations)
5. [UPDATE: PUT vs PATCH](#update-put-vs-patch)
6. [DELETE: Hard vs Soft Delete](#delete-hard-vs-soft-delete)
7. [Query Parameters & Filtering](#query-parameters--filtering)
8. [Pagination & Sorting](#pagination--sorting)
9. [File Upload for Images](#file-upload-for-images)
10. [Complete PizzaStore API](#complete-pizzastore-api)
11. [Summary](#summary)

---

## 🔄 Recap: What We've Learned So Far

Before building our complete REST API, let's recap the key concepts from previous lessons:

### From Lesson 4 & 5: Spring Boot & Spring MVC
- ✅ Spring MVC architecture
- ✅ `@RestController` and `@RequestMapping`
- ✅ Request handling with `@GetMapping`, `@PostMapping`, etc.
- ✅ `ResponseEntity<T>` for full control over HTTP responses

### From Lesson 6: Working with JPA
- ✅ Entity relationships (@OneToOne, @OneToMany, @ManyToOne, @ManyToMany)
- ✅ Spring Data JPA repositories
- ✅ Custom queries and pagination
- ✅ JOIN FETCH to avoid N+1 problems

### From Lesson 7: DTOs & Mappers
- ✅ Never expose entities directly
- ✅ Request DTOs for input (CreatePizzaRequest, UpdatePizzaRequest)
- ✅ Response DTOs for output (PizzaResponse)
- ✅ MapStruct for entity ↔ DTO mapping

### From Lesson 8: REST Principles
- ✅ Resource-based URLs (`/api/pizzas`, not `/api/getPizzas`)
- ✅ HTTP methods for actions (GET, POST, PUT, DELETE)
- ✅ HTTP status codes (200, 201, 204, 404, etc.)
- ✅ Proper use of headers (Location, Content-Type)


**Now we combine everything into a production-ready REST API!** 🚀

---

## 📋 CRUD Operations Overview

CRUD = **C**reate, **R**ead, **U**pdate, **D**elete

| Operation | HTTP Method | Endpoint | Success Status | Purpose |
|-----------|-------------|----------|----------------|---------|
| **Create** | POST | `/api/pizzas` | 201 Created | Create new resource |
| **Read All** | GET | `/api/pizzas` | 200 OK | List all resources |
| **Read One** | GET | `/api/pizzas/{id}` | 200 OK | Get single resource |
| **Update** | PUT | `/api/pizzas/{id}` | 200 OK | Full update (replace) |
| **Partial Update** | PATCH | `/api/pizzas/{id}` | 200 OK | Partial update (modify) |
| **Delete** | DELETE | `/api/pizzas/{id}` | 204 No Content | Delete resource |

### REST Principles Applied

1. **Resources**, not actions in URLs
   - ✅ `/api/pizzas` (resource)
   - ❌ `/api/getAllPizzas` (action)

2. **HTTP methods** define the action
   - ✅ `GET /api/pizzas` (read all)
   - ✅ `POST /api/pizzas` (create)
   - ❌ `GET /api/createPizza` (action in URL)

3. **Proper status codes**
   - `200 OK` - Success with body
   - `201 Created` - Resource created
   - `204 No Content` - Success without body
   - `404 Not Found` - Resource doesn't exist

4. **Consistent resource naming**
   - Use **plural** nouns: `/api/pizzas`, `/api/customers`, `/api/orders`
   - Use **kebab-case** for multi-word: `/api/order-items`

---

## ➕ CREATE: POST Operations

### Basic Create

```java
@PostMapping
public ResponseEntity<PizzaResponse> createPizza(
        @RequestBody CreatePizzaRequest request) {
    
    PizzaResponse created = pizzaService.create(request);
    
    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(created);
}
```

**Response**:
```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": 1,
  "name": "Margherita",
  "price": 8.50,
  "description": "Classic tomato and mozzarella",
  "createdAt": "2024-11-05T10:30:00"
}
```

### Create with Location Header

Best practice: Return a `Location` header pointing to the new resource.

```java
@PostMapping
public ResponseEntity<PizzaResponse> createPizza(
        @RequestBody CreatePizzaRequest request) {
    
    PizzaResponse created = pizzaService.create(request);
    
    // Build Location URI: /api/pizzas/{id}
    URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id())
            .toUri();
    
    return ResponseEntity
            .created(location)
            .body(created);
}
```

**Response**:
```http
HTTP/1.1 201 Created
Location: http://localhost:8080/api/pizzas/1
Content-Type: application/json

{
  "id": 1,
  "name": "Margherita",
  ...
}
```

The client can use the `Location` header to immediately fetch or reference the created resource.

### Creating Related Entities

When creating an Order with OrderLines:

```java
@PostMapping
public ResponseEntity<OrderResponse> createOrder(
        @RequestBody CreateOrderRequest request) {
    
    OrderResponse created = orderService.create(request);
    
    URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id())
            .toUri();
    
    return ResponseEntity.created(location).body(created);
}
```

**Request**:
```json
{
  "customerId": 1,
  "orderLines": [
    { "pizzaId": 1, "quantity": 2 },
    { "pizzaId": 2, "quantity": 1 }
  ]
}
```

**Response**:
```json
{
  "id": 1,
  "orderNumber": "ORD-2024-001",
  "customerId": 1,
  "customerName": "John Doe",
  "orderLines": [
    {
      "pizzaId": 1,
      "pizzaName": "Margherita",
      "quantity": 2,
      "unitPrice": 8.50,
      "subtotal": 17.00
    },
    {
      "pizzaId": 2,
      "pizzaName": "Pepperoni",
      "quantity": 1,
      "unitPrice": 9.50,
      "subtotal": 9.50
    }
  ],
  "totalAmount": 26.50,
  "status": "PENDING",
  "orderDate": "2024-11-05T10:30:00"
}
```

---

## 📖 READ: GET Operations

### Read All (List)

```java
@GetMapping
public ResponseEntity<List<PizzaResponse>> getAllPizzas() {
    List<PizzaResponse> pizzas = pizzaService.findAll();
    return ResponseEntity.ok(pizzas);
}
```

**Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "id": 1,
    "name": "Margherita",
    "price": 8.50
  },
  {
    "id": 2,
    "name": "Pepperoni",
    "price": 9.50
  }
]
```

### Read One (Single Resource)

```java
@GetMapping("/{id}")
public ResponseEntity<PizzaResponse> getPizza(@PathVariable Long id) {
    return pizzaService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
}
```

**Success Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": 1,
  "name": "Margherita",
  "price": 8.50
}
```

**Not Found Response**:
```http
HTTP/1.1 404 Not Found
```

### Read with Query Parameters

Filter resources using query parameters:

```java
@GetMapping
public ResponseEntity<List<PizzaResponse>> getPizzas(
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) String name) {
    
    if (maxPrice != null) {
        List<PizzaResponse> pizzas = pizzaService.findByPriceLessThan(maxPrice);
        return ResponseEntity.ok(pizzas);
    }
    
    if (name != null) {
        List<PizzaResponse> pizzas = pizzaService.findByNameContaining(name);
        return ResponseEntity.ok(pizzas);
    }
    
    List<PizzaResponse> pizzas = pizzaService.findAll();
    return ResponseEntity.ok(pizzas);
}
```

**Usage**:
```bash
# All pizzas
GET /api/pizzas

# Pizzas under €10
GET /api/pizzas?maxPrice=10.00

# Pizzas containing "Margherita"
GET /api/pizzas?name=Margherita
```

### Read with Pagination

For large datasets, use pagination:

```java
@GetMapping
public ResponseEntity<Page<PizzaResponse>> getPizzas(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sortBy) {
    
    Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
    Page<PizzaResponse> pizzaPage = pizzaService.findAll(pageable);
    
    return ResponseEntity.ok(pizzaPage);
}
```

**Usage**:
```bash
# First page (10 items)
GET /api/pizzas?page=0&size=10

# Second page, sorted by price
GET /api/pizzas?page=1&size=10&sortBy=price
```

**Response**:
```json
{
  "content": [
    { "id": 1, "name": "Margherita", "price": 8.50 },
    { "id": 2, "name": "Pepperoni", "price": 9.50 }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalPages": 5,
  "totalElements": 47,
  "first": true,
  "last": false
}
```

---

## ✏️ UPDATE: PUT vs PATCH

### PUT: Full Replacement

PUT replaces the **entire** resource. All fields must be provided.

```java
@PutMapping("/{id}")
public ResponseEntity<PizzaResponse> updatePizza(
        @PathVariable Long id,
        @RequestBody UpdatePizzaRequest request) {
    
    return pizzaService.update(id, request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
}
```

**Request**:
```json
{
  "name": "Margherita Deluxe",
  "price": 9.50,
  "description": "Premium mozzarella and fresh basil"
}
```

**All fields** must be present. Missing fields would be set to null/default values.

### PATCH: Partial Update

PATCH updates **only specified** fields. Other fields remain unchanged.

```java
@PatchMapping("/{id}")
public ResponseEntity<PizzaResponse> partialUpdatePizza(
        @PathVariable Long id,
        @RequestBody Map<String, Object> updates) {
    
    return pizzaService.partialUpdate(id, updates)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
}
```

**Request** (only update price):
```json
{
  "price": 9.50
}
```

The `name` and `description` fields remain unchanged.

### Implementation: Partial Update Service

```java
public Optional<PizzaResponse> partialUpdate(Long id, Map<String, Object> updates) {
    return pizzaRepository.findById(id)
            .map(pizza -> {
                updates.forEach((key, value) -> {
                    switch (key) {
                        case "name" -> pizza.setName((String) value);
                        case "price" -> pizza.setPrice(new BigDecimal(value.toString()));
                        case "description" -> pizza.setDescription((String) value);
                    }
                });
                Pizza updated = pizzaRepository.save(pizza);
                return pizzaMapper.toResponse(updated);
            });
}
```

### When to Use PUT vs PATCH

- **PUT**: Client knows and provides **all** fields
  - Example: Edit form with all fields
  - Complete replacement

- **PATCH**: Client updates **specific** fields
  - Example: Toggle switch, price adjustment
  - Partial modification

**Best Practice**: Use PUT for most update operations in REST APIs. PATCH is less commonly implemented.

---

## 🗑️ DELETE: Hard vs Soft Delete

### Hard Delete

Permanently removes the resource from the database.

```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> deletePizza(@PathVariable Long id) {
    if (pizzaService.delete(id)) {
        return ResponseEntity.noContent().build();  // 204 No Content
    }
    return ResponseEntity.notFound().build();       // 404 Not Found
}
```

**Response**:
```http
HTTP/1.1 204 No Content
```

Note: `204 No Content` means success **without** a response body.

### Hard Delete Implementation

```java
public boolean delete(Long id) {
    if (pizzaRepository.existsById(id)) {
        pizzaRepository.deleteById(id);
        return true;
    }
    return false;
}
```

### Soft Delete

Marks the resource as deleted without actually removing it.

**Add `deleted` field to entity**:
```java
@Entity
public class Pizza {
    // ... other fields
    
    @Column(nullable = false)
    private boolean deleted = false;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
```

**Soft delete implementation**:
```java
public boolean softDelete(Long id) {
    return pizzaRepository.findById(id)
            .map(pizza -> {
                pizza.setDeleted(true);
                pizza.setDeletedAt(LocalDateTime.now());
                pizzaRepository.save(pizza);
                return true;
            })
            .orElse(false);
}
```

**Exclude deleted resources**:
```java
@Query("SELECT p FROM Pizza p WHERE p.deleted = false")
List<Pizza> findAllActive();
```

### When to Use Hard vs Soft Delete

**Hard Delete**:
- ✅ When data should be permanently removed
- ✅ When storage is critical
- ✅ For test/temporary data

**Soft Delete**:
- ✅ When you need audit trail
- ✅ When data might need to be restored
- ✅ When other records reference the deleted data
- ✅ For customer/order data (legal requirements)

---

## 🔍 Query Parameters & Filtering

### Simple Filtering

```java
@GetMapping
public ResponseEntity<List<PizzaResponse>> getPizzas(
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) String name) {
    
    List<PizzaResponse> pizzas;
    
    if (minPrice != null && maxPrice != null) {
        pizzas = pizzaService.findByPriceBetween(minPrice, maxPrice);
    } else if (name != null) {
        pizzas = pizzaService.findByNameContaining(name);
    } else {
        pizzas = pizzaService.findAll();
    }
    
    return ResponseEntity.ok(pizzas);
}
```

**Usage**:
```bash
# Pizzas between €8 and €12
GET /api/pizzas?minPrice=8.00&maxPrice=12.00

# Pizzas with "quattro" in name
GET /api/pizzas?name=quattro
```

### Advanced Filtering with Specification

For complex filters, use Spring Data JPA Specifications:

```java
public interface PizzaRepository extends JpaRepository<Pizza, Long>,
                                         JpaSpecificationExecutor<Pizza> {
}
```

```java
@GetMapping
public ResponseEntity<List<PizzaResponse>> getPizzas(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice) {
    
    Specification<Pizza> spec = Specification.where(null);
    
    if (name != null) {
        spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
    }
    
    if (minPrice != null) {
        spec = spec.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("price"), minPrice));
    }
    
    if (maxPrice != null) {
        spec = spec.and((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("price"), maxPrice));
    }
    
    List<Pizza> pizzas = pizzaRepository.findAll(spec);
    return ResponseEntity.ok(pizzaMapper.toResponseList(pizzas));
}
```

---

## 📄 Pagination & Sorting

### Basic Pagination

```java
@GetMapping
public ResponseEntity<Page<PizzaResponse>> getPizzas(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
    
    Pageable pageable = PageRequest.of(page, size);
    Page<PizzaResponse> pizzaPage = pizzaService.findAll(pageable);
    
    return ResponseEntity.ok(pizzaPage);
}
```

### Pagination with Sorting

```java
@GetMapping
public ResponseEntity<Page<PizzaResponse>> getPizzas(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "asc") String direction) {
    
    Sort sort = direction.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();
    
    Pageable pageable = PageRequest.of(page, size, sort);
    Page<PizzaResponse> pizzaPage = pizzaService.findAll(pageable);
    
    return ResponseEntity.ok(pizzaPage);
}
```

**Usage**:
```bash
# Page 0, 10 items, sorted by price ascending
GET /api/pizzas?page=0&size=10&sortBy=price&direction=asc

# Page 1, 20 items, sorted by name descending
GET /api/pizzas?page=1&size=20&sortBy=name&direction=desc
```

### Service Layer Implementation

```java
public Page<PizzaResponse> findAll(Pageable pageable) {
    Page<Pizza> pizzaPage = pizzaRepository.findAll(pageable);
    return pizzaPage.map(pizzaMapper::toResponse);
}
```

### Response Structure

```json
{
  "content": [ /* array of pizzas */ ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalPages": 5,
  "totalElements": 47,
  "first": true,
  "last": false,
  "number": 0,
  "numberOfElements": 10,
  "size": 10
}
```

**Key fields**:
- `content`: Array of resources
- `totalElements`: Total number of items
- `totalPages`: Total number of pages
- `first`: Is this the first page?
- `last`: Is this the last page?
- `number`: Current page number

---

## 📤 File Upload for Images

Spring Boot provides built-in support for file uploads using `MultipartFile`. This is commonly used for uploading images, documents, or other files.

### Configuration

Configure file upload limits in `application.properties`:

```properties
# File Upload Configuration
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB
file.upload-dir=uploads/pizzas
file.base-url=http://localhost:8080
```

### File Storage Service

Create a service to handle file storage: `FileStorageService`

### Controller Endpoint

```java
@PostMapping("/{id}/image")
public ResponseEntity<PizzaResponse> uploadPizzaImage(
        @PathVariable Long id,
        @RequestParam("image") MultipartFile file) {
    
    try {
        return pizzaService.uploadImage(id, file)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().build();
    }
}
```

### Service Layer

```java
public Optional<PizzaResponse> uploadImage(Long id, MultipartFile file) {
    return pizzaRepository.findById(id)
            .map(pizza -> {
                String imageUrl = fileStorageService.storeFile(file, id);
                pizza.setImageUrl(imageUrl);
                Pizza updatedPizza = pizzaRepository.save(pizza);
                return pizzaMapper.toResponse(updatedPizza);
            });
}
```

### Serving Static Files

Configure Spring MVC to serve uploaded files:

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = Paths.get(uploadDir)
            .toAbsolutePath().toUri().toString();
        
        registry.addResourceHandler("/uploads/pizzas/**")
                .addResourceLocations(uploadPath);
    }
}
```

### Usage Example

Upload a pizza image using curl or Postman:

```bash
# Upload image for pizza with ID 1
curl -X POST http://localhost:8080/api/pizzas/1/image \
  -F "image=@/path/to/pizza.jpg"
```

**Response:**
```json
{
  "id": 1,
  "name": "Margherita",
  "price": 8.50,
  "imageUrl": "http://localhost:8080/uploads/pizzas/1-20240115143022.jpg",
  "available": true
}
```

The image is now accessible at the returned URL and will be served by Spring Boot.

---

## 🍕 Complete PizzaStore API

A complete REST API for the PizzaStore with all concepts applied.

### API Endpoints Overview

#### Pizza API
```
GET    /api/pizzas              - List all pizzas (with pagination)
GET    /api/pizzas/{id}         - Get single pizza
GET    /api/pizzas/search       - Search pizzas
POST   /api/pizzas              - Create new pizza
PUT    /api/pizzas/{id}         - Update pizza (full)
PATCH  /api/pizzas/{id}         - Update pizza (partial)
DELETE /api/pizzas/{id}         - Delete pizza
POST   /api/pizzas/{id}/image   - Upload pizza image
```

#### Customer API
```
GET    /api/customers           - List all customers
GET    /api/customers/{id}      - Get single customer
GET    /api/customers/{id}/orders - Get customer's orders
GET    /api/customers/{id}/favorites - Get customer's favorite pizzas
POST   /api/customers           - Create new customer
PUT    /api/customers/{id}      - Update customer
DELETE /api/customers/{id}      - Delete customer
POST   /api/customers/{id}/favorites/{pizzaId} - Add favorite
DELETE /api/customers/{id}/favorites/{pizzaId} - Remove favorite
```

#### Order API
```
GET    /api/orders              - List all orders (with filtering)
GET    /api/orders/{id}         - Get single order (with order lines)
POST   /api/orders              - Create new order
PUT    /api/orders/{id}/status  - Update order status
DELETE /api/orders/{id}         - Cancel order
```

---

## 💡 Best Practices Summary

### 1. Use Proper HTTP Methods
- ✅ GET for reading (safe, idempotent, cacheable)
- ✅ POST for creating (not idempotent)
- ✅ PUT for full updates (idempotent)
- ✅ PATCH for partial updates
- ✅ DELETE for removing (idempotent)

### 2. Return Proper Status Codes
- ✅ `200 OK` - Success with response body
- ✅ `201 Created` - Resource created successfully
- ✅ `204 No Content` - Success without response body
- ✅ `404 Not Found` - Resource doesn't exist

### 3. Use Location Headers
```java
return ResponseEntity
    .created(location)  // Sets Location header + 201 status
    .body(created);
```

### 4. Always Use DTOs
- ✅ Never expose entities
- ✅ Request DTOs for input
- ✅ Response DTOs for output
- ✅ MapStruct for mapping

### 5. Implement Pagination
```java
Page<PizzaResponse> findAll(Pageable pageable);
```

### 6. Use Query Parameters for Filtering
```java
GET /api/pizzas?maxPrice=10.00&name=margherita
```

### 7. Consistent Resource Naming
- ✅ Use plural nouns: `/api/pizzas`
- ✅ Use kebab-case: `/api/order-items`
- ❌ Avoid verbs: `/api/getPizzas`

### 8. Handle Optional Results
```java
return pizzaService.findById(id)
    .map(ResponseEntity::ok)
    .orElse(ResponseEntity.notFound().build());
```

---

## 🎓 Summary

### What We Learned

1. **CRUD Operations**
   - **CREATE**: POST with 201 Created + Location header
   - **READ**: GET with 200 OK, support pagination
   - **UPDATE**: PUT for full, PATCH for partial
   - **DELETE**: Hard delete or soft delete based on requirements

2. **Query Parameters**
   - Filter resources: `/api/pizzas?maxPrice=10.00`
   - Pagination: `/api/pizzas?page=0&size=10`
   - Sorting: `/api/pizzas?sortBy=price&direction=asc`

3. **HTTP Status Codes**
   - `200 OK`: Success with body
   - `201 Created`: Resource created
   - `204 No Content`: Success without body
   - `404 Not Found`: Resource not found

4. **Best Practices**
   - Always use DTOs
   - Return Location headers for created resources
   - Implement pagination for lists
   - Use proper HTTP methods
   - Consistent naming conventions

5. **Integration of Previous Lessons**
   - ✅ Spring MVC (controllers, request mapping)
   - ✅ REST principles (resource naming, HTTP methods)
   - ✅ DTOs & Mappers (never expose entities)
   - ✅ JPA (relationships, queries, pagination)

### Key Takeaways

⚠️ **Always use DTOs** - Never expose entities  
✅ **Location headers** for created resources  
📄 **Pagination** for large datasets  
🔍 **Query parameters** for filtering  
✏️ **PUT vs PATCH** - Full vs partial updates  
🗑️ **Hard vs Soft delete** - Based on requirements  

---

## 📖 Additional Resources

- [Spring Web MVC Documentation](https://docs.spring.io/spring-framework/reference/web/webmvc.html)
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/reference/)
- [RESTful API Design Best Practices](https://restfulapi.net/)
- [HTTP Status Codes Reference](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status)
- [Roy Fielding's REST Dissertation](https://www.ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm)

---

## 🚀 Runnable Project

A complete, production-ready Spring Boot project demonstrating all concepts is available in:

**`pizzastore-complete-api/`**

The project includes:
- ✅ Complete CRUD operations for Pizza, Customer, and Order
- ✅ All JPA relationships from Lesson 7
- ✅ DTOs and MapStruct mappers from Lesson 6
- ✅ Pagination and sorting
- ✅ Query parameters and filtering
- ✅ Proper HTTP status codes and Location headers
- ✅ File upload for pizza images
- ✅ application.properties configuration
- ✅ Logging configuration
- ✅ Sample data with all relationships

See the project README for setup instructions and API documentation.

---

**Congratulations!** 🎉 You now know how to build a complete, production-ready REST API with Spring Boot!
