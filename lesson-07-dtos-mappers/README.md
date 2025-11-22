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
5. [Mapping Strategies](#-mapping-strategies)
6. [MapStruct: The Best Choice](#-mapstruct-the-best-choice)
7. [Service Layer Pattern](#-service-layer-pattern)
8. [Project Structure with DTOs](#-project-structure-with-dtos)
9. [Best Practices](#-best-practices)
10. [Common Pitfalls](#-common-pitfalls)
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

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreatePizzaRequest(
        @NotBlank(message = "Pizza name is required")
        String name,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be positive")
        BigDecimal price,

        String description,

        Boolean available,

        NutritionalInfoRequest nutritionalInfo
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

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record UpdatePizzaRequest(
        String name,

        @DecimalMin(value = "0.01", message = "Price must be positive")
        BigDecimal price,

        String description,

        Boolean available,  // Note: Boolean (nullable) for partial updates

        NutritionalInfoRequest nutritionalInfo
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
    return pizzaMapper.toResponse(saved);
}
```

---

## 🗺️ Mapping Strategies

### How to Convert Between Entities and DTOs?

#### 1. **Manual Mapping** ❌

```java
public PizzaResponse toResponse(Pizza pizza) {
    NutritionalInfoResponse nutritionalInfo = null;
    if (pizza.getNutritionalInfo() != null) {
        nutritionalInfo = new NutritionalInfoResponse(
            pizza.getNutritionalInfo().getCalories(),
            pizza.getNutritionalInfo().getProtein(),
            pizza.getNutritionalInfo().getCarbohydrates(),
            pizza.getNutritionalInfo().getFat()
        );
    }
    
    return new PizzaResponse(
        pizza.getId(),
        pizza.getName(),
        pizza.getPrice(),
        pizza.getDescription(),
        pizza.getImageUrl(),
        pizza.getAvailable(),
        nutritionalInfo
    );
}

public Pizza toEntity(CreatePizzaRequest request) {
    Pizza pizza = new Pizza();
    pizza.setName(request.name());
    pizza.setPrice(request.price());
    pizza.setDescription(request.description());
    pizza.setAvailable(request.available());
    
    if (request.nutritionalInfo() != null) {
        NutritionalInfo nutritionalInfo = new NutritionalInfo();
        nutritionalInfo.setCalories(request.nutritionalInfo().calories());
        nutritionalInfo.setProtein(request.nutritionalInfo().protein());
        nutritionalInfo.setCarbohydrates(request.nutritionalInfo().carbohydrates());
        nutritionalInfo.setFat(request.nutritionalInfo().fat());
        pizza.setNutritionalInfo(nutritionalInfo);
    }
    
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
    PizzaResponse toResponse(Pizza pizza);
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
import org.mapstruct.*;

import java.util.List;

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
    @Mapping(target = "favoritedByCustomers", ignore = true)
    Pizza toEntity(CreatePizzaRequest request);

    // Update existing entity from Request DTO
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
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
@Mapping(source = "customer.id", target = "customerId")
@Mapping(source = "customer.name", target = "customerName")
OrderResponse toResponse(Order order);
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
void updateEntity(UpdatePizzaRequest request, @MappingTarget Pizza pizza);
```

- `IGNORE`: Don't update if DTO field is `null`
- Allows partial updates (only change provided fields)

#### `@MappingTarget`

Updates an existing object instead of creating a new one:

```java
void updateEntity(UpdatePizzaRequest request, @MappingTarget Pizza pizza);

// Usage:
Pizza existing = pizzaRepository.findById(id).orElseThrow();
pizzaMapper.updateEntity(updateRequest, existing);
// existing is now updated with values from updateRequest
```

### Using Other Mappers

When a DTO contains nested objects, MapStruct can use other mappers:

```java
@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.name", target = "customerName")
    OrderResponse toResponse(Order order);
    
    List<OrderResponse> toResponseList(List<Order> orders);

    @Mapping(source = "pizza.id", target = "pizzaId")
    @Mapping(source = "pizza.name", target = "pizzaName")
    OrderLineResponse toOrderLineResponse(OrderLine orderLine);

    List<OrderLineResponse> toOrderLineResponseList(List<OrderLine> orderLines);
}
```

**How it works:**

```java
// Order entity has a Customer
@Entity
public class Order {
    @ManyToOne
    private Customer customer;
    // ...
}

// OrderResponse DTO flattens the customer data
public record OrderResponse(
    Long id,
    Long customerId,
    String customerName,
    List<OrderLineResponse> orderLines,
    BigDecimal totalAmount,
    OrderStatus status,
    LocalDateTime orderDate
) {}

// MapStruct automatically maps customer.id → customerId
```

### Summary DTOs for Nested Objects

To avoid circular references, use summary DTOs:

```java
// Full response
public record PizzaResponse(
        Long id,
        String name,
        BigDecimal price,
        String description,
        String imageUrl,
        Boolean available,
        NutritionalInfoResponse nutritionalInfo
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
// OrderLineResponse flattens pizza data (alternative approach)
public record OrderLineResponse(
        Long id,
        Long pizzaId,
        String pizzaName,
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
@Transactional  // All methods are transactional
public class PizzaService {
    
    // Read method - uses transaction
    public Optional<PizzaResponse> findById(Long id) { ... }
    
    // Write method - uses transaction
    public PizzaResponse create(CreatePizzaRequest request) { ... }
}
```

#### 3. **Always Return DTOs**

```java
// ❌ NEVER return entities from service
public Pizza findById(Long id) { ... }

// ✅ ALWAYS return DTOs
public Optional<PizzaResponse> findById(Long id) { ... }
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
        return Optional.of(mapper.toResponse(pizza));  // Changes might not be saved!
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
                    // Changes automatically saved when transaction commits
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

// And in mapper:
@Mapper(componentModel = "spring")
public interface CustomerMapper {
    @Mapping(target = "password", ignore = true)  // Extra safety when creating
    Customer toEntity(CreateCustomerRequest request);
}
```

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
        Long customerId,             // ← Just the ID
        String customerName,         // ← Just the name
        List<OrderLineResponse> orderLines,
        BigDecimal totalAmount,
        OrderStatus status,
        LocalDateTime orderDate
) {}
```

### 6. **Not Using `NullValuePropertyMappingStrategy.IGNORE` for Updates**

```java
// ❌ BAD: Null values overwrite existing data
@Mapper(componentModel = "spring")
public interface PizzaMapper {
    void updateEntity(UpdatePizzaRequest request, @MappingTarget Pizza pizza);
}

// Request: { "name": "New Name", "description": null }
// Result: description is set to null (data loss!)

// ✅ GOOD: Ignore null values (MapStruct default behavior for Records)
@Mapper(componentModel = "spring")
public interface PizzaMapper {
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(UpdatePizzaRequest request, @MappingTarget Pizza pizza);
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

## 🚀 Runnable Project

A complete, runnable Spring Boot project demonstrating **DTOs, MapStruct, and Service Layer** from this lesson is available in:

**`pizzastore-with-dtos/`**

The project includes:
- ✅ **Request DTOs**: PizzaRequest, OrderRequest, OrderLineRequest for incoming data
- ✅ **Response DTOs**: PizzaResponse, OrderResponse, OrderLineResponse for outgoing data
- ✅ **Summary DTOs**: CustomerSummary, PizzaSummary for nested objects
- ✅ **MapStruct Mappers**: Automatic, type-safe entity-DTO conversion
- ✅ **Service Layer**: PizzaService, OrderService, CustomerService with business logic
- ✅ **Transaction Management**: @Transactional annotations on service methods
- ✅ **No Audit Fields**: createdAt, updatedAt, createdBy, updatedBy are never exposed in responses
- ✅ Complete domain model with JPA relationships (from Lesson 6)
- ✅ Comprehensive sample data (12 pizzas, 6 customers, 10 orders)

### How to Run

```bash
cd pizzastore-with-dtos
mvn clean install
mvn spring-boot:run
```

The application starts on `http://localhost:8080` with H2 in-memory database.

### Verify the Service Layer

Once running, you can test the service layer by temporarily adding a simple test endpoint or by checking the logs during startup. The sample data is automatically loaded via `data.sql`.

You can also access the H2 console at `http://localhost:8080/h2-console`:
- JDBC URL: `jdbc:h2:mem:pizzastore`
- Username: `sa`
- Password: *(leave empty)*

See the project README for more details on testing the service methods.

---

## 🎓 Further Reading

- [MapStruct Documentation](https://mapstruct.org/)
- [Spring Boot Best Practices](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Java Records](https://docs.oracle.com/en/java/javase/17/language/records.html)
- [Transaction Management](https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction)

---

