# Lesson 7: DTOs & Mappers

**Data Transfer Objects and Entity-DTO Mapping**

---

## 📋 Learning Objectives

By the end of this lesson, you will be able to:
- Understand why DTOs are essential and why entities should **never** be exposed directly
- Differentiate between Request DTOs and Response DTOs
- Implement DTOs for proper API design using Java Records
- Use MapStruct for automatic mapping between entities and DTOs
- Structure your Spring Boot project with proper layering (Service Layer)
- Apply the DTO pattern to the PizzaStore application

---

## 📚 Table of Contents

1. [The Problem: Why Not Expose Entities?](#-the-problem-why-not-expose-entities)
2. [What Are DTOs?](#-what-are-dtos)
3. [Request vs Response DTOs](#-request-vs-response-dtos)
4. [Java Records for DTOs](#-java-records-for-dtos)
5. [Mapping Strategies](#️-mapping-strategies)
6. [MapStruct: The Best Choice](#-mapstruct-the-best-choice)
7. [Service Layer Pattern](#-service-layer-pattern)
8. [Project Structure with DTOs](#-project-structure-with-dtos)
9. [Best Practices](#-best-practices)
10. [Common Pitfalls](#️-common-pitfalls)
11. [Summary](#-summary)
12. [Further Reading](#-further-reading)

---

## 🚫 The Problem: Why Not Expose Entities?

### What's Wrong with This Code?

```java
@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    
    @Autowired
    private CustomerRepository customerRepository;
    
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

Entities often contain sensitive data that should never be exposed:

```java
@Entity
@Table(name = "customers")
public class Customer {
    private Long id;
    private String name;
    private String email;
    private String password;           // ❌ Exposed!
    private String address;
    private String phone;
    
    // Audit fields - internal information
    private LocalDateTime createdAt;   // ❌ Internal data exposed!
    private String createdBy;          // ❌ Internal data exposed!
    private LocalDateTime updatedAt;   // ❌ Internal data exposed!
    private String updatedBy;          // ❌ Internal data exposed!
    
    @OneToMany(mappedBy = "customer")
    private List<Order> orders;        // ❌ Can cause circular references!
}
```

When you return this entity, **all fields** are serialized to JSON, including:
- Passwords (even if hashed!)
- Audit fields (who created/updated the record)
- Relationships that cause circular references

#### 2. **Circular References** ♻️

JPA relationships can cause infinite loops during JSON serialization:

```java
@Entity
public class Order {
    private Long id;
    
    @ManyToOne
    private Customer customer;              // Customer → Order → Customer → Order → ...
    
    @OneToMany(mappedBy = "order")
    private List<OrderLine> orderLines;  // Order → OrderLine → Order → ...
}
```

Result:
```
java.lang.StackOverflowError: Cannot construct instance (no Creators, like default constructor, exist)
```

#### 3. **Lazy Loading Exceptions** 💥

When entities are serialized outside the transaction:

```java
@GetMapping("/{id}")
public Order getOrder(@PathVariable Long id) {
    // Transaction ends here ↓
    return orderRepository.findById(id).orElse(null);
}
// When Jackson tries to serialize orderLines:
// LazyInitializationException: could not initialize proxy - no Session
```

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

**Data Transfer Objects (DTOs)** are simple objects designed specifically for transferring data between layers.

### Key Characteristics

| Aspect | Entity | DTO |
|--------|--------|-----|
| **Purpose** | Represent database table | Transfer data over API |
| **Location** | Domain layer | DTO layer |
| **Annotations** | `@Entity`, `@Table`, `@Column` | None (or validation) |
| **Relationships** | `@OneToMany`, `@ManyToOne` | Flat structure or nested DTOs |
| **Mutability** | Mutable | Immutable (preferably) |
| **Contains** | All table columns | Only data needed for API |

### Benefits of DTOs

✅ **Security**: Only expose what's needed  
✅ **Flexibility**: API independent from database  
✅ **Versioning**: Support multiple API versions  
✅ **Performance**: Fetch only required data  
✅ **Clarity**: Clear API contract  
✅ **Validation**: Different rules for create/update  

---

## 🔄 Request vs Response DTOs

### Why Separate Request and Response DTOs?

Different operations need different data:

| Operation | Needs | Example                                                     |
|-----------|-------|-------------------------------------------------------------|
| **Create** | Data to create entity | Name, description, price                   |
| **Update** | Data to update entity | Name, description, (ID not needed)         |
| **Response** | Data to return | ID, name, description, price, timestamps, calculated fields |

### Example: Pizza DTOs

#### Request DTO (Create)

```java
package be.vives.pizzastore.dto.request;

import java.math.BigDecimal;

public record CreatePizzaRequest(
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        boolean available
) {
}
```

**Characteristics:**
- No `id` (generated by database)
- No audit fields (set by system)
- Only data client can provide

#### Request DTO (Update)

```java
package be.vives.pizzastore.dto.request;

import java.math.BigDecimal;

public record UpdatePizzaRequest(
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        Boolean available  // Note: Boolean (nullable) for partial updates
) {
}
```

**Characteristics:**
- Similar to Create but fields can be `null`
- Enables partial updates
- No `id` (passed in URL)

#### Response DTO

```java
package be.vives.pizzastore.dto.response;

import java.math.BigDecimal;

public record PizzaResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        boolean available
) {
}
```

**Characteristics:**
- Includes `id` (client needs to know it)
- **Never** includes audit fields (internal data)
- Read-only representation

---

## 📝 Java Records for DTOs

Since Java 14, **Records** are the perfect choice for DTOs!

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
- Constructor
- Getters
- `equals()`, `hashCode()`, `toString()`
- Immutability

### Records Work Perfectly with Jackson

```java
// Jackson automatically serializes/deserializes records
@PostMapping
public PizzaResponse createPizza(@RequestBody CreatePizzaRequest request) {
    // Jackson deserializes JSON → CreatePizzaRequest
    Pizza pizza = pizzaMapper.toEntity(request);
    Pizza saved = pizzaRepository.save(pizza);
    // Jackson serializes PizzaResponse → JSON
    return pizzaMapper.toPizzaResponse(saved);
}
```

---

## 🗺️ Mapping Strategies

### How to Convert Between Entities and DTOs?

#### 1. **Manual Mapping** ❌

```java
public PizzaResponse toPizzaResponse(Pizza pizza) {
    return new PizzaResponse(
        pizza.getId(),
        pizza.getName(),
        pizza.getDescription(),
        pizza.getPrice(),
        pizza.getImageUrl(),
        pizza.isAvailable()
    );
}

public Pizza toEntity(CreatePizzaRequest request) {
    Pizza pizza = new Pizza();
    pizza.setName(request.name());
    pizza.setDescription(request.description());
    pizza.setPrice(request.price());
    pizza.setImageUrl(request.imageUrl());
    pizza.setAvailable(request.available());
    return pizza;
}
```

**Problems:**
- Tedious and error-prone
- Must update manually when fields change
- Lots of boilerplate code

#### 2. **MapStruct** ✅ (Recommended)

MapStruct generates mapping code at **compile time**.

```java
@Mapper(componentModel = "spring")
public interface PizzaMapper {
    PizzaResponse toPizzaResponse(Pizza pizza);
    Pizza toEntity(CreatePizzaRequest request);
}
```

**Benefits:**
- Type-safe (compile-time checking)
- Fast (no reflection at runtime)
- Easy to use
- Maintainable

---

## 🎯 MapStruct: The Best Choice

### Setup

Add to `pom.xml`:

```xml
<properties>
    <org.mapstruct.version>1.6.3</org.mapstruct.version>
</properties>

<dependencies>
    <!-- MapStruct -->
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
            <version>3.13.0</version>
            <configuration>
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

### Basic Mapper

```java
package be.vives.pizzastore.mapper;

import be.vives.pizzastore.domain.Pizza;
import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.request.UpdatePizzaRequest;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.dto.response.PizzaSummaryResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PizzaMapper {

    // Entity → Response DTO
    PizzaResponse toResponse(Pizza pizza);
    
    List<PizzaResponse> toResponseList(List<Pizza> pizzas);

    // Request DTO → Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "nutritionalInfo", ignore = true)
    @Mapping(target = "favoritedByCustomers", ignore = true)
    Pizza toEntity(CreatePizzaRequest request);

    // Update existing entity from Request DTO
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "nutritionalInfo", ignore = true)
    @Mapping(target = "favoritedByCustomers", ignore = true)
    void updateEntity(UpdatePizzaRequest request, @MappingTarget Pizza pizza);
}
```

### Key Annotations

#### `@Mapper(componentModel = "spring")`

Makes MapStruct generate a Spring bean:

```java
// Generated code:
@Component
public class PizzaMapperImpl implements PizzaMapper {
    // Implementation...
}

// You can inject it:
@Service
public class PizzaService {
    private final PizzaMapper pizzaMapper;
    
    public PizzaService(PizzaMapper pizzaMapper) {
        this.pizzaMapper = pizzaMapper;
    }
}
```

#### `@Mapping`

Maps fields explicitly:

```java
@Mapping(source = "user.id", target = "userId")
@Mapping(source = "user.name", target = "userName")
OrderResponse toOrderResponse(Order order);
```

- `source`: Field in source object (Entity)
- `target`: Field in target object (DTO)

#### `@Mapping(target = "...", ignore = true)`

Ignore fields that shouldn't be mapped:

```java
@Mapping(target = "id", ignore = true)        // ID generated by DB
@Mapping(target = "createdAt", ignore = true) // Set by JPA auditing
@Mapping(target = "password", ignore = true)  // Never map password from request
Pizza toEntity(CreatePizzaRequest request);
```

#### `@BeanMapping(nullValuePropertyMappingStrategy = ...)`

For partial updates:

```java
@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
void updateEntityFromRequest(UpdatePizzaRequest request, @MappingTarget Pizza pizza);
```

- `IGNORE`: Don't update if DTO field is `null`
- Allows partial updates (only change provided fields)

#### `@MappingTarget`

Updates an existing object instead of creating a new one:

```java
void updateEntityFromRequest(UpdatePizzaRequest request, @MappingTarget Pizza pizza);

// Usage:
Pizza existing = pizzaRepository.findById(id).orElseThrow();
pizzaMapper.updateEntityFromRequest(updateRequest, existing);
// existing is now updated with values from updateRequest
```

### Using Other Mappers

When a DTO contains nested objects, MapStruct can use other mappers:

```java
@Mapper(componentModel = "spring", uses = {UserMapper.class, PizzaMapper.class})
public interface OrderMapper {

    OrderResponse toOrderResponse(Order order);
    
    List<OrderResponse> toResponseList(List<Order> orders);

    OrderLineResponse toOrderLineResponse(OrderLine orderLine);

    List<OrderLineResponse> toOrderLineResponseList(List<OrderLine> orderLines);
}
```

**How it works:**

```java
// Order entity has a User
@Entity
public class Order {
    @ManyToOne
    private User user;
    // ...
}

// OrderResponse DTO has a UserSummaryResponse
public record OrderResponse(
    Long id,
    UserSummaryResponse user,  // Nested DTO
    // ...
) {}

// MapStruct automatically uses UserMapper.toSummaryResponse()
```

### Summary DTOs for Nested Objects

To avoid circular references, use summary DTOs:

```java
// Full response
public record PizzaResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        boolean available
) {}

// Summary for nested objects (only essential fields)
public record PizzaSummaryResponse(
        Long id,
        String name,
        BigDecimal price
) {}
```

**Usage:**

```java
// OrderLineResponse contains PizzaSummaryResponse
public record OrderLineResponse(
        Long id,
        PizzaSummaryResponse pizza,  // ← Summary, not full Pizza
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
```

---

## 🏢 Service Layer Pattern

The **Service Layer** sits between Controllers and Repositories.

### Why a Service Layer?

```
┌─────────────────┐
│   Controller    │  ← Handles HTTP, validates input
└────────┬────────┘
         ↓
┌─────────────────┐
│    Service      │  ← Business logic, DTO mapping, transactions
└────────┬────────┘
         ↓
┌─────────────────┐
│   Repository    │  ← Data access
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
| **Service** | Business logic | Validate business rules, map DTOs, orchestrate |
| **Repository** | Data access | CRUD operations, queries |

### Service Layer Implementation

```java
package be.vives.pizzastore.service;

import be.vives.pizzastore.domain.Pizza;
import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.request.UpdatePizzaRequest;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.mapper.PizzaMapper;
import be.vives.pizzastore.repository.PizzaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)  // All methods read-only by default
public class PizzaService {

    private final PizzaRepository pizzaRepository;
    private final PizzaMapper pizzaMapper;

    // Constructor injection (best practice)
    public PizzaService(PizzaRepository pizzaRepository, PizzaMapper pizzaMapper) {
        this.pizzaRepository = pizzaRepository;
        this.pizzaMapper = pizzaMapper;
    }

    // Read operation - returns DTO
    public List<PizzaResponse> getAllPizzas() {
        return pizzaRepository.findAll()
                .stream()
                .map(pizzaMapper::toPizzaResponse)
                .toList();
    }

    public List<PizzaResponse> getAvailablePizzas() {
        return pizzaRepository.findByAvailableTrue()
                .stream()
                .map(pizzaMapper::toPizzaResponse)
                .toList();
    }

    public PizzaResponse getPizzaById(Long id) {
        Pizza pizza = pizzaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pizza not found with id: " + id));
        return pizzaMapper.toPizzaResponse(pizza);
    }

    // Write operation - needs its own transaction
    @Transactional
    public PizzaResponse createPizza(CreatePizzaRequest request) {
        // 1. Map DTO → Entity
        Pizza pizza = pizzaMapper.toEntity(request);
        
        // 2. Save entity
        Pizza savedPizza = pizzaRepository.save(pizza);
        
        // 3. Map Entity → DTO and return
        return pizzaMapper.toPizzaResponse(savedPizza);
    }

    @Transactional
    public PizzaResponse updatePizza(Long id, UpdatePizzaRequest request) {
        // 1. Find existing entity
        Pizza pizza = pizzaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pizza not found with id: " + id));
        
        // 2. Update entity from DTO (only non-null fields)
        pizzaMapper.updateEntityFromRequest(request, pizza);
        
        // 3. Save and return
        Pizza updatedPizza = pizzaRepository.save(pizza);
        return pizzaMapper.toPizzaResponse(updatedPizza);
    }

    @Transactional
    public void deletePizza(Long id) {
        if (!pizzaRepository.existsById(id)) {
            throw new RuntimeException("Pizza not found with id: " + id);
        }
        pizzaRepository.deleteById(id);
    }

    public List<PizzaResponse> searchPizzas(String keyword) {
        return pizzaRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword)
                .stream()
                .map(pizzaMapper::toPizzaResponse)
                .toList();
    }
}
```

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
- Easy to test (can pass mocks)
- Explicit dependencies

#### 2. **Transaction Management**

```java
@Service
@Transactional(readOnly = true)  // Default for all methods
public class PizzaService {
    
    // Read method - uses default read-only transaction
    public PizzaResponse getPizzaById(Long id) { ... }
    
    // Write method - overrides with read-write transaction
    @Transactional
    public PizzaResponse createPizza(CreatePizzaRequest request) { ... }
}
```

#### 3. **Always Return DTOs**

```java
// ❌ NEVER return entities from service
public Pizza getPizza(Long id) { ... }

// ✅ ALWAYS return DTOs
public PizzaResponse getPizza(Long id) { ... }
```

---

## 📁 Project Structure with DTOs

```
src/main/java/be/vives/pizzastore/
├── domain/                      # JPA Entities
│   ├── Pizza.java
│   ├── Order.java
│   ├── OrderLine.java
│   ├── Customer.java
│   ├── NutritionalInfo.java
│   ├── OrderStatus.java         # Enum
│   └── Role.java                # Enum
│
├── repository/                  # Spring Data JPA Repositories
│   ├── PizzaRepository.java
│   ├── OrderRepository.java
│   └── CustomerRepository.java
│
├── dto/                         # Data Transfer Objects
│   ├── request/                 # Request DTOs (incoming)
│   │   ├── CreatePizzaRequest.java
│   │   ├── UpdatePizzaRequest.java
│   │   ├── CreateCustomerRequest.java
│   │   ├── UpdateCustomerRequest.java
│   │   ├── CreateOrderRequest.java
│   │   ├── UpdateOrderStatusRequest.java
│   │   ├── OrderLineRequest.java
│   │   └── NutritionalInfoRequest.java
│   │
│   └── response/                # Response DTOs (outgoing)
│       ├── PizzaResponse.java
│       ├── PizzaSummaryResponse.java
│       ├── OrderResponse.java
│       ├── OrderLineResponse.java
│       ├── CustomerResponse.java
│       └── NutritionalInfoResponse.java
│
├── mapper/                      # MapStruct Mappers
│   ├── PizzaMapper.java
│   ├── OrderMapper.java
│   └── CustomerMapper.java
│
├── service/                     # Service Layer
│   ├── PizzaService.java
│   ├── OrderService.java
│   └── CustomerService.java
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

### 2. **Package Structure**

```
dto/
├── request/
│   ├── CreatePizzaRequest.java
│   └── UpdatePizzaRequest.java
└── response/
    ├── PizzaResponse.java
    └── PizzaSummaryResponse.java
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

### 4. **Validation**

```java
// Add validation to Request DTOs
public record CreatePizzaRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,
        
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        BigDecimal price
) {}
```

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

### 6. **Use Summary DTOs for Nested Objects**

```java
// ✅ GOOD: Avoid circular references
public record OrderResponse(
        Long id,
        UserSummaryResponse user,     // Summary, not full User
        List<OrderLineResponse> orderLines
) {}

// ❌ BAD: Can cause circular references or over-fetching
public record OrderResponse(
        Long id,
        UserResponse user,            // Full user with all orders → infinite loop!
        List<OrderLineResponse> orderLines
) {}
```

### 7. **Service Layer Always Returns DTOs**

```java
// ✅ GOOD
@Service
public class PizzaService {
    public PizzaResponse getPizza(Long id) {
        Pizza pizza = repository.findById(id).orElseThrow();
        return mapper.toPizzaResponse(pizza);
    }
}

// ❌ BAD
@Service
public class PizzaService {
    public Pizza getPizza(Long id) {
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
public PizzaResponse getPizza(@PathVariable Long id) {
    return pizzaService.getPizzaById(id);
}
```

### 2. **Forgetting `@Transactional` on Write Operations**

```java
// ❌ BAD: No transaction
@Service
public class PizzaService {
    public PizzaResponse updatePizza(Long id, UpdatePizzaRequest request) {
        Pizza pizza = repository.findById(id).orElseThrow();
        mapper.updateEntityFromRequest(request, pizza);
        return mapper.toPizzaResponse(pizza);  // Changes might not be saved!
    }
}

// ✅ GOOD: Explicit transaction
@Service
@Transactional(readOnly = true)
public class PizzaService {
    @Transactional  // Write transaction
    public PizzaResponse updatePizza(Long id, UpdatePizzaRequest request) {
        Pizza pizza = repository.findById(id).orElseThrow();
        mapper.updateEntityFromRequest(request, pizza);
        // Changes automatically saved when transaction commits
        return mapper.toPizzaResponse(pizza);
    }
}
```

### 3. **Not Ignoring Fields in Mappers**

```java
// ❌ BAD: Doesn't ignore generated fields
@Mapper(componentModel = "spring")
public interface PizzaMapper {
    Pizza toEntity(CreatePizzaRequest request);
    // This will try to map id, createdAt, etc. from request!
}

// ✅ GOOD: Explicitly ignore generated fields
@Mapper(componentModel = "spring")
public interface PizzaMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Pizza toEntity(CreatePizzaRequest request);
}
```

### 4. **Exposing Passwords**

```java
// ❌ DANGER: Password exposed in response
public record UserResponse(
        Long id,
        String name,
        String email,
        String password  // ❌❌❌
) {}

// ✅ SAFE: Password never included
public record UserResponse(
        Long id,
        String name,
        String email
) {}

// And in mapper:
@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "password", ignore = true)  // Extra safety
    UserResponse toUserResponse(User user);
}
```

### 5. **Circular References in DTOs**

```java
// ❌ BAD: Circular reference
public record UserResponse(
        Long id,
        String name,
        List<OrderResponse> orders
) {}

public record OrderResponse(
        Long id,
        UserResponse user,           // ← Circular! User → Order → User → ...
        List<OrderLineResponse> orderLines
) {}

// ✅ GOOD: Use Summary DTO
public record OrderResponse(
        Long id,
        UserSummaryResponse user,    // ← Summary, no orders
        List<OrderLineResponse> orderLines
) {}
```

### 6. **Not Using `NullValuePropertyMappingStrategy.IGNORE` for Updates**

```java
// ❌ BAD: Null values overwrite existing data
@Mapper(componentModel = "spring")
public interface PizzaMapper {
    void updateEntityFromRequest(UpdatePizzaRequest request, @MappingTarget Pizza pizza);
}

// Request: { "name": "New Name", "description": null }
// Result: description is set to null (data loss!)

// ✅ GOOD: Ignore null values
@Mapper(componentModel = "spring")
public interface PizzaMapper {
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(UpdatePizzaRequest request, @MappingTarget Pizza pizza);
}

// Request: { "name": "New Name", "description": null }
// Result: Only name is updated, description keeps its existing value
```

---

## 📝 Summary

### Key Takeaways

1. **Never expose entities directly** in your API
   - Security risks (passwords, audit fields)
   - Circular references
   - Tight coupling
   - Lazy loading exceptions

2. **Use DTOs** for data transfer
   - Request DTOs for incoming data
   - Response DTOs for outgoing data
   - Summary DTOs for nested objects

3. **Use Java Records** for DTOs
   - Concise syntax
   - Immutable by default
   - Perfect for data transfer

4. **Use MapStruct** for mapping
   - Type-safe
   - Compile-time generation
   - No runtime reflection
   - Easy to maintain

5. **Implement a Service Layer**
   - Business logic
   - DTO mapping
   - Transaction management
   - Sits between Controller and Repository

6. **Best Practices**
   - Constructor injection
   - `@Transactional` annotations
   - Never include audit fields in responses
   - Never include passwords in responses
   - Use Summary DTOs for nested objects

### Architecture

```
┌──────────────────────────────────────┐
│         Controller Layer              │
│  - HTTP concerns                      │
│  - Request validation                 │
│  - Response status codes              │
└──────────────┬───────────────────────┘
               │ DTOs
               ↓
┌──────────────────────────────────────┐
│          Service Layer                │
│  - Business logic                     │
│  - DTO ↔ Entity mapping               │
│  - Transaction management             │
└──────────────┬───────────────────────┘
               │ Entities
               ↓
┌──────────────────────────────────────┐
│        Repository Layer               │
│  - Data access                        │
│  - JPA queries                        │
└──────────────┬───────────────────────┘
               │
               ↓
┌──────────────────────────────────────┐
│            Database                   │
└──────────────────────────────────────┘
```

### What's Next?

In **Lesson 8 (REST Principles)** we'll add:
- Controllers to expose REST endpoints
- HTTP methods (GET, POST, PUT, DELETE)
- Status codes and error handling
- REST best practices

In **Lesson 9 (Complete REST API)** we'll:
- Build a complete REST API for PizzaStore
- Implement HATEOAS
- Add pagination and filtering
- Handle complex scenarios

---

## 🎓 Further Reading

- [MapStruct Documentation](https://mapstruct.org/)
- [Spring Boot Best Practices](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Java Records](https://docs.oracle.com/en/java/javase/17/language/records.html)
- [Transaction Management](https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction)

---

