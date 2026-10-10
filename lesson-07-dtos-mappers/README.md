# Lesson 7: DTOs, Mappers & the Service Layer

**Data Transfer Objects, Entity-DTO Mapping, the Service Layer and Transactions**

---

## 📋 Learning Objectives

By the end of this lesson, you will be able to:
- Understand why DTOs are essential and why entities should **never** be exposed directly
- Differentiate between Request DTOs and Response DTOs
- Implement DTOs for proper API design using Java Records
- Compare the mapping strategies (Jackson annotations, manual mapping, MapStruct) and know when to use which
- Use MapStruct for automatic, compile-time mapping between entities and DTOs
- Structure your Spring Boot project with proper layering and explain the responsibilities of the service layer
- Explain what a transaction is (ACID) and why the service method is the natural transaction boundary
- Use `@Transactional` and explain how it works (proxy, commit/rollback, dirty checking, rollback rules, `readOnly`, propagation, isolation)
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
8. [Transactions](#-transactions)
9. [Project Structure with DTOs](#-project-structure-with-dtos)
10. [Best Practices](#-best-practices)
11. [Common Pitfalls](#%EF%B8%8F-common-pitfalls)
12. [Summary](#-summary)
13. [Runnable Project](#-runnable-project)
14. [Further Reading](#-further-reading)

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

[`CreatePizzaRequest.java`](pizzastore-with-dtos/src/main/java/be/vives/pizzastore/dto/request/CreatePizzaRequest.java):

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

[`UpdatePizzaRequest.java`](pizzastore-with-dtos/src/main/java/be/vives/pizzastore/dto/request/UpdatePizzaRequest.java):

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

[`PizzaResponse.java`](pizzastore-with-dtos/src/main/java/be/vives/pizzastore/dto/response/PizzaResponse.java):

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

It's an interface with four methods — MapStruct writes the implementation:

| Method | Direction | Purpose |
|--------|-----------|---------|
| `PizzaResponse toResponse(Pizza pizza)` | Entity → Response DTO | Every read operation |
| `List<PizzaResponse> toResponseList(List<Pizza> pizzas)` | Entity list → DTO list | Search results |
| `Pizza toEntity(CreatePizzaRequest request)` | Request DTO → **new** entity | Create |
| `void updateEntity(UpdatePizzaRequest request, @MappingTarget Pizza pizza)` | Request DTO → **existing** entity | Update |

The two "Request DTO → entity" methods carry a list of `@Mapping(target = "...", ignore = true)` annotations for the fields a client may never set (`id`, the audit fields, `favoritedByCustomers`). Open the file next to this section; the annotations it uses are explained [below](#key-annotations).

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

PizzaStore is built from the bottom up. [Lesson 6a](../lesson-06a-spring-data-jpa/README.md) laid the foundation: the domain model and the repositories that store it. This lesson adds the next layer on top of that: the **service layer**, which implements the application's use cases and is also the place where DTOs are converted to entities and back. The web layer comes last: in [Lesson 9](../lesson-09-complete-rest-api/README.md), controllers expose these services as a REST API.

### What Is a Service?

A service is a Spring bean, annotated with `@Service`, that implements **one use case of your application per public method**: "create a pizza", "place an order", "add a pizza to a customer's favorites". As the book puts it, the service layer "acts as an orchestrator": a single business request often needs several repositories, a few business rules and some mapping — the service puts those together.

```java
@Service
@Transactional
public class OrderService { ... }
```

`@Service` is one of the stereotype annotations from [Lesson 2](../lesson-02-spring-di-ioc/README.md#-spring-stereotype-annotations). Technically it's just a `@Component`, so component scanning picks it up and it can be injected everywhere — but the name tells every reader (and tools) that this class holds business logic.

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

| Layer | Responsibility | Knows about | Must *not* know about |
|-------|----------------|-------------|------------------------|
| **Controller** | HTTP concerns: URLs, request parsing, status codes, headers | Services, DTOs | Repositories, entities, SQL |
| **Service** | Business logic: use cases, business rules, orchestration, DTO ↔ entity mapping, transactions | Repositories, mappers, entities, DTOs | HTTP (`ResponseEntity`, status codes, `HttpServletRequest`) |
| **Repository** | Data access: CRUD, queries | Entities | Business rules, DTOs, HTTP |

Each layer only talks to the layer directly below it. Entities never leave the service layer; controllers only ever see DTOs.

Why go to this trouble instead of calling repositories directly from the controller?

1. **One place for business logic.** "An order must contain existing pizzas", "a delivered order can no longer be cancelled" (Lesson 10) — these rules belong to the application, not to one HTTP endpoint. In a controller they would get copied every time a second endpoint needs them.
2. **Reuse beyond HTTP.** The same `OrderService.create()` can be called by a REST controller, a scheduled job, a message listener or a test. None of those care about HTTP.
3. **A clear transaction boundary.** One service method = one use case = one transaction (see [Transactions](#-transactions)).
4. **A clear mapping boundary.** Entities go in and out of the repositories, DTOs go in and out of the services. The controller never sees a lazy-loading proxy.
5. **Testability.** A service has no HTTP dependency: you can unit-test it with mocked repositories (Lesson 12).

### Business Logic: Service or Entity?

Not all logic belongs in the service. A good rule of thumb:

- Logic that is about **one object and its own data** belongs in the **entity**. `Order.addOrderLine()` adds the line, sets the back-reference and recalculates the total, and `Customer.addFavoritePizza()` keeps both sides of the many-to-many in sync. These rules must hold *no matter who* changes the order.
- Logic that **coordinates** several objects, repositories or other services belongs in the **service**. `OrderService.create()` looks up a customer and several pizzas, builds an order from them and saves it.

```java
// CustomerService: orchestration (look up two aggregates, save)
public boolean addFavoritePizza(Long customerId, Long pizzaId) {
    Optional<Customer> customerOpt = customerRepository.findById(customerId);
    Optional<Pizza> pizzaOpt = pizzaRepository.findById(pizzaId);

    if (customerOpt.isPresent() && pizzaOpt.isPresent()) {
        Customer customer = customerOpt.get();
        Pizza pizza = pizzaOpt.get();
        customer.addFavoritePizza(pizza);   // ← domain logic lives in the entity
        customerRepository.save(customer);
        return true;
    }
    return false;
}
```

### PizzaStore's Services

| Service | Uses | Use cases |
|---------|------|-----------|
| `PizzaService` | `PizzaRepository`, `PizzaMapper` | list (paginated), find by id / price / name, create, update, delete |
| `CustomerService` | `CustomerRepository`, `PizzaRepository`, `CustomerMapper`, `PizzaMapper` | list, find, create, update, delete, list/add/remove favorite pizzas |
| `OrderService` | `OrderRepository`, `CustomerRepository`, `PizzaRepository`, `OrderMapper` | list, find by id / customer / status, place an order, change status, cancel |

A service may use *several* repositories (`OrderService` needs customers and pizzas to build an order). Services can also call other services, but avoid circular dependencies between them — if two services need each other, some logic is probably in the wrong place.

> 💡 **Interface + implementation?** You'll often see a `PizzaService` interface with a `PizzaServiceImpl` class. With only one implementation that adds little: Spring can proxy classes directly and Mockito can mock them. PizzaStore uses plain classes; introduce an interface when you really have more than one implementation.

### Service Layer Implementation

Open [`service/PizzaService.java`](pizzastore-with-dtos/src/main/java/be/vives/pizzastore/service/PizzaService.java) and read it alongside this section. Its structure is typical for every PizzaStore service:

- `@Service` + `@Transactional` on the class
- `final` fields for the repository and the mapper, filled through the constructor
- **read methods** (`findAll(Pageable)`, `findById`, `findByPriceLessThan`, `findByPriceBetween`, `findByNameContaining`): call the repository, map the entities to `PizzaResponse`s
- **write methods** (`create`, `update`, `delete`): map the request to an entity (or look up the existing one), save it, map the result back
- logging with SLF4J (Lesson 3): `debug` on entry, `info` after a successful change

A typical write method, `create()`:

```java
public PizzaResponse create(CreatePizzaRequest request) {
    log.debug("Creating new pizza: {}", request.name());
    Pizza pizza = pizzaMapper.toEntity(request);                 // DTO → entity

    // Set bidirectional relationship for NutritionalInfo
    if (pizza.getNutritionalInfo() != null) {
        pizza.getNutritionalInfo().setPizza(pizza);
    }

    Pizza savedPizza = pizzaRepository.save(pizza);             // persist
    log.info("Created pizza with id: {}", savedPizza.getId());
    return pizzaMapper.toResponse(savedPizza);                   // entity → DTO
}
```

[`CustomerService.java`](pizzastore-with-dtos/src/main/java/be/vives/pizzastore/service/CustomerService.java) and [`OrderService.java`](pizzastore-with-dtos/src/main/java/be/vives/pizzastore/service/OrderService.java) follow the same pattern.

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
- Easy to test (can pass mocks — Lesson 12)
- Explicit dependencies

#### 2. **Always Return DTOs**

```java
// ❌ NEVER return entities from service
public Pizza findById(Long id) { ... }

// ✅ ALWAYS return DTOs
public Optional<PizzaResponse> findById(Long id) { ... }
```

#### 3. **Map Inside the Transaction**

The service maps entities to DTOs *before* the method returns — i.e. while the transaction is still open (next section). Lazy associations like `Order.customer` or `Customer.favoritePizzas` can still be loaded at that moment. Once the DTO leaves the service, it is plain data: no proxies, no session needed.

#### 4. **"Not Found" as `Optional` / `boolean` — for Now**

At this point the services signal "not found" with `Optional.empty()` or `false`, so that the controllers in Lesson 9 can turn that into a `404 Not Found`. In [Lesson 10](../lesson-10-validation-exception-handling/README.md) this evolves into throwing a `ResourceNotFoundException` that a global exception handler converts into a proper error response — that's what the final PizzaStore does.

---

## 🔁 Transactions

Every PizzaStore service is annotated with `@Transactional`. This section explains what that means and why it belongs on the service layer.

### What Is a Transaction?

A **transaction** groups several database operations into one unit of work that either **completely succeeds** (commit) or **completely fails** (rollback). Relational databases guarantee the **ACID** properties for a transaction:

| Property | Meaning | PizzaStore example |
|----------|---------|--------------------|
| **Atomicity** | All or nothing | An order is saved *with* all its order lines, or not at all |
| **Consistency** | The database goes from one valid state to another; constraints hold | No order line without an existing order and pizza |
| **Isolation** | Concurrent transactions don't see each other's half-finished work | Nobody reads an order whose lines are only half inserted |
| **Durability** | Once committed, the data survives a crash | A confirmed order doesn't disappear after a restart |

### The Problem: Atomicity

A use case often needs **more than one** database operation. Suppose a (hypothetical) "place order and remember the pizzas as favorites" use case:

```java
public OrderResponse createAndRememberFavorites(CreateOrderRequest request) {
    Order order = ...;                        // build the order
    orderRepository.save(order);              // ① INSERT order + order lines
    customer.addFavoritePizza(pizza);
    customerRepository.save(customer);        // ② INSERT into customer_favorite_pizzas
    // What if ② fails?
}
```

If ① succeeds and ② throws an exception, you're left with half a use case in the database. Every Spring Data repository method is already transactional *on its own* (`SimpleJpaRepository` is annotated with `@Transactional`), so without a transaction around the whole method, ① has already been committed when ② fails. The **use case** has to be the unit of work — and use cases live in the service layer.

### Declarative Transactions with `@Transactional`

Spring's solution is **declarative transaction management**: you *declare* that a method is transactional with an annotation, and Spring takes care of the rest.

```java
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional            // on the class: applies to every public method
public class OrderService {

    public OrderResponse create(CreateOrderRequest request) {
        // everything in here runs in ONE transaction
    }
}
```

**How it works:**

1. Spring **opens a transaction** before the method starts.
2. The method runs. Every repository call inside it **joins** that same transaction.
3. If the method completes normally, Spring **commits** the transaction.
4. If the method throws a `RuntimeException` (or an `Error`), Spring **rolls back** the transaction.

### Behind the Scenes: a Proxy

How does an annotation open and close a transaction? At startup, Spring doesn't inject your `OrderService` itself, but a **proxy** — a generated subclass that wraps every call in transaction logic. This is *aspect-oriented programming* (AOP): transaction management is a "cross-cutting concern" that Spring adds around your business logic without you writing it.

```
 OrderController                OrderService proxy                    OrderService
      │  create(request)               │                                   │
      │ ─────────────────────────────► │  begin transaction                │
      │                                │ ────────────────────────────────► │  create(request)
      │                                │                                   │  repositories join
      │                                │ ◄──────────────────────────────── │  the transaction
      │                                │  commit (or rollback on exception)│
      │ ◄───────────────────────────── │                                   │
```

You can see the proxy by printing the class of an injected service:

```java
System.out.println(pizzaService.getClass().getName());
// be.vives.pizzastore.service.PizzaService$$SpringCGLIB$$0
```

### See It Happen: Transaction Logging

Add this line to `application.properties` of `pizzastore-with-dtos` to watch Spring's transaction manager at work:

```properties
logging.level.org.springframework.orm.jpa.JpaTransactionManager=DEBUG
```

This is the (abridged) output of `customerService.addFavoritePizza(2L, 1L)`:

```
Creating new transaction with name [be.vives.pizzastore.service.CustomerService.addFavoritePizza]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
Opened new EntityManager [SessionImpl(461445108<open>)] for JPA transaction
Participating in existing transaction          ← customerRepository.findById(2)
Participating in existing transaction          ← pizzaRepository.findById(1)
Participating in existing transaction          ← customerRepository.save(customer)
Added pizza 1 to customer 2 favorites
Initiating transaction commit
Committing JPA transaction on EntityManager [SessionImpl(461445108<open>)]
```

One transaction, three repository calls that join it, one commit. And this is `orderService.create(...)` for an order whose second line refers to a pizza that doesn't exist (id `999`):

```
Creating new transaction with name [be.vives.pizzastore.service.OrderService.create]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
Participating in existing transaction          ← customer, count() for the order number, pizza 1, pizza 999
Initiating transaction rollback
Rolling back JPA transaction on EntityManager [SessionImpl(1695983081<open>)]
```

The `RuntimeException("Pizza not found: 999")` triggers a rollback; the number of orders in the database stays exactly the same.

### Where to Put `@Transactional`

- **On the service layer** — on the class, or on individual public methods. The service method is the use case, so it's the natural transaction boundary.
- **Not on controllers**: HTTP handling doesn't belong in a transaction, and a controller would have to know which operations belong together.
- **Not needed on repositories**: Spring Data's repository methods are already transactional; when they're called from a transactional service method, they simply join its transaction (`Participating in existing transaction`).
- A `@Transactional` on a **method** overrides the one on the **class**.

⚠️ Use **Spring's** annotation, `org.springframework.transaction.annotation.Transactional`. There's also a `jakarta.transaction.Transactional` which Spring understands too, but it lacks options like `readOnly`.

### Dirty Checking: Changes Are Saved Automatically

Inside a transaction, every entity you load is **managed** by JPA's *persistence context* (Lesson 6a). At commit, Hibernate compares each managed entity with its original state and automatically executes an `UPDATE` for everything that changed — this is called **dirty checking**.

```java
@Transactional
public Optional<PizzaResponse> update(Long id, UpdatePizzaRequest request) {
    return pizzaRepository.findById(id)            // pizza is now managed
            .map(pizza -> {
                pizzaMapper.updateEntity(request, pizza);   // just change the Java object...
                return pizzaMapper.toResponse(pizza);       // ...Hibernate UPDATEs it at commit
            });
}
```

So `pizzaRepository.save(pizza)` in PizzaStore's `update()` isn't strictly needed for a managed entity. PizzaStore still calls it because it makes the intent explicit — and it's harmless. Without a transaction, however, the entity is no longer managed after `findById()` returns, and your changes are silently lost ([pitfall 2](#2-forgetting-transactional-on-write-operations)).

### Rollback Rules

By default Spring rolls back on **unchecked** exceptions only:

| Thrown by the method | Default behavior |
|----------------------|------------------|
| `RuntimeException` (and subclasses) | **Rollback** |
| `Error` | **Rollback** |
| Checked `Exception` (e.g. `IOException`) | **Commit!** |
| An exception you catch yourself inside the method | Nothing — Spring never sees it → **Commit** |

That's one reason why PizzaStore's own exceptions (Lesson 10) extend `RuntimeException`. If a checked exception must cause a rollback, say so explicitly:

```java
@Transactional(rollbackFor = IOException.class)
public void importPizzas(Path file) throws IOException { ... }
```

### Read-Only Transactions

```java
@Transactional(readOnly = true)
public Optional<PizzaResponse> findById(Long id) { ... }
```

`readOnly = true` is a hint that the method doesn't change data. Hibernate then skips dirty checking and flushing for that transaction (less work, less memory), and the JDBC driver/database can optimize too. Spring Data uses it itself: in the log you'll see `SimpleJpaRepository.count ... readOnly`. The book applies the same idea to its read methods (`@Transactional(readOnly = true)` on `getCustomerDetails` in Chapter 8).

A common pattern is read-only as the class default, overridden by the write methods:

```java
@Service
@Transactional(readOnly = true)             // default for all methods: read-only
public class PizzaService {

    public Optional<PizzaResponse> findById(Long id) { ... }

    @Transactional                          // write methods: read-write
    public PizzaResponse create(CreatePizzaRequest request) { ... }
}
```

PizzaStore keeps it simpler with one read-write `@Transactional` on each service class, which is perfectly correct — `readOnly` is an optimization, not a requirement.

### Propagation: Calling a Transactional Method from Another One

What happens when a transactional method calls another transactional method (in another bean)? That's decided by the **propagation**:

| Propagation | Behavior |
|-------------|----------|
| `REQUIRED` (**default**) | Join the existing transaction; start a new one if there is none |
| `REQUIRES_NEW` | Always start a new, independent transaction (the outer one is suspended), e.g. for an audit log entry that must be saved even if the outer transaction rolls back |

That's what the log line `Participating in existing transaction` means: the repository methods have the default `REQUIRED` propagation and join the service's transaction. In PizzaStore the default is all you need.

### Transaction Pitfalls

1. **Self-invocation.** A call from one method to another method *in the same class* doesn't go through the proxy, so the `@Transactional` settings of the called method are ignored:

   ```java
   @Service
   public class OrderService {
       public void importOrders(List<CreateOrderRequest> requests) {
           requests.forEach(this::create);      // ❌ this.create() bypasses the proxy
       }

       @Transactional(propagation = Propagation.REQUIRES_NEW)
       public OrderResponse create(CreateOrderRequest request) { ... }   // REQUIRES_NEW is ignored here
   }
   ```

   Move the method to another bean if it really needs its own transaction settings.
2. **Private methods.** `@Transactional` on a `private` method has no effect — the proxy can't override it.
3. **Catching the exception yourself.** If you `catch` an exception inside the transactional method and don't rethrow it, Spring doesn't know anything went wrong and commits.
4. **Checked exceptions** don't trigger a rollback by default (see [Rollback Rules](#rollback-rules)).
5. **Long transactions.** A transaction holds a database connection (and possibly locks) until it ends. Don't call slow external systems (HTTP calls, sending e-mails) inside one.

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

Without a transaction, `findById()` runs in its own short transaction and returns an entity that is no longer managed once it returns — nothing tracks your changes anymore. See [Dirty Checking](#dirty-checking-changes-are-saved-automatically) and the other [transaction pitfalls](#transaction-pitfalls).

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

(`UpdateCustomerRequest` doesn't even *have* those fields — the `ignore`s document the intent and keep MapStruct from warning.) Hashing the password before it's stored is part of the security lesson ([Lesson 13](../lesson-13-jwt-authentication/README.md)).

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
   - One public method per use case; orchestrates repositories, mappers and business rules
   - DTO ↔ Entity mapping
   - Transaction boundary
   - Sits between Controller and Repository; entities never leave it, HTTP never enters it

6. **Use `@Transactional` on the service layer**
   - A transaction is all-or-nothing (ACID)
   - Spring wraps the service in a proxy: begin → method → commit, or rollback on a `RuntimeException`
   - Repository calls join the service's transaction (propagation `REQUIRED`)
   - Managed entities are saved automatically at commit (dirty checking)
   - Checked exceptions commit by default 
   - `readOnly = true` for read methods is an optimization
   - Watch out for self-invocation and private methods — they bypass the proxy

7. **Best Practices**
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

**`pizzastore-with-dtos/`** is Lesson 6a's `pizzastore-jpa` project plus the `dto`, `mapper` and `service` packages of this lesson. It's a step on the way to the final PizzaStore: the `domain`, `repository`, `dto` and `mapper` packages are the same as in the final project — except that validation (Lesson 10) and OpenAPI annotations (Lesson 14) have not been added to the DTOs yet.

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

**Note on the book**: *Pro Spring Boot 4* has no dedicated chapter on DTOs or mapping, and doesn't mention MapStruct at all. The idea shows up in a few places instead: Chapter 2's JSON section shows `@JsonProperty`/`@JsonIgnore` to shape the JSON of a type (e.g. to hide a password) — the approach [Mapping Strategies](#%EF%B8%8F-mapping-strategies) warns against for entities; Chapter 3's validation best practices advise to "prefer applying validation on Data Transfer Objects (DTOs)" rather than on domain entities (Lesson 10 does exactly that); and the book's Management CRM (Chapter 1, and again in Chapters 5–8) uses a `CustomerDetailsDTO` record that its service layer assembles by hand from several domain objects — manual mapping, as in [option 2](#2-manual-mapping--fine-for-small-projects). For the service layer and transactions the book is much closer to this lesson: Chapter 1's *Creating the Service Layer* introduces its `ManagementService` as the orchestrator between several repositories, and Chapter 5's *Managing Transactions and Concurrency* (atomicity, declarative `@Transactional`, isolation levels, programmatic `TransactionTemplate`) is the basis for the [Transactions](#-transactions) section — translated to PizzaStore's JPA services instead of the book's `JdbcClient` repositories. The book's Chapter 2 names transaction management as a classic example of a cross-cutting concern handled with AOP (the proxy explained above), and Chapter 8 uses `@Transactional(readOnly = true)` on read methods. This lesson adds dirty checking, rollback rules, propagation and the common proxy pitfalls, which the book doesn't cover explicitly.

On DTOs, this lesson goes beyond the book by separating request and response DTOs and by generating the mapping code with MapStruct, which PizzaStore uses because it scales better to a larger domain model and catches forgotten fields at compile time.

---

**Great work!** 🎉 PizzaStore now has a clean boundary between its database model and the outside world. Continue to [Lesson 8: REST Principles](../lesson-08-rest-principles/README.md).
