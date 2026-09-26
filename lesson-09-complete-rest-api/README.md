# Lesson 9: Building a Complete REST API

**Implementing Full CRUD Operations with REST Best Practices**

---

## 📋 Learning Objectives

By the end of this lesson, you will be able to:
- Implement complete CRUD operations for a REST API on top of a service layer
- Apply the right HTTP method (GET, POST, PUT, PATCH, DELETE) for each operation
- Return appropriate HTTP status codes and a `Location` header for created resources
- Handle query parameters for filtering
- Use Spring Data's `Pageable` for pagination and sorting, and understand the JSON a `Page` produces
- Choose between hard deletes and soft deletes
- Upload and serve files (images) in a Spring Boot application
- Recognize the limits of this lesson's error handling, which Lesson 10 fixes

---

## 📚 Table of Contents

1. [Recap: What We've Learned So Far](#-recap-what-weve-learned-so-far)
2. [What This Lesson Adds to PizzaStore](#-what-this-lesson-adds-to-pizzastore)
3. [CRUD Operations Overview](#-crud-operations-overview)
4. [CREATE: POST Operations](#-create-post-operations)
5. [READ: GET Operations](#-read-get-operations)
6. [UPDATE: PUT vs PATCH](#%EF%B8%8F-update-put-vs-patch)
7. [DELETE: Hard vs Soft Delete](#%EF%B8%8F-delete-hard-vs-soft-delete)
8. [Query Parameters & Filtering](#-query-parameters--filtering)
9. [Pagination & Sorting](#-pagination--sorting)
10. [File Upload for Images](#-file-upload-for-images)
11. [Complete PizzaStore API](#-complete-pizzastore-api)
12. [What Happens When Things Go Wrong?](#%EF%B8%8F-what-happens-when-things-go-wrong)
13. [Best Practices Summary](#-best-practices-summary)
14. [Summary](#-summary)
15. [Runnable Project](#-runnable-project)

---

## 🔄 Recap: What We've Learned So Far

Before building our complete REST API, let's recap the key concepts from previous lessons:

### From Lesson 4 & 5: Spring Boot & Spring MVC
- ✅ Spring MVC architecture (`DispatcherServlet`, handler mapping, message converters)
- ✅ `@RestController` and `@RequestMapping`
- ✅ Request handling with `@GetMapping`, `@PostMapping`, `@PathVariable`, `@RequestParam`, `@RequestBody`
- ✅ `ResponseEntity<T>` for full control over HTTP responses

### From Lesson 6a: Spring Data JPA
- ✅ Entity relationships (`@OneToOne`, `@OneToMany`, `@ManyToOne`, `@ManyToMany`)
- ✅ Spring Data JPA repositories and derived query methods
- ✅ Custom queries, `Pageable` and `Page` in repositories
- ✅ `JOIN FETCH` to avoid N+1 problems

### From Lesson 7: DTOs, Mappers & the Service Layer
- ✅ Never expose entities directly
- ✅ Request DTOs for input (`CreatePizzaRequest`, `UpdatePizzaRequest`)
- ✅ Response DTOs for output (`PizzaResponse`)
- ✅ MapStruct for entity ↔ DTO mapping
- ✅ Service layer (`PizzaService`, `CustomerService`, `OrderService`) with `@Transactional`
- ✅ Services signal "not found" with `Optional.empty()` or `false`

### From Lesson 8: REST Principles
- ✅ Resource-based URLs (`/api/pizzas`, not `/api/getPizzas`)
- ✅ HTTP methods for actions, and their safety/idempotency
- ✅ HTTP status codes (200, 201, 204, 404, ...)
- ✅ Proper use of headers (`Location`, `Content-Type`)

**Now we put a web layer on top of the services from Lesson 7 and turn PizzaStore into a working REST API.** 🚀

---

## 🧱 What This Lesson Adds to PizzaStore

PizzaStore is built from the bottom up. Lesson 6a gave us the domain model and repositories, Lesson 7 added DTOs, mappers and the service layer. This lesson adds the **top layer**: the controllers that turn HTTP requests into service calls.

```
┌─────────────────┐
│   Controller    │  ← NEW in this lesson: HTTP ↔ DTOs, status codes, headers
├─────────────────┤
│    Service      │  ← Lesson 7: use cases, transactions, DTO ↔ entity mapping
├─────────────────┤
│   Repository    │  ← Lesson 6a: database access
├─────────────────┤
│     Domain      │  ← Lesson 6a: JPA entities
└─────────────────┘
```

The project in this lesson, [`pizzastore-complete-api`](pizzastore-complete-api), is **Lesson 7's [`pizzastore-with-dtos`](../lesson-07-dtos-mappers/pizzastore-with-dtos) plus exactly these additions**:

| Added / changed | What it does |
|-----------------|--------------|
| [`controller/PizzaController.java`](pizzastore-complete-api/src/main/java/be/vives/pizzastore/controller/PizzaController.java) | CRUD for pizzas, filtering on price/name, pagination, image upload |
| [`controller/CustomerController.java`](pizzastore-complete-api/src/main/java/be/vives/pizzastore/controller/CustomerController.java) | CRUD for customers, a customer's orders, favorite pizzas |
| [`controller/OrderController.java`](pizzastore-complete-api/src/main/java/be/vives/pizzastore/controller/OrderController.java) | Create/read orders, filter by customer or status, change status, cancel |
| [`service/FileStorageService.java`](pizzastore-complete-api/src/main/java/be/vives/pizzastore/service/FileStorageService.java) | Validates and stores uploaded images on disk |
| [`config/WebConfig.java`](pizzastore-complete-api/src/main/java/be/vives/pizzastore/config/WebConfig.java) | Serves the uploaded images as static files |
| [`service/PizzaService.java`](pizzastore-complete-api/src/main/java/be/vives/pizzastore/service/PizzaService.java) | One new method: `uploadImage(id, file)` |
| [`application.properties`](pizzastore-complete-api/src/main/resources/application.properties) | Multipart limits, upload directory, pretty-printed JSON, DEBUG logging for our own code |

Everything else — `domain`, `repository`, `dto`, `mapper`, the other service methods and `data.sql` — is **unchanged** from Lesson 7. The controllers are the same as in the final PizzaStore, minus two things that later lessons add: `@Valid` on the request bodies (Lesson 10) and the OpenAPI annotations like `@Operation` and `@Tag` (Lesson 13).

---

## 📋 CRUD Operations Overview

CRUD = **C**reate, **R**ead, **U**pdate, **D**elete. Mapped onto HTTP for the `Pizza` resource:

| Operation | HTTP Method | Endpoint | Success Status | Purpose |
|-----------|-------------|----------|----------------|---------|
| **Create** | POST | `/api/pizzas` | 201 Created | Create a new pizza |
| **Read All** | GET | `/api/pizzas` | 200 OK | List pizzas (paginated / filtered) |
| **Read One** | GET | `/api/pizzas/{id}` | 200 OK | Get a single pizza |
| **Update** | PUT | `/api/pizzas/{id}` | 200 OK | Replace a pizza's data |
| **Delete** | DELETE | `/api/pizzas/{id}` | 204 No Content | Delete a pizza |

A **partial update** with PATCH is the sixth common operation. PizzaStore uses it for exactly one thing: changing the status of an order (`PATCH /api/orders/{id}/status`) — see [PUT vs PATCH](#%EF%B8%8F-update-put-vs-patch).

### REST Principles Applied

These come straight from [Lesson 8](../lesson-08-rest-principles/README.md):

1. **Resources**, not actions, in URLs — ✅ `/api/pizzas`, ❌ `/api/getAllPizzas`
2. **HTTP methods** define the action — ✅ `POST /api/pizzas`, ❌ `GET /api/createPizza`
3. **Proper status codes** — `200 OK`, `201 Created`, `204 No Content`, `404 Not Found`
4. **Consistent resource naming** — plural nouns (`/api/pizzas`, `/api/customers`, `/api/orders`) and sub-resources for relationships (`/api/customers/{id}/orders`)

### The Controller Pattern

Every PizzaStore controller follows the same pattern: inject the service through the constructor, delegate each request to one service method, and translate the result into a `ResponseEntity`. The controller never touches a repository or an entity — it only sees DTOs.

```java
@RestController
@RequestMapping("/api/pizzas")
public class PizzaController {

    private static final Logger log = LoggerFactory.getLogger(PizzaController.class);

    private final PizzaService pizzaService;

    public PizzaController(PizzaService pizzaService) {
        this.pizzaService = pizzaService;
    }

    // one method per endpoint ...
}
```

---

## ➕ CREATE: POST Operations

### Basic Create

The simplest version returns `201 Created` with the new resource in the body:

```java
@PostMapping
public ResponseEntity<PizzaResponse> createPizza(@RequestBody CreatePizzaRequest request) {
    PizzaResponse created = pizzaService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
}
```

### Create with Location Header

Best practice: also return a `Location` header that points to the new resource. This is what PizzaStore does:

```java
@PostMapping
public ResponseEntity<PizzaResponse> createPizza(@RequestBody CreatePizzaRequest request) {
    log.debug("POST /api/pizzas - {}", request);

    PizzaResponse created = pizzaService.create(request);

    // Build Location URI: <current request URL>/{id}
    URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id())
            .toUri();

    return ResponseEntity.created(location).body(created);   // 201 + Location header
}
```

`ServletUriComponentsBuilder.fromCurrentRequest()` starts from the URL of the request being handled (`http://localhost:8080/api/pizzas`), so the controller never has to hard-code a host name or port.

**Request**:
```bash
curl -i -X POST http://localhost:8080/api/pizzas \
  -H "Content-Type: application/json" \
  -d '{"name":"Hawaii","price":10.5,"description":"Ham and pineapple","available":true,
       "nutritionalInfo":{"calories":280,"protein":12,"carbohydrates":33,"fat":9}}'
```

**Response**:
```http
HTTP/1.1 201
Location: http://localhost:8080/api/pizzas/7
Content-Type: application/json

{
  "id" : 7,
  "name" : "Hawaii",
  "price" : 10.5,
  "description" : "Ham and pineapple",
  "imageUrl" : null,
  "available" : true,
  "nutritionalInfo" : {
    "calories" : 280,
    "protein" : 12,
    "carbohydrates" : 33,
    "fat" : 9
  }
}
```

The client can use the `Location` header to immediately fetch or reference the created resource. `imageUrl` is `null`: the image is set through a separate [upload endpoint](#-file-upload-for-images).

### Creating Related Entities

Creating an order is the same pattern in the controller — the complexity (looking up the customer and pizzas, creating the order lines, calculating the total) lives in `OrderService.create` from Lesson 7:

```java
@PostMapping
public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
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
  "customerId": 2,
  "orderLines": [
    { "pizzaId": 1, "quantity": 2 },
    { "pizzaId": 2, "quantity": 1 }
  ]
}
```

**Response** (`201 Created`, `Location: http://localhost:8080/api/orders/4`):
```json
{
  "id" : 4,
  "orderNumber" : "ORD-2026-000004",
  "customerId" : 2,
  "customerName" : "Liam Smith",
  "orderLines" : [ {
    "id" : 7,
    "pizzaId" : 1,
    "pizzaName" : "Margherita",
    "quantity" : 2,
    "unitPrice" : 8.99,
    "subtotal" : 17.98
  }, {
    "id" : 8,
    "pizzaId" : 2,
    "pizzaName" : "Pepperoni",
    "quantity" : 1,
    "unitPrice" : 10.99,
    "subtotal" : 10.99
  } ],
  "totalAmount" : 28.97,
  "status" : "PENDING",
  "orderDate" : "2026-09-26T16:02:11.229556"
}
```

---

## 📖 READ: GET Operations

### Read One (Single Resource)

The services from Lesson 7 return an `Optional` when a resource might not exist. The controller turns an empty `Optional` into a `404 Not Found`:

```java
@GetMapping("/{id}")
public ResponseEntity<PizzaResponse> getPizza(@PathVariable Long id) {
    log.debug("GET /api/pizzas/{}", id);
    return pizzaService.findById(id)
            .map(ResponseEntity::ok)                      // found     → 200 OK + body
            .orElse(ResponseEntity.notFound().build());   // not found → 404, no body
}
```

**Success Response**:
```http
HTTP/1.1 200
Content-Type: application/json

{
  "id" : 1,
  "name" : "Margherita",
  "price" : 8.99,
  "description" : "Classic tomato sauce, fresh mozzarella, basil, and extra virgin olive oil",
  "imageUrl" : "https://images.unsplash.com/photo-1574071318508-1cdbab80d002",
  "available" : true,
  "nutritionalInfo" : {
    "calories" : 266,
    "protein" : 11.00,
    "carbohydrates" : 33.00,
    "fat" : 10.00
  }
}
```

**Not Found Response** (`GET /api/pizzas/999`):
```http
HTTP/1.1 404
Content-Length: 0
```

An empty 404 is correct, but it doesn't tell the client *what* wasn't found. In Lesson 10 the services throw a `ResourceNotFoundException` instead, and a global exception handler turns it into a `ProblemDetail` body.

### Read All (List)

For the collection resource, `GET /api/pizzas` returns either a paginated `Page` or a filtered `List`, depending on the query parameters. Both are explained in their own sections below: [Query Parameters & Filtering](#-query-parameters--filtering) and [Pagination & Sorting](#-pagination--sorting).

### Read Sub-Resources

Relationships are exposed as **sub-resources** under their owner:

```java
@GetMapping("/{id}/favorites")
public ResponseEntity<List<PizzaResponse>> getFavoritePizzas(@PathVariable Long id) {
    List<PizzaResponse> favorites = customerService.findFavoritePizzas(id);
    return ResponseEntity.ok(favorites);
}
```

```bash
GET /api/customers/1/favorites    # the favorite pizzas of customer 1
GET /api/customers/1/orders       # the orders of customer 1 (newest first)
```

---

## ✏️ UPDATE: PUT vs PATCH

### PUT: Full Replacement

PUT **replaces** the resource's updatable state with what the client sends. PizzaStore's `updatePizza` has the same "found → 200 / not found → 404" structure as `getPizza`:

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

**Request** (`PUT /api/pizzas/1`, body = `UpdatePizzaRequest`):
```json
{
  "name": "Margherita Deluxe",
  "price": 9.50,
  "description": "Premium mozzarella and fresh basil",
  "available": true,
  "nutritionalInfo": {
    "calories": 250,
    "protein": 12.5,
    "carbohydrates": 30.0,
    "fat": 8.5
  }
}
```

With PUT, the client sends **all** updatable fields. That's not just a convention: `PizzaMapper.updateEntity` (Lesson 7) copies every field of the request onto the entity, and MapStruct's default for a `null` source value on an update method is to set the target to `null` as well. So a "PUT" with only a price

```json
{ "price": 12 }
```

sets `name` and `available` to `null` too — and because those columns are `NOT NULL`, the request fails with a `DataIntegrityViolationException` and a **500 Internal Server Error**. Leaving out `nutritionalInfo` in a PUT likewise removes the pizza's nutritional info. That's PUT semantics working as designed (the client said "this is the new state"); the problem is that a client mistake shows up as a server error. Lesson 10 adds Bean Validation (`@Valid` plus constraints on the DTOs) so incomplete input is rejected with a `400 Bad Request` before it ever reaches the database.

### PATCH: Partial Update

PATCH changes **only the specified** fields; everything else stays as it is. PizzaStore uses PATCH for one clearly scoped change — the status of an order:

```java
@PatchMapping("/{id}/status")
public ResponseEntity<OrderResponse> updateOrderStatus(
        @PathVariable Long id,
        @RequestBody UpdateOrderStatusRequest request) {

    return orderService.updateStatus(id, request.status())
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
}
```

**Request** (`PATCH /api/orders/1/status`, body = `UpdateOrderStatusRequest`):
```json
{
  "status": "PREPARING"
}
```

The service method from Lesson 7 changes just that field:

```java
public Optional<OrderResponse> updateStatus(Long id, OrderStatus status) {
    return orderRepository.findById(id)
            .map(order -> {
                order.setStatus(status);
                Order updatedOrder = orderRepository.save(order);
                return orderMapper.toResponse(updatedOrder);
            });
}
```

Modelling the status as its own sub-resource (`/orders/{id}/status`) with its own tiny DTO keeps the PATCH simple: there's no ambiguity about what a missing field means, because there's only one field.

### When to Use PUT vs PATCH

| | PUT | PATCH |
|---|---|---|
| Meaning | "Here is the complete new state" | "Change only these fields" |
| Missing field | Becomes empty/`null` | Stays unchanged |
| Idempotent? | Yes | Not guaranteed (e.g. "add 1 to quantity") |
| Typical use | Edit form with all fields | Toggle, status change, single-field update |
| In PizzaStore | Update pizza, update customer | Update order status |

**Best practice**: use PUT for regular "edit this resource" operations. Use PATCH for small, well-defined changes — ideally, as in PizzaStore, on a dedicated sub-resource so the semantics are obvious.

---

## 🗑️ DELETE: Hard vs Soft Delete

### Hard Delete

Permanently removes the resource from the database. PizzaStore hard-deletes pizzas and customers:

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
HTTP/1.1 204
```

`204 No Content` means success **without** a response body. The service (Lesson 7) returns a `boolean` so the controller can tell the difference between "deleted" and "didn't exist":

```java
public boolean delete(Long id) {
    if (pizzaRepository.existsById(id)) {
        pizzaRepository.deleteById(id);
        return true;
    }
    return false;
}
```

Is DELETE still idempotent if the second call returns 404 instead of 204? Yes: idempotency is about the **state on the server** — after one call or after ten, the pizza is gone. The status code may differ.

> ⚠️ A hard delete can collide with foreign keys. Deleting a pizza that is still in someone's favorites (`DELETE /api/pizzas/1`) currently fails with a `DataIntegrityViolationException` → **500**. A `409 Conflict` with an explanation would be better; that's the kind of error mapping Lesson 10 introduces.

### Soft Delete

Marks the resource as deleted (or inactive) without actually removing it. PizzaStore never deletes orders — `DELETE /api/orders/{id}` **cancels** the order by changing its status to `CANCELLED`:

```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
    if (orderService.cancel(id)) {
        return ResponseEntity.noContent().build();
    }
    return ResponseEntity.notFound().build();
}
```

```java
public boolean cancel(Long id) {
    return orderRepository.findById(id)
            .map(order -> {
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                return true;
            })
            .orElse(false);
}
```

The order stays retrievable (`GET /api/orders/2` shows `"status" : "CANCELLED"`) and can be listed with `GET /api/orders?status=CANCELLED`.

A common extension — **not** part of PizzaStore — is to hide soft-deleted rows from the default listing, for example with a derived query method (Lesson 6a) that the service uses instead of `findAll(pageable)`:

```java
Page<Order> findByStatusNot(OrderStatus status, Pageable pageable);   // called with OrderStatus.CANCELLED
```

### When to Use Hard vs Soft Delete

**Hard Delete**:
- ✅ When data should be permanently removed (e.g. GDPR "right to be forgotten")
- ✅ For data without historical value (test data, temporary records)

**Soft Delete**:
- ✅ When you need an audit trail
- ✅ When data might need to be restored
- ✅ When other records reference the deleted data
- ✅ For financial data like orders (legal retention requirements)

---

## 🔍 Query Parameters & Filtering

### Filters Belong in the Query String

A filter doesn't identify a **different resource** — it narrows down the **same collection**. That's why filters are query parameters on the collection URL, not extra path segments or separate endpoints:

- ✅ `GET /api/pizzas?maxPrice=10`
- ❌ `GET /api/pizzas/cheap`
- ❌ `GET /api/pizzas/maxPrice/10`

Path variables (`/api/pizzas/{id}`) identify *which* resource you want; query parameters (`?maxPrice=10`) describe *how* you want to see a collection — filtered, sorted, paged.

### `@RequestParam` for Optional Filters

`@RequestParam` binds a query parameter to a method parameter. 

By default a `@RequestParam` is **required**: leaving it out gives `400 Bad Request`. For filters that's not what we want, so PizzaStore marks them `required = false` — the parameter is then simply `null` when the client doesn't send it.

Two other attributes you'll come across:

```java
@RequestParam(defaultValue = "10") int size        // used when the parameter is absent (implies required = false)
@RequestParam("q") String name                     // query parameter name differs from the Java name: ?q=marg
```

### Filtering Pizzas

This is PizzaStore's `getPizzas` (see [`PizzaController.java`](pizzastore-complete-api/src/main/java/be/vives/pizzastore/controller/PizzaController.java)):

```java
@GetMapping
public ResponseEntity<?> getPizzas(
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) String name,
        Pageable pageable) {

    // If filtering by price range
    if (minPrice != null && maxPrice != null) {
        List<PizzaResponse> pizzas = pizzaService.findByPriceBetween(minPrice, maxPrice);
        return ResponseEntity.ok(pizzas);
    }

    // If filtering by max price
    if (maxPrice != null) {
        List<PizzaResponse> pizzas = pizzaService.findByPriceLessThan(maxPrice);
        return ResponseEntity.ok(pizzas);
    }

    // If filtering by name
    if (name != null) {
        List<PizzaResponse> pizzas = pizzaService.findByNameContaining(name);
        return ResponseEntity.ok(pizzas);
    }

    // Default: return paginated pizzas
    Page<PizzaResponse> pizzaPage = pizzaService.findAll(pageable);
    return ResponseEntity.ok(pizzaPage);
}
```

Each branch calls one service method, which in turn uses one derived query method from Lesson 6a:

| Request | Service method | Repository method |
|---------|----------------|-------------------|
| `?minPrice=8&maxPrice=12` | `findByPriceBetween` | `findByPriceBetween` (bounds inclusive) |
| `?maxPrice=10` | `findByPriceLessThan` | `findByPriceLessThan` (strictly less than) |
| `?name=quattro` | `findByNameContaining` | `findByNameContainingIgnoreCase` |
| no filter | `findAll(pageable)` | `findAll(pageable)` |

**Usage**:
```bash
# Pizzas between €8 and €12
GET /api/pizzas?minPrice=8.00&maxPrice=12.00

# Pizzas under €10
GET /api/pizzas?maxPrice=10.00

# Pizzas with "quattro" in the name (case-insensitive)
GET /api/pizzas?name=quattro
```

Things to notice about this implementation:

- **The first matching branch wins.** The filters don't combine: `?maxPrice=10&name=marg` only applies `maxPrice`, and `minPrice` on its own isn't a filter at all — it falls through to the unfiltered, paginated list.
- **Two response shapes.** A filtered request returns a JSON **array** (`List<PizzaResponse>`), an unfiltered one a **page object** (`Page<PizzaResponse>`, see [Pagination & Sorting](#-pagination--sorting)). Because the method can return either, its return type is `ResponseEntity<?>`. A client has to know which shape to expect for which request.
- **Filtered results aren't paginated.** `page`, `size` and `sort` are ignored as soon as a filter is used. With six pizzas on the menu that's no problem; for a large collection you'd pass the `Pageable` on to the filter queries as well, as the orders do below.

### Filtering Orders

`OrderController.getOrders` (see [`OrderController.java`](pizzastore-complete-api/src/main/java/be/vives/pizzastore/controller/OrderController.java)) uses the same idea, but every branch returns a `Page`, so the response always has the same shape:

```java
@GetMapping
public ResponseEntity<Page<OrderResponse>> getOrders(
        @RequestParam(required = false) Long customerId,
        @RequestParam(required = false) OrderStatus status,
        Pageable pageable) {

    Page<OrderResponse> orders;

    if (customerId != null) {
        orders = orderService.findByCustomerId(customerId, pageable);
    } else if (status != null) {
        orders = orderService.findByStatus(status, pageable);
    } else {
        orders = orderService.findAll(pageable);
    }

    return ResponseEntity.ok(orders);
}
```

**Usage**:
```bash
# All orders of customer 1, newest first
GET /api/orders?customerId=1&sort=orderDate,desc

# All cancelled orders
GET /api/orders?status=CANCELLED
```

Here too the filters don't combine — `customerId` takes precedence over `status`.

### Filter or Sub-Resource?

`GET /api/orders?customerId=1` and `GET /api/customers/1/orders` return the same orders. Both styles are common, and PizzaStore offers both:

- the **sub-resource** reads naturally when you navigate from a customer ("the orders *of* customer 1");
- the **filter** fits when the collection is the starting point and the customer is just one of several criteria ("orders, restricted to customer 1").

---

## 📄 Pagination & Sorting

### Pageable as a Controller Parameter

A `Pageable` controller parameter is filled in automatically by Spring Data's web support (auto-configured by Spring Boot) from three query parameters:

| Query parameter | Meaning | Default |
|-----------------|---------|---------|
| `page` | Page number, **0-indexed** | `0` |
| `size` | Items per page | `20` |
| `sort` | `property,direction` — may be repeated | unsorted |

```java
@GetMapping
public ResponseEntity<Page<CustomerResponse>> getAllCustomers(Pageable pageable) {
    Page<CustomerResponse> customers = customerService.findAll(pageable);
    return ResponseEntity.ok(customers);
}
```

The service passes the `Pageable` to the repository and maps each entity to a DTO — `Page.map` keeps all the paging metadata:

```java
public Page<CustomerResponse> findAll(Pageable pageable) {
    Page<Customer> customerPage = customerRepository.findAll(pageable);
    return customerPage.map(customerMapper::toResponse);
}
```

**Usage**:
```bash
# First page with the default size (20)
GET /api/pizzas

# Page 0, 10 items
GET /api/pizzas?page=0&size=10

# Sorted by price ascending
GET /api/pizzas?sort=price,asc

# Page 1, 2 items per page, sorted by name
GET /api/pizzas?page=1&size=2&sort=name

# Multiple sort criteria
GET /api/pizzas?sort=available,desc&sort=price,asc
```

`sort` uses the **entity property names** (`price`, `name`), not column names. Sorting on a property that doesn't exist (`?sort=foo`) currently results in a 500 error.

### Fixed Paging in the Controller

Sometimes you don't want the client to choose everything. `CustomerController.getCustomerOrders` accepts only `page` and `size` and always sorts newest-first, by building the `Pageable` itself — the same `PageRequest.of(...)` approach the book uses in its service layer:

```java
@GetMapping("/{id}/orders")
public ResponseEntity<Page<OrderResponse>> getCustomerOrders(
        @PathVariable Long id,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {

    Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
    Page<OrderResponse> orders = orderService.findByCustomerId(id, pageable);
    return ResponseEntity.ok(orders);
}
```

The service hands the `Pageable` straight to a derived query method (Lesson 6a), so filtering on the customer, sorting and paging all happen in one database query:

```java
public Page<OrderResponse> findByCustomerId(Long customerId, Pageable pageable) {
    Page<Order> orderPage = orderRepository.findByCustomerId(customerId, pageable);
    return orderPage.map(orderMapper::toResponse);
}
```

`GET /api/orders?customerId=1` uses the same service method, with the client's own `page`/`size`/`sort`.

### Response Structure

Returning a `Page<T>` produces a JSON object with the items in `content` plus paging metadata. This is the actual output of `GET /api/pizzas?size=2&sort=price,desc`:

```json
{
  "content" : [ {
    "id" : 5,
    "name" : "Diavola",
    "price" : 12.99,
    ...
  }, {
    "id" : 3,
    "name" : "Quattro Formaggi",
    "price" : 11.99,
    ...
  } ],
  "empty" : false,
  "first" : true,
  "last" : false,
  "number" : 0,
  "numberOfElements" : 2,
  "pageable" : {
    "offset" : 0,
    "pageNumber" : 0,
    "pageSize" : 2,
    "paged" : true,
    "sort" : { "empty" : false, "sorted" : true, "unsorted" : false },
    "unpaged" : false
  },
  "size" : 2,
  "sort" : { "empty" : false, "sorted" : true, "unsorted" : false },
  "totalElements" : 6,
  "totalPages" : 3
}
```

**Key fields**:
- `content`: the resources on this page
- `totalElements`: total number of items across all pages
- `totalPages`: total number of pages
- `number`: current page number (0-indexed)
- `size`: requested page size; `numberOfElements`: actual number of items on this page
- `first` / `last`: is this the first / last page?

---

## 📤 File Upload for Images

Spring MVC supports file uploads through `MultipartFile`: the client sends a `multipart/form-data` request (the same encoding a browser uses for `<input type="file">`), and Spring hands you the uploaded file as a parameter.

PizzaStore lets an admin upload a photo for a pizza. Four pieces work together:

```
POST /api/pizzas/1/image  (multipart/form-data, part "image")
        │
        ▼
PizzaController.uploadPizzaImage   → HTTP concerns: 200 / 400 / 404 / 500
        │
        ▼
PizzaService.uploadImage           → find pizza, store file, save imageUrl
        │
        ▼
FileStorageService.storeFile       → validate + write to uploads/pizzas/, return URL
        │
GET /uploads/pizzas/1-20260926160153.png
        ▲
WebConfig                          → serves the uploads/pizzas/ folder as static files
```

### Configuration

In [`application.properties`](pizzastore-complete-api/src/main/resources/application.properties):

```properties
# File Upload Configuration
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB
file.upload-dir=uploads/pizzas
file.base-url=http://localhost:8080
```

The `spring.servlet.multipart.*` properties are Spring Boot's; `file.upload-dir` and `file.base-url` are PizzaStore's own properties, injected with `@Value` (Lesson 3). A file larger than 5 MB is rejected by Spring before it reaches the controller, with `413 Payload Too Large`.

### File Storage Service

[`FileStorageService`](pizzastore-complete-api/src/main/java/be/vives/pizzastore/service/FileStorageService.java) keeps all file-system details out of `PizzaService`:

- its **constructor** resolves `file.upload-dir` to an absolute path and creates the directory if needed;
- **`storeFile(file, pizzaId)`** validates the file, generates a unique name `<pizzaId>-<yyyyMMddHHmmss>.<ext>`, copies the bytes to disk and returns the public URL;
- **`validateFile`** rejects empty files, files over 5 MB and anything that isn't `jpg`, `jpeg` or `png`, by throwing an `IllegalArgumentException`.

The core of `storeFile`:

```java
String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
String filename = pizzaId + "-" + timestamp + "." + extension;

Path targetLocation = this.fileStorageLocation.resolve(filename);
Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

return baseUrl + "/uploads/pizzas/" + filename;
```

Generating the filename on the server — instead of using `file.getOriginalFilename()` — is a security measure: a client-chosen name like `../../application.properties` could otherwise write outside the upload folder (path traversal).

### Controller Endpoint

```java
@PostMapping("/{id}/image")
public ResponseEntity<PizzaResponse> uploadPizzaImage(
        @PathVariable Long id,
        @RequestParam("image") MultipartFile file) {

    log.debug("POST /api/pizzas/{}/image", id);

    try {
        return pizzaService.uploadImage(id, file)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    } catch (IllegalArgumentException e) {
        log.warn("Invalid file upload: {}", e.getMessage());
        return ResponseEntity.badRequest().build();
    } catch (RuntimeException e) {
        log.error("Error uploading file: {}", e.getMessage());
        return ResponseEntity.internalServerError().build();
    }
}
```

`@RequestParam("image")` binds the multipart **part** named `image`. The `try/catch` translates the service's exceptions into status codes: invalid file → `400`, I/O problem → `500`. This is the only place in this lesson's project where a controller catches exceptions itself — Lesson 10 replaces this kind of per-method `try/catch` with a global `@RestControllerAdvice`.

### Service Layer

The one method this lesson adds to [`PizzaService`](pizzastore-complete-api/src/main/java/be/vives/pizzastore/service/PizzaService.java):

```java
public Optional<PizzaResponse> uploadImage(Long id, MultipartFile file) {
    log.debug("Uploading image for pizza with id: {}", id);
    return pizzaRepository.findById(id)
            .map(pizza -> {
                String imageUrl = fileStorageService.storeFile(file, id);
                pizza.setImageUrl(imageUrl);
                Pizza updatedPizza = pizzaRepository.save(pizza);
                return pizzaMapper.toResponse(updatedPizza);
            });
}
```

Only the **URL** goes into the database, not the image bytes. Databases are poor at storing and serving large binary files; file systems (or object storage like Amazon S3 in production) are good at it.

### Serving Static Files

[`WebConfig`](pizzastore-complete-api/src/main/java/be/vives/pizzastore/config/WebConfig.java) implements `WebMvcConfigurer` to map the URL path `/uploads/pizzas/**` onto the upload folder on disk:

```java
@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String uploadPath = Paths.get(uploadDir).toAbsolutePath().toUri().toString();

    registry.addResourceHandler("/uploads/pizzas/**")
            .addResourceLocations(uploadPath);
}
```

Without this, the URL returned by the upload would give a 404: Spring Boot only serves static files from the classpath (`src/main/resources/static`) by default, not from an arbitrary folder.

### Usage Example

```bash
# Upload an image for the pizza with ID 1
curl -X POST http://localhost:8080/api/pizzas/1/image \
  -F "image=@/path/to/pizza.png"
```

**Response** (`200 OK`):
```json
{
  "id" : 1,
  "name" : "Margherita",
  "price" : 8.99,
  "description" : "Classic tomato sauce, fresh mozzarella, basil, and extra virgin olive oil",
  "imageUrl" : "http://localhost:8080/uploads/pizzas/1-20260926160153.png",
  "available" : true,
  "nutritionalInfo" : { ... }
}
```

The image is now available at the returned URL. Uploading a `.txt` file returns `400 Bad Request`; uploading to a pizza that doesn't exist returns `404 Not Found`. The `uploads/` folder is created relative to the directory you start the application from.

---

## 🍕 Complete PizzaStore API

All endpoints of this lesson's project. This is the same set of endpoints as the final PizzaStore, apart from `/api/auth/**`, which is added in Lesson 12.

#### Pizza API — [`PizzaController`](pizzastore-complete-api/src/main/java/be/vives/pizzastore/controller/PizzaController.java)
```
GET    /api/pizzas              - List pizzas (paginated, or filtered by price/name)
GET    /api/pizzas/{id}         - Get a single pizza
POST   /api/pizzas              - Create a pizza                     → 201 + Location
PUT    /api/pizzas/{id}         - Update a pizza
DELETE /api/pizzas/{id}         - Delete a pizza                     → 204
POST   /api/pizzas/{id}/image   - Upload a pizza image (multipart)
```

**Query Parameters for GET /api/pizzas:**
- `minPrice` & `maxPrice`: filter by price range (both required)
- `maxPrice`: filter on price below a maximum
- `name`: search by name (contains, case-insensitive)
- `page`, `size`, `sort`: pagination and sorting (only when no filter is used)

#### Customer API — [`CustomerController`](pizzastore-complete-api/src/main/java/be/vives/pizzastore/controller/CustomerController.java)
```
GET    /api/customers                              - List customers (paginated)
GET    /api/customers/{id}                         - Get a single customer
GET    /api/customers/{id}/orders                  - Get a customer's orders (newest first)
GET    /api/customers/{id}/favorites               - Get a customer's favorite pizzas
POST   /api/customers                              - Create a customer          → 201 + Location
PUT    /api/customers/{id}                         - Update a customer
DELETE /api/customers/{id}                         - Delete a customer          → 204
POST   /api/customers/{customerId}/favorites/{pizzaId} - Add a favorite pizza   → 200
DELETE /api/customers/{customerId}/favorites/{pizzaId} - Remove a favorite pizza → 204
```

#### Order API — [`OrderController`](pizzastore-complete-api/src/main/java/be/vives/pizzastore/controller/OrderController.java)
```
GET    /api/orders              - List orders (paginated, optionally filtered)
GET    /api/orders/{id}         - Get a single order with its order lines
POST   /api/orders              - Create an order                    → 201 + Location
PATCH  /api/orders/{id}/status  - Update the order status
DELETE /api/orders/{id}         - Cancel an order (soft delete)      → 204
```

**Query Parameters for GET /api/orders:**
- `customerId`: filter by customer
- `status`: filter by order status (`PENDING`, `CONFIRMED`, `PREPARING`, `READY`, `DELIVERED`, `CANCELLED`)
- `page`, `size`, `sort`: pagination and sorting

Notice that the favorites endpoints model the relationship itself as a resource: `POST` adds the link between customer and pizza, `DELETE` removes it — neither the customer nor the pizza is created or deleted.

---

## ⚠️ What Happens When Things Go Wrong?

So far every controller handles "not found" by checking an `Optional` or `boolean`. That covers the happy path and the most common error, but not much more. This is what the project in this lesson **actually** returns for a few typical client mistakes (all verified by running it):

| Request | Result now | What we'd want |
|---------|-----------|----------------|
| `GET /api/pizzas/999` | `404`, empty body | `404` with an explanation |
| `GET /api/pizzas/abc` | `400` (type conversion) | ✅ |
| `POST /api/pizzas` with malformed JSON | `400` | ✅ (with details) |
| `PUT /api/pizzas/1` with `{ "price": 12 }` | `500` (`DataIntegrityViolationException`) | `400` — "name is required" |
| `POST /api/orders` with an unknown `customerId` | `500` (`RuntimeException("Customer not found")`) | `404` |
| `DELETE /api/pizzas/1` (pizza is someone's favorite) | `500` (foreign-key violation) | `409 Conflict` |
| Upload a 6 MB image | `413 Payload Too Large` | ✅ |

The error bodies Spring Boot generates for these look like this:

```json
{
  "timestamp" : "2026-09-26T16:02:17.644+02:00",
  "status" : 500,
  "error" : "Internal Server Error",
  "path" : "/api/orders"
}
```

Two problems stand out: **client errors show up as server errors** (500 means "our bug", but these are the client's mistakes), and the body never says **what** went wrong. [Lesson 10](../lesson-10-validation-exception-handling/README.md) fixes both: Bean Validation rejects invalid input with a `400`, the services throw specific exceptions like `ResourceNotFoundException`, and a single `@RestControllerAdvice` turns every exception into an RFC 7807 `ProblemDetail` response.

---

## 💡 Best Practices Summary

### 1. Use Proper HTTP Methods
- ✅ GET for reading (safe, idempotent)
- ✅ POST for creating (not idempotent)
- ✅ PUT for full updates (idempotent)
- ✅ PATCH for partial updates
- ✅ DELETE for removing (idempotent)

### 2. Return Proper Status Codes
- ✅ `200 OK` - Success with response body
- ✅ `201 Created` - Resource created successfully (plus `Location`)
- ✅ `204 No Content` - Success without response body
- ✅ `404 Not Found` - Resource doesn't exist

### 3. Use Location Headers
```java
return ResponseEntity
    .created(location)  // Sets Location header + 201 status
    .body(created);
```

### 4. Keep Controllers Thin
- ✅ One service call per endpoint
- ✅ Controllers only see DTOs — never entities or repositories
- ✅ Business rules (prices, totals, order numbers) live in the service

### 5. Implement Pagination
```java
@GetMapping
public ResponseEntity<Page<CustomerResponse>> getAllCustomers(Pageable pageable) {
    return ResponseEntity.ok(customerService.findAll(pageable));
}
```

### 6. Use Query Parameters for Filtering
```
GET /api/pizzas?maxPrice=10.00
GET /api/orders?status=PENDING
```

### 7. Consistent Resource Naming
- ✅ Use plural nouns: `/api/pizzas`
- ✅ Use sub-resources for relationships: `/api/customers/{id}/favorites`
- ❌ Avoid verbs: `/api/getPizzas`

### 8. Handle Optional Results
```java
return pizzaService.findById(id)
    .map(ResponseEntity::ok)
    .orElse(ResponseEntity.notFound().build());
```

### 9. Never Trust Uploaded File Names
- ✅ Generate the stored file name on the server
- ✅ Validate type and size
- ✅ Store the file on disk / object storage and only the URL in the database

---

## 🎓 Summary

### What We Learned

1. **CRUD Operations**
   - **CREATE**: POST with `201 Created` + `Location` header
   - **READ**: GET with `200 OK`, `404` when not found, sub-resources for relationships
   - **UPDATE**: PUT for full replacement, PATCH for partial updates
   - **DELETE**: hard delete (pizzas, customers) or soft delete (orders → `CANCELLED`)

2. **Query Parameters**
   - Filter resources: `/api/pizzas?maxPrice=10.00`
   - Pagination: `/api/pizzas?page=0&size=10`
   - Sorting: `/api/pizzas?sort=price,asc`

3. **Pagination**
   - `Pageable` is resolved from `page`, `size`, `sort`
   - `Page.map` converts entities to DTOs and keeps the metadata
   - Paging and filtering belong in the database query, not in Java afterwards

4. **File Upload**
   - `MultipartFile` + `spring.servlet.multipart.*` limits
   - A dedicated `FileStorageService` and a resource handler to serve the files

5. **Integration of Previous Lessons**
   - ✅ Spring MVC (controllers, request mapping)
   - ✅ REST principles (resource naming, HTTP methods, status codes)
   - ✅ Service layer, DTOs & mappers (never expose entities)
   - ✅ JPA (relationships, derived queries, pagination)

### Key Takeaways

- ⚠️ **Always use DTOs** - Never expose entities
- ✅ **Location headers** for created resources
- 📄 **Pagination** for large datasets
- 🔍 **Query parameters** for filtering
- ✏️ **PUT vs PATCH** - Full vs partial updates
- 🗑️ **Hard vs Soft delete** - Based on requirements
- 🚧 **Error handling is still minimal** - Lesson 10 fixes that

---

## 📖 Additional Resources

- [Spring Web MVC Documentation](https://docs.spring.io/spring-framework/reference/web/webmvc.html)
- [Spring MVC: Multipart](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/multipart-forms.html)
- [Spring Data Commons: Web Support (Pageable)](https://docs.spring.io/spring-data/commons/reference/repositories/core-extensions.html#core.web)
- [HTTP Status Codes Reference](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status)
- [Roy Fielding's REST Dissertation](https://www.ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm)

**Note on the book**: *Pro Spring Boot 4* covers this lesson's core in Chapter 3's *Implementing Full CRUD* section, whose *The Complete CustomerController* is the book's counterpart of the controllers in this lesson: `ResponseEntity<T>` for full control over the response, `ServletUriComponentsBuilder` to build the `Location` header of a `201 Created`, `@PathVariable` for the resource ID and `@RequestBody` for its state, and `204 No Content` / `404 Not Found` for delete and missing resources — the same patterns PizzaStore uses. The book's controller talks to a repository directly and exchanges its `Customer` record as-is; PizzaStore puts the Lesson 7 service layer and DTOs in between. The book adds `@Valid` in the same listing; this course deliberately postpones validation, `ProblemDetail` and `@RestControllerAdvice` to [Lesson 10](../lesson-10-validation-exception-handling/README.md), which is why this lesson's error handling is still minimal. Pagination appears in Chapter 6's *Pagination and Sorting* section (`PageRequest.of(page, size, Sort.by(...))` in the service layer, as in `getCustomerOrders` above). PATCH, soft deletes, filtering with query parameters, `Pageable` as a controller parameter and file upload with `MultipartFile` are not covered in the book; this lesson adds them because a complete API needs them.

---

## 🚀 Runnable Project

**[`pizzastore-complete-api/`](pizzastore-complete-api)** is Lesson 7's [`pizzastore-with-dtos`](../lesson-07-dtos-mappers/pizzastore-with-dtos) plus the web layer described in [What This Lesson Adds to PizzaStore](#-what-this-lesson-adds-to-pizzastore). It's the next step towards the final PizzaStore: the `controller` package, `FileStorageService` and `WebConfig` are the same as in the final project, except that `@Valid` (Lesson 10) and the OpenAPI annotations (Lesson 13) have not been added yet, and the services still use `Optional`/`boolean` instead of exceptions (Lesson 10).

The project includes:
- ✅ **Spring Boot 4.0** on **Java 25** (`spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, H2, MapStruct 1.6.3)
- ✅ The domain model, repositories, DTOs, mappers, services and seed data from Lessons 6a and 7
- ✅ Complete CRUD for pizzas, customers and orders
- ✅ Pagination, sorting and filtering with query parameters
- ✅ Proper HTTP status codes and `Location` headers
- ✅ File upload for pizza images, served as static files
- ❌ No validation or global exception handling yet — Lesson 10
- ❌ No security — every endpoint is open until Lesson 12

### Running It

```bash
cd pizzastore-complete-api
mvn spring-boot:run
```

Then try it out:

```bash
curl "http://localhost:8080/api/pizzas?size=3&sort=price,asc"
curl http://localhost:8080/api/customers/1/favorites
curl -X PATCH http://localhost:8080/api/orders/1/status \
  -H "Content-Type: application/json" -d '{"status":"PREPARING"}'
```

The H2 console is available at http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:pizzastore_api`, user `sa`, no password).

---

**Congratulations!** 🎉 PizzaStore is now a working REST API. Continue to [Lesson 10: Validation & Exception Handling](../lesson-10-validation-exception-handling/README.md) to make it robust.
