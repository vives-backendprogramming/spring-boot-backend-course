# Lesson 6: Spring Data with Spring Boot

**The concepts every Spring Data module shares, before we pick a technology**

---

## 📋 Learning Objectives

By the end of this lesson, you will be able to:

- Explain what Spring Data is and which problem it solves
- Explain the repository proxy pattern: why you only write an interface
- Read and write query methods using Spring Data's naming conventions
- Explain centralized exception translation and why it decouples your service layer from the database technology
- Choose the right Spring Data module for a given storage technology

---

## 📚 Table of Contents

1. [Recap: Where Lesson 5 Left Us](#-recap-where-lesson-5-left-us)
2. [What Is Spring Data?](#️-what-is-spring-data)
3. [The Repository Abstraction](#-the-repository-abstraction)
4. [Query Methods and Naming Conventions](#-query-methods-and-naming-conventions)
5. [Centralized Exception Translation](#️-centralized-exception-translation)
6. [Choosing a Spring Data Module](#️-choosing-a-spring-data-module)
7. [Best Practices](#-best-practices)
8. [Summary](#-summary)
9. [Additional Resources](#-additional-resources)

---

## 🔄 Recap: Where Lesson 5 Left Us

Lesson 5 showed how a request travels through Spring MVC to a controller. Controllers need data to return, and that data has to live somewhere. Lesson 4's `pizzastore-intro` already stored its one `Pizza` entity with Spring Data JPA and H2 — without explaining what Spring Data actually is.

This lesson fills that gap with the concepts every Spring Data module shares. It has no project of its own: the real PizzaStore data layer is built in [Lesson 6a](../lesson-06a-spring-data-jpa/README.md) (JPA) and, as an alternative, [Lesson 6b](../lesson-06b-spring-data-mongodb/README.md) (MongoDB).

---

## 🗃️ What Is Spring Data?

**Spring Data** is an umbrella project. Its mission is to provide a familiar, consistent, Spring-based programming model for data access, no matter which storage technology sits underneath — a relational database, a document store like MongoDB, a key-value store like Redis, or a graph database like Neo4j. Each technology gets its own module (Spring Data JPA, Spring Data MongoDB, Spring Data Redis, Spring Data Neo4j, …), but they all share the same core building blocks described in this lesson.

The book lists several Spring Boot 4 (Spring Framework 7) era features across the whole Spring Data family that are worth knowing about regardless of which module you end up using:

- **AOT-optimized repositories** — query derivation logic can move from runtime to build time, which matters most for GraalVM native images (dramatically faster startup, less reflection).
- **Virtual threads** — blocking data access (JDBC, JPA) scales far better under load once `spring.threads.virtual.enabled=true` is set, without changing a line of repository code. (Available since Spring Boot 3.2 on Java 21+.)
- **Jakarta EE 11 alignment** — first-class support for Jakarta Persistence (JPA) 3.2 and modern drivers.
- **Improved null safety** — JSpecify-based annotations across Spring Data's own APIs reduce the risk of `NullPointerException` in the data access layer.

---

## 🧩 The Repository Abstraction

The central abstraction in every Spring Data module is the **repository interface**. You define an interface extending one of Spring Data's base repositories, and Spring generates a working implementation for you at runtime — you never write a class that implements it yourself.

```
┌────────────────────┐            ┌────────────────────────┐            ┌────────────────┐
│  Your interface    │◀───────────│  Spring Data proxy     │───────────▶│  Data store    │
│  PizzaRepository   │ implements │ (generated at runtime) │  talks to  │ (SQL, Mongo…)  │
│  extends JpaRepo…  │            │                        │            │                │
└────────────────────┘            └────────────────────────┘            └────────────────┘
```

At startup, Spring Data scans for repository interfaces, generates a dynamic proxy that implements each one, and registers it as a bean. Whatever module you use — JPA, MongoDB, JDBC — the interface you write looks the same:

```java
public interface PizzaRepository extends JpaRepository<Pizza, Long> {
    // Spring generates the implementation - you only declare what you need
}
```

Only the base interface you extend (`JpaRepository<Pizza, Long>` vs. `MongoRepository<Pizza, String>`, for example) and the id type change between technologies. The pattern — interface in, proxy out — is identical everywhere. That's also why no `@Repository` annotation is needed on these interfaces.

---

## 🔍 Query Methods and Naming Conventions

Spring Data's query derivation mechanism is one of its most useful features. By following a naming convention, you express queries without writing a single line of SQL, JPQL, or a MongoDB query document — Spring Data parses the method name and builds the query for you.

| Method Name | Derived Query (approximate) |
|---|---|
| `findByEmail(String email)` | `... WHERE email = ?` |
| `findByLastNameAndFirstName(String last, String first)` | `... WHERE last_name = ? AND first_name = ?` |
| `findByAgeGreaterThan(int age)` | `... WHERE age > ?` |
| `findByStartDateBetween(Date start, Date end)` | `... WHERE start_date BETWEEN ? AND ?` |
| `findByLastNameLike(String pattern)` | `... WHERE last_name LIKE ?` |
| `findByActiveTrue()` | `... WHERE active = true` |
| `findFirst3ByOrderByLastNameAsc()` | `... ORDER BY last_name ASC LIMIT 3` |

The exact translation (SQL, JPQL, or a MongoDB filter document) depends on which module is behind the interface, but the naming rules — `And`, `Or`, `Between`, `LessThan`, `Containing`, `OrderBy`, `IgnoreCase`, and so on — are the same everywhere. You'll see the same table put to work with real PizzaStore repositories in both [Lesson 6a](../lesson-06a-spring-data-jpa/README.md) and [Lesson 6b](../lesson-06b-spring-data-mongodb/README.md).

---

## 🛡️ Centralized Exception Translation

Every Spring Data module automatically translates low-level, technology-specific exceptions (a JDBC `SQLException`, a MongoDB driver exception, …) into Spring's uniform `DataAccessException` hierarchy.

```
Your service layer
      ↓ catches
DataAccessException hierarchy     ← always the same, regardless of technology
      ↑ translated from
SQLException / MongoException / … ← technology-specific, hidden from you
```

This is what lets a service class stay clean and portable: it can catch `DataAccessException` (or a specific subclass like `DataIntegrityViolationException`) without knowing or caring whether the repository underneath talks to PostgreSQL, H2, or MongoDB.

---

## ⚖️ Choosing a Spring Data Module

The book (*Pro Spring Boot 4*) walks through several modules: Spring Data JDBC, Spring Data JPA, Spring Data MongoDB, Spring Data Redis, and Spring Data Neo4j. This course focuses on two — a relational domain model (JPA) and a document-oriented alternative (MongoDB) — so you can compare the same data model under two different paradigms.

| Feature | Spring Data Common | Spring Data JPA | Spring Data MongoDB |
|---|---|---|---|
| Role | Base infrastructure shared by every module | Full-featured ORM (relational) | Document mapping (NoSQL) |
| Core interface | `Repository`, `CrudRepository` | `JpaRepository` | `MongoRepository` |
| Typical annotations | `@CreatedDate`, `@LastModifiedDate` (auditing) | `@Entity`, `@Table`, `@OneToMany` | `@Document`, `@Indexed` |
| Query logic | Method-name derivation (shared) | JPQL / Hibernate | MongoDB query documents (BSON) |
| Relationships | — | Joins (`@OneToMany`, `@ManyToOne`, …) | Embedding or manual references |
| Best fit | — | Complex relational graphs, reporting, legacy databases | Flexible schemas, horizontal scaling, document-shaped data |

Spring Data JDBC (the book's third option, aimed at small DDD aggregates without JPA's overhead) is intentionally left out of this course — see the "Note on the book" below for why.

The two follow-up lessons each ship a runnable project:

- **[Lesson 6a: Spring Data JPA](../lesson-06a-spring-data-jpa/README.md)** — entities, relationships (`@OneToOne`, `@OneToMany`, `@ManyToOne`, `@ManyToMany`), fetch types, cascading, auditing, and custom queries against a relational database. Its project contains the final PizzaStore's `domain` and `repository` packages, and is the start of the PizzaStore that Lessons 7–14 keep building on.
- **[Lesson 6b: Spring Data MongoDB](../lesson-06b-spring-data-mongodb/README.md)** — the same PizzaStore data model translated into MongoDB documents: embedding vs. referencing, indexes, and MongoDB-flavored queries. The final PizzaStore has no MongoDB version; this project is a side branch for comparison.

---

## 💡 Best Practices

1. **Write the interface, not the implementation** — let Spring Data generate repositories; reach for a custom implementation only when a query can't be expressed otherwise.
2. **Prefer derived query methods for simple queries**, and switch to an explicit query (`@Query`) as soon as a method name gets long and hard to read.
3. **Catch `DataAccessException` subclasses, not driver exceptions**, so the service layer stays independent of the database.
4. **Pick the module by data shape**: relational graphs with constraints → JPA; self-contained documents → MongoDB.

---

## 🎓 Summary

### What We Learned

1. **Spring Data** is one consistent programming model spread across many storage technologies.
2. **Repository interfaces** are implemented by a proxy at runtime — you declare, Spring implements.
3. **Query method naming conventions** are shared across every module; only the generated query language differs.
4. **Exception translation** keeps your service layer decoupled from the underlying database technology.

### Key Takeaways

- The **repository** is the only data-access type you write for most use cases
- What you learn about one Spring Data module transfers almost entirely to the others
- This course goes deep on **Spring Data JPA** and **Spring Data MongoDB** instead of the book's JDBC/JPA pairing

### What's Next?

[Lesson 6a](../lesson-06a-spring-data-jpa/README.md) builds PizzaStore's relational data layer with Spring Data JPA — the base for every later lesson. [Lesson 6b](../lesson-06b-spring-data-mongodb/README.md) then shows the same model as MongoDB documents.

---

## 📖 Additional Resources

- [Spring Data Commons Documentation](https://docs.spring.io/spring-data/commons/reference/)
- [Spring Data JPA Documentation](https://docs.spring.io/spring-data/jpa/reference/)
- [Spring Data MongoDB Documentation](https://docs.spring.io/spring-data/mongodb/reference/)

**Note on the book**: This lesson corresponds to *Pro Spring Boot 4*, Chapter 6: *Spring Data with Spring Boot* — the introductory *Spring Data in the Spring Boot 4 Era* (p. 139), *Spring Data Common Concepts* (p. 140) and *Query Methods and Naming Conventions* (p. 142, the naming table above) sections, plus the module comparison of *Comparison: Spring Data Common vs …* (p. 164). This course deliberately skips Chapter 4 (*JDBC with Spring Boot*) and Chapter 5 (*Advanced Data Access with JdbcClient*) — plain `JdbcTemplate`/`JdbcClient` usage is out of scope — and, for the same reason, Chapter 6's *Spring Data JDBC with the Customer CRM* section (pp. 143–149). Where the book pairs Spring Data JDBC with Spring Data JPA as its two case studies, this course instead pairs **Spring Data JPA** with **Spring Data MongoDB** (from Chapter 7, *NoSQL with Spring Boot*), covered respectively in [Lesson 6a](../lesson-06a-spring-data-jpa/README.md) and [Lesson 6b](../lesson-06b-spring-data-mongodb/README.md).

---

**Congratulations!** 🎉 You know the concepts every Spring Data module shares. Continue to [Lesson 6a: Spring Data JPA](../lesson-06a-spring-data-jpa/README.md) to build PizzaStore's relational data layer.
