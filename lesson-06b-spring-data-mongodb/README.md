# Lesson 6b: Spring Data MongoDB

**The same PizzaStore domain model, re-expressed as MongoDB documents**

---

> This lesson assumes you've read [Lesson 6: Spring Data with Spring Boot](../lesson-06-spring-data/README.md) (repository proxy pattern, query-method naming conventions) and, ideally, [Lesson 6a: Spring Data JPA](../lesson-06a-spring-data-jpa/README.md) — this lesson constantly compares against the relational version you just built.

## 📋 Learning Objectives

By the end of this lesson, you will be able to:
- Explain how MongoDB's document model differs from a relational schema
- Map a domain model to `@Document` classes with `@Id`, `@Indexed`, and Spring Data auditing
- Decide when to **embed** and when to **reference** related data in a document database
- Write derived query methods and MongoDB-flavored `@Query` methods
- Explain why JPA's `JOIN FETCH` has no equivalent in Spring Data MongoDB, and what to do instead
- Run a Spring Boot application against a local MongoDB instance

---

## 📚 Table of Contents

- [📋 Learning Objectives](#-learning-objectives)
- [🍃 From Tables to Documents](#-from-tables-to-documents)
- [⭐ Key Features of Spring Data MongoDB](#-key-features-of-spring-data-mongodb)
- [🗺️ Mapping the PizzaStore Domain Model](#️-mapping-the-pizzastore-domain-model)
- [🧱 Embedding vs. Referencing](#-embedding-vs-referencing)
- [🗄️ The MongoRepository Interface](#️-the-mongorepository-interface)
- [🔍 Custom Queries](#-custom-queries)
- [🕐 Auditing](#-auditing)
- [📇 Indexes](#-indexes)
- [🚫 No Joins: What Replaces JOIN FETCH](#-no-joins-what-replaces-join-fetch)
- [🧪 Testing with Testcontainers](#-testing-with-testcontainers)
- [🚀 Runnable Project](#-runnable-project)
- [⚖️ Comparison: JPA vs. MongoDB](#️-comparison-jpa-vs-mongodb)
- [🎓 Summary](#-summary)
- [📖 Additional Resources](#-additional-resources)

---

## 🍃 From Tables to Documents

MongoDB is the leading document-oriented NoSQL database. Instead of rows in tables connected by foreign keys, it stores data as flexible, JSON-like **BSON documents**. There's no fixed schema to migrate every time the domain model changes, which makes MongoDB a natural fit for rapidly evolving domain models — at the cost of giving up the relational database's joins and multi-table integrity constraints.

Spring Data MongoDB gives you the exact same programming model as Spring Data JPA from [Lesson 6a](../lesson-06a-spring-data-jpa/README.md) — a repository interface, query-method derivation, auditing annotations — over a fundamentally different storage engine underneath.

## ⭐ Key Features of Spring Data MongoDB

- **`MongoRepository`** — the module's equivalent of `JpaRepository`, adding MongoDB-specific operations like geo-spatial queries.
- **`MongoTemplate`** — a lower-level, fluent API for complex aggregations and custom updates, comparable to `JdbcClient` for relational access.
- **GridFS support** — for storing files (images, videos) that exceed MongoDB's per-document BSON size limit.
- **Multi-document transactions** — MongoDB supports ACID transactions across documents and collections, so complex business logic isn't automatically off the table just because you're using a document store.

Best practices the book calls out, and that this lesson's project follows:

- **Use records or plain immutable-leaning classes** — MongoDB documents map well to immutability. This lesson's project keeps mutable classes instead, matching the style of the final PizzaStore project (see the [comparison table](#-comparison-jpa-vs-mongodb) below for why).
- **Implement `Persistable`** when you assign IDs yourself, to avoid an extra existence check before every insert. This lesson lets MongoDB generate `ObjectId`-backed string IDs instead, so it doesn't come up — worth knowing about if you ever assign your own IDs.
- **Manage indexes explicitly** with `@Indexed`, so query performance doesn't silently degrade as a collection grows.

## 🗺️ Mapping the PizzaStore Domain Model

Every entity from [Lesson 6a](../lesson-06a-spring-data-jpa/README.md) has a MongoDB counterpart in this lesson's project, with the same field names wherever the mapping allows it:

| JPA (Lesson 6a) | MongoDB (this lesson) | What changed |
|---|---|---|
| `@Entity @Table(name = "pizzas") Pizza` | `@Document(collection = "pizzas") Pizza` | `Long id` → `String id`; `favoritedByCustomers` dropped |
| `@Entity @Table(name = "nutritional_info") NutritionalInfo` | plain embedded class `NutritionalInfo` | No longer its own table/collection — embedded inside `Pizza` |
| `@Entity @Table(name = "customers") Customer` | `@Document(collection = "customers") Customer` | `orders` list dropped; `favoritePizzas` (join table) → `favoritePizzaIds` (`List<String>`) |
| `@Entity @Table(name = "orders") Order` | `@Document(collection = "orders") Order` | `customer` object → `customerId` (`String`); `orderLines` stays, now embedded |
| `@Entity @Table(name = "order_lines") OrderLine` | plain embedded class `OrderLine` | No longer its own table — embedded inside `Order`; `pizza` object → `pizzaId` (`String`) |
| `OrderStatus`, `Role` (enums) | `OrderStatus`, `Role` (enums) | Unchanged — Spring Data MongoDB stores enums as their name (`"PENDING"`, …) automatically, no `@Enumerated` needed |

```java
// domain/Pizza.java
@Document(collection = "pizzas")
public class Pizza {

    @Id
    private String id;

    private String name;
    private BigDecimal price;
    private String description;

    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;

    private NutritionalInfo nutritionalInfo;   // embedded, see below
}
```

Compare that to the JPA version's `@OneToOne` + separate `nutritional_info` table from Lesson 6a — the MongoDB version needs neither a second collection nor a foreign key, because `nutritionalInfo` is simply a nested object inside the `pizzas` document.

## 🧱 Embedding vs. Referencing

This is *the* modeling decision in every document database, and it doesn't have a JPA equivalent — in a relational database, every relationship becomes a foreign key by default, and you rarely choose otherwise. In MongoDB, you choose per relationship:

| Relationship | Choice | Why |
|---|---|---|
| `Pizza` → `NutritionalInfo` | **Embed** | 1:1, never accessed independently of its `Pizza`, small and fixed shape — the textbook embedding case. |
| `Order` → `OrderLine`s | **Embed** | An order's lines are always read and written together with the order (the "aggregate root" pattern the book also uses for Spring Data JDBC) — never queried on their own. |
| `Order` → `Customer` | **Reference** (`customerId`) | A customer can place unboundedly many orders over time; embedding them would make the `Customer` document grow without limit, and Mongo enforces a 16 MB per-document cap. |
| `Customer` ↔ `Pizza` (favorites) | **Reference**, one-sided (`favoritePizzaIds` on `Customer` only) | A relational `@ManyToMany` needs a join table because *neither* side can hold the relationship alone. A document database doesn't need that: one side (`Customer`) simply stores the list of referenced ids, and it's the only side that does — storing it on both `Customer` and `Pizza`, as the JPA version's bidirectional `favoritePizzas`/`favoritedByCustomers` does, would mean keeping two copies of the same fact in sync across documents, which MongoDB has no join to help you enforce. |

The rule of thumb: **embed what you always read together and that has a bounded size; reference what grows without bound, is large, or needs to be queried/updated independently.**

## 🗄️ The MongoRepository Interface

Just like `JpaRepository<T, ID>` in Lesson 6a, `MongoRepository<T, ID>` is the module-specific specialization of the shared `Repository`/`CrudRepository` hierarchy from [Lesson 6](../lesson-06-spring-data/README.md#the-repository-abstraction) — the id type is `String` here instead of `Long`:

```java
// repository/PizzaRepository.java
public interface PizzaRepository extends MongoRepository<Pizza, String> {
    // findAll(), findById(), save(), deleteById(), count(), … already included
}
```

## 🔍 Custom Queries

Derived query methods use the exact same naming conventions from [Lesson 6](../lesson-06-spring-data/README.md#query-methods-and-naming-conventions) — Spring Data MongoDB parses the method name into a BSON query filter instead of JPQL:

```java
// repository/PizzaRepository.java
Optional<Pizza> findByName(String name);
List<Pizza> findByPriceLessThan(BigDecimal maxPrice);
List<Pizza> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
```

For anything the naming convention can't express, `@Query` takes a **MongoDB query document** instead of JPQL — this is the MongoDB-flavored equivalent of Lesson 6a's `searchByKeyword`:

```java
// repository/PizzaRepository.java (JPA version used JPQL LIKE; this is the Mongo equivalent)
@Query("{ '$or': [ { 'name': { '$regex': ?0, '$options': 'i' } }, { 'description': { '$regex': ?0, '$options': 'i' } } ] }")
List<Pizza> searchByKeyword(String keyword);
```

For real full-text search or aggregation pipelines, you'd reach for `MongoTemplate` instead of a repository method — out of scope here, but worth knowing it exists for when `@Query` isn't enough.

## 🕐 Auditing

Spring Data's auditing annotations (`@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy`) are shared infrastructure from `spring-data-commons` — the exact same annotations from [Lesson 6a](../lesson-06a-spring-data-jpa/README.md#-jpa-auditing) work here, just activated differently:

```java
// config/MongoConfig.java
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
```

The one difference from JPA: **no `@EntityListeners(AuditingEntityListener.class)` is needed on each document class.** `@EnableMongoAuditing` alone is enough to make `@CreatedDate`/`@LastModifiedDate` work on every `@Document`. As in Lesson 6a, there's no `AuditorAware` bean in this project (it needs Spring Security), so `@CreatedBy`/`@LastModifiedBy` on `Order` simply stay `null` — harmless, since MongoDB documents have no `NOT NULL` constraint to violate.

## 📇 Indexes

`@Indexed` defines an index directly on the field, right next to the mapping annotation — no separate DDL or migration file:

```java
// domain/Customer.java
@Indexed(unique = true)
private String email;

// domain/Order.java
@Indexed(unique = true)
private String orderNumber;
```

These mirror the `unique = true` constraints on the same two fields in the JPA version's `@Column` annotations — same intent, index created automatically by Spring Data MongoDB on startup instead of via a `UNIQUE` constraint in generated DDL.

## 🚫 No Joins: What Replaces JOIN FETCH

Lesson 6a's `CustomerRepository` has a `findByIdWithOrders()` using `JOIN FETCH` to load a customer and its orders in one query. **There is no MongoDB equivalent** — `orders` isn't even a field on `Customer` here (see the [embedding vs. referencing](#-embedding-vs-referencing) table above), so there's nothing to fetch-join in the first place. To get a customer's orders, make two calls:

```java
Customer customer = customerRepository.findById(id).orElseThrow();
List<Order> orders = orderRepository.findByCustomerId(customer.getId());
```

This is a real trade-off, not just an inconvenience: a relational join happens inside the database in one round trip; two repository calls are two round trips. For cases where that matters, `MongoTemplate`'s aggregation pipeline supports a `$lookup` stage that performs a join-like operation inside MongoDB itself — worth knowing exists, but not covered hands-on in this lesson.

## 🧪 Testing with Testcontainers

The book's recommended way to test a `MongoRepository` is **Testcontainers**, not an embedded/in-memory MongoDB — an embedded database only implements a subset of the real engine's behavior and can hide bugs that only show up against the real thing:

```java
@DataMongoTest
@Testcontainers
class PizzaRepositoryTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @Autowired
    private PizzaRepository pizzaRepository;

    // ...
}
```

`@ServiceConnection` (Spring Boot 3.1+, refined in 4.0) is what makes this "zero configuration": it detects the running container and wires `spring.data.mongodb.uri` automatically — no manual `@DynamicPropertySource` needed. This lesson's project doesn't include tests (domain + repository only, as with Lesson 6a), but the `pom.xml` already has the Testcontainers dependencies in place if you want to add one yourself as an exercise.

## 🚀 Runnable Project

**`pizzastore-mongodb/`** mirrors [Lesson 6a](../lesson-06a-spring-data-jpa/README.md#-runnable-project)'s project exactly in scope — `domain` and `repository` only, plus the minimal infrastructure those packages need to actually run:

```
pizzastore-mongodb/
└── src/main/java/be/vives/pizzastore/
    ├── PizzaStoreApplication.java
    ├── config/
    │   ├── MongoConfig.java     (@EnableMongoAuditing)
    │   └── DataSeeder.java      (dummy data - see below)
    ├── domain/
    │   ├── Customer.java
    │   ├── NutritionalInfo.java   (embedded in Pizza)
    │   ├── Order.java
    │   ├── OrderLine.java         (embedded in Order)
    │   ├── OrderStatus.java
    │   ├── Pizza.java
    │   └── Role.java
    └── repository/
        ├── CustomerRepository.java
        ├── OrderRepository.java
        └── PizzaRepository.java
```

### About `DataSeeder.java` (there is no `data.sql` here)

Lesson 6a loads dummy data from `data.sql`, a mechanism built into Spring Boot's *SQL* datasource initializer. **MongoDB has no equivalent** — there's no fixed schema to run SQL `INSERT` statements against, and Spring Boot doesn't ship a JSON/BSON counterpart to `data.sql`. The idiomatic replacement is a `CommandLineRunner` that inserts the seed data in code, guarded so it only runs once:

```java
// config/DataSeeder.java
@Component
public class DataSeeder implements CommandLineRunner {

    @Override
    public void run(String... args) {
        if (pizzaRepository.count() > 0) {
            return;
        }
        // ... build and save Pizza, Customer, Order documents ...
    }
}
```

This seeds the same pizzas, customers, and orders as Lesson 6a's `data.sql` (adapted to the embedded/referenced shape described above).

### Running It

This project needs a real MongoDB instance — there's no in-memory MongoDB the way H2 stands in for a relational database. The simplest option is Docker:

```bash
docker run -d --name pizzastore-mongo -p 27017:27017 mongo:7.0
```

Then run the application:

```bash
cd lesson-06b-spring-data-mongodb/pizzastore-mongodb
mvn spring-boot:run
```

`application.properties` points at `mongodb://localhost:27017/pizzastore_dev` by default. Once it's running, inspect the seeded data with `mongosh` or MongoDB Compass:

```bash
mongosh mongodb://localhost:27017/pizzastore_dev --eval "db.pizzas.find().pretty()"
```

> This lesson's compilation and dependency wiring were verified in this environment; the live run against a real MongoDB instance was not (no Docker available here) — run it yourself as described above to see the seeded data.

## ⚖️ Comparison: JPA vs. MongoDB

| Feature | Spring Data JPA (Lesson 6a) | Spring Data MongoDB (this lesson) |
|---|---|---|
| Data model | Tables and rows, fixed schema | Flexible BSON documents |
| Id type | `Long` (auto-increment) | `String` (MongoDB `ObjectId`) |
| Relationships | Foreign keys, joins | Embedding or manual references |
| Many-to-many | Join table, both sides mapped | One-sided list of referenced ids |
| Query language | JPQL / native SQL | MongoDB query documents (BSON) |
| Auditing | `@EntityListeners` + `@EnableJpaAuditing` | `@EnableMongoAuditing` alone |
| Dummy data | `data.sql` (SQL init) | `CommandLineRunner` (no built-in equivalent) |
| Local dev database | H2, in-memory, zero setup | Real MongoDB instance (e.g. via Docker) |
| Best fit | Complex relational graphs, reporting, legacy databases | Flexible/evolving schemas, horizontal scaling |

One deliberate style choice: the book models MongoDB documents as Java **records** (promoting immutability). This lesson keeps mutable classes with getters/setters instead, matching the established style of the final PizzaStore project and Lesson 6a — the same kind of "keep the reference solution's conventions" trade-off already made for MapStruct vs. the book's manual DTO mapping in Lesson 7.

## 🎓 Summary

1. MongoDB stores **documents**, not rows — there's no fixed schema and no joins.
2. **Embed** data that's always read together and bounded in size (`NutritionalInfo` in `Pizza`, `OrderLine`s in `Order`); **reference** data that grows without bound or needs independent access (`Order.customerId`, `Customer.favoritePizzaIds`).
3. `MongoRepository`, derived queries, and Spring Data auditing all work the same way conceptually as their JPA counterparts — only the underlying query language and a few annotations differ.
4. There is no `JOIN FETCH` equivalent — plan for extra repository calls, or reach for `MongoTemplate`'s aggregation pipeline when a real join is unavoidable.
5. `data.sql` has no MongoDB equivalent — seed dummy data with a `CommandLineRunner` instead.

---

## 📖 Additional Resources

- [Spring Data MongoDB Documentation](https://docs.spring.io/spring-data/mongodb/reference/)
- [MongoDB Manual](https://www.mongodb.com/docs/manual/)
- [MongoDB Data Modeling: Embedding vs. Referencing](https://www.mongodb.com/docs/manual/core/data-model-design/)
- [Testcontainers for MongoDB](https://testcontainers.com/modules/mongodb/)

**Note on the book**: This lesson corresponds to *Pro Spring Boot 4*, Chapter 7: *NoSQL with Spring Boot* — specifically the *Document Persistence with Spring Data MongoDB* section (key features, best practices, the Customer CRM implementation, and testing with Testcontainers). The rest of Chapter 7 — Spring Data Redis (key-value), Spring Data Neo4j (graph), and the closing section on AI vector search — is out of scope for this course; PizzaStore stays with the two technologies covered in depth here and in [Lesson 6a](../lesson-06a-spring-data-jpa/README.md): relational (JPA) and document (MongoDB).

---

**Congratulations!** 🎉 You've now seen the same domain model implemented two ways — relational with Spring Data JPA, and document-oriented with Spring Data MongoDB — and understand the modeling trade-offs between them.
