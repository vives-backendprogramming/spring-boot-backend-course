# Lesson 7: DTOs & Mappers

**Data Transfer Objects and Entity-DTO Mapping**

---

## 📋 Learning Objectives

By the end of this lesson, you will be able to:
- Understand why DTOs are essential and why entities should **never** be exposed directly
- Differentiate between Request DTOs and Response DTOs
- Implement DTOs for proper API design using Java Records
- Compare the mapping strategies (Jackson annotations, manual mapping, MapStruct) and know when to use which
- Use MapStruct for automatic, compile-time mapping between entities and DTOs
- Structure your Spring Boot project with proper layering (Service Layer)
- Apply the DTO pattern to the PizzaStore application

---

## 📚 Table of Contents

1. [The Problem: Why Not Expose Entities?](#-the-problem-why-not-expose-entities)
2. [What Are DTOs?](#-what-are-dtos)
3. [Request vs Response DTOs](#-request-vs-response-dtos)
4. [Java Records for DTOs](#-java-records-for-dtos)
5. [Mapping Strategies](#%EF%B8%8F-mapping-strategies)
6. [MapStruct: The Best Choice](#-mapstruct-the-best-choice)
7. [Service Layer Pattern](#-service-layer-pattern)
8. [Project Structure with DTOs](#-project-structure-with-dtos)
9. [Best Practices](#-best-practices)
10. [Common Pitfalls](#%EF%B8%8F-common-pitfalls)
11. [Summary](#-summary)
12. [Runnable Project](#-runnable-project)
13. [Further Reading](#-further-reading)

---

## 🚫 The Problem: Why Not Expose Entities?

In [Lesson 6a](../lesson-06a-spring-data-jpa/README.md) we built PizzaStore's data layer: JPA entities and Spring Data repositories. The tempting next step is to hand those entities straight to a controller.

### What's Wrong with This Code?

```java
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    // ❌ BAD: Returning entity directly
    @GetMapping("/{id}")
    public Customer getCustomer(@PathVariable Long id) {
        return customerRepository.findById(id).orElse(null);
    }

    // ❌ BAD: Accepting entity directly
    @PostMapping
    public Customer createCustomer(@RequestBody Customer customer) {
        return customerRepository.save(customer);
    }
}
```

### Problems with Exposing Entities

#### 1. **Security & Privacy Risks** 🔒

Entities often contain sensitive data that should never be exposed. This is PizzaStore's actual `Customer` entity (from Lesson 6a, simplified):

```java
@Entity
@Table(name = "customers")
public class Customer {
    private Long id;
    private String name;
    private String email;
    private String password;           // ❌ Exposed!
    private String phone;
    private String address;
    private Role role;                 // ❌ Clients could send "role": "ADMIN"!

    // Audit fields - internal information
    private LocalDateTime createdAt;   // ❌ Internal data exposed!
    private LocalDateTime updatedAt;   // ❌ Internal data exposed!

    @OneToMany(mappedBy = "customer")
    private List<Order> orders;        // ❌ Can cause circular references!

    @ManyToMany
    private Set<Pizza> favoritePizzas; // ❌ Pulls in half the database
}
```

When you return this entity, **all fields** are serialized to JSON, including:
- Passwords (even if hashed!)
- Audit fields (when/by whom the record was created or updated)
- Relationships that cause circular references

And when you *accept* it with `@RequestBody Customer`, a client can set any field it likes — including `id`, `role` or `createdAt`. This is called **mass assignment** (or over-posting).

#### 2. **Circular References** ♻️

Bidirectional JPA relationships can cause infinite loops during JSON serialization:

```java
@Entity
public class Order {
    @ManyToOne
    private Customer customer;           // Order → Customer → orders → Order → Customer → ...

    @OneToMany(mappedBy = "order")
    private List<OrderLine> orderLines;  // Order → OrderLine → order → Order → ...
}
```

Jackson follows these references until it gives up: an infinite-recursion / "nesting depth exceeds the maximum allowed" error, and a `500 Internal Server Error` for your client.

#### 3. **Lazy Loading Surprises** 💥

`Order.customer` is `FetchType.LAZY` — Hibernate puts a *proxy* there instead of a real `Customer`. What happens when Jackson touches it depends on a Spring Boot setting:

- **`spring.jpa.open-in-view=false`** (the recommended setting): the transaction has already ended when Jackson serializes the result → `LazyInitializationException: could not initialize proxy - no Session`.
- **`spring.jpa.open-in-view=true`** (Spring Boot's default — note the warning it logs at startup, `spring.jpa.open-in-view is enabled by default...`): the session stays open during serialization, so every lazy association Jackson touches fires *extra SQL queries* while the response is being written, and Hibernate's proxy internals (`hibernateLazyInitializer`) can end up in your JSON or break serialization.

Neither is what you want. With DTOs, *you* decide inside the service's transaction exactly which data is loaded and copied.

#### 4. **Tight Coupling** 🔗

Your API structure becomes tightly coupled to your database structure:

- Change entity field → API breaks
- Add new database column → Clients receive unexpected fields
- Refactor database → Must refactor API simultaneously
- Cannot version API independently

#### 5. **Over-fetching & Under-fetching** 📊

```java
// Client only needs pizza name and price
// But gets EVERYTHING including nutritional info, audit fields, etc.
GET /api/pizzas/1
```

**The Solution?** → **Use DTOs!**

---

## 🎯 What Are DTOs?

**Data Transfer Objects (DTOs)** are simple objects designed specifically for transferring data between layers — here: between your service layer and the outside world (the JSON of your REST API).

### Key Characteristics

| Aspect | Entity | DTO |
|--------|--------|-----|
| **Purpose** | Represent database table | Transfer data over API |
| **Location** | `domain` package | `dto` package |
| **Annotations** | `@Entity`, `@Table`, `@Column` | None (validation added in Lesson 10) |
| **Relationships** | `@OneToMany`, `@ManyToOne` | Flat structure or nested DTOs |
| **Mutability** | Mutable | Immutable (Java record) |
| **Contains** | All table columns | Only data needed for API |

### Benefits of DTOs

✅ **Security**: Only expose what's needed, only accept what's allowed  
✅ **Flexibility**: API independent from database  
✅ **Versioning**: Support multiple API versions  
✅ **Performance**: Fetch only required data  
✅ **Clarity**: Clear API contract  
✅ **Validation**: Different rules for create/update

### DTOs vs. Projections (Lesson 6a)

Lesson 6a's [DTO Projections](../lesson-06a-spring-data-jpa/README.md#-dto-projections) (`PizzaSalesStatistics`) are also records that are not entities — but they are produced *by the query itself*. The DTOs in this lesson work the other way around: the repository returns full entities, and the service layer *maps* them to DTOs in Java afterwards. Projections are for read-only views and aggregates; mapped DTOs are for the regular request/response contract of your API. PizzaStore uses both.

---

## 🔄 Request vs Response DTOs

### Why Separate Request and Response DTOs?

Different operations need different data:

| Operation | Needs | Example |
|-----------|-------|---------|
| **Create** | Data to create entity | Name, description, price |
| **Update** | Data to update entity | Name, description, price (ID comes from the URL) |
| **Response** | Data to return | ID, name, description, price, calculated fields |

### Example: Pizza DTOs

#### Request DTO (Create)

```java
package be.vives.pizzastore.dto.request;

import java.math.BigDecimal;

public record CreatePizzaRequest(
        String name,
        BigDecimal price,
        String description,
        Boolean available,
        NutritionalInfoRequest nutritionalInfo
) {
}
```

**Characteristics:**
- No `id` (generated by database)
- No audit fields (set by JPA Auditing)
- No `imageUrl` (set by a separate upload endpoint in Lesson 9)
- Only data the client can provide

> 💡 In [Lesson 10](../lesson-10-validation-exception-handling/README.md) we add Jakarta Bean Validation constraints (`@NotBlank`, `@NotNull`, `@DecimalMin`, ...) to exactly these request DTOs. That's no coincidence: validation belongs on the DTO that describes *incoming* data, not on the entity.

#### Request DTO (Update)

```java
package be.vives.pizzastore.dto.request;

import java.math.BigDecimal;

public record UpdatePizzaRequest(
        String name,
        BigDecimal price,
        String description,
        Boolean available,
        NutritionalInfoRequest nutritionalInfo
) {
}
```

**Characteristics:**
- Same fields as Create, but a separate type — so create and update can evolve (and be validated) independently
- No `id` (passed in URL)
- Wrapper types (`Boolean`, `BigDecimal`) instead of primitives, so "not sent" (`null`) is distinguishable from `false`/`0` — see [pitfall 6](#6-null-values-in-update-mappings) for what the mapper does with those `null`s

#### Response DTO

```java
package be.vives.pizzastore.dto.response;

import java.math.BigDecimal;

public record PizzaResponse(
        Long id,
        String name,
        BigDecimal price,
        String description,
        String imageUrl,
        Boolean available,
        NutritionalInfoResponse nutritionalInfo
) {
}
```

**Characteristics:**
- Includes `id` (client needs to know it)
- **Never** includes audit fields (internal data)
- No `favoritedByCustomers` — no way to create a cycle
- Read-only representation

---

## 📝 Java Records for DTOs

Since Java 16, **Records** are the perfect choice for DTOs!

### Why Records?

```java
// Old way (verbose)
public class PizzaResponse {
    private final Long id;
    private final String name;
    private final BigDecimal price;

    public PizzaResponse(Long id, String name, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }

    @Override
    public boolean equals(Object o) { /* ... */ }

    @Override
    public int hashCode() { /* ... */ }

    @Override
    public String toString() { /* ... */ }
}

// New way (concise) ✨
public record PizzaResponse(
    Long id,
    String name,
    BigDecimal price
) {}
```

Records automatically generate:
- A canonical constructor
- Accessor methods (`id()`, `name()` — no `get` prefix!)
- `equals()`, `hashCode()`, `toString()`
- Immutability (all fields are `final`)

Records can also be **nested** when a type only makes sense inside another one. PizzaStore does this for the order lines of a new order:

```java
public record CreateOrderRequest(
        Long customerId,
        List<OrderLineRequest> orderLines
) {
    public record OrderLineRequest(
            Long pizzaId,
            Integer quantity
    ) {
    }
}
```

### Records Work Perfectly with Jackson

Spring Boot 4 ships with **Jackson 3** (package `tools.jackson.databind`, central class `JsonMapper`). Just like Jackson 2 before it, it (de)serializes records out of the box: JSON → record via the canonical constructor, record → JSON via the accessors. No annotations, no default constructor, no setters needed.

```java
// Preview of Lesson 9 — the controller only ever sees DTOs
@PostMapping
public ResponseEntity<PizzaResponse> createPizza(@RequestBody CreatePizzaRequest request) {
    // Jackson deserializes JSON → CreatePizzaRequest
    PizzaResponse created = pizzaService.create(request);
    // Jackson serializes PizzaResponse → JSON
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
}
```

---

## 🗺️ Mapping Strategies

### How to Convert Between Entities and DTOs?

#### 1. **Jackson Annotations on the Entity** ⚠️ (not a DTO at all)

The quickest "fix" is to keep returning the entity and hide fields with Jackson annotations:

```java
@Entity
public class Customer {
    @JsonIgnore
    private String password;           // never serialized

    @JsonIgnore
    @OneToMany(mappedBy = "customer")
    private List<Order> orders;        // breaks the cycle
}
```

This works for a tiny demo, but your entity now serves two masters (the database *and* the JSON contract), all the other problems above (mass assignment, lazy loading, tight coupling) remain, and forgetting one annotation on a new field leaks it. Use `@JsonIgnore`/`@JsonProperty` to fine-tune the JSON of a **DTO**, not to turn an entity into one.

#### 2. **Manual Mapping** ✅ (fine for small projects)

You write the conversion yourself — typically as a static factory method on the record, or in a hand-written mapper class:

```java
public record PizzaResponse(Long id, String name, BigDecimal price, String description,
                            String imageUrl, Boolean available,
                            NutritionalInfoResponse nutritionalInfo) {

    public static PizzaResponse from(Pizza pizza) {
        NutritionalInfo info = pizza.getNutritionalInfo();
        return new PizzaResponse(
                pizza.getId(),
                pizza.getName(),
                pizza.getPrice(),
                pizza.getDescription(),
                pizza.getImageUrl(),
                pizza.getAvailable(),
                info == null ? null : new NutritionalInfoResponse(
                        info.getCalories(), info.getProtein(),
                        info.getCarbohydrates(), info.getFat())
        );
    }
}

public record CreatePizzaRequest(String name, BigDecimal price, String description,
                                 Boolean available, NutritionalInfoRequest nutritionalInfo) {

    public Pizza toEntity() {
        Pizza pizza = new Pizza(name, price, description);
        pizza.setAvailable(available);
        // ... and the nutritional info, and ...
        return pizza;
    }
}
```

**Pros:** no extra dependency, completely explicit, easy to debug.
**Cons:** tedious for large models, and the compiler does **not** warn you when you add a field to the entity and forget to map it.

#### 3. **Reflection-based Libraries** (e.g. ModelMapper) ❌

Libraries like ModelMapper copy fields by matching names *at runtime* via reflection. Less code, but mapping errors only show up when the code runs, it's slower, and it's hard to see what actually happens. Not recommended.

#### 4. **MapStruct** ✅ (PizzaStore's choice)

MapStruct is an **annotation processor**: you write an interface, and MapStruct generates the plain-Java implementation (the same code you would write by hand in option 2) at **compile time**.

```java
@Mapper(componentModel = "spring")
public interface PizzaMapper {
    PizzaResponse toResponse(Pizza pizza);
    Pizza toEntity(CreatePizzaRequest request);
}
```

**Benefits:**
- Type-safe: compile-time checking, and **warnings for target fields you forgot to map**
- Fast: generated plain Java code, no reflection at runtime
- Transparent: you can open and read the generated code
- Maintainable: new fields with the same name are mapped automatically

---

## 🎯 MapStruct: The Best Choice

### Setup

Add to `pom.xml` (this is exactly what `pizzastore-with-dtos` and the final PizzaStore use):

```xml
<properties>
    <java.version>25</java.version>
    <org.mapstruct.version>1.6.3</org.mapstruct.version>
</properties>

<dependencies>
    <!-- MapStruct annotations (@Mapper, @Mapping, ...) -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>${org.mapstruct.version}</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <source>25</source>
                <target>25</target>
                <!-- The processor that generates the *MapperImpl classes -->
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>${org.mapstruct.version}</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

MapStruct is not managed by Spring Boot's dependency management, so you specify the version yourself (both entries must use the same version).

### Basic Mapper

```java
package be.vives.pizzastore.mapper;

import be.vives.pizzastore.domain.Pizza;
import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.request.UpdatePizzaRequest;
import be.vives.pizzastore.dto.response.PizzaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PizzaMapper {

    // Entity → Response DTO
    PizzaResponse toResponse(Pizza pizza);

    List<PizzaResponse> toResponseList(List<Pizza> pizzas);

    // Request DTO → new Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "favoritedByCustomers", ignore = true)
    Pizza toEntity(CreatePizzaRequest request);

    // Request DTO → existing Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "favoritedByCustomers", ignore = true)
    void updateEntity(UpdatePizzaRequest request, @MappingTarget Pizza pizza);
}
```

### What MapStruct Generates

Run `mvn compile` and open `target/generated-sources/annotations/be/vives/pizzastore/mapper/PizzaMapperImpl.java`. There's no magic — it's the code you'd otherwise write yourself (abridged):

```java
@Component
public class PizzaMapperImpl implements PizzaMapper {

    @Override
    public PizzaResponse toResponse(Pizza pizza) {
        if ( pizza == null ) {
            return null;
        }
        // ... one local variable per record component ...
        id = pizza.getId();
        name = pizza.getName();
        // ...
        nutritionalInfo = nutritionalInfoToNutritionalInfoResponse( pizza.getNutritionalInfo() );

        return new PizzaResponse( id, name, price, description, imageUrl, available, nutritionalInfo );
    }

    // Nested type → MapStruct generated a helper method for it automatically
    protected NutritionalInfoResponse nutritionalInfoToNutritionalInfoResponse(NutritionalInfo nutritionalInfo) {
        // ...
    }
}
```

Notice that MapStruct uses the record's **canonical constructor** for the response and the entity's **setters** for `toEntity`/`updateEntity`.

### Key Annotations

#### `@Mapper(componentModel = "spring")`

Makes MapStruct put `@Component` on the generated class, so it becomes a Spring bean you can inject:

```java
@Service
public class PizzaService {
    private final PizzaMapper pizzaMapper;

    public PizzaService(PizzaRepository pizzaRepository, PizzaMapper pizzaMapper) {
        // Spring injects the generated PizzaMapperImpl
        this.pizzaMapper = pizzaMapper;
    }
}
```

#### `@Mapping(source = ..., target = ...)`

Maps fields whose names differ, including **nested source properties** (flattening):

```java
@Mapping(source = "customer.id", target = "customerId")
@Mapping(source = "customer.name", target = "customerName")
OrderResponse toResponse(Order order);
```

- `source`: property path in the source object (the entity)
- `target`: property in the target object (the DTO)

Fields with the same name and a compatible type need no `@Mapping` at all. MapStruct also converts common types automatically — e.g. the `Role` enum on `Customer` becomes the `String role` in `CustomerResponse` (`"CUSTOMER"`, `"ADMIN"`).

#### `@Mapping(target = "...", ignore = true)`

Explicitly *don't* fill a target field:

```java
@Mapping(target = "id", ignore = true)        // ID generated by the database
@Mapping(target = "createdAt", ignore = true) // Set by JPA Auditing
Pizza toEntity(CreatePizzaRequest request);
```

If a target field has no matching source and you did *not* ignore it, MapStruct prints a compiler **warning**, e.g.:

```
[WARNING] PizzaMapper.java:[26,11] Unmapped target property: "imageUrl".
```

That's MapStruct's biggest advantage over manual mapping: add a field to `Pizza`, recompile, and you're told about every mapper that doesn't handle it yet. (Tip: read these warnings when you build `pizzastore-with-dtos` — you'll see exactly this one, because `imageUrl` is deliberately not part of `CreatePizzaRequest`.)

#### `@MappingTarget`

Updates an existing object instead of creating a new one:

```java
void updateEntity(UpdatePizzaRequest request, @MappingTarget Pizza pizza);

// Usage in PizzaService:
pizzaRepository.findById(id).map(pizza -> {
    pizzaMapper.updateEntity(request, pizza);   // pizza is now updated in place
    ...
});
```

Why update the managed entity instead of creating a new one? Because it keeps its `id`, its audit fields and its relationships — and inside a `@Transactional` method Hibernate's dirty checking writes the changes to the database.

#### `@BeanMapping(nullValuePropertyMappingStrategy = ...)`

Controls what an update mapping does with `null` values in the source:

```java
@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
void updateEntity(UpdatePizzaRequest request, @MappingTarget Pizza pizza);
```

- `SET_TO_NULL` (**the default**): a `null` in the DTO overwrites the entity's value with `null`
- `IGNORE`: a `null` in the DTO leaves the entity's value untouched → partial updates

See [pitfall 6](#6-null-values-in-update-mappings) for why this matters.

### Nested Objects and Collections

When a DTO contains nested objects or lists, MapStruct looks for a method that can map the element type — first in the same mapper, then in the mappers listed in `@Mapper(uses = ...)` — and generates one itself if it finds none.

```java
@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.name", target = "customerName")
    OrderResponse toResponse(Order order);          // orderLines → uses toOrderLineResponse below

    List<OrderResponse> toResponseList(List<Order> orders);

    @Mapping(source = "pizza.id", target = "pizzaId")
    @Mapping(source = "pizza.name", target = "pizzaName")
    OrderLineResponse toOrderLineResponse(OrderLine orderLine);

    List<OrderLineResponse> toOrderLineResponseList(List<OrderLine> orderLines);
}
```

**How it works:**

```java
// Order entity has a Customer and a list of OrderLines
@Entity
public class Order {
    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> orderLines;
    // ...
}

// OrderResponse DTO flattens the customer and nests the order lines
public record OrderResponse(
        Long id,
        String orderNumber,
        Long customerId,
        String customerName,
        List<OrderLineResponse> orderLines,
        BigDecimal totalAmount,
        OrderStatus status,
        LocalDateTime orderDate
) {}

// MapStruct maps customer.id → customerId, and every OrderLine via toOrderLineResponse()
```

`PizzaMapper` works the same way for `Pizza.nutritionalInfo`: it doesn't declare a mapper for `NutritionalInfo` in `uses`, so MapStruct generates a private helper method for it (that's the `nutritionalInfoToNutritionalInfoResponse` you saw above). PizzaStore also contains a standalone `NutritionalInfoMapper` you can inject wherever you need to map nutritional info on its own.

### Flattening vs. Summary DTOs

Nested *entities* don't have to become nested *DTOs*. There are two common approaches:

```java
// Option A — flattening (what PizzaStore does): copy just the fields the client needs
public record OrderLineResponse(
        Long id,
        Long pizzaId,
        String pizzaName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}

// Option B — a small "summary" DTO for the nested object
public record PizzaSummaryResponse(Long id, String name, BigDecimal price) {}

public record OrderLineResponse(
        Long id,
        PizzaSummaryResponse pizza,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
```

Both avoid circular references and over-fetching; never nest the *full* response DTO of a related object. PizzaStore flattens (`customerId`/`customerName` in `OrderResponse`, `pizzaId`/`pizzaName` in `OrderLineResponse`): the client gets the id to fetch details if it needs them, and the name to display right away.

### Not Everything Goes Through MapStruct

MapStruct is ideal for *copying* data. When building an object requires **business logic** or **database lookups**, write that code in the service instead. `CreateOrderRequest` has no mapper method at all: `OrderService.create()` looks up the `Customer` and each `Pizza` by id, copies the pizza's current price into the order line, and lets `Order.addOrderLine()` recalculate the total:

```java
Customer customer = customerRepository.findById(request.customerId())
        .orElseThrow(() -> new RuntimeException("Customer not found: " + request.customerId()));

Order order = new Order(generateOrderNumber(), customer, OrderStatus.PENDING);

for (CreateOrderRequest.OrderLineRequest lineRequest : request.orderLines()) {
    Pizza pizza = pizzaRepository.findById(lineRequest.pizzaId())
            .orElseThrow(() -> new RuntimeException("Pizza not found: " + lineRequest.pizzaId()));
    order.addOrderLine(new OrderLine(pizza, lineRequest.quantity()));
}

return orderMapper.toResponse(orderRepository.save(order));   // back to MapStruct for the response
```

(The generic `RuntimeException` is temporary — Lesson 10 replaces it with PizzaStore's own exception classes.)

---

## 🏢 Service Layer Pattern

The **Service Layer** sits between Controllers and Repositories. It is the place where DTOs are converted to entities and back.

### Why a Service Layer?

```
┌─────────────────┐
│   Controller    │  ← Handles HTTP (Lesson 9)             ↕ DTOs
└────────┬────────┘
         ↓
┌─────────────────┐
│    Service      │  ← Business logic, DTO mapping, transactions
└────────┬────────┘
         ↓                                                  ↕ Entities
┌─────────────────┐
│   Repository    │  ← Data access (Lesson 6a)
└────────┬────────┘
         ↓
┌─────────────────┐
│    Database     │
└─────────────────┘
```

**Responsibilities:**

| Layer | Responsibility | Example |
|-------|---------------|---------|
| **Controller** | HTTP concerns | Parse request, return status codes |
| **Service** | Business logic | Validate business rules, map DTOs, orchestrate, transactions |
| **Repository** | Data access | CRUD operations, queries |

Entities never leave the service layer; controllers only ever see DTOs.

### Service Layer Implementation

This is `PizzaService` from `pizzastore-with-dtos`:

```java
package be.vives.pizzastore.service;

import be.vives.pizzastore.domain.Pizza;
import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.request.UpdatePizzaRequest;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.mapper.PizzaMapper;
import be.vives.pizzastore.repository.PizzaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PizzaService {

    private static final Logger log = LoggerFactory.getLogger(PizzaService.class);

    private final PizzaRepository pizzaRepository;
    private final PizzaMapper pizzaMapper;

    // Constructor injection (best practice)
    public PizzaService(PizzaRepository pizzaRepository, PizzaMapper pizzaMapper) {
        this.pizzaRepository = pizzaRepository;
        this.pizzaMapper = pizzaMapper;
    }

    // Read operations with pagination
    public Page<PizzaResponse> findAll(Pageable pageable) {
        log.debug("Finding pizzas with pagination: {}", pageable);
        Page<Pizza> pizzaPage = pizzaRepository.findAll(pageable);
        return pizzaPage.map(pizzaMapper::toResponse);
    }

    public Optional<PizzaResponse> findById(Long id) {
        log.debug("Finding pizza with id: {}", id);
        return pizzaRepository.findById(id)
                .map(pizzaMapper::toResponse);
    }

    public List<PizzaResponse> findByPriceLessThan(BigDecimal maxPrice) {
        log.debug("Finding pizzas with price less than: {}", maxPrice);
        List<Pizza> pizzas = pizzaRepository.findByPriceLessThan(maxPrice);
        return pizzaMapper.toResponseList(pizzas);
    }

    public List<PizzaResponse> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        log.debug("Finding pizzas with price between {} and {}", minPrice, maxPrice);
        List<Pizza> pizzas = pizzaRepository.findByPriceBetween(minPrice, maxPrice);
        return pizzaMapper.toResponseList(pizzas);
    }

    public List<PizzaResponse> findByNameContaining(String name) {
        log.debug("Finding pizzas with name containing: {}", name);
        List<Pizza> pizzas = pizzaRepository.findByNameContainingIgnoreCase(name);
        return pizzaMapper.toResponseList(pizzas);
    }

    // Write operations
    public PizzaResponse create(CreatePizzaRequest request) {
        log.debug("Creating new pizza: {}", request.name());
        Pizza pizza = pizzaMapper.toEntity(request);

        // Set bidirectional relationship for NutritionalInfo
        if (pizza.getNutritionalInfo() != null) {
            pizza.getNutritionalInfo().setPizza(pizza);
        }

        Pizza savedPizza = pizzaRepository.save(pizza);
        log.info("Created pizza with id: {}", savedPizza.getId());
        return pizzaMapper.toResponse(savedPizza);
    }

    public Optional<PizzaResponse> update(Long id, UpdatePizzaRequest request) {
        log.debug("Updating pizza with id: {}", id);
        return pizzaRepository.findById(id)
                .map(pizza -> {
                    pizzaMapper.updateEntity(request, pizza);

                    // Set bidirectional relationship for NutritionalInfo
                    if (pizza.getNutritionalInfo() != null) {
                        pizza.getNutritionalInfo().setPizza(pizza);
                    }

                    Pizza updatedPizza = pizzaRepository.save(pizza);
                    log.info("Updated pizza with id: {}", id);
                    return pizzaMapper.toResponse(updatedPizza);
                });
    }

    public boolean delete(Long id) {
        log.debug("Deleting pizza with id: {}", id);
        if (pizzaRepository.existsById(id)) {
            pizzaRepository.deleteById(id);
            log.info("Deleted pizza with id: {}", id);
            return true;
        }
        log.warn("Pizza with id {} not found for deletion", id);
        return false;
    }
}
```

Note the `setPizza(pizza)` after mapping: MapStruct only copies data, it knows nothing about JPA. Keeping both sides of the bidirectional `@OneToOne` in sync (the owning side `NutritionalInfo.pizza` holds the foreign key — see Lesson 6a) is the service's job.

### Key Patterns

#### 1. **Constructor Injection**

```java
private final PizzaRepository pizzaRepository;
private final PizzaMapper pizzaMapper;

public PizzaService(PizzaRepository pizzaRepository, PizzaMapper pizzaMapper) {
    this.pizzaRepository = pizzaRepository;
    this.pizzaMapper = pizzaMapper;
}
```

**Benefits:**
- Immutable dependencies (`final`)
- Easy to test (can pass mocks — Lesson 11)
- Explicit dependencies

#### 2. **Transaction Management**

```java
@Service
@Transactional  // All public methods run in a transaction
public class PizzaService {

    // Read method - lazy associations can be loaded while mapping to DTOs
    public Optional<PizzaResponse> findById(Long id) { ... }

    // Write method - all changes are committed together, or rolled back together
    public PizzaResponse create(CreatePizzaRequest request) { ... }
}
```

Because the mapping to DTOs happens *inside* the transaction, lazy associations (like `Order.customer` or `Customer.favoritePizzas`) can still be loaded. Once the DTO leaves the service, it is plain data — no proxies, no session needed.

#### 3. **Always Return DTOs**

```java
// ❌ NEVER return entities from service
public Pizza findById(Long id) { ... }

// ✅ ALWAYS return DTOs
public Optional<PizzaResponse> findById(Long id) { ... }
```

#### 4. **"Not Found" as `Optional` / `boolean` — for Now**

At this point the services signal "not found" with `Optional.empty()` or `false`, so that the controllers in Lesson 9 can turn that into a `404 Not Found`. In [Lesson 10](../lesson-10-validation-exception-handling/README.md) this evolves into throwing a `ResourceNotFoundException` that a global exception handler converts into a proper error response — that's what the final PizzaStore does.

---

## 📁 Project Structure with DTOs

Everything from Lesson 6a stays exactly as it was; this lesson adds the `dto`, `mapper` and `service` packages:

```
src/main/java/be/vives/pizzastore/
├── config/
│   └── JpaConfig.java           # @EnableJpaAuditing (from Lesson 6a)
│
├── domain/                      # JPA Entities (from Lesson 6a)
│   ├── Pizza.java
│   ├── Order.java
│   ├── OrderLine.java
│   ├── Customer.java
│   ├── NutritionalInfo.java
│   ├── OrderStatus.java         # Enum
│   └── Role.java                # Enum
│
├── repository/                  # Spring Data JPA Repositories (from Lesson 6a)
│   ├── PizzaRepository.java
│   ├── OrderRepository.java
│   ├── CustomerRepository.java
│   └── projection/
│       └── PizzaSalesStatistics.java
│
├── dto/                         # 🆕 Data Transfer Objects
│   ├── request/                 # Request DTOs (incoming)
│   │   ├── CreatePizzaRequest.java
│   │   ├── UpdatePizzaRequest.java
│   │   ├── NutritionalInfoRequest.java
│   │   ├── CreateCustomerRequest.java
│   │   ├── UpdateCustomerRequest.java
│   │   ├── CreateOrderRequest.java      # contains nested record OrderLineRequest
│   │   └── UpdateOrderStatusRequest.java
│   │
│   └── response/                # Response DTOs (outgoing)
│       ├── PizzaResponse.java
│       ├── NutritionalInfoResponse.java
│       ├── CustomerResponse.java
│       ├── OrderResponse.java
│       └── OrderLineResponse.java
│
├── mapper/                      # 🆕 MapStruct Mappers
│   ├── PizzaMapper.java
│   ├── NutritionalInfoMapper.java
│   ├── CustomerMapper.java
│   └── OrderMapper.java
│
├── service/                     # 🆕 Service Layer
│   ├── PizzaService.java
│   ├── CustomerService.java
│   └── OrderService.java
│
└── PizzaStoreApplication.java   # Main application class
```

---

## ✅ Best Practices

### 1. **Naming Conventions**

```java
// Request DTOs
CreatePizzaRequest      // For creating
UpdatePizzaRequest      // For updating
PatchPizzaRequest       // For partial updates (alternative to Update)

// Response DTOs
PizzaResponse           // Full representation
PizzaSummaryResponse    // Minimal representation (for nested objects)
PizzaDetailResponse     // Extra detailed representation (if needed)
```

You'll also see the suffix `DTO` (`PizzaDTO`, `CustomerDetailsDTO`) in other code bases and in the book. PizzaStore prefers `...Request`/`...Response`, which says in which *direction* the data flows. Whatever you pick: be consistent.

### 2. **Package Structure**

```
dto/
├── request/
│   ├── CreatePizzaRequest.java
│   └── UpdatePizzaRequest.java
└── response/
    ├── PizzaResponse.java
    └── NutritionalInfoResponse.java
```

### 3. **Immutability**

```java
// ✅ Use records (immutable by default)
public record PizzaResponse(Long id, String name, BigDecimal price) {}

// ❌ Don't use mutable classes for DTOs
public class PizzaResponse {
    private Long id;
    public void setId(Long id) { this.id = id; }  // Bad!
}
```

### 4. **Validation Belongs on Request DTOs**

```java
// Lesson 10 adds constraints like these to the request DTOs:
public record CreatePizzaRequest(
        @NotBlank(message = "Pizza name is required")
        String name,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be positive")
        BigDecimal price,
        ...
) {}
```

Different DTOs can have different rules (a name is required on create, optional on update) — something you can't express on a single entity.

### 5. **Never Include Audit Fields in Response DTOs**

```java
// ❌ BAD: Exposing internal audit data
public record PizzaResponse(
        Long id,
        String name,
        LocalDateTime createdAt,      // ❌ Internal data
        String createdBy,             // ❌ Internal data
        LocalDateTime updatedAt,      // ❌ Internal data
        String updatedBy              // ❌ Internal data
) {}

// ✅ GOOD: Only business data
public record PizzaResponse(
        Long id,
        String name,
        BigDecimal price
) {}
```

### 6. **Flatten or Summarize Nested Objects**

```java
// ✅ GOOD: Only what the client needs about the customer
public record OrderResponse(
        Long id,
        Long customerId,
        String customerName,
        List<OrderLineResponse> orderLines
) {}

// ❌ BAD: Can cause circular references or over-fetching
public record OrderResponse(
        Long id,
        CustomerResponse customer,    // If CustomerResponse ever gets a list of orders → loop!
        List<OrderLineResponse> orderLines
) {}
```

### 7. **Service Layer Always Returns DTOs**

```java
// ✅ GOOD
@Service
public class PizzaService {
    public Optional<PizzaResponse> findById(Long id) {
        return repository.findById(id)
                .map(mapper::toResponse);
    }
}

// ❌ BAD
@Service
public class PizzaService {
    public Pizza findById(Long id) {
        return repository.findById(id).orElseThrow();  // Never return entity!
    }
}
```

### 8. **Use Constructor Injection**

```java
// ✅ GOOD
@Service
public class PizzaService {
    private final PizzaRepository repository;
    private final PizzaMapper mapper;

    public PizzaService(PizzaRepository repository, PizzaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }
}

// ❌ AVOID field injection
@Service
public class PizzaService {
    @Autowired
    private PizzaRepository repository;

    @Autowired
    private PizzaMapper mapper;
}
```

---

## ⚠️ Common Pitfalls

### 1. **Returning Entities from Controllers/Services**

```java
// ❌ NEVER DO THIS
@GetMapping("/{id}")
public Pizza getPizza(@PathVariable Long id) {
    return pizzaRepository.findById(id).orElseThrow();
}

// ✅ ALWAYS RETURN DTOs
@GetMapping("/{id}")
public ResponseEntity<PizzaResponse> getPizza(@PathVariable Long id) {
    return pizzaService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
}
```

### 2. **Forgetting `@Transactional` on Write Operations**

```java
// ❌ BAD: No transaction
@Service
public class PizzaService {
    public Optional<PizzaResponse> update(Long id, UpdatePizzaRequest request) {
        Pizza pizza = repository.findById(id).orElseThrow();
        mapper.updateEntity(request, pizza);
        return Optional.of(mapper.toResponse(pizza));  // Changes are never saved!
    }
}

// ✅ GOOD: Explicit transaction
@Service
@Transactional
public class PizzaService {
    public Optional<PizzaResponse> update(Long id, UpdatePizzaRequest request) {
        return repository.findById(id)
                .map(pizza -> {
                    mapper.updateEntity(request, pizza);
                    // Changes automatically saved when transaction commits (dirty checking)
                    return mapper.toResponse(pizza);
                });
    }
}
```

### 3. **Not Ignoring Fields in Mappers**

```java
// ❌ BAD: Doesn't ignore generated fields
@Mapper(componentModel = "spring")
public interface PizzaMapper {
    Pizza toEntity(CreatePizzaRequest request);
    // MapStruct warns: Unmapped target properties: "id, createdAt, createdBy, ..."
}

// ✅ GOOD: Explicitly ignore generated fields
@Mapper(componentModel = "spring")
public interface PizzaMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "favoritedByCustomers", ignore = true)
    Pizza toEntity(CreatePizzaRequest request);
}
```

Don't silence the warnings globally (`unmappedTargetPolicy = ReportingPolicy.IGNORE`) — they are your safety net. Ignoring a field *explicitly* documents that it's intentional.

### 4. **Exposing Passwords**

```java
// ❌ DANGER: Password exposed in response
public record CustomerResponse(
        Long id,
        String name,
        String email,
        String password  // ❌❌❌
) {}

// ✅ SAFE: Password never included
public record CustomerResponse(
        Long id,
        String name,
        String email,
        String phone,
        String address,
        String role
) {}
```

The password *is* part of `CreateCustomerRequest` (you need one to register), but it only ever flows **in**. PizzaStore's `CustomerMapper` also guards the other direction on updates:

```java
@Mapper(componentModel = "spring")
public interface CustomerMapper {
    // ...
    @Mapping(target = "email", ignore = true)     // email can't be changed via a profile update
    @Mapping(target = "password", ignore = true)  // password never changed via a profile update
    @Mapping(target = "role", ignore = true)      // clients can never promote themselves to ADMIN
    // ... (id, orders, favoritePizzas, audit fields also ignored)
    void updateEntity(UpdateCustomerRequest request, @MappingTarget Customer customer);
}
```

(`UpdateCustomerRequest` doesn't even *have* those fields — the `ignore`s document the intent and keep MapStruct from warning.) Hashing the password before it's stored is part of the security lesson ([Lesson 12](../lesson-12-jwt-authentication/README.md)).

### 5. **Circular References in DTOs**

```java
// ❌ BAD: Circular reference
public record CustomerResponse(
        Long id,
        String name,
        List<OrderResponse> orders
) {}

public record OrderResponse(
        Long id,
        CustomerResponse customer,    // ← Circular! Customer → Order → Customer → ...
        List<OrderLineResponse> orderLines
) {}

// ✅ GOOD: Flatten customer data or use IDs
public record OrderResponse(
        Long id,
        String orderNumber,
        Long customerId,             // ← Just the ID
        String customerName,         // ← Just the name
        List<OrderLineResponse> orderLines,
        BigDecimal totalAmount,
        OrderStatus status,
        LocalDateTime orderDate
) {}
```

### 6. **`null` Values in Update Mappings**

MapStruct's default for `@MappingTarget` methods is `NullValuePropertyMappingStrategy.SET_TO_NULL`: **every** `null` in the request overwrites the corresponding entity field. You can see it in the generated `PizzaMapperImpl`:

```java
@Override
public void updateEntity(UpdatePizzaRequest request, Pizza pizza) {
    ...
    pizza.setName( request.name() );             // no null check!
    pizza.setPrice( request.price() );
    pizza.setDescription( request.description() );
    ...
    pizza.setAvailable( request.available() );
}
```

So with PizzaStore's `PizzaMapper`, an update is a **full replacement**: the client must send *all* fields. That matches the semantics of HTTP `PUT` (Lesson 8/9), but it means a request that only contains a new price:

```json
{ "price": 10.00 }
```

sets `name` and `available` to `null` — and since those are `NOT NULL` columns, the save fails with a `DataIntegrityViolationException`.

If you want **partial updates** (typically for HTTP `PATCH`), tell MapStruct to skip `null`s:

```java
@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
void updateEntity(UpdatePizzaRequest request, @MappingTarget Pizza pizza);

// Request: { "price": 10.00 }
// Result: only the price changes, every other field keeps its existing value
```

The trade-off: with `IGNORE`, a client can no longer *deliberately* clear a field by sending `null`. Choose the strategy that matches the HTTP semantics of your endpoint, and in Lesson 10 back it up with validation (e.g. `@NotBlank` on fields that a full update must contain).

---

## 📝 Summary

### Key Takeaways

1. **Never expose entities directly** in your API
   - Security risks (passwords, audit fields, mass assignment)
   - Circular references
   - Tight coupling
   - Lazy loading surprises

2. **Use DTOs** for data transfer
   - Request DTOs for incoming data
   - Response DTOs for outgoing data
   - Flatten (or summarize) nested objects

3. **Use Java Records** for DTOs
   - Concise syntax
   - Immutable by default
   - (De)serialized out of the box by Jackson 3

4. **Use MapStruct** for mapping
   - Type-safe, with warnings for unmapped fields
   - Compile-time generation — readable plain-Java code in `target/generated-sources`
   - No runtime reflection
   - Know its update default (`SET_TO_NULL`)
   - Keep business logic (lookups, calculations) in the service

5. **Implement a Service Layer**
   - Business logic
   - DTO ↔ Entity mapping
   - Transaction management
   - Sits between Controller and Repository; entities never leave it

6. **Best Practices**
   - Constructor injection
   - `@Transactional` services
   - Never include audit fields in responses
   - Never include passwords in responses
   - Validation on request DTOs (Lesson 10)

### Architecture

```
┌──────────────────────────────────────┐
│         Controller Layer             │
│  - HTTP concerns                     │
│  - Request validation                │
│  - Response status codes             │
└──────────────┬───────────────────────┘
               │ DTOs
               ↓
┌──────────────────────────────────────┐
│          Service Layer               │
│  - Business logic                    │
│  - DTO ↔ Entity mapping              │
│  - Transaction management            │
└──────────────┬───────────────────────┘
               │ Entities
               ↓
┌──────────────────────────────────────┐
│        Repository Layer              │
│  - Data access                       │
│  - JPA queries                       │
└──────────────┬───────────────────────┘
               │
               ↓
┌──────────────────────────────────────┐
│            Database                  │
└──────────────────────────────────────┘
```

### What's Next?

In **[Lesson 8 (REST Principles)](../lesson-08-rest-principles/README.md)** we look at how a good REST API is designed:
- Resource-based URLs, HTTP methods and status codes
- REST best practices and API versioning

In **[Lesson 9 (Complete REST API)](../lesson-09-complete-rest-api/README.md)** we build on this lesson's project:
- Controllers that expose the services from this lesson as REST endpoints
- Full CRUD, pagination and filtering
- File upload for pizza images

In **[Lesson 10 (Validation & Exception Handling)](../lesson-10-validation-exception-handling/README.md)** we:
- Add Bean Validation to the request DTOs
- Replace the `Optional`/`boolean`/`RuntimeException` "not found" handling with proper exceptions and a global exception handler

---

## 🚀 Runnable Project

**`pizzastore-with-dtos/`** is Lesson 6a's `pizzastore-jpa` project plus the `dto`, `mapper` and `service` packages of this lesson. It's a step on the way to the final PizzaStore: the `domain`, `repository`, `dto` and `mapper` packages are the same as in the final project — except that validation (Lesson 10) and OpenAPI annotations (Lesson 13) have not been added to the DTOs yet.

The project includes:
- ✅ **Spring Boot 4** on **Java 25** (`spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, H2)
- ✅ The complete domain model, repositories and seed data from Lesson 6a
- ✅ **Request DTOs**: `CreatePizzaRequest`, `UpdatePizzaRequest`, `NutritionalInfoRequest`, `CreateCustomerRequest`, `UpdateCustomerRequest`, `CreateOrderRequest` (with nested `OrderLineRequest`), `UpdateOrderStatusRequest`
- ✅ **Response DTOs**: `PizzaResponse`, `NutritionalInfoResponse`, `CustomerResponse`, `OrderResponse`, `OrderLineResponse`
- ✅ **MapStruct 1.6.3 Mappers**: `PizzaMapper`, `NutritionalInfoMapper`, `CustomerMapper`, `OrderMapper`
- ✅ **Service Layer**: `PizzaService`, `CustomerService`, `OrderService` with `@Transactional`
- ✅ **No audit fields and no passwords** are ever exposed in responses
- ❌ No controllers yet — those follow in Lesson 9

---

## 🎓 Further Reading

- [MapStruct Reference Guide](https://mapstruct.org/documentation/stable/reference/html/)
- [Java Records (Java 25)](https://docs.oracle.com/en/java/javase/25/language/records.html)
- [Spring Framework: Transaction Management](https://docs.spring.io/spring-framework/reference/data-access/transaction.html)
- [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/)

**Note on the book**: *Pro Spring Boot 4* has no dedicated chapter on DTOs or mapping, and doesn't mention MapStruct at all. The idea shows up in a few places instead: Chapter 2's JSON section shows `@JsonProperty`/`@JsonIgnore` to shape the JSON of a type (e.g. to hide a password) — the approach [Mapping Strategies](#%EF%B8%8F-mapping-strategies) warns against for entities; Chapter 3's validation best practices advise to "prefer applying validation on Data Transfer Objects (DTOs)" rather than on domain entities (Lesson 10 does exactly that); and the book's Management CRM (Chapter 1, and again in Chapters 5–8) uses a `CustomerDetailsDTO` record that its service layer assembles by hand from several domain objects — manual mapping, as in [option 2](#2-manual-mapping--fine-for-small-projects). This lesson goes beyond the book by separating request and response DTOs and by generating the mapping code with MapStruct, which PizzaStore uses because it scales better to a larger domain model and catches forgotten fields at compile time.

---

**Great work!** 🎉 PizzaStore now has a clean boundary between its database model and the outside world. Continue to [Lesson 8: REST Principles](../lesson-08-rest-principles/README.md).
