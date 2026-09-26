# Lesson 6a: Spring Data JPA

**Java Persistence API and Spring Data JPA, applied to the PizzaStore domain model**

---

> This lesson assumes you've read [Lesson 6: Spring Data with Spring Boot](../lesson-06-spring-data/README.md) — the repository proxy pattern, query-method naming conventions, and exception translation described there apply here too and aren't repeated.

## 📋 Learning Objectives

By the end of this lesson, you will be able to:
- Create JPA entities with proper annotations
- Implement JPA Auditing for automatic tracking of entity changes
- Implement entity relationships: `@OneToOne`, `@OneToMany`, `@ManyToOne`, `@ManyToMany`
- Choose between `LAZY` and `EAGER` fetch types
- Use cascade types effectively
- Leverage the Spring Data JPA repository interface hierarchy
- Write custom queries using property expressions, `@Query`, and JPQL
- Implement pagination and sorting
- Handle `Optional` results properly
- Configure DDL generation strategies

---

## 📚 Table of Contents

- [📋 Learning Objectives](#-learning-objectives)
- [🗃️ From JPA to Spring Data JPA](#️-from-jpa-to-spring-data-jpa)
- [📦 JPA Entities in Depth](#-jpa-entities-in-depth)
- [🕐 JPA Auditing](#-jpa-auditing)
- [🔗 Entity Relationships](#-entity-relationships)
- [⏳ Fetch Types: LAZY vs EAGER](#-fetch-types-lazy-vs-eager)
- [🌊 Cascade Types](#-cascade-types)
- [🗄️ The JpaRepository Interface](#️-the-jparepository-interface)
- [🔍 Custom Queries](#-custom-queries)
- [🧮 DTO Projections](#-dto-projections)
- [📄 Pagination and Sorting](#-pagination-and-sorting)
- [🎁 Optional Handling](#-optional-handling)
- [🧪 Testing](#-testing)
- [🗂️ Database Configuration](#️-database-configuration)
- [🚀 Runnable Project](#-runnable-project)
- [🎓 Summary](#-summary)
- [📖 Additional Resources](#-additional-resources)

---

## 🗃️ From JPA to Spring Data JPA

**JPA (Java Persistence API)** is a specification for Object-Relational Mapping (ORM) in Java — it defines annotations (`@Entity`, `@Id`, …) and an `EntityManager` API, but it is vendor-neutral: **Hibernate** is the JPA implementation Spring Boot uses by default (EclipseLink and OpenJPA are alternatives).

**Spring Data JPA** builds on top of plain JPA to make it easier to use, by adding the repository abstraction from [Lesson 6](../lesson-06-spring-data/README.md#the-repository-abstraction):

```
Application Code
       ↓
Spring Data JPA      ← repository abstraction (JpaRepository, query derivation, auditing)
       ↓
JPA API              ← standard specification (@Entity, EntityManager, JPQL)
       ↓
Hibernate             ← JPA implementation
       ↓
JDBC                 ← low-level database access
       ↓
Database (H2, PostgreSQL, MySQL, …)
```

---

## 📦 JPA Entities in Depth

### Basic Entity Anatomy

```java
@Entity                              // 1. Marks as JPA entity
@Table(name = "pizzas")             // 2. Maps to database table
public class Pizza {

    @Id                              // 3. Primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // 4. ID generation: Auto-increment
    private Long id;

    @Column(nullable = false, length = 100)  // 5. Column mapping
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 1000)
    private String description;

    @Transient                         // 6. Not persisted
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
@GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment (used by PizzaStore, H2/PostgreSQL)
private Long id;

@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE)  // Sequence (Oracle, PostgreSQL)
private Long id;

@Id
@GeneratedValue(strategy = GenerationType.UUID)      // UUID (Java 25+)
private UUID id;
```

**Recommendation**: Use `IDENTITY` for H2, MySQL, PostgreSQL — this is what every PizzaStore entity uses.

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
    columnDefinition = "TEXT"      // Custom SQL type
)
private String name;
```

#### @Enumerated

Maps enum types — see `OrderStatus` in the PizzaStore project.

```java
// domain/OrderStatus.java
public enum OrderStatus {
    PENDING, CONFIRMED, PREPARING, READY, DELIVERED, CANCELLED
}

// domain/Order.java
@Enumerated(EnumType.STRING)  // Save as "PENDING", "CONFIRMED", etc.
@Column(nullable = false, length = 20)
private OrderStatus status;

// ❌ DON'T USE EnumType.ORDINAL
@Enumerated(EnumType.ORDINAL) // Save as 0, 1, 2 (breaks if enum order changes!)
private OrderStatus status;
```

**Recommendation**: Always use `EnumType.STRING` for maintainability and clarity.

---

## 🕐 JPA Auditing

**JPA Auditing** automatically tracks **who** created/modified an entity and **when** it happened, without a manual `@PrePersist`/`@PreUpdate` callback.

```java
@Entity
@Table(name = "pizzas")
@EntityListeners(AuditingEntityListener.class)  // Enable auditing for this entity
public class Pizza {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by")
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
}
```

| Annotation | Description |
|---|---|
| `@CreatedDate` | When the entity was created |
| `@CreatedBy` | Who created the entity |
| `@LastModifiedDate` | When last updated |
| `@LastModifiedBy` | Who last updated |

Two things have to be in place for this to work:

1. **`@EnableJpaAuditing` on a configuration class** turns auditing on for the whole application. In the full PizzaStore project this lives in `config/JpaConfig.java`; the copy of it in this lesson's project ([see below](#-runnable-project)) is the same.
2. **An `AuditorAware<String>` bean** (`config/AuditorAwareImpl.java` in the full project, which reads the currently authenticated user from Spring Security) is what fills in `@CreatedBy`/`@LastModifiedBy`. This lesson's project doesn't include it — there is no security layer here — so `createdBy`/`updatedBy` simply stay `null`. `@CreatedDate`/`@LastModifiedDate` work regardless, since they don't need an `AuditorAware` bean.

---

## 🔗 Entity Relationships

PizzaStore's domain model (`Customer`, `Pizza`, `Order`, `OrderLine`, `NutritionalInfo`) demonstrates every relationship type JPA supports:

| Relationship | PizzaStore Example | Database Implementation |
|---|---|---|
| `@OneToOne` | `Pizza` ↔ `NutritionalInfo` | Foreign key in either table |
| `@OneToMany` / `@ManyToOne` | `Customer` → `Order`, `Order` → `OrderLine` | Foreign key on the "many" side |
| `@ManyToMany` | `Customer` ↔ `Pizza` (favorites) | Join table |

### @OneToOne — Pizza ↔ NutritionalInfo

```java
// domain/Pizza.java
@OneToOne(mappedBy = "pizza", cascade = CascadeType.ALL, orphanRemoval = true)
private NutritionalInfo nutritionalInfo;

// domain/NutritionalInfo.java
@OneToOne
@JoinColumn(name = "pizza_id", nullable = false)
private Pizza pizza;
```

- `@JoinColumn` marks the **owning side** (the side with the foreign key) — here, `NutritionalInfo`.
- `mappedBy` marks the **inverse side** — `Pizza` doesn't have a `pizza_id` column itself; it just knows how to find its `NutritionalInfo` through the relationship.
- `orphanRemoval = true` deletes the `NutritionalInfo` row automatically if it's ever detached from its `Pizza`.

### @OneToMany / @ManyToOne — Customer → Order, Order → OrderLine

```java
// domain/Customer.java
@OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Order> orders = new ArrayList<>();

public void addOrder(Order order) {
    orders.add(order);
    order.setCustomer(this);
}
```

```java
// domain/Order.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "customer_id", nullable = false)
private Customer customer;

@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
private List<OrderLine> orderLines = new ArrayList<>();
```

- The `@ManyToOne` side (`Order.customer`, `OrderLine.order`) always owns the foreign key (`@JoinColumn`).
- The `@OneToMany` side (`Customer.orders`, `Order.orderLines`) is always the inverse side (`mappedBy`).
- PizzaStore uses **helper methods** (`addOrder`/`removeOrder`) to keep both sides of a bidirectional relationship in sync — forgetting to do this is one of the most common JPA bugs.

### @ManyToMany — Customer ↔ Pizza (favorites)

```java
// domain/Customer.java
@ManyToMany
@JoinTable(
        name = "customer_favorite_pizzas",
        joinColumns = @JoinColumn(name = "customer_id"),
        inverseJoinColumns = @JoinColumn(name = "pizza_id")
)
private Set<Pizza> favoritePizzas = new HashSet<>();
```

```java
// domain/Pizza.java
@ManyToMany(mappedBy = "favoritePizzas")
private Set<Customer> favoritedByCustomers = new HashSet<>();
```

- `@JoinTable` on `Customer` defines the join table `customer_favorite_pizzas`; `Pizza` uses `mappedBy` for the inverse side.
- PizzaStore uses `Set` (not `List`) for `@ManyToMany` — no duplicates, and a `HashSet` avoids the ordering overhead a `List` would imply for an unordered relationship.

**When to avoid `@ManyToMany`**: if the join needs its own data (a rating, a comment, a timestamp), model it as its own entity instead — e.g. `PizzaRating` with a `@ManyToOne` to both `Customer` and `Pizza` — rather than trying to attach extra columns to a `@ManyToMany` join table.

---

## ⏳ Fetch Types: LAZY vs EAGER

Should related entities load immediately with their parent, or only when accessed?

| Relationship | Default Fetch Type |
|---|---|
| `@OneToOne` | `EAGER` |
| `@ManyToOne` | `EAGER` |
| `@OneToMany` | `LAZY` |
| `@ManyToMany` | `LAZY` |

**`FetchType.EAGER`** loads related entities immediately — simple, but risks fetching far more data than needed and can trigger the N+1 query problem.

**`FetchType.LAZY`** (PizzaStore's `Order.customer` uses this explicitly) loads related entities only when accessed — better performance, but can throw `LazyInitializationException` if accessed after the persistence session has closed.

**Best practice**: default to `LAZY` for collections, and override with `JOIN FETCH` in a custom query when you know you'll need the association — exactly what PizzaStore's repositories do:

```java
// repository/CustomerRepository.java
@Query("SELECT c FROM Customer c JOIN FETCH c.orders WHERE c.id = :id")
Optional<Customer> findByIdWithOrders(@Param("id") Long id);
```

---

## 🌊 Cascade Types

Should an operation on the parent (save, delete, …) propagate to its children?

```java
public enum CascadeType {
    PERSIST, MERGE, REMOVE, REFRESH, DETACH, ALL
}
```

PizzaStore uses `CascadeType.ALL` with `orphanRemoval = true` on every true parent-child relationship (`Customer.orders`, `Order.orderLines`, `Pizza.nutritionalInfo`):

```java
@OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Order> orders = new ArrayList<>();
```

- Saving the `Customer` also saves any new `Order`s in the collection (`PERSIST`).
- Deleting the `Customer` also deletes all their `Order`s (`REMOVE`).
- Removing an `Order` from the collection (without deleting the `Customer`) also deletes that `Order` row (`orphanRemoval`) — this is the difference between `orphanRemoval` and `CascadeType.REMOVE`: the latter only fires when the *parent* is deleted.

**Be careful with cascading on the owning (`@ManyToOne`) side** — PizzaStore's `Order.customer` and `OrderLine.pizza`/`OrderLine.order` deliberately have **no** cascade, so deleting an `Order` never accidentally deletes the `Customer` or `Pizza` it references. Likewise, `Customer.favoritePizzas` has no cascade — removing a favorite should never delete the `Pizza` itself.

---

## 🗄️ The JpaRepository Interface

Spring Data JPA's interfaces form a hierarchy:

```
Repository<T, ID>                    (marker interface - empty)
    ↓
CrudRepository<T, ID>                (save, findById, findAll, delete, …)
    ↓
PagingAndSortingRepository<T, ID>    (+ findAll(Sort), findAll(Pageable))
    ↓
JpaRepository<T, ID>                 (+ List instead of Iterable, batch ops, flush())
```

PizzaStore extends `JpaRepository` everywhere — it's the most feature-rich option and the standard choice for building REST APIs:

```java
// repository/PizzaRepository.java
@Repository
public interface PizzaRepository extends JpaRepository<Pizza, Long> {
    // findAll(), findById(), save(), deleteById(), count(), … already included
}
```

---

## 🔍 Custom Queries

### 1. Query Methods (Derived Queries)

Following the naming conventions from [Lesson 6](../lesson-06-spring-data/README.md#query-methods-and-naming-conventions), PizzaStore's `PizzaRepository` defines several derived queries:

```java
// repository/PizzaRepository.java
Optional<Pizza> findByName(String name);

List<Pizza> findByPriceLessThan(BigDecimal maxPrice);

List<Pizza> findByPriceGreaterThanEqual(BigDecimal minPrice);

List<Pizza> findByNameContainingIgnoreCase(String keyword);

List<Pizza> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
```

### 2. @Query with JPQL

For queries that don't fit the naming convention, `@Query` lets you write JPQL directly. JPQL operates on **entities and their properties**, not on tables and columns:

```java
// repository/PizzaRepository.java
@Query("SELECT p FROM Pizza p WHERE p.name LIKE %:keyword% OR p.description LIKE %:keyword%")
List<Pizza> searchByKeyword(@Param("keyword") String keyword);

@Query("SELECT p FROM Pizza p LEFT JOIN FETCH p.nutritionalInfo WHERE p.id = :id")
Optional<Pizza> findByIdWithNutritionalInfo(@Param("id") Long id);
```

### 3. @Query with Native SQL

For database-specific features (full-text search, window functions), `nativeQuery = true` lets you drop down to raw SQL — at the cost of portability across databases.

```java
@Query(value = "SELECT * FROM pizzas WHERE price < :maxPrice", nativeQuery = true)
List<Pizza> findCheapPizzasNative(@Param("maxPrice") BigDecimal maxPrice);
```

### 4. Modifying Queries

`UPDATE`/`DELETE` queries need `@Modifying`, must run inside a transaction, and return the number of affected rows:

```java
@Modifying
@Query("UPDATE Pizza p SET p.price = p.price * 1.1 WHERE p.id IN :ids")
int increasePrices(@Param("ids") List<Long> ids);
```

**The transaction usually comes from the caller.** A `@Transactional` service method starts the transaction, and the repository call joins it (transactions are explained in detail in [Lesson 7](../lesson-07-dtos-mappers/README.md#-transactions)). Adding `@Transactional` on the repository method itself is optional. It's a safety net for callers that don't start a transaction, such as a test or a `CommandLineRunner`. Without any transaction you get `TransactionRequiredException: Executing an update/delete query`.

---

## 🧮 DTO Projections

Every query so far returns entities (`Pizza`, `Order`, …) or collections of them. But not everything you query for *is* an entity — sometimes you want a statistic computed *by the database*, not assembled afterwards in Java. Spring Data JPA calls the result of such a query a **projection**: a query result mapped into a shape that is not a managed `@Entity`.

There are two ways to write one:

**1. Interface-based projection** — declare an interface with getter methods matching the columns you want; Spring Data generates a proxy at runtime:

```java
public interface PizzaNameOnly {
    String getName();
    BigDecimal getPrice();
}

// repository/PizzaRepository.java
List<PizzaNameOnly> findByAvailableTrue();
```

**2. DTO / record projection** — write the target type yourself (a `record` is a natural fit) and construct it directly in JPQL with `SELECT new fully.qualified.ClassName(...)`:

```java
// repository/projection/PizzaSalesStatistics.java
public record PizzaSalesStatistics(
        String pizzaName,
        Long timesOrdered,
        Long totalQuantitySold,
        BigDecimal totalRevenue
) {
}

// repository/OrderRepository.java
@Query("""
        SELECT new be.vives.pizzastore.repository.projection.PizzaSalesStatistics(
            ol.pizza.name, COUNT(ol), SUM(ol.quantity), SUM(ol.subtotal))
        FROM Order o JOIN o.orderLines ol
        GROUP BY ol.pizza.name
        ORDER BY SUM(ol.quantity) DESC
        """)
List<PizzaSalesStatistics> findPizzaSalesStatistics();
```

`PizzaSalesStatistics` is a plain record, not an `@Entity` — Hibernate never loads full `Order`/`OrderLine` rows into memory to compute this; the `COUNT`/`SUM`/`GROUP BY` run in the database, and only the aggregated numbers cross the wire.

**When to reach for a projection instead of the full entity:**
- A statistic or aggregate (totals, counts, averages) that doesn't correspond to any single entity.
- A list view that only needs a few columns — skips loading (and lazy-fetching) the rest of a large entity graph.
- The consumer only needs read-only data and will never turn it back into a persisted entity.

**Don't confuse this with [Lesson 7](../lesson-07-dtos-mappers/README.md)'s DTOs**: there, a full entity is loaded first and then mapped to a DTO afterwards (in the mapper/service layer) — the mapping happens in Java, after the query. Here, the *query itself* never produces an entity in the first place; the projection is what the database handed back.

---

## 📄 Pagination and Sorting

```java
// Dynamic sorting
Sort sort = Sort.by("price").ascending();
List<Pizza> pizzas = pizzaRepository.findAll(sort);

// Pagination
Pageable pageable = PageRequest.of(0, 10, Sort.by("price"));
Page<Pizza> page = pizzaRepository.findAll(pageable);
```

`Page<T>` carries the content plus metadata (`totalPages`, `totalElements`, `first`, `last`, …) — essential for any list endpoint on a growing table. Pagination and sorting also work with derived query methods and custom `@Query` methods, simply by adding a `Pageable` parameter. PizzaStore's `OrderRepository` has both variants of the same query:

```java
List<Order> findByCustomerId(Long customerId);                     // all orders of a customer
Page<Order> findByCustomerId(Long customerId, Pageable pageable);  // one page of them
```

With the `Pageable` variant the filtering, sorting and `LIMIT`/`OFFSET` all happen in the database. Don't be tempted to page over *all* orders with `findAll(pageable)` and filter the result in Java afterwards: the pages would contain fewer (or no) matching orders and `totalElements` would count the wrong rows.

---

## 🎁 Optional Handling

Spring Data JPA returns `Optional<T>` for single results that might not exist, so absence is explicit instead of a bare `null`:

```java
// ❌ Old way - can return null
Pizza pizza = pizzaRepository.findById(1L);

// ✅ Explicit Optional handling
Optional<Pizza> pizzaOpt = pizzaRepository.findById(1L);

pizzaOpt.orElseThrow(() -> new PizzaNotFoundException(id));   // throw if missing

pizzaOpt.map(pizzaMapper::toResponse)                         // transform if present
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());

pizzaOpt.ifPresent(p -> System.out.println("Found: " + p.getName())); // side effect if present
```

---

## 🧪 Testing

The book's own version of this section (Listings 6-14 and 6-15) goes on to write repository tests at this point — saving and querying entities, then asserting on the result, plus a pagination test. This course doesn't repeat that here: **[Lesson 11: Testing Spring Boot Applications](../lesson-11-testing/README.md)** already covers exactly this ground for the real PizzaStore project, with `PizzaRepositoryTest`, `OrderRepositoryTest`, and `CustomerRepositoryTest`.

One difference worth knowing about if you compare the two: the book uses `@SpringBootTest` + `@Transactional` for its repository tests, not `@DataJpaTest`, specifically because its Management CRM tests need to coordinate two repositories (`CustomerRepository` and `CompanyRepository`) in the same test. [Lesson 11](../lesson-11-testing/README.md) uses `@DataJpaTest` — the narrower Spring Boot test slice that loads only JPA components against an in-memory database — because PizzaStore's repository tests don't need that broader context. Both are correct; reach for `@DataJpaTest` first, and only widen to `@SpringBootTest` when a test genuinely needs beans outside the JPA slice.

---

## 🗂️ Database Configuration

### application.properties (this lesson's project)

```properties
spring.datasource.url=jdbc:h2:mem:pizzastore_jpa
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

# Load data.sql after Hibernate creates the schema
spring.jpa.defer-datasource-initialization=true
```

### DDL Generation Strategies

`spring.jpa.hibernate.ddl-auto` controls schema generation:

| Value | Behavior | Use Case |
|---|---|---|
| `none` | Do nothing | Production |
| `validate` | Validate schema matches entities, change nothing | Production (manual schema management) |
| `update` | Update schema if needed, never delete | Development |
| `create` | Drop and recreate schema on startup | Testing |
| `create-drop` | Like `create`, and also drop on shutdown | Testing / this lesson's project |

### Sample Data (data.sql)

```sql
INSERT INTO pizzas (name, description, price, image_url, available, created_at, updated_at) VALUES
('Margherita', 'Classic tomato sauce, fresh mozzarella, basil, and extra virgin olive oil', 8.99, '...', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

`spring.jpa.defer-datasource-initialization=true` is what makes `data.sql` run *after* Hibernate has created the tables from the `@Entity` classes — without it, the inserts would run first and fail against tables that don't exist yet.

---

## 🚀 Runnable Project

**`pizzastore-jpa/`** is an exact copy of the final PizzaStore project's `domain` and `repository` packages — nothing more. No controllers, no services, no security, no DTOs — just the entities and the repositories that persist them, so you can focus entirely on the JPA/Spring Data layer.

```
pizzastore-jpa/
└── src/main/java/be/vives/pizzastore/
    ├── PizzaStoreApplication.java
    ├── config/
    │   └── JpaConfig.java          (@EnableJpaAuditing - needed for @CreatedDate to work)
    ├── domain/
    │   ├── Customer.java
    │   ├── NutritionalInfo.java
    │   ├── Order.java
    │   ├── OrderLine.java
    │   ├── OrderStatus.java
    │   ├── Pizza.java
    │   └── Role.java
    └── repository/
        ├── CustomerRepository.java
        ├── OrderRepository.java
        ├── PizzaRepository.java
        └── projection/
            └── PizzaSalesStatistics.java   (DTO projection, not an entity — see DTO Projections above)
```

`config/JpaConfig.java` is the one file here that goes beyond "domain and repository" — it's required infrastructure, not a feature: without `@EnableJpaAuditing`, saving a fresh `Pizza`/`Order`/`Customer` would fail, because `createdAt` is a non-null column populated only through JPA Auditing.

### Running It

```bash
cd lesson-06a-spring-data-jpa/pizzastore-jpa
mvn spring-boot:run
```

Then open the H2 console at <http://localhost:8080/h2-console> with JDBC URL `jdbc:h2:mem:pizzastore_jpa`, user `sa`, empty password — `data.sql` has already loaded a handful of pizzas, customers, and orders. From a test or a scratch `CommandLineRunner`, try the repository methods from this lesson directly, e.g. `pizzaRepository.findByPriceBetween(new BigDecimal("9.00"), new BigDecimal("12.00"))`.

---

## 🎓 Summary

1. **JPA** is the specification, **Hibernate** the implementation, **Spring Data JPA** the repository abstraction on top.
2. **Entity annotations** (`@Entity`, `@Column`, `@Enumerated`) map Java classes to tables.
3. **JPA Auditing** (`@CreatedDate`, `@CreatedBy`, …) needs `@EnableJpaAuditing`, and `@CreatedBy`/`@LastModifiedBy` additionally need an `AuditorAware` bean.
4. **Relationships** (`@OneToOne`, `@OneToMany`/`@ManyToOne`, `@ManyToMany`) always have an owning side (`@JoinColumn`) and an inverse side (`mappedBy`).
5. Default to **`LAZY`** fetching for collections; use `JOIN FETCH` when you know you need the association.
6. **`CascadeType.ALL` + `orphanRemoval`** for true parent-child relationships; no cascade on the owning `@ManyToOne` side.
7. **`JpaRepository`** is the recommended base interface; derived queries, `@Query`, pagination, and `Optional` cover almost every data-access need.
8. Not every query result needs to be an entity — **DTO/record projections** (`SELECT new ...(...)`) let the database compute an aggregate or a narrow view directly, without loading full entity graphs first.

---

## 📖 Additional Resources

- [Spring Data JPA Documentation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [JPA Specification (JSR 338)](https://jcp.org/en/jsr/detail?id=338)
- [Hibernate Documentation](https://hibernate.org/orm/documentation/)
- [Baeldung: Spring Data JPA](https://www.baeldung.com/the-persistence-layer-with-spring-data-jpa)

**Note on the book**: This lesson corresponds to *Pro Spring Boot 4*, Chapter 6: *Spring Data with Spring Boot* — specifically the *Spring Data JPA with the Management CRM* section (entity annotations, relationships, the `JpaRepository` hierarchy, custom queries, and pagination). Two things the book covers at this point are deliberately not repeated here: its repository-testing walkthrough (see [Testing](#-testing) above — covered instead by [Lesson 11](../lesson-11-testing/README.md)), and its brief closing note on AOT-optimized repositories for GraalVM native images (*Advanced AOT: Optimizing for Native Images*), which points forward to the book's own Chapter 14 on Ahead-of-Time compilation — out of scope for this course.

The book itself never teaches DTO/record projections as their own topic — it shows exactly one `SELECT new ...CustomerSummary(...)` query (Listing 6-16), and only as a vehicle to introduce `@RegisterReflectionForBinding` (an AOT/native-image reflection hint, itself out of scope here per the previous paragraph). The dedicated [DTO Projections](#-dto-projections) section above goes beyond the book: it names both projection styles (interface-based and DTO/record), explains *why* you'd reach for one instead of loading full entities, and gives PizzaStore its own worked example (`OrderRepository.findPizzaSalesStatistics()`), because this is a genuinely useful, commonly-needed Spring Data JPA feature the book only gestures at in passing.

---

**Great work!** 🎉 You've built PizzaStore's relational data layer. Continue to [Lesson 6b: Spring Data MongoDB](../lesson-06b-spring-data-mongodb/README.md) to see the exact same domain model re-expressed as documents.
