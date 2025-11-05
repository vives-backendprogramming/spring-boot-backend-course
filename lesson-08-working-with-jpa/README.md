# Lesson 8: Working with JPA

**Java Persistence API and Spring Data JPA**

---

## 📋 Learning Objectives

By the end of this lesson, you will be able to:
- Understand ORM, JPA, and Spring Data JPA concepts
- Create JPA entities with proper annotations
- Implement entity relationships: `@OneToOne`, `@OneToMany`, `@ManyToOne`, `@ManyToMany`
- Choose between `LAZY` and `EAGER` fetch types
- Use cascade types effectively
- Leverage different Spring Data JPA repository interfaces
- Write custom queries using property expressions, `@Query`, and JPQL
- Implement pagination and sorting
- Handle Optional results properly
- Configure DDL generation strategies
- Expand PizzaStore with Customer and Order entities

---

## 📚 Table of Contents

1. [ORM, JPA, and Spring Data JPA](#orm-jpa-and-spring-data-jpa)
2. [JPA Entities in Depth](#jpa-entities-in-depth)
3. [Entity Relationships](#entity-relationships)
4. [Fetch Types: LAZY vs EAGER](#fetch-types-lazy-vs-eager)
5. [Cascade Types](#cascade-types)
6. [Spring Data JPA Repository Interfaces](#spring-data-jpa-repository-interfaces)
7. [Custom Queries](#custom-queries)
8. [Pagination and Sorting](#pagination-and-sorting)
9. [Optional Handling](#optional-handling)
10. [Database Configuration](#database-configuration)
11. [PizzaStore: Adding Customer and Order](#pizzastore-adding-customer-and-order)
12. [Summary](#summary)

---

## 🗃️ ORM, JPA, and Spring Data JPA

### What is ORM?

**ORM (Object-Relational Mapping)** bridges the gap between object-oriented programming and relational databases.

```
Java Objects          ←→          Database Tables
--------------                    ----------------
Pizza (object)        ↔           pizzas (table)
  - id                            - id
  - name                          - name
  - price                         - price
```

**Without ORM**:
```java
// Manual JDBC (painful!)
String sql = "INSERT INTO pizzas (name, price) VALUES (?, ?)";
PreparedStatement stmt = connection.prepareStatement(sql);
stmt.setString(1, "Margherita");
stmt.setBigDecimal(2, new BigDecimal("8.50"));
stmt.executeUpdate();
```

**With ORM**:
```java
// JPA (elegant!)
Pizza pizza = new Pizza("Margherita", new BigDecimal("8.50"));
entityManager.persist(pizza);
```

### What is JPA?

**JPA (Java Persistence API)** is a specification for ORM in Java.

- **Specification**, not implementation
- Defines annotations (`@Entity`, `@Id`, etc.)
- Defines EntityManager API
- Vendor-neutral (can switch implementations)

**JPA Implementations**:
- **Hibernate** (most popular, used by Spring Boot by default)
- EclipseLink
- OpenJPA

### What is Spring Data JPA?

**Spring Data JPA** builds on top of JPA to make it even easier.

```
Application Code
       ↓
Spring Data JPA      ← Provides repository abstractions
       ↓
JPA API              ← Standard specification
       ↓
Hibernate            ← JPA implementation
       ↓
JDBC                 ← Low-level database access
       ↓
Database (H2, PostgreSQL, MySQL, etc.)
```

**Spring Data JPA Features**:
- Repository interfaces (no implementation needed!)
- Query methods by naming convention
- Pagination and sorting
- Custom queries with `@Query`
- Auditing support

---

## 📦 JPA Entities in Depth

### Basic Entity Anatomy

```java
@Entity                              // 1. Marks as JPA entity
@Table(name = "pizzas")             // 2. Maps to database table
public class Pizza {
    
    @Id                              // 3. Primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // 4. Auto-increment
    private Long id;
    
    @Column(nullable = false, length = 100)  // 5. Column mapping
    private String name;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(length = 1000)
    private String description;
    
    @Column(name = "created_at")
    private LocalDate createdAt;
    
    @Transient                         // 7. Not persisted
    private String temporaryData;
}
```

### Important Annotations

#### @Entity
Marks a class as a JPA entity (persistent domain object).

```java
@Entity  // Table name defaults to class name (lowercase)
public class Pizza { }

@Entity(name = "PizzaItem")  // Custom entity name
public class Pizza { }
```

#### @Table
Specifies the table name and constraints.

```java
@Table(name = "pizzas")  // Custom table name
public class Pizza { }

@Table(
    name = "pizzas",
    uniqueConstraints = @UniqueConstraint(columnNames = "name"),
    indexes = @Index(name = "idx_price", columnList = "price")
)
public class Pizza { }
```

#### @Id and @GeneratedValue
Define the primary key and generation strategy.

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment (MySQL, PostgreSQL)
private Long id;

@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE)  // Sequence (Oracle, PostgreSQL)
private Long id;

@Id
@GeneratedValue(strategy = GenerationType.AUTO)      // Let JPA choose
private Long id;

@Id
@GeneratedValue(strategy = GenerationType.UUID)      // UUID (Java 25+)
private UUID id;
```

**Recommendation**: Use `IDENTITY` for H2, MySQL, PostgreSQL.

#### @Column
Defines column properties.

```java
@Column(
    name = "pizza_name",           // Column name (default: field name)
    nullable = false,              // NOT NULL constraint
    unique = true,                 // UNIQUE constraint
    length = 100,                  // VARCHAR length
    precision = 10,                // For decimal numbers
    scale = 2,                     // Decimal places
    insertable = true,             // Include in INSERT
    updatable = true,              // Include in UPDATE
    columnDefinition = "TEXT"      // Custom SQL type
)
private String name;
```

#### @Transient
Excludes field from persistence.

```java
@Transient
private String calculatedField;  // Not saved to database
```

#### @Enumerated
Maps enum types.

```java
public enum PizzaSize {
    SMALL, MEDIUM, LARGE
}

@Enumerated(EnumType.STRING)  // Save as "SMALL", "MEDIUM", "LARGE"
private PizzaSize size;

@Enumerated(EnumType.ORDINAL) // Save as 0, 1, 2 (not recommended!)
private PizzaSize size;
```

**Recommendation**: Always use `EnumType.STRING` for maintainability.

### Entity Lifecycle Callbacks

```java
@Entity
public class Pizza {
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    @PostPersist
    protected void afterCreate() {
        System.out.println("Pizza created: " + id);
    }
    
    @PreRemove
    protected void beforeDelete() {
        System.out.println("About to delete: " + id);
    }
    
    @PostRemove
    protected void afterDelete() {
        System.out.println("Pizza deleted");
    }
}
```

---

## 🔗 Entity Relationships

### Types of Relationships

| Relationship | Example                      | Database Implementation |
|--------------|------------------------------|------------------------|
| `@OneToOne` | Pizza ↔ NutritionalInfo      | Foreign key in either table |
| `@OneToMany` | Customer → Orders            | Foreign key in Orders table |
| `@ManyToOne` | Order → Customer             | Foreign key in Order table |
| `@ManyToMany` | Customer ↔ Pizza (favorites) | Join table |

---

### @OneToOne

One entity is associated with exactly one instance of another entity.

**Example**: Pizza has one NutritionalInfo, NutritionalInfo belongs to one Pizza.

```java
@Entity
@Table(name = "pizzas")
public class Pizza {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private BigDecimal price;
    
    @OneToOne(mappedBy = "pizza", cascade = CascadeType.ALL, orphanRemoval = true)
    private NutritionalInfo nutritionalInfo;
}

@Entity
@Table(name = "nutritional_info")
public class NutritionalInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Integer calories;
    private Integer protein;
    private Integer carbohydrates;
    private Integer fat;
    
    @OneToOne
    @JoinColumn(name = "pizza_id", nullable = false)
    private Pizza pizza;
}
```

**Database Structure**:
```sql
pizzas                      nutritional_info
------                      ----------------
id (PK)                     id (PK)
name                        pizza_id (FK) → pizzas.id
price                       calories
                            protein
                            carbohydrates
                            fat
```

**Key Points**:
- `@JoinColumn` defines which side owns the relationship (foreign key side)
- `mappedBy` indicates the inverse side (doesn't have the FK)
- `orphanRemoval = true`: Delete nutritional info if removed from pizza

---

### @OneToMany and @ManyToOne

One entity is associated with multiple instances of another entity.

**Example**: Customer has many Orders, each Order belongs to one Customer.

```java
@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String email;
    
    // One customer → many orders
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();
    
    // Helper method
    public void addOrder(Order order) {
        orders.add(order);
        order.setCustomer(this);
    }
}

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String orderNumber;
    private LocalDateTime orderDate;
    private BigDecimal totalAmount;
    
    // Many orders → one customer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
}
```

**Database Structure**:
```sql
customers               orders
---------               ------
id (PK)                 id (PK)
name                    customer_id (FK) → customers.id
email                   orderNumber
                        orderDate
                        totalAmount
```

**Key Points**:
- `@ManyToOne` side has the foreign key (`@JoinColumn`)
- `@OneToMany` side uses `mappedBy` (inverse side)
- Always use `@ManyToOne` for the relationship owner
- Use helper methods to maintain both sides of the relationship

---

### @ManyToMany

Multiple entities are associated with multiple instances of another entity.

**Example**: Customers have favorite Pizzas, Pizzas are favorited by Customers.

```java
@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String email;
    
    @ManyToMany
    @JoinTable(
        name = "customer_favorite_pizzas",
        joinColumns = @JoinColumn(name = "customer_id"),
        inverseJoinColumns = @JoinColumn(name = "pizza_id")
    )
    private Set<Pizza> favoritePizzas = new HashSet<>();
    
    public void addFavoritePizza(Pizza pizza) {
        favoritePizzas.add(pizza);
        pizza.getFavoritedByCustomers().add(this);
    }
    
    public void removeFavoritePizza(Pizza pizza) {
        favoritePizzas.remove(pizza);
        pizza.getFavoritedByCustomers().remove(this);
    }
}

@Entity
@Table(name = "pizzas")
public class Pizza {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private BigDecimal price;
    
    @ManyToMany(mappedBy = "favoritePizzas")
    private Set<Customer> favoritedByCustomers = new HashSet<>();
}
```

**Database Structure**:
```sql
customers                pizzas                customer_favorite_pizzas (join table)
---------                ------                ---------------------------------
id (PK)                  id (PK)               customer_id (FK) → customers.id
name                     name                  pizza_id (FK) → pizzas.id
email                    price                 PRIMARY KEY (customer_id, pizza_id)
```

**Key Points**:
- Requires a join table (created automatically or manually with `@JoinTable`)
- One side defines the join table, the other uses `mappedBy`
- Use `Set` instead of `List` (better performance, no duplicates)
- Always maintain both sides of the relationship

**When to Avoid `@ManyToMany`**:
If you need additional data in the join table, create a separate entity:

```java
// Instead of Customer ↔ Pizza (favorites)
// Use: Customer → PizzaRating ← Pizza

@Entity
@Table(name = "pizza_ratings")
public class PizzaRating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
    
    @ManyToOne
    @JoinColumn(name = "pizza_id")
    private Pizza pizza;
    
    private LocalDateTime ratedAt;
    private Integer rating;  // 1-5 stars
    private String comment;  // Additional data!
}
```

---

## ⏳ Fetch Types: LAZY vs EAGER

### What is Fetching?

When you load an entity, should related entities be loaded immediately?

```java
Customer customer = customerRepository.findById(1L);
// Are customer.orders loaded now, or later when accessed?
```

### FetchType.EAGER

Related entities are loaded **immediately** with the parent entity.

```java
@Entity
public class Customer {
    @OneToMany(mappedBy = "customer", fetch = FetchType.EAGER)
    private List<Order> orders;
}

// When you load customer, all orders are fetched immediately
Customer customer = customerRepository.findById(1L);
// SQL: SELECT * FROM customers WHERE id = 1
//      SELECT * FROM orders WHERE customer_id = 1
```

**Pros**:
- ✅ No lazy loading exceptions
- ✅ All data available immediately

**Cons**:
- ❌ Can fetch too much data unnecessarily
- ❌ Performance issues with large datasets
- ❌ N+1 query problem

### FetchType.LAZY

Related entities are loaded **only when accessed** (on-demand).

```java
@Entity
public class Customer {
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)  // Default for @OneToMany
    private List<Order> orders;
}

// When you load customer, orders are NOT fetched
Customer customer = customerRepository.findById(1L);
// SQL: SELECT * FROM customers WHERE id = 1

// Orders are fetched when accessed
List<Order> orders = customer.getOrders();  // NOW fetched
// SQL: SELECT * FROM orders WHERE customer_id = 1
```

**Pros**:
- ✅ Better performance (only load what you need)
- ✅ Less memory usage
- ✅ Faster initial queries

**Cons**:
- ❌ Can cause `LazyInitializationException` if session closed
- ❌ Requires careful transaction management

### Default Fetch Types

| Relationship | Default Fetch Type |
|--------------|--------------------|
| `@OneToOne` | `EAGER` |
| `@ManyToOne` | `EAGER` |
| `@OneToMany` | `LAZY` |
| `@ManyToMany` | `LAZY` |

### Best Practices

1. **Always use LAZY for collections** (`@OneToMany`, `@ManyToMany`)
   ```java
   @OneToMany(fetch = FetchType.LAZY)  // Good
   private List<Order> orders;
   ```

2. **Use EAGER sparingly** (only for small, frequently-needed data)
   ```java
   @ManyToOne(fetch = FetchType.EAGER)  // OK if Category is small
   private Category category;
   ```

3. **Override with JOIN FETCH in queries when needed**
   ```java
   @Query("SELECT c FROM Customer c JOIN FETCH c.orders WHERE c.id = :id")
   Optional<Customer> findByIdWithOrders(@Param("id") Long id);
   ```

4. **Use DTOs to avoid lazy loading issues**
   ```java
   // In service layer (transaction active)
   Customer customer = customerRepository.findById(id);
   List<Order> orders = customer.getOrders();  // Trigger fetch
   return mapper.toDTO(customer, orders);      // Convert to DTO
   ```

---

## 🌊 Cascade Types

### What is Cascading?

Should operations on the parent entity cascade (propagate) to child entities?

```java
Customer customer = customerRepository.findById(1L);
customerRepository.delete(customer);
// Should the customer's orders also be deleted?
```

### Cascade Types

```java
public enum CascadeType {
    PERSIST,   // Save operation cascades
    MERGE,     // Update operation cascades
    REMOVE,    // Delete operation cascades
    REFRESH,   // Refresh operation cascades
    DETACH,    // Detach operation cascades
    ALL        // All operations cascade
}
```

### CascadeType.PERSIST

When you save the parent, children are also saved.

```java
@Entity
public class Customer {
    @OneToMany(mappedBy = "customer", cascade = CascadeType.PERSIST)
    private List<Order> orders;
}

// Create customer with orders
Customer customer = new Customer("John");
Order order1 = new Order("ORD-001");
Order order2 = new Order("ORD-002");
customer.addOrder(order1);
customer.addOrder(order2);

customerRepository.save(customer);  // Also saves order1 and order2!
```

### CascadeType.REMOVE

When you delete the parent, children are also deleted.

```java
@Entity
public class Customer {
    @OneToMany(mappedBy = "customer", cascade = CascadeType.REMOVE)
    private List<Order> orders;
}

Customer customer = customerRepository.findById(1L);
customerRepository.delete(customer);  // Also deletes all customer's orders!
```

⚠️ **Careful with REMOVE**: Can accidentally delete important data!

### CascadeType.ALL

All operations cascade (PERSIST + MERGE + REMOVE + REFRESH + DETACH).

```java
@OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Order> orders;
```

### orphanRemoval

Delete child entities when they're removed from the collection.

```java
@Entity
public class Customer {
    @OneToMany(
        mappedBy = "customer", 
        cascade = CascadeType.ALL, 
        orphanRemoval = true  // Delete orphaned orders
    )
    private List<Order> orders;
}

Customer customer = customerRepository.findById(1L);
customer.getOrders().clear();  // Remove all orders from collection
customerRepository.save(customer);  // Orders are DELETED from database!
```

**Difference from CascadeType.REMOVE**:
- `CascadeType.REMOVE`: Delete children when parent is deleted
- `orphanRemoval`: Delete children when removed from collection (even if parent still exists)

### Best Practices

1. **Use CascadeType.ALL + orphanRemoval for parent-child relationships**
   ```java
   @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
   private List<Order> orders;
   ```

2. **Be careful with CascadeType.REMOVE**
   ```java
   // ❌ Dangerous: Deleting order would delete customer!
   @ManyToOne(cascade = CascadeType.REMOVE)
   private Customer customer;
   
   // ✅ Better: No cascading on @ManyToOne
   @ManyToOne
   private Customer customer;
   ```

3. **Don't cascade on @ManyToMany** (usually)
   ```java
   // ❌ Dangerous: Deleting student would delete all their courses!
   @ManyToMany(cascade = CascadeType.ALL)
   private Set<Course> courses;
   
   // ✅ Better: Only cascade PERSIST and MERGE
   @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
   private Set<Course> courses;
   ```

---

## 🗄️ Spring Data JPA Repository Interfaces

Spring Data JPA provides a hierarchy of repository interfaces with increasing functionality.

### Repository Hierarchy

```
Repository<T, ID>                    (Marker interface - empty)
    ↓
CrudRepository<T, ID>                (Basic CRUD operations)
    ↓
PagingAndSortingRepository<T, ID>    (+ Pagination and sorting)
    ↓
JpaRepository<T, ID>                 (+ JPA-specific operations)
```

### 1. Repository

Base marker interface - no methods.

```java
public interface PizzaRepository extends Repository<Pizza, Long> {
    // Define only the methods you need
    Optional<Pizza> findById(Long id);
    Pizza save(Pizza pizza);
}
```

**Use when**: You want full control over available methods.

### 2. CrudRepository

Basic CRUD operations.

```java
public interface CrudRepository<T, ID> {
    <S extends T> S save(S entity);
    <S extends T> Iterable<S> saveAll(Iterable<S> entities);
    Optional<T> findById(ID id);
    boolean existsById(ID id);
    Iterable<T> findAll();
    Iterable<T> findAllById(Iterable<ID> ids);
    long count();
    void deleteById(ID id);
    void delete(T entity);
    void deleteAllById(Iterable<? extends ID> ids);
    void deleteAll(Iterable<? extends T> entities);
    void deleteAll();
}
```

**Use when**: You need basic CRUD without pagination.

### 3. PagingAndSortingRepository

Adds pagination and sorting.

```java
public interface PagingAndSortingRepository<T, ID> extends CrudRepository<T, ID> {
    Iterable<T> findAll(Sort sort);
    Page<T> findAll(Pageable pageable);
}
```

**Use when**: You need pagination or sorting.

### 4. JpaRepository (Recommended)

Most feature-rich repository interface.

```java
public interface JpaRepository<T, ID> extends PagingAndSortingRepository<T, ID> {
    List<T> findAll();
    List<T> findAll(Sort sort);
    List<T> findAllById(Iterable<ID> ids);
    <S extends T> List<S> saveAll(Iterable<S> entities);
    void flush();
    <S extends T> S saveAndFlush(S entity);
    void deleteInBatch(Iterable<T> entities);
    void deleteAllInBatch();
    T getOne(ID id);  // Deprecated - use getReferenceById
    T getReferenceById(ID id);
}
```

**Key differences from CrudRepository**:
- Returns `List` instead of `Iterable` (easier to work with)
- Batch operations (`deleteInBatch`, `deleteAllInBatch`)
- `flush()` to force synchronization with database
- `saveAndFlush()` to save and immediately flush

**Use when**: Building REST APIs (most common choice).

### Example

```java
package be.vives.pizzastore.repository;

import be.vives.pizzastore.domain.Pizza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PizzaRepository extends JpaRepository<Pizza, Long> {
    // Already have all methods from JpaRepository!
    // findAll(), findById(), save(), deleteById(), count(), etc.
}
```

---

## 🔍 Custom Queries

Spring Data JPA provides multiple ways to query data.

### 1. Query Methods (Derived Queries)

Define methods following naming conventions - Spring generates the query!

#### Property Expressions

```java
public interface PizzaRepository extends JpaRepository<Pizza, Long> {
    
    // find...By<Property>
    Optional<Pizza> findByName(String name);
    
    List<Pizza> findByPriceLessThan(BigDecimal price);
    
    List<Pizza> findByPriceGreaterThanEqual(BigDecimal price);
    
    List<Pizza> findByNameContaining(String keyword);
    
    List<Pizza> findByNameStartingWith(String prefix);
    
    // Combining conditions
    List<Pizza> findByNameAndPrice(String name, BigDecimal price);
    
    List<Pizza> findByNameOrDescription(String name, String description);
    
    // Sorting
    List<Pizza> findByPriceLessThanOrderByPriceAsc(BigDecimal maxPrice);
    
    // Count
    long countByPriceGreaterThan(BigDecimal price);
    
    // Exists
    boolean existsByName(String name);
    
    // Delete
    void deleteByName(String name);
}
```

#### Supported Keywords

| Keyword | Sample | JPQL |
|---------|--------|------|
| `And` | `findByNameAndPrice` | `... WHERE x.name = ?1 AND x.price = ?2` |
| `Or` | `findByNameOrPrice` | `... WHERE x.name = ?1 OR x.price = ?2` |
| `Is`, `Equals` | `findByName`, `findByNameIs` | `... WHERE x.name = ?1` |
| `Between` | `findByPriceBetween` | `... WHERE x.price BETWEEN ?1 AND ?2` |
| `LessThan` | `findByPriceLessThan` | `... WHERE x.price < ?1` |
| `LessThanEqual` | `findByPriceLessThanEqual` | `... WHERE x.price <= ?1` |
| `GreaterThan` | `findByPriceGreaterThan` | `... WHERE x.price > ?1` |
| `GreaterThanEqual` | `findByPriceGreaterThanEqual` | `... WHERE x.price >= ?1` |
| `After` | `findByCreatedAtAfter` | `... WHERE x.createdAt > ?1` |
| `Before` | `findByCreatedAtBefore` | `... WHERE x.createdAt < ?1` |
| `IsNull` | `findByDescriptionIsNull` | `... WHERE x.description IS NULL` |
| `IsNotNull` | `findByDescriptionIsNotNull` | `... WHERE x.description IS NOT NULL` |
| `Like` | `findByNameLike` | `... WHERE x.name LIKE ?1` |
| `NotLike` | `findByNameNotLike` | `... WHERE x.name NOT LIKE ?1` |
| `StartingWith` | `findByNameStartingWith` | `... WHERE x.name LIKE ?1%` |
| `EndingWith` | `findByNameEndingWith` | `... WHERE x.name LIKE %?1` |
| `Containing` | `findByNameContaining` | `... WHERE x.name LIKE %?1%` |
| `OrderBy` | `findByPriceOrderByNameAsc` | `... ORDER BY x.name ASC` |
| `Not` | `findByPriceNot` | `... WHERE x.price <> ?1` |
| `In` | `findByNameIn(Collection)` | `... WHERE x.name IN ?1` |
| `NotIn` | `findByNameNotIn(Collection)` | `... WHERE x.name NOT IN ?1` |
| `True` | `findByAvailableTrue` | `... WHERE x.available = TRUE` |
| `False` | `findByAvailableFalse` | `... WHERE x.available = FALSE` |
| `IgnoreCase` | `findByNameIgnoreCase` | `... WHERE UPPER(x.name) = UPPER(?1)` |

### 2. @Query with JPQL

Write queries explicitly using JPQL (Java Persistence Query Language).

```java
public interface PizzaRepository extends JpaRepository<Pizza, Long> {
    
    @Query("SELECT p FROM Pizza p WHERE p.price < :maxPrice")
    List<Pizza> findCheapPizzas(@Param("maxPrice") BigDecimal maxPrice);
    
    @Query("SELECT p FROM Pizza p WHERE p.name LIKE %:keyword% OR p.description LIKE %:keyword%")
    List<Pizza> searchByKeyword(@Param("keyword") String keyword);
    
    // JOIN FETCH to avoid N+1 problem
    @Query("SELECT c FROM Customer c JOIN FETCH c.orders WHERE c.id = :id")
    Optional<Customer> findByIdWithOrders(@Param("id") Long id);
    
    // Projection (selecting specific fields)
    @Query("SELECT p.name, p.price FROM Pizza p WHERE p.price < :maxPrice")
    List<Object[]> findCheapPizzaNames(@Param("maxPrice") BigDecimal maxPrice);
    
    // Count query
    @Query("SELECT COUNT(p) FROM Pizza p WHERE p.price > :minPrice")
    long countExpensivePizzas(@Param("minPrice") BigDecimal minPrice);
}
```

**JPQL vs SQL**:
- JPQL operates on **entities** (Java classes), not tables
- Use **entity names** and **property names**, not table/column names
- `SELECT p FROM Pizza p` (not `SELECT * FROM pizzas`)

### 3. @Query with Native SQL

Write native SQL queries (database-specific).

```java
@Query(value = "SELECT * FROM pizzas WHERE price < :maxPrice", nativeQuery = true)
List<Pizza> findCheapPizzasNative(@Param("maxPrice") BigDecimal maxPrice);

@Query(
    value = "SELECT * FROM pizzas WHERE MATCH(name, description) AGAINST (?1)",
    nativeQuery = true
)
List<Pizza> fullTextSearch(String searchTerm);
```

**When to use native queries**:
- Database-specific features (full-text search, window functions)
- Complex queries that are hard to express in JPQL
- Performance optimization

⚠️ **Downside**: Ties you to a specific database (less portable).

### 4. Modifying Queries

For UPDATE and DELETE operations.

```java
@Modifying
@Transactional
@Query("UPDATE Pizza p SET p.price = p.price * 1.1 WHERE p.id IN :ids")
int increasePrices(@Param("ids") List<Long> ids);

@Modifying
@Transactional
@Query("DELETE FROM Pizza p WHERE p.createdAt < :date")
int deleteOldPizzas(@Param("date") LocalDateTime date);
```

**Important**:
- Always use `@Modifying` for UPDATE/DELETE queries
- Must be `@Transactional`
- Returns number of affected rows

---

## 📄 Pagination and Sorting

### Sorting

```java
// In repository (derived query with sorting)
List<Pizza> findAllByOrderByPriceAsc();
List<Pizza> findAllByOrderByNameDesc();

// In service/controller (dynamic sorting)
Sort sort = Sort.by("price").ascending();
List<Pizza> pizzas = pizzaRepository.findAll(sort);

// Multiple sort criteria
Sort sort = Sort.by("price").ascending().and(Sort.by("name").descending());
List<Pizza> pizzas = pizzaRepository.findAll(sort);
```

### Pagination

```java
// In controller
@GetMapping
public ResponseEntity<Page<PizzaResponse>> getAllPizzas(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "asc") String direction) {
    
    Sort sort = direction.equalsIgnoreCase("desc") 
        ? Sort.by(sortBy).descending() 
        : Sort.by(sortBy).ascending();
    
    Pageable pageable = PageRequest.of(page, size, sort);
    Page<Pizza> pizzaPage = pizzaRepository.findAll(pageable);
    Page<PizzaResponse> responsePage = pizzaPage.map(pizzaMapper::toResponse);
    
    return ResponseEntity.ok(responsePage);
}
```

**Request**:
```bash
GET /api/pizzas?page=0&size=10&sortBy=price&direction=asc
```

**Response**:
```json
{
  "content": [
    {"id": 1, "name": "Margherita", "price": 8.50},
    {"id": 2, "name": "Pepperoni", "price": 9.50}
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {"sorted": true, "unsorted": false},
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalPages": 5,
  "totalElements": 47,
  "last": false,
  "first": true,
  "size": 10,
  "number": 0,
  "numberOfElements": 10
}
```

### Pagination with Custom Queries

```java
@Query("SELECT p FROM Pizza p WHERE p.price < :maxPrice")
Page<Pizza> findCheapPizzas(@Param("maxPrice") BigDecimal maxPrice, Pageable pageable);

// Usage
Pageable pageable = PageRequest.of(0, 10, Sort.by("price"));
Page<Pizza> pizzas = pizzaRepository.findCheapPizzas(new BigDecimal("10.00"), pageable);
```

---

## 🎁 Optional Handling

Spring Data JPA returns `Optional<T>` for single results that might not exist.

### Why Optional?

Prevents `NullPointerException` and makes absence explicit.

```java
// ❌ Old way - can return null
Pizza pizza = pizzaRepository.findById(1L);
if (pizza != null) {
    // Use pizza
}

// ✅ New way - explicit Optional handling
Optional<Pizza> pizzaOpt = pizzaRepository.findById(1L);
if (pizzaOpt.isPresent()) {
    Pizza pizza = pizzaOpt.get();
    // Use pizza
}
```

### Optional Patterns

#### 1. isPresent() and get()

```java
Optional<Pizza> pizzaOpt = pizzaRepository.findById(id);
if (pizzaOpt.isPresent()) {
    Pizza pizza = pizzaOpt.get();
    return ResponseEntity.ok(pizza);
} else {
    return ResponseEntity.notFound().build();
}
```

#### 2. orElse()

```java
Pizza pizza = pizzaRepository.findById(id)
    .orElse(new Pizza("Default", BigDecimal.ZERO));
```

#### 3. orElseThrow()

```java
Pizza pizza = pizzaRepository.findById(id)
    .orElseThrow(() -> new PizzaNotFoundException(id));
```

#### 4. map()

```java
return pizzaRepository.findById(id)
    .map(pizzaMapper::toResponse)
    .map(ResponseEntity::ok)
    .orElse(ResponseEntity.notFound().build());
```

#### 5. ifPresent()

```java
pizzaRepository.findById(id)
    .ifPresent(pizza -> System.out.println("Found: " + pizza.getName()));
```

#### 6. ifPresentOrElse()

```java
pizzaRepository.findById(id)
    .ifPresentOrElse(
        pizza -> System.out.println("Found: " + pizza.getName()),
        () -> System.out.println("Not found")
    );
```

---

## 🗂️ Database Configuration

### H2 Database Configuration

#### application.properties

```
spring.datasource.url=jdbc:h2:file:./data/pizzastore
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect

```

### DDL Generation Strategies

**`spring.jpa.hibernate.ddl-auto`** controls schema generation:

| Value | Behavior | Use Case |
|-------|----------|----------|
| `none` | Do nothing | Production  |
| `validate` | Validate schema matches entities, don't change anything | Production (with manual schema management) |
| `update` | Update schema if needed, never delete | Development (careful - can cause issues!) |
| `create` | Drop and recreate schema on startup | Testing |
| `create-drop` | Drop schema on shutdown | Testing |

**Recommendations**:
- **Development**: `update` or `create-drop`
- **Production**: `validate` or `none` 

### Sample Data

#### data.sql

```sql
-- Loaded automatically on startup if ddl-auto is create or create-drop
INSERT INTO pizzas (name, price, description) VALUES 
    ('Margherita', 8.50, 'Classic tomato and mozzarella'),
    ('Pepperoni', 9.50, 'Tomato, mozzarella, and pepperoni'),
    ('Hawaiian', 10.00, 'Tomato, mozzarella, ham, and pineapple'),
    ('Quattro Formaggi', 11.50, 'Four cheese blend');
```

**Configuration**:
```
spring.sql.init.mode=always  # Load data.sql on every startup
              # mode: never   # Don't load data.sql
```

---

## 🍕 PizzaStore: Adding Customer and Order

Let's expand PizzaStore with Customer and Order entities.

### Step 1: Create Customer Entity

```java
package be.vives.pizzastore.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
public class Customer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @Column(length = 20)
    private String phone;
    
    @Column(length = 200)
    private String address;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public Customer() {}
    
    public Customer(String name, String email) {
        this.name = name;
        this.email = email;
    }
    
    // Helper method to maintain bidirectional relationship
    public void addOrder(Order order) {
        orders.add(order);
        order.setCustomer(this);
    }
    
    public void removeOrder(Order order) {
        orders.remove(order);
        order.setCustomer(null);
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public List<Order> getOrders() { return orders; }
    public void setOrders(List<Order> orders) { this.orders = orders; }
}
```

### Step 2: Create Order Entity

```java
package be.vives.pizzastore.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;
    
    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;
    
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> orderLines = new ArrayList<>();
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (orderDate == null) {
            orderDate = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public Order() {}
    
    public Order(String orderNumber, Customer customer, OrderStatus status) {
        this.orderNumber = orderNumber;
        this.customer = customer;
        this.status = status;
        this.orderDate = LocalDateTime.now();
        this.totalAmount = BigDecimal.ZERO;
    }
    
    // Helper methods
    public void addOrderLine(OrderLine orderLine) {
        orderLines.add(orderLine);
        orderLine.setOrder(this);
        calculateTotalAmount();
    }
    
    public void removeOrderLine(OrderLine orderLine) {
        orderLines.remove(orderLine);
        orderLine.setOrder(null);
        calculateTotalAmount();
    }
    
    public void calculateTotalAmount() {
        totalAmount = orderLines.stream()
            .map(OrderLine::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    
    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    
    public List<OrderLine> getOrderLines() { return orderLines; }
    public void setOrderLines(List<OrderLine> orderLines) { this.orderLines = orderLines; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

### Step 3: Create OrderStatus Enum

```java
package be.vives.pizzastore.domain;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PREPARING,
    READY,
    DELIVERED,
    CANCELLED
}
```

### Step 4: Create OrderLine Entity

```java
package be.vives.pizzastore.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_lines")
public class OrderLine {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pizza_id", nullable = false)
    private Pizza pizza;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;
    
    // Constructors
    public OrderLine() {}
    
    public OrderLine(Pizza pizza, Integer quantity) {
        this.pizza = pizza;
        this.quantity = quantity;
        this.unitPrice = pizza.getPrice();
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    
    public Pizza getPizza() { return pizza; }
    public void setPizza(Pizza pizza) { this.pizza = pizza; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { 
        this.quantity = quantity;
        calculateSubtotal();
    }
    
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { 
        this.unitPrice = unitPrice;
        calculateSubtotal();
    }
    
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    
    private void calculateSubtotal() {
        if (unitPrice != null && quantity != null) {
            this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
```

### Step 5: Create Repositories

```java
package be.vives.pizzastore.repository;

import be.vives.pizzastore.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    Optional<Customer> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT c FROM Customer c JOIN FETCH c.orders WHERE c.id = :id")
    Optional<Customer> findByIdWithOrders(@Param("id") Long id);
}
```

```java
package be.vives.pizzastore.repository;

import be.vives.pizzastore.domain.Order;
import be.vives.pizzastore.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    List<Order> findByCustomerId(Long customerId);
    
    List<Order> findByStatus(OrderStatus status);
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    @Query("SELECT o FROM Order o JOIN FETCH o.orderLines ol JOIN FETCH ol.pizza WHERE o.id = :id")
    Optional<Order> findByIdWithOrderLines(@Param("id") Long id);
    
    @Query("SELECT o FROM Order o JOIN FETCH o.customer WHERE o.customer.id = :customerId")
    List<Order> findByCustomerIdWithCustomer(@Param("customerId") Long customerId);
}
```

### Step 6: Test Relationships

```java
@SpringBootTest
@Transactional
class OrderRepositoryTest {
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private PizzaRepository pizzaRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Test
    void shouldCreateOrderWithOrderLines() {
        // Given
        Customer customer = new Customer("John Doe", "john@example.com");
        customerRepository.save(customer);
        
        Pizza margherita = new Pizza("Margherita", new BigDecimal("8.50"), "Classic");
        Pizza pepperoni = new Pizza("Pepperoni", new BigDecimal("9.50"), "Spicy");
        pizzaRepository.saveAll(List.of(margherita, pepperoni));
        
        // When
        Order order = new Order("ORD-001", customer, OrderStatus.PENDING);
        order.addOrderLine(new OrderLine(margherita, 2));
        order.addOrderLine(new OrderLine(pepperoni, 1));
        orderRepository.save(order);
        
        // Then
        Order savedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(savedOrder.getOrderLines()).hasSize(2);
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo("26.50"); // 8.50*2 + 9.50*1
    }
}
```

---

## 💡 Tips & Best Practices

### 1. Always Use Bidirectional Helper Methods

```java
public class Customer {
    public void addOrder(Order order) {
        orders.add(order);
        order.setCustomer(this);  // Maintain both sides!
    }
}
```

### 2. Use @Transactional in Service Layer

```java
@Service
@Transactional  // All methods are transactional
public class OrderService {
    // ...
}
```

### 3. Be Careful with toString(), equals(), hashCode()

Don't include bidirectional relationships:

```java
@Override
public String toString() {
    return "Order{id=" + id + ", orderNumber='" + orderNumber + "'}";
    // Don't include customer or orderLines!
}
```

### 4. Use JOIN FETCH to Avoid N+1 Problem

```java
// ❌ Bad: N+1 queries
List<Order> orders = orderRepository.findAll();
orders.forEach(order -> {
    System.out.println(order.getCustomer().getName());  // Extra query for each order!
});

// ✅ Good: Single query with JOIN FETCH
@Query("SELECT o FROM Order o JOIN FETCH o.customer")
List<Order> findAllWithCustomer();
```

### 5. Use Proper Fetch Types

```java
// ✅ Good
@ManyToOne(fetch = FetchType.LAZY)  // Always LAZY for @ManyToOne
private Customer customer;

@OneToMany(fetch = FetchType.LAZY)  // Always LAZY for collections
private List<Order> orders;
```

---

## 🎓 Summary

### What We Learned

1. **ORM, JPA, and Spring Data JPA**
   - ORM bridges objects and relational databases
   - JPA is the specification, Hibernate is the implementation
   - Spring Data JPA provides repository abstractions

2. **Entity Annotations**
   - `@Entity`, `@Table`, `@Id`, `@GeneratedValue`
   - `@Column` for fine-grained control
   - `@Enumerated`, `@Transient`
   - Lifecycle callbacks: `@PrePersist`, `@PreUpdate`

3. **Entity Relationships**
   - `@OneToOne`: One-to-one relationship
   - `@OneToMany` / `@ManyToOne`: One-to-many relationship
   - `@ManyToMany`: Many-to-many relationship
   - Always maintain bidirectional relationships with helper methods

4. **Fetch Types**
   - `LAZY`: Load on-demand (better performance)
   - `EAGER`: Load immediately (can cause issues)
   - Use LAZY by default, override with JOIN FETCH when needed

5. **Cascade Types**
   - Control how operations propagate to related entities
   - Use `CascadeType.ALL` + `orphanRemoval = true` for parent-child
   - Be careful with `CascadeType.REMOVE`

6. **Repository Interfaces**
   - `JpaRepository`: Most feature-rich (recommended)
   - `CrudRepository`: Basic CRUD
   - `PagingAndSortingRepository`: Adds pagination

7. **Custom Queries**
   - Derived queries: Naming conventions generate queries
   - `@Query` with JPQL: Write explicit queries
   - Native SQL: Database-specific features
   - Pagination and sorting support

8. **Best Practices**
   - Use LAZY fetching by default
   - Use JOIN FETCH to avoid N+1 problems
   - Use DTOs to avoid lazy loading exceptions
   - Be careful with cascade types
   - Use bidirectional helper methods

---

## 📖 Additional Resources

- [Spring Data JPA Documentation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [JPA Specification (JSR 338)](https://jcp.org/en/jsr/detail?id=338)
- [Hibernate Documentation](https://hibernate.org/orm/documentation/)
- [Baeldung: Spring Data JPA](https://www.baeldung.com/the-persistence-layer-with-spring-data-jpa)
- [Vlad Mihalcea's Blog](https://vladmihalcea.com/) - JPA & Hibernate performance tips
- [JPA Buddy Plugin](https://www.jpa-buddy.com/) - IntelliJ IDEA plugin for JPA development

---

**Congratulations!** 🎉 You now have a deep understanding of JPA and Spring Data JPA. You can create complex data models with proper relationships and query them efficiently!


---

## 🚀 Runnable Project

A complete, runnable Spring Boot project demonstrating **all JPA relationships** from this lesson is available in:

**`pizzastore-with-relationships/`**

The project includes:
- ✅ **@OneToOne**: Pizza ↔ NutritionalInfo
- ✅ **@ManyToOne** / **@OneToMany**: Customer → Orders
- ✅ **@ManyToMany**: Customer ↔ Pizza (favorites)
- ✅ Complete database schema with sample data
- ✅ Custom queries with JOIN FETCH
- ✅ Bidirectional helper methods

See the project README for detailed setup instructions and SQL queries to explore the relationships.


