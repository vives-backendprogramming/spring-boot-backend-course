# Lesson 6: Spring Data with Spring Boot

**The concepts every Spring Data module shares, before we pick a technology**

---

## 📋 Learning Objectives

By the end of this lesson, you will be able to:
- Explain what Spring Data is and which problem it solves
- Understand the repository proxy pattern: why you only write an interface
- Read and write query methods using Spring Data's naming conventions
- Explain centralized exception translation and why it decouples your service layer from the database technology
- Choose the right Spring Data module for a given storage technology

---

## 📚 Table of Contents

- [📋 Learning Objectives](#-learning-objectives)
- [🗃️ What Is Spring Data?](#️-what-is-spring-data)
- [🧩 The Repository Abstraction](#-the-repository-abstraction)
- [🔍 Query Methods and Naming Conventions](#-query-methods-and-naming-conventions)
- [🛡️ Centralized Exception Translation](#️-centralized-exception-translation)
- [⚖️ Choosing a Spring Data Module](#️-choosing-a-spring-data-module)
- [🚦 Where This Lesson Goes Next](#-where-this-lesson-goes-next)
- [🎓 Summary](#-summary)
- [📖 Additional Resources](#-additional-resources)

---

## 🗃️ What Is Spring Data?

So far, PizzaStore's data layer has only existed on paper — we haven't written any persistence code yet. Starting with this lesson, we give the domain model a real home in a database.

**Spring Data** is an umbrella project. Its mission is to provide a familiar, consistent, Spring-based programming model for data access, no matter which storage technology sits underneath — a relational database, a document store like MongoDB, a key-value store like Redis, or a graph database like Neo4j. Each technology gets its own module (Spring Data JPA, Spring Data MongoDB, Spring Data Redis, Spring Data Neo4j, …), but they all share the same core building blocks described in this lesson.

Spring Boot 4 (built on Spring Framework 7) adds several enhancements across the whole Spring Data family that are worth knowing about regardless of which module you end up using:

- **AOT-optimized repositories** — query derivation logic can move from runtime to build time, which matters most for GraalVM native images (dramatically faster startup, less reflection).
- **Virtual threads support** — blocking data access (JDBC, JPA) scales far better under load once `spring.threads.virtual.enabled=true` is set, without changing a line of repository code.
- **Jakarta EE 11 alignment** — first-class support for JPA 3.2 and modern drivers.
- **Improved null safety** — JSpecify-based annotations across Spring Data's own APIs reduce the risk of `NullPointerException` in the data access layer.

## 🧩 The Repository Abstraction

The central abstraction in every Spring Data module is the **repository interface**. You define an interface extending one of Spring Data's base repositories, and Spring generates a working implementation for you at runtime — you never write a class that implements it yourself.

```
┌────────────────────┐         ┌────────────────────────┐            ┌────────────────┐
│  Your interface    │────────▶│  Spring Data proxy     │───────────▶│  Data store    │
│  PizzaRepository   │ extends │ (generated at runtime) │  talks to  │ (SQL, Mongo…)  │
│  extends CrudRepo  │         │                        │            │                │
└────────────────────┘         └────────────────────────┘            └────────────────┘
```

At startup, Spring Data scans for repository interfaces, generates a dynamic proxy for each one, and registers it as a bean. Whatever module you use — JPA, MongoDB, JDBC — the interface you write looks the same:

```java
public interface PizzaRepository extends JpaRepository<Pizza, Long> {
    // Spring generates the implementation - you only declare what you need
}
```

Only the base interface you extend (`JpaRepository<Pizza, Long>` vs. `MongoRepository<Pizza, String>`, for example) and the id type change between technologies. The pattern — interface in, proxy out — is identical everywhere.

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

## ⚖️ Choosing a Spring Data Module

The book (*Pro Spring Boot 4*) walks through several modules: Spring Data JDBC, Spring Data JPA, Spring Data MongoDB, Spring Data Redis, and Spring Data Neo4j. This course focuses on the two that matter most for PizzaStore's own evolution — a relational domain model (JPA) and a document-oriented alternative (MongoDB) — so you can compare the same data model under two different paradigms.

| Feature | Spring Data Common | Spring Data JPA | Spring Data MongoDB |
|---|---|---|---|
| Role | Base infrastructure shared by every module | Full-featured ORM (relational) | Document mapping (NoSQL) |
| Core interface | `Repository`, `CrudRepository` | `JpaRepository` | `MongoRepository` |
| Typical annotations | `@Id`, `@CreatedDate`, `@Version` | `@Entity`, `@Table`, `@OneToMany` | `@Document`, `@Indexed` |
| Query logic | Method-name derivation (shared) | JPQL / Hibernate | MongoDB query documents (BSON) |
| Relationships | — | Joins (`@OneToMany`, `@ManyToOne`, …) | Embedding or manual references |
| Best fit | — | Complex relational graphs, reporting, legacy databases | Flexible schemas, horizontal scaling, document-shaped data |

Spring Data JDBC (the book's third option, aimed at small DDD aggregates without JPA's overhead) is intentionally left out of this course — see the "Note on the book" section below for why.

## 🚦 Where This Lesson Goes Next

This lesson only covers what every Spring Data module has in common. The actual, hands-on data layer for PizzaStore is built in two follow-up lessons, each with its own runnable project:

- **[Lesson 6a: Spring Data JPA](../lesson-06a-spring-data-jpa/README.md)** — entities, relationships (`@OneToOne`, `@OneToMany`, `@ManyToOne`, `@ManyToMany`), fetch types, cascading, auditing, and custom queries against a relational database.
- **[Lesson 6b: Spring Data MongoDB](../lesson-06b-spring-data-mongodb/README.md)** — the same PizzaStore data model re-expressed as MongoDB documents: embedding vs. referencing, indexes, and MongoDB-flavored queries.

Both lessons ship a project containing an exact copy of the final PizzaStore project's `domain` and `repository` packages — one persisted with JPA, one with MongoDB — so you can compare the same problem solved two ways.

## 🎓 Summary

1. **Spring Data** is one consistent programming model spread across many storage technologies.
2. **Repository interfaces** are proxied at runtime — you declare, Spring implements.
3. **Query method naming conventions** are shared across every module; only the generated query language differs.
4. **Exception translation** keeps your service layer decoupled from the underlying database technology.
5. This course goes deep on two modules — **Spring Data JPA** ([Lesson 6a](../lesson-06a-spring-data-jpa/README.md)) and **Spring Data MongoDB** ([Lesson 6b](../lesson-06b-spring-data-mongodb/README.md)) — instead of the book's JDBC/JPA pairing.

---

## 📖 Additional Resources

- [Spring Data Documentation](https://docs.spring.io/spring-data/commons/reference/html/)
- [Spring Data JPA Documentation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Spring Data MongoDB Documentation](https://docs.spring.io/spring-data/mongodb/reference/)

**Note on the book**: This lesson corresponds to *Pro Spring Boot 4*, Chapter 6: *Spring Data with Spring Boot* — specifically the introductory *Spring Data in the Spring Boot 4 Era* and *Spring Data Common Concepts* sections (including the query-method naming table). This course deliberately skips Chapter 4 (*JDBC with Spring Boot*) and Chapter 5 (*Advanced Data Access with JdbcClient*) — plain `JdbcTemplate`/`JdbcClient` usage is out of scope for this course — and, for the same reason, skips the rest of Chapter 6's *Spring Data JDBC with the Customer CRM* section too. Where the book pairs Spring Data JDBC with Spring Data JPA as its two case studies, this course instead pairs **Spring Data JPA** with **Spring Data MongoDB** (from Chapter 7, *NoSQL with Spring Boot*), covered respectively in [Lesson 6a](../lesson-06a-spring-data-jpa/README.md) and [Lesson 6b](../lesson-06b-spring-data-mongodb/README.md).

---

**Ready to get hands-on?** 🎉 Continue to [Lesson 6a: Spring Data JPA](../lesson-06a-spring-data-jpa/README.md) to build PizzaStore's relational data layer, or jump ahead to [Lesson 6b: Spring Data MongoDB](../lesson-06b-spring-data-mongodb/README.md) to see the same model as documents.
