# Lesson 4: Getting Started with Spring Boot

**Convention over configuration: starters, auto-configuration and a first PizzaStore**

---

## 📋 Learning Objectives

By the end of this lesson, you will be able to:

- Explain **what Spring Boot is**, why it exists and what **Convention over Configuration** means
- Explain how **starters** manage dependencies and versions
- Explain how **auto-configuration** works, inspect it and override it
- Create a Spring Boot project with **Spring Initializr**
- Explain what **`@SpringBootApplication`** combines
- Use **Spring Boot DevTools** for rapid development
- Build a first small REST API with **JPA and H2** on the **embedded servlet container**
- Recognize what **Spring Boot Actuator** offers

---

## 📚 Table of Contents

1. [Recap: Where Lesson 3 Left Us](#-recap-where-lesson-3-left-us)
2. [Spring Rebooted](#-spring-rebooted)
3. [What is Spring Boot?](#-what-is-spring-boot)
4. [Spring Boot Key Features](#-spring-boot-key-features)
5. [A Simple Web Application - Just the Essentials](#-a-simple-web-application---just-the-essentials)
6. [Spring Boot Starters](#-spring-boot-starters)
7. [Auto-Configuration](#️-auto-configuration)
8. [Creating Spring Boot Projects](#️-creating-spring-boot-projects)
9. [Spring Boot Project Structure](#-spring-boot-project-structure)
10. [Spring Boot DevTools](#-spring-boot-devtools)
11. [Demo: Building a first PizzaStore](#-demo-building-a-first-pizzastore)
12. [What Just Happened?](#-what-just-happened)
13. [Spring Boot Build Plugin](#️-spring-boot-build-plugin)
14. [Spring Boot Actuator](#-spring-boot-actuator)
15. [Best Practices](#-best-practices)
16. [Summary](#-summary)
17. [Additional Resources](#-additional-resources)
18. [Runnable Project](#-runnable-project)

---

## 🔄 Recap: Where Lesson 3 Left Us

In Lessons 2 and 3, we learned the Spring Framework itself: dependency injection, IoC, configuration with properties and profiles, and logging. Every piece worked — but every piece needed **hand-written setup**: a `@Configuration` class with `@ComponentScan`, `@PropertySource` plus a placeholder configurer, Logback dependencies, and an `AnnotationConfigApplicationContext` started from `main()`. And we haven't even added a web server or a database yet.

**Spring Boot** changes this by embracing **"Convention over Configuration"**. It makes it easy to create **stand-alone, production-grade** Spring applications that you can **"just run"**.

In this lesson, we'll discover how Spring Boot simplifies Spring development and build a first, small **PizzaStore API** as a demo. From Lesson 6a on, the real PizzaStore grows step by step towards the final reference solution.

---

## 🤔 Spring Rebooted

### The Evolution of Spring

**Spring Framework** started as a lightweight alternative to Jakarta EE (formerly Java EE):

- ✅ Simpler approach to enterprise Java development
- ✅ Lightweight in terms of component code
- ❌ **But heavyweight in terms of configuration**

**Timeline**:

- **Spring < 2.5**: XML configuration everywhere
- **Spring 2.5**: Annotation-based component scanning
- **Spring 3.0**: Java-based configuration classes
- **But still**: Spring required a lot of boilerplate configuration

### The Problem

> **"Any time spent writing configuration is time spent NOT writing application logic."**

---

## 🚀 What is Spring Boot?

> **Spring Boot makes it easy to create stand-alone, production-grade Spring based applications that you can "just run".**

Spring Boot is built on top of the Spring Framework but adds:

- **Automatic configuration** based on what's on the classpath
- **Opinionated defaults** that work for most applications
- **Starter dependencies** that simplify build configuration
- **Embedded servers** - no need to deploy WAR files
- **Production-ready features** - metrics, health checks, monitoring

**Philosophy**: **CONVENTION over CONFIGURATION**

---

## ✨ Spring Boot Key Features

### 1. Create Stand-Alone Spring Applications

- Self-contained applications with embedded server
- No need for external application servers (Tomcat, Glassfish, etc.)
- Just run: `java -jar myapp.jar`

### 2. Embed Servers Directly

- **Tomcat** embedded by default; **Jetty** as an alternative (Spring Boot 4 dropped Undertow, which doesn't support Servlet 6.1)
- No need to deploy WAR files
- Application becomes a single executable JAR

### 3. Opinionated Starter Dependencies

- Simplify your build configuration
- Pre-configured sets of dependencies
- Tested to work together

### 4. Auto-Configuration

- Automatically configure Spring and 3rd party libraries
- Based on what's on your classpath
- Can be overridden when needed

### 5. Production-Ready Features

- **Metrics** - monitor application performance
- **Health checks** - verify application status
- **Externalized configuration** - properties and profiles built-in

### 6. No Code Generation, No XML

- **Zero** code generation
- **Zero** XML configuration required
- Pure Java and annotations

---

## 🎯 A Simple Web Application - Just the Essentials

**The controller is all you need**...

### The Entire Application

```java
@RestController
@SpringBootApplication
public class HelloWorldApplication {
    
    @GetMapping("/")
    public String hello() {
        return "Hello World!";
    }
    
    public static void main(String[] args) {
        SpringApplication.run(HelloWorldApplication.class, args);
    }
}
```

**That's it!** No XML, no web.xml, no configuration classes, no server setup.

Run it: `mvn spring-boot:run` or just run the `main` method!

### What Spring Boot Does For You

1. ✅ Sets up Spring MVC automatically
2. ✅ Configures DispatcherServlet
3. ✅ Starts embedded Tomcat server
4. ✅ Configures JSON message converters
5. ✅ Sets up component scanning
6. ✅ Provides sensible defaults for everything

**We wrote ZERO configuration for any of this!**

---

## 📦 Spring Boot Starters

### The Problem

Project dependency management is challenging:

- What library do you need?
- What's its group and artifact ID?
- Which version should you use?
- Will that version work with other dependencies?
- What about transitive dependencies?

### Example Without Starters

To build a REST API with Spring MVC, you'd need:

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-core</artifactId>
    <version>7.0.9</version>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-web</artifactId>
    <version>7.0.9</version>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-webmvc</artifactId>
    <version>7.0.9</version>
</dependency>
<dependency>
    <groupId>tools.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>3.1.5</version>
</dependency>
<dependency>
    <groupId>org.apache.tomcat.embed</groupId>
    <artifactId>tomcat-embed-core</artifactId>
    <version>11.0.24</version>   <!-- does this one work with Spring 7.0.9? You have to find out. -->
</dependency>
<!-- ... and many more! -->
```

**Problem**: Version management nightmare!

### The Solution: Starters

**Starters** are pre-configured dependency descriptors that bring in all the dependencies you need for a particular functionality.

**Just one dependency**:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
```

**No version needed** - inherited from `spring-boot-starter-parent`.

This single starter brings in:

- Spring MVC
- Jackson 3 for JSON
- Embedded Tomcat
- All with compatible versions!

Bean Validation (Hibernate Validator) is **not** included: that's the separate `spring-boot-starter-validation`.

### Common Starters

| Starter | Purpose | Includes |
|---------|---------|----------|
| `spring-boot-starter-webmvc` | Web applications with Spring MVC, REST | Spring MVC, Tomcat, JSON |
| `spring-boot-starter-data-jpa` | JPA with Hibernate | Spring Data JPA, Hibernate, JDBC |
| `spring-boot-starter-security` | Spring Security | Spring Security |
| `spring-boot-starter-webmvc-test` | Web layer testing | JUnit Jupiter 6, Spring Test, AssertJ, Mockito |
| `spring-boot-starter-validation` | Bean Validation | Hibernate Validator |
| `spring-boot-starter-actuator` | Production monitoring | Metrics, health checks |

**Full list**: [Spring Boot reference — Starters](https://docs.spring.io/spring-boot/reference/using/build-systems.html#using.build-systems.starters)

**Benefits**:

- ✅ **Single dependency** instead of many
- ✅ **Tested combinations** that work together
- ✅ **Version management** handled automatically
- ✅ **Semantic naming** - clear what functionality you get

---

## ⚙️ Auto-Configuration

### The Concept

> **Spring Boot can automatically configure components based on what's on the classpath.**

If configuration is so common, why should you write it manually?

### How Auto-Configuration Works

```
1. Spring Boot scans your classpath
   ↓
2. Finds dependencies (e.g., spring-web, hibernate)
   ↓
3. Applies sensible defaults automatically
   ↓
4. Your application is configured!
```

Spring Boot configures components automatically based on:

1. **Classpath content** - What JARs are present?
2. **Bean presence** - Are certain beans already defined?
3. **System properties** - Any relevant properties set?
4. **Configuration files** - Any settings in application.properties?

### Auto-Configuration Examples

#### Example 1: Spring MVC

**If** `spring-webmvc` and an embedded server (both come with `spring-boot-starter-webmvc`) are on the classpath:

- ✅ Configures `DispatcherServlet`
- ✅ Sets up Spring MVC
- ✅ Configures view resolvers
- ✅ Configures message converters
- ✅ Starts embedded Tomcat

**You get**: A fully functional web application!

#### Example 2: Database

**If** H2 driver is on classpath:

- ✅ Configures an in-memory `DataSource`
- ✅ Initializes the database
- ✅ Ready for JPA/JDBC

**If** JPA is on classpath:

- ✅ Configures `EntityManagerFactory`
- ✅ Configures `JpaTransactionManager`
- ✅ Sets up Hibernate

#### Example 3: Security

**If** `spring-boot-starter-security` is on classpath:

- ✅ Enables basic authentication
- ✅ Creates a default user (username: `user`, password: printed in console)
- ✅ Secures all endpoints

### Conditional Auto-Configuration

Auto-configuration is intelligent - it only activates when certain conditions are met:

```java
@AutoConfiguration                              // a @Configuration that Boot loads last
@ConditionalOnClass(DataSource.class)           // Only if DataSource is on classpath
public class DataSourceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DataSource.class) // Only if you haven't defined one
    public DataSource dataSource(...) { ... }
}
```

(Simplified — the real class has more conditions.) **You can always override** auto-configuration by defining your own beans: auto-configuration classes are processed *after* your own configuration, so `@ConditionalOnMissingBean` sees your bean and backs off.

### The @SpringBootApplication Annotation

```java
@SpringBootApplication
public class PizzaStoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(PizzaStoreApplication.class, args);
    }
}
```

**`@SpringBootApplication`** is a composite annotation:

```java
@SpringBootApplication
= 
@SpringBootConfiguration    // Same as @Configuration - marks this as a configuration class
+ @EnableAutoConfiguration  // Enable Spring Boot auto-configuration
+ @ComponentScan            // Scan for components in this package and sub-packages
```

It's a convenience annotation that combines three annotations into one!

### @EnableAutoConfiguration

This is the magic! It tells Spring Boot:

- Look at the classpath
- Look at existing beans
- Make educated guesses about what you need
- Configure it automatically

### Viewing Auto-Configuration Report

Run your application with debug logging:

**application.properties**:

```properties
debug=true
```

Or run with:

```bash
java -jar pizzastore.jar --debug
```

You'll see output like:

```
============================
CONDITIONS EVALUATION REPORT
============================

Positive matches:
-----------------
   DataSourceAutoConfiguration matched:
      - @ConditionalOnClass found required classes 'javax.sql.DataSource'
   
   WebMvcAutoConfiguration matched:
      - @ConditionalOnClass found required classes 'DispatcherServlet'

Negative matches:
-----------------
   SecurityAutoConfiguration:
      Did not match:
         - @ConditionalOnClass did not find required class 'SecurityFilterChain'
```

### Customizing Auto-Configuration

#### Override with Properties

Many aspects can be customized via properties:

```properties
# Server
server.port=9090
server.servlet.context-path=/pizzastore

# Tomcat
server.tomcat.threads.max=200

# JSON
spring.jackson.serialization.indent-output=true

# Logging
logging.level.root=WARN
logging.level.com.example=DEBUG
```

#### Override with Your Own Bean

Define a bean of the same type, and the auto-configured one backs off:

```java
@Configuration
public class JsonConfig {

    @Bean
    public JsonMapper jsonMapper() {   // Jackson 3; replaces Boot's auto-configured mapper
        return JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
    }
}
```

Prefer a property (above) when one exists — replacing a whole bean also drops every other default Boot had configured on it.

> **Don't** put `@ConditionalOnMissingBean` on beans in your *own* `@Configuration` classes: the Spring Boot docs warn it is only reliable in auto-configuration classes, because the result depends on the order in which configuration classes are processed.

#### Disable Auto-Configuration

Disable specific auto-configuration classes:

```java
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class PizzaStoreApplication {
    // ...
}
```

Or in properties:

```properties
spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
```

Note the package: Spring Boot 4 split auto-configuration into per-technology modules (`spring-boot-jdbc`, `spring-boot-webmvc`, ...), so `DataSourceAutoConfiguration` now lives in `org.springframework.boot.jdbc.autoconfigure`. Older tutorials use the Boot 3 package `org.springframework.boot.autoconfigure.jdbc`.

---

## 🏗️ Creating Spring Boot Projects

There are three main ways to initialize a Spring Boot project:

### 1. Spring Initializr - Web Interface

**URL**: https://start.spring.io

**Steps**:

1. Go to https://start.spring.io
2. Choose project settings:
   - **Project**: Maven
   - **Language**: Java
   - **Spring Boot version**: a 4.x release — this course's projects use **4.0.8**; Initializr may offer a newer 4.x by default, which works for this lesson too
   - **Group**: `be.vives`
   - **Artifact**: `pizzastore-intro`
   - **Packaging**: Jar
   - **Java**: 25
3. Add dependencies: "Spring Web", "Spring Data JPA", "H2 Database", "Validation", "Spring Boot DevTools" (Initializr adds the H2 Console module automatically when you pick both Spring Web and H2)
4. Click "Generate" - downloads a ZIP file
5. Extract and open in IntelliJ IDEA

### 2. IntelliJ IDEA

**Steps**:

1. **File → New → Project**
2. Select **Spring Initializr** from the left panel
3. Fill in project metadata:
   - **Group**: `be.vives`
   - **Artifact**: `pizzastore-intro`
   - **Java**: 25
4. Select dependencies
5. Click **Create**

IntelliJ generates the project structure directly in your workspace.

### 3. Spring Boot CLI

**Install Spring Boot CLI** first, then:

```bash
spring init --dependencies=web,data-jpa,h2,validation,devtools pizzastore
```

**Options**:

- `--dependencies` or `-d`: Add dependencies
- `--build`: maven or gradle
- `--java-version`: 17, 21, 25, etc.
- `--packaging`: jar or war

**Example**:

```bash
spring init -dweb,data-jpa,h2,validation,devtools -b4.0.8 --java-version=25 pizzastore
```

The dependency ids are Initializr's, not Maven artifact names: `web` ("Spring Web") produces `spring-boot-starter-webmvc`.

---

## 📂 Spring Boot Project Structure

After creating a project, you'll see:

```
pizzastore-intro/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── be/
│   │   │       └── vives/
│   │   │           └── pizzastore/
│   │   │               ├── PizzaStoreApplication.java    # Main class
│   │   │               ├── controller/
│   │   │               │   └── PizzaController.java      # REST controllers
│   │   │               ├── dto/
│   │   │               │   └── PizzaRequest.java         # Data transfer objects
│   │   │               ├── service/
│   │   │               │   └── PizzaService.java         # Business logic
│   │   │               ├── repository/
│   │   │               │   └── PizzaRepository.java      # Data access (JPA)
│   │   │               └── domain/
│   │   │                   └── Pizza.java                # Domain entities
│   │   └── resources/
│   │       ├── application.properties                    # Configuration
│   │       └── data.sql                                  # Sample data
│   └── test/
│       └── java/
│           └── be/
│               └── vives/
│                   └── pizzastore/
│                       └── PizzaStoreApplicationTests.java
└── pom.xml
```

**💡 Example Project**: the complete working example is [`pizzastore-intro/`](pizzastore-intro/).

### Key Files

**[`pom.xml`](pizzastore-intro/pom.xml)** — the important parts:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.8</version>          <!-- manages the versions of everything below -->
    <relativePath/>
</parent>

<properties>
    <java.version>25</java.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webmvc</artifactId>   <!-- no <version>! -->
    </dependency>
    <!-- + spring-boot-starter-data-jpa, spring-boot-starter-validation,
         spring-boot-h2console, h2 (runtime), spring-boot-devtools (runtime, optional),
         and a *-test starter per main starter (test scope) -->
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

💡 This is what start.spring.io generates for Group `be.vives`, Artifact `pizzastore-intro`, Java 25 and the dependencies from Step 1 below, apart from the project's version (`1.0.0` instead of Initializr's `0.0.1-SNAPSHOT`) and the explanatory comments. Notice that **no dependency has a `<version>`**: the parent decides, and every starter has a matching `-test` starter.

**PizzaStoreApplication.java**:

```java
package be.vives.pizzastore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PizzaStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(PizzaStoreApplication.class, args);
    }
}
```

---

## 🔍 Spring Boot DevTools

For faster development, Spring Boot DevTools is already included if you selected it during project creation.

**Features:**

- **Automatic restart** when code changes
- **LiveReload** support for browser refresh
- **Caching disabled** in development
- **Enhanced development experience**

### IntelliJ Configuration for Auto-restart

1. **File** → **Settings** (**IntelliJ IDEA** → **Settings** on Mac) → **Build, Execution, Deployment** → **Compiler**
2. Check ✅ **"Build project automatically"**
3. In the same Settings dialog, open **Advanced Settings**
4. Under **Compiler**, enable ✅ **"Allow auto-make to start even if developed application is currently running"**

Now your application will restart automatically when you make changes! (Old tutorials put step 4 in the hidden *Registry* as `compiler.automake.allow.when.app.running`; IntelliJ moved it to Advanced Settings.)

### What Triggers a Restart?

- ✅ Java code changes
- ✅ Configuration file changes (application.properties)
- ❌ Static resources (no restart, just LiveReload)

### How It Works

DevTools uses two classloaders:

- **Base classloader**: For libraries that don't change (dependencies)
- **Restart classloader**: For your application code

When you change code, only the restart classloader reloads → **much faster than full restart**!

---

## 🍕 Demo: Building a first PizzaStore

Now let's build the PizzaStore REST API using proper JPA and H2 database.

### Step 1: Create the Project

Follow the **Creating Spring Boot Projects** section above to create a project with:

- Spring Web
- Spring Data JPA
- H2 Database
- Validation
- Spring Boot DevTools

**Note**: as of Spring Boot 4, the H2 Console has its own module — `spring-boot-h2console` — separate from the `h2` driver itself. Both are needed if you want to browse the database in your browser (see Step 2). Initializr adds it for you when you select Spring Web and H2 Database.

### Step 2: Configure application.properties

**Create [`src/main/resources/application.properties`](pizzastore-intro/src/main/resources/application.properties)**:

```properties
# Application name
spring.application.name=pizzastore

# Database configuration
spring.datasource.url=jdbc:h2:mem:pizzastoredb
spring.datasource.username=sa
spring.datasource.password=
spring.datasource.driver-class-name=org.h2.Driver

# H2 Console (accessible at http://localhost:8080/h2-console)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.defer-datasource-initialization=true

# Server
server.port=8080

# Logging
logging.level.root=INFO
logging.level.be.vives.pizzastore=DEBUG
logging.level.org.hibernate.SQL=DEBUG

# DevTools (optional - usually defaults are fine)
spring.devtools.restart.enabled=true
```

**Explanation**:

- **H2 Database**: In-memory database, perfect for development
- **ddl-auto=create-drop**: Creates tables on startup, drops on shutdown
- **show-sql=true**: Shows SQL statements in console
- **h2.console.enabled=true**: Enables the web console — but only takes effect if the `spring-boot-h2console` starter from Step 1 is on the classpath; without it, `/h2-console` returns a plain `404`
- **defer-datasource-initialization=true**: Runs `data.sql` *after* Hibernate creates the schema (instead of before), since Hibernate — not a manual schema script — owns table creation here

### Step 3: Create the Pizza Entity

For this introductory demo, we keep the Pizza entity simple with just three fields: id, name, and price. This matches the `Pizza` entity of the final PizzaStore project — later lessons will simply *add* fields and relationships (description, image, availability, nutritional info, ...) on top of this same entity.

**[`src/main/java/be/vives/pizzastore/domain/Pizza.java`](pizzastore-intro/src/main/java/be/vives/pizzastore/domain/Pizza.java)** — the mapping (the no-arg and `(name, price)` constructors, getters/setters and `toString()` are in the file):

```java
@Entity
@Table(name = "pizzas")
public class Pizza {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    public Pizza() {          // JPA needs a no-arg constructor
    }
    
    // Pizza(String name, BigDecimal price), getters, setters, toString()
}
```

**Key JPA Annotations**:

- `@Entity`: Marks this as a JPA entity
- `@Table(name = "pizzas")`: Specifies the table name
- `@Id`: Marks the primary key
- `@GeneratedValue`: Auto-generates the ID
- `@Column`: Configures column properties (nullable, length, precision)

### Step 4: Create a Request DTO with Validation

**Important principle**: Never expose entities directly via REST APIs. Use DTOs (Data Transfer Objects) instead.

This demo applies that principle to the **input** only: the controller below still *returns* the `Pizza` entity, to keep the first version small. The real PizzaStore adds response DTOs and a mapper in Lesson 7.

For creating a pizza, we use a **PizzaRequest** record that contains only the data needed from the client. We also add validation to ensure data quality.

**[`src/main/java/be/vives/pizzastore/dto/PizzaRequest.java`](pizzastore-intro/src/main/java/be/vives/pizzastore/dto/PizzaRequest.java)**:

```java
package be.vives.pizzastore.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PizzaRequest(
        @NotBlank(message = "Name is required")
        String name,
        
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Price must be at least 0")
        @DecimalMax(value = "20.0", inclusive = true, message = "Price must not exceed 20")
        BigDecimal price
) {
}
```

**Key Points**:

- **Java Record**: Concise syntax for immutable data carriers (Java 14+)
- **@NotBlank**: Validates that name is not null, empty, or whitespace
- **@NotNull**: Validates that price is provided
- **@DecimalMin / @DecimalMax**: Validates that price is between 0 and 20
- **Custom messages**: Provide user-friendly error messages

**Why use a DTO?**:

- **Separation of concerns**: API contracts are independent from database schema
- **Validation**: Input validation at the API boundary
- **Security**: Prevents clients from manipulating fields they shouldn't (like `id`)
- **Flexibility**: Can change the entity without breaking the API
- **Clarity**: Clear contract of what data is expected

### Step 5: Create the Repository Interface

**[`src/main/java/be/vives/pizzastore/repository/PizzaRepository.java`](pizzastore-intro/src/main/java/be/vives/pizzastore/repository/PizzaRepository.java)**:

```java
package be.vives.pizzastore.repository;

import be.vives.pizzastore.domain.Pizza;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PizzaRepository extends JpaRepository<Pizza, Long> {
}
```

**Magic of Spring Data JPA**:

- You only write the **interface**
- Spring Data JPA creates the **implementation** at runtime
- Basic CRUD methods are provided automatically: `findAll()`, `findById()`, `save()`, `deleteById()`, etc.
- No `@Repository` needed: Spring Data finds every interface that extends one of its repository interfaces, registers it as a bean, and adds the exception translation that `@Repository` gives a class (Lesson 2)

### Step 6: Create the Service Layer

The service layer handles business logic and performs the **mapping** between DTOs and entities.

**[`src/main/java/be/vives/pizzastore/service/PizzaService.java`](pizzastore-intro/src/main/java/be/vives/pizzastore/service/PizzaService.java)**:

```java
package be.vives.pizzastore.service;

import be.vives.pizzastore.dto.PizzaRequest;
import be.vives.pizzastore.domain.Pizza;
import be.vives.pizzastore.repository.PizzaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PizzaService {
    
    private static final Logger log = LoggerFactory.getLogger(PizzaService.class);
    private final PizzaRepository pizzaRepository;
    
    public PizzaService(PizzaRepository pizzaRepository) {
        this.pizzaRepository = pizzaRepository;
        log.info("PizzaService initialized");
    }
    
    public List<Pizza> getAllPizzas() {
        log.debug("Fetching all pizzas");
        return pizzaRepository.findAll();
    }
    
    public Pizza getPizzaById(Long id) {
        log.debug("Fetching pizza with id: {}", id);
        return pizzaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Pizza not found with id: " + id));
    }
    
    public Pizza createPizza(PizzaRequest request) {
        log.info("Creating pizza: {}", request.name());
        // Manual mapping from DTO to Entity
        Pizza pizza = new Pizza(request.name(), request.price());
        return pizzaRepository.save(pizza);
    }
}
```

**Key Points**:

- `@Service`: Marks this as a Spring service component
- `@Transactional`: Ensures database operations are transactional
- **Manual mapping**: Simple, one-to-one mapping from `PizzaRequest` to `Pizza` entity

**Note**: For more complex mappings, you would use a library like MapStruct. We'll cover that in a later lesson.

### Step 7: Create the REST Controller

**[`src/main/java/be/vives/pizzastore/controller/PizzaController.java`](pizzastore-intro/src/main/java/be/vives/pizzastore/controller/PizzaController.java)**:

```java
package be.vives.pizzastore.controller;

import be.vives.pizzastore.dto.PizzaRequest;
import be.vives.pizzastore.domain.Pizza;
import be.vives.pizzastore.service.PizzaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pizzas")
public class PizzaController {
    
    private final PizzaService pizzaService;
    
    public PizzaController(PizzaService pizzaService) {
        this.pizzaService = pizzaService;
    }
    
    @GetMapping
    public List<Pizza> getAllPizzas() {
        return pizzaService.getAllPizzas();
    }
    
    @GetMapping("/{id}")
    public Pizza getPizza(@PathVariable Long id) {
        return pizzaService.getPizzaById(id);
    }
    
    @PostMapping
    public ResponseEntity<Pizza> createPizza(@Valid @RequestBody PizzaRequest request) {
        Pizza created = pizzaService.createPizza(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
```

**Key Annotations**:

- `@RestController`: Combines `@Controller` and `@ResponseBody`
- `@RequestMapping("/api/pizzas")`: Base path for all endpoints
- `@GetMapping`, `@PostMapping`: HTTP methods
- `@PathVariable`: Extracts value from URL path
- `@RequestBody`: Converts JSON to Java object
- `@Valid`: Triggers validation on the `PizzaRequest` object

**Validation in Action**:
When `@Valid` is used, Spring automatically validates the request object based on the constraints defined in `PizzaRequest`. If validation fails, Spring returns a `400 Bad Request` without calling your method. This project doesn't have a custom exception handler yet, so what the body contains depends on how you run the app (see Step 10) — we'll build a proper `GlobalExceptionHandler` that returns field-level error details in [Lesson 10](../lesson-10-validation-exception-handling/README.md).

### Step 8: Add Sample Data (Optional)

**Create [`src/main/resources/data.sql`](pizzastore-intro/src/main/resources/data.sql)**:

```sql
INSERT INTO pizzas (name, price)
VALUES ('Classic tomato and mozzarella', 8.99);

INSERT INTO pizzas (name, price)
VALUES ('Pepperoni and cheese', 10.99);

INSERT INTO pizzas (name, price)
VALUES ('Four cheese blend', 11.99);

INSERT INTO pizzas (name, price)
VALUES ('Fresh vegetables', 9.99);

INSERT INTO pizzas (name, price)
VALUES ('Spicy salami', 12.99);
```

**Note**: Spring Boot automatically executes `data.sql` on startup when using H2, after Hibernate creates the schema (thanks to `spring.jpa.defer-datasource-initialization=true` from Step 2).

### Step 9: Run the Application

#### From IntelliJ

1. Right-click `PizzaStoreApplication.java`
2. Select **Run 'PizzaStoreApplication'**

#### From Command Line

```bash
# Using Maven
mvn spring-boot:run

# Or build and run the JAR
mvn clean package
java -jar target/pizzastore-intro-1.0.0.jar
```

#### Expected Console Output

```
 :: Spring Boot ::                (v4.0.8)

... INFO ... [  restartedMain] .e.DevToolsPropertyDefaultsPostProcessor : Devtools property defaults active! Set 'spring.devtools.add-properties' to 'false' to disable
... INFO ... [  restartedMain] b.vives.pizzastore.service.PizzaService  : PizzaService initialized
... INFO ... [  restartedMain] o.s.boot.tomcat.TomcatWebServer          : Tomcat started on port 8080 (http) with context path '/'
... INFO ... [  restartedMain] b.v.pizzastore.PizzaStoreApplication     : Started PizzaStoreApplication in 3.716 seconds (process running for 4.172)
```

(Timestamps and process ids shortened to `...`.) You'll also see the SQL Hibernate runs, because of `show-sql=true`. The thread name `restartedMain` and the "Devtools property defaults active" line tell you DevTools is running — remember that for Step 10.

### Step 10: Test the API

#### Using Browser

Open: http://localhost:8080/api/pizzas

#### Using H2 Console

Open: http://localhost:8080/h2-console

- **JDBC URL**: `jdbc:h2:mem:pizzastoredb`
- **Username**: `sa`
- **Password**: (leave empty)

You can run SQL queries directly:

```sql
SELECT * FROM pizzas;
```

#### Using curl

**Get all pizzas** — `200 OK`:

```bash
curl http://localhost:8080/api/pizzas
```

```json
[{"name":"Classic tomato and mozzarella","price":8.99,"id":1},{"name":"Pepperoni and cheese","price":10.99,"id":2},{"name":"Four cheese blend","price":11.99,"id":3},{"name":"Fresh vegetables","price":9.99,"id":4},{"name":"Spicy salami","price":12.99,"id":5}]
```

**Get specific pizza** — `200 OK`:

```bash
curl http://localhost:8080/api/pizzas/1
```

```json
{"name":"Classic tomato and mozzarella","price":8.99,"id":1}
```

**Create new pizza (valid)** — `201 Created`:

```bash
curl -X POST http://localhost:8080/api/pizzas \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Hawaii",
    "price": 11.99
  }'
```

```json
{"name":"Hawaii","price":11.99,"id":6}
```

The five rows from `data.sql` took ids 1–5, so the database assigns 6.

**Create pizza with validation error (price too high)** — `400 Bad Request`:

```bash
curl -X POST http://localhost:8080/api/pizzas \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Expensive Pizza",
    "price": 25.00
  }'
```

**Create pizza with validation error (missing name)** — `400 Bad Request`:

```bash
curl -X POST http://localhost:8080/api/pizzas \
  -H "Content-Type: application/json" \
  -d '{
    "price": 10.00
  }'
```

**Get a pizza that doesn't exist** — `500 Internal Server Error`:

```bash
curl http://localhost:8080/api/pizzas/99
```

#### What the Error Responses Look Like

The body of those three error responses depends on **how you started the app**.

**Run as a plain JAR** (`java -jar target/pizzastore-intro-1.0.0.jar`) — Spring Boot's production default. The client learns *that* the request failed, but not *why*:

```json
{"timestamp":"2026-10-25T12:20:42.439Z","status":400,"error":"Bad Request","path":"/api/pizzas"}
```

```json
{"timestamp":"2026-10-25T12:20:42.486Z","status":500,"error":"Internal Server Error","path":"/api/pizzas/99"}
```

**Run from IntelliJ or with `mvn spring-boot:run`** — DevTools is active and switches on `server.error.include-message`, `include-binding-errors` and `include-stacktrace` for development. The same 400 now also contains (shortened):

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for object='pizzaRequest'. Error count: 1",
  "errors": [
    {
      "field": "price",
      "rejectedValue": 25.00,
      "code": "DecimalMax",
      "defaultMessage": "Price must not exceed 20"
    }
  ],
  "trace": "org.springframework.web.bind.MethodArgumentNotValidException: ...",
  "path": "/api/pizzas"
}
```

and the 500 contains `"message":"Pizza not found with id: 99"` plus the full stack trace. Handy while developing — but a stack trace is not something to send to a real client, and DevTools is never active in a packaged JAR. In both modes, the server log shows the validation failure as a `WARN` from `DefaultHandlerExceptionResolver`.

Two things are clearly unfinished: a missing pizza is a `500` (it should be a `404 Not Found`), and the error body depends on a development tool. A `GlobalExceptionHandler` that returns proper status codes and field-level errors in every mode comes in [Lesson 10](../lesson-10-validation-exception-handling/README.md).

#### Using Postman

1. **GET** `http://localhost:8080/api/pizzas` - Get all pizzas
2. **GET** `http://localhost:8080/api/pizzas/1` - Get pizza by ID
3. **POST** `http://localhost:8080/api/pizzas` - Create new pizza (set Body → raw → JSON)

   ```json
   {
     "name": "Hawaii",
     "price": 11.99
   }
   ```

Notice that Jackson writes `id` *last*: the field order in the JSON doesn't follow the order of the fields in `Pizza`. JSON object members are unordered, so clients must never rely on it.

---

## 🔧 What Just Happened?

### Auto-Configuration in Action

Spring Boot automatically configured:

1. ✅ **DataSource** - H2 in-memory database connection
2. ✅ **EntityManagerFactory** - JPA entity manager
3. ✅ **TransactionManager** - Database transaction management
4. ✅ **JPA Repositories** - Implementation of PizzaRepository
5. ✅ **DispatcherServlet** - Handles HTTP requests
6. ✅ **Embedded Tomcat** - Runs on port 8080
7. ✅ **JSON conversion** - Jackson serializes/deserializes objects
8. ✅ **Component scanning** - Found all our `@Service`, `@Repository`, `@RestController`
9. ✅ **Dependency injection** - Wired all dependencies
10. ✅ **Hibernate** - ORM for database operations
11. ✅ **H2 Console** - Web interface for database
12. ✅ **Logging** - SLF4J with Logback

**We wrote ZERO configuration for any of this!**

### Key Spring Boot Annotations

**Application-Level**:

```java
@SpringBootApplication     // Main application class
@SpringBootConfiguration   // Configuration class
@EnableAutoConfiguration  // Enable auto-config
@ComponentScan           // Scan for components
```

**JPA/Persistence**:

```java
@Entity                  // Marks a JPA entity
@Table                   // Specifies table name
@Id                      // Primary key
@GeneratedValue          // Auto-generate ID
@Column                  // Column configuration
```

**Service Layer**:

```java
@Service                // Service component
@Transactional         // Transactional methods
```

**REST API**:

```java
@RestController         // REST controller (returns JSON/XML)
@RequestMapping("/api") // Base URL mapping
@GetMapping            // HTTP GET
@PostMapping           // HTTP POST
@PutMapping            // HTTP PUT
@DeleteMapping         // HTTP DELETE
@PathVariable          // URL path variable: /api/pizzas/{id}
@RequestParam          // Query parameter: /api/pizzas?name=Margherita
@RequestBody           // Request body (JSON → Object)
```

---

## 🏗️ Spring Boot Build Plugin

The `spring-boot-maven-plugin` makes your application executable:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

**What it does:**

- Creates an **executable JAR** with embedded server
- Includes all dependencies

### Build Commands

```bash
# Clean and build
mvn clean package

# Run tests
mvn test

# Run application
mvn spring-boot:run

# Build without tests (faster)
mvn clean package -DskipTests

# Run the JAR
java -jar target/pizzastore-intro-1.0.0.jar
```

---

## 📊 Spring Boot Actuator

Spring Boot Actuator provides **production-ready features** to monitor and manage your application.

### Adding Actuator

**pom.xml**:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Actuator Endpoints

After adding the dependency, you get endpoints:

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health` | Application health status |
| `/actuator/info` | Application information |
| `/actuator/metrics` | Application metrics |
| `/actuator/env` | Environment properties |
| `/actuator/beans` | All Spring beans |
| `/actuator/mappings` | All request mappings |

### Example

**Check health**:

```bash
curl http://localhost:8080/actuator/health
```

Response:

```json
{
  "status": "UP"
}
```

### Enabling More Endpoints

**application.properties**:

```properties
management.endpoints.web.exposure.include=health,info,metrics,beans,mappings
```

**View all beans**:

```bash
curl http://localhost:8080/actuator/beans
```

**View all URL mappings**:

```bash
curl http://localhost:8080/actuator/mappings
```

**Note**: Actuator isn't covered in a dedicated lesson in this course — this is just a quick taste of what Spring Boot offers for production monitoring.

---

## 💡 Best Practices

1. **Let the parent manage versions** — never put a `<version>` on a Spring Boot starter.
2. **Prefer properties over custom beans** to change auto-configuration (`server.port`, `spring.jackson.*`, ...); replace a bean only when no property exists.
3. **Check what was auto-configured** with `--debug` (the conditions evaluation report) instead of guessing.
4. **Use constructor injection** in controllers and services, exactly as in Lesson 2 — Boot doesn't change that.
5. **Keep request DTOs separate from entities**, and validate them with `@Valid`.
6. **Don't rely on DevTools behaviour** — it is development-only; test the packaged JAR too.
7. **Keep `ddl-auto=create-drop` and the H2 console for development** — never for a production database.

---

## 🎓 Summary

### What We Learned

1. **Spring Boot Philosophy**
   - Convention over Configuration
   - Opinionated defaults that work out of the box
   - Zero XML configuration required
   - Stand-alone, production-grade applications

2. **Key Features**
   - **Starters** - Simplified dependency management with pre-configured bundles
   - **Auto-configuration** - Automatic setup based on classpath scanning
   - **Embedded servers** - No external deployment needed (Tomcat, or Jetty)
   - **Production features** - Actuator for monitoring, metrics, and health checks
   - **DevTools** - Automatic restart and enhanced development experience

3. **Creating Projects**
   - Spring Initializr (web interface, IntelliJ, CLI)
   - Pre-configured project structure
   - Ready to run in minutes

4. **@SpringBootApplication**
   - Composite annotation combining:
     - `@SpringBootConfiguration` (same as `@Configuration`)
     - `@EnableAutoConfiguration` (enables auto-configuration)
     - `@ComponentScan` (scans for components)
   - Single annotation to bootstrap everything

5. **Building with JPA and H2**
   - Created JPA entities with proper annotations
   - Used Spring Data JPA repositories (no implementation needed!)
   - Configured H2 in-memory database
   - Built a small REST API: list, get by id, and create with validation
   - Used a service layer with transactions

6. **DevTools**
   - Fast automatic restarts
   - LiveReload for static resources
   - Dramatically improves development speed

### Spring vs Spring Boot

| Aspect | Spring Framework | Spring Boot |
|--------|------------------|-------------|
| **Configuration** | Manual, XML or Java config classes | Auto-configuration based on classpath |
| **Dependency Management** | Manual version management | Starters with managed versions |
| **Server** | External (Tomcat, Glassfish, etc.) | Embedded (Tomcat or Jetty) |
| **Deployment** | WAR file to application server | Executable JAR with `java -jar` |
| **Setup Time** | Hours to days | Minutes |
| **Production Features** | Manual setup required | Actuator built-in |
| **Learning Curve** | Steeper | Gentler - sensible defaults |

### Key Takeaways

✅ **Faster development** - Less boilerplate and configuration  
✅ **Easier dependency management** - Starters with compatible versions  
✅ **Production-ready** - Actuator for monitoring and metrics  
✅ **Microservices-friendly** - Self-contained executable JARs  
✅ **Convention over Configuration** - Sensible defaults that just work  
✅ **Still flexible** - Can override any auto-configuration  
✅ **Better developer experience** - DevTools for rapid development

### What's Next?

The demo controller does everything in one quick pass. Lesson 5 looks at the web layer underneath it — Spring MVC, the `DispatcherServlet`, request mapping and Jackson — before the real PizzaStore starts in Lesson 6.

---

## 📖 Additional Resources

- [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/index.html)
- [Spring Initializr](https://start.spring.io)
- [Spring Boot Starters List](https://docs.spring.io/spring-boot/reference/using/build-systems.html#using.build-systems.starters)
- [Spring Boot DevTools Documentation](https://docs.spring.io/spring-boot/reference/using/devtools.html)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/reference/actuator/index.html)
- [Spring Boot Auto-Configuration](https://docs.spring.io/spring-boot/reference/using/auto-configuration.html)

**Note on the book**: This lesson follows *Pro Spring Boot 4*, Chapter 1: *Spring Boot Quick Start*, project *Customer CRM* (pp. 1–14): the Spring Initializr walkthrough (p. 2), project structure and `pom.xml` (pp. 4–5), a domain model, repository and REST controller (pp. 6–11), running it (p. 12) and a `@SpringBootTest` context test (p. 13) — the same shape as `pizzastore-intro`. The book uses Java 21 and an in-memory hardcoded list instead of a database; this lesson uses Java 25 and wires up JPA + H2 (the book's JPA material is Chapter 6, *Spring Data with Spring Boot*, which Lesson 6a follows). The `@SpringBootApplication` breakdown and how auto-configuration works come from Chapter 2, *How Spring Boot Works (The Magic)* (pp. 33–36). Actuator, shown briefly here, has its own Chapter 13 (p. 361); this course doesn't cover it further. The book's Chapter 1 second project (*Management CRM*, a reactive WebFlux app, pp. 15–29) is out of scope.

---

## 🚀 Runnable Project

[`pizzastore-intro/`](pizzastore-intro/) is the complete demo from this lesson: a stand-alone Spring Boot 4.0.8 / Java 25 project.

- ✅ `Pizza` entity (id, name, price) with Spring Data JPA on H2, seeded from `data.sql`
- ✅ `PizzaRequest` record with Bean Validation
- ✅ `PizzaService` with `@Transactional` and manual DTO → entity mapping
- ✅ `PizzaController`: `GET /api/pizzas`, `GET /api/pizzas/{id}`, `POST /api/pizzas`
- ✅ H2 console and DevTools
- ❌ Spring MVC in depth — Lesson 5
- ❌ The real PizzaStore domain (customers, orders, relationships) — Lesson 6a
- ❌ Response DTOs and MapStruct mappers — Lesson 7
- ❌ Full CRUD, `404`s and `Location` headers — Lesson 9
- ❌ Error handling with `ProblemDetail` — Lesson 10

### Running It

```bash
cd lesson-04-spring-boot-intro/pizzastore-intro
mvn spring-boot:run

# 200 - all five seeded pizzas
curl http://localhost:8080/api/pizzas

# 201 - created with id 6
curl -X POST http://localhost:8080/api/pizzas -H "Content-Type: application/json" -d '{"name":"Hawaii","price":11.99}'

# 400 - price above 20
curl -X POST http://localhost:8080/api/pizzas -H "Content-Type: application/json" -d '{"name":"Expensive Pizza","price":25.00}'

# 500 - no pizza 99 (a 404 comes in Lesson 9/10)
curl http://localhost:8080/api/pizzas/99
```

`mvn test` runs the single `contextLoads()` test, which starts the whole application context.

---

**Congratulations!** 🎉 You've created your first Spring Boot application with JPA and H2, and started the PizzaStore journey! Continue to [Lesson 5: Spring MVC & Building a REST API](../lesson-05-spring-mvc/README.md) to see what happens inside the web layer.
