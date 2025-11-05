# Lesson 4: Getting Started with Spring Boot

## 📚 Table of Contents

- [📘 Overview](#-overview)
- [🎯 Learning Objectives](#-learning-objectives)
- [🤔 Spring Rebooted](#-spring-rebooted)
- [🚀 What is Spring Boot?](#-what-is-spring-boot)
- [✨ Spring Boot Key Features](#-spring-boot-key-features)
- [🎯 A Simple Web Application - Just the Essentials](#-a-simple-web-application---just-the-essentials)
- [📦 Spring Boot Starters](#-spring-boot-starters)
- [⚙️ Auto-Configuration](#️-auto-configuration)
- [🏗️ Creating Spring Boot Projects](#️-creating-spring-boot-projects)
- [📂 Spring Boot Project Structure](#-spring-boot-project-structure)
- [🔍 Spring Boot DevTools](#-spring-boot-devtools)
- [🍕 Building the PizzaStore - Complete Example with JPA](#-building-the-pizzastore---complete-example-with-jpa)
- [🔧 What Just Happened?](#-what-just-happened)
- [🏗️ Spring Boot Build Plugin](#️-spring-boot-build-plugin)
- [📊 Spring Boot Actuator](#-spring-boot-actuator)
- [💡 Tips & Tricks](#-tips--tricks)
- [🎓 Summary](#-summary)
- [📖 Additional Resources](#-additional-resources)

---

## 📘 Overview

In the previous lessons, we learned about Spring Framework - Dependency Injection, IoC, configuration with properties and profiles. While Spring is powerful, it requires **a lot of configuration**.

**Spring Boot** changes this by embracing **"Convention over Configuration"**. It makes it easy to create **stand-alone, production-grade** Spring applications that you can **"just run"**.

In this lesson, we'll discover how Spring Boot simplifies Spring development dramatically and start building our **PizzaStore API** project.

## 🎯 Learning Objectives

By the end of this lesson, you will:
- Understand **what Spring Boot is** and why it exists
- Learn about **Convention over Configuration**
- Understand **Spring Boot Starters** and dependency management
- Learn about **Auto-configuration** and how it works
- Create Spring Boot projects with **Spring Initializr**
- Understand the **@SpringBootApplication** annotation
- Use **Spring Boot DevTools** for rapid development
- Build your first REST API with Spring Boot using **JPA and H2**
- Work with the **embedded servlet container**
- Get introduced to **Spring Boot Actuator**
- **Start the PizzaStore project**

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

- **Tomcat**, **Jetty**, or **Undertow** embedded by default
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
    <version>6.1.0</version>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-web</artifactId>
    <version>6.1.0</version>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-webmvc</artifactId>
    <version>6.1.0</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.0</version>
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
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

**No version needed** - inherited from `spring-boot-starter-parent`.

This single starter brings in:
- Spring MVC
- Jackson for JSON
- Hibernate Validator
- Embedded Tomcat
- All with compatible versions!

### Common Starters

| Starter | Purpose | Includes |
|---------|---------|----------|
| `spring-boot-starter-web` | Web applications with Spring MVC, REST | Spring MVC, Tomcat, JSON |
| `spring-boot-starter-data-jpa` | JPA with Hibernate | Spring Data JPA, Hibernate, JDBC |
| `spring-boot-starter-security` | Spring Security | Spring Security |
| `spring-boot-starter-test` | Testing | JUnit, Mockito, AssertJ |
| `spring-boot-starter-validation` | Bean Validation | Hibernate Validator |
| `spring-boot-starter-actuator` | Production monitoring | Metrics, health checks |

**Full list**: https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.build-systems.starters

### How Starters Work

```
spring-boot-starter-web
    ├── spring-boot-starter
    │   ├── spring-boot
    │   ├── spring-boot-autoconfigure
    │   └── logging dependencies
    ├── spring-boot-starter-tomcat
    │   └── tomcat-embed-*
    ├── spring-web
    ├── spring-webmvc
    └── jackson-databind
```

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

**If** `spring-webmvc` is on classpath:
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
@Configuration
@ConditionalOnClass(DataSource.class)           // Only if DataSource is on classpath
@ConditionalOnMissingBean(DataSource.class)     // Only if you haven't defined one
public class DataSourceAutoConfiguration {
    // Auto-configuration logic
}
```

**You can always override** auto-configuration by defining your own beans.

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

#### Conditional Configuration

Use `@ConditionalOnMissingBean` to provide defaults:

```java
@Configuration
public class CustomConfig {
    
    @Bean
    @ConditionalOnMissingBean
    public MyService myService() {
        return new DefaultMyService();
    }
}
```

**If** no `MyService` bean exists, Spring Boot creates this one.  
**If** you define your own, Spring Boot uses yours.

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
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

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
   - **Spring Boot version**: 3.5.7 (latest stable)
   - **Group**: `be.vives`
   - **Artifact**: `pizzastore-intro`
   - **Packaging**: Jar
   - **Java**: 25
3. Add dependencies: "Spring Web", "Spring Data JPA", "H2 Database", "Spring Boot DevTools"
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
spring init --dependencies=web,data-jpa,h2,devtools pizzastore
```

**Options**:
- `--dependencies` or `-d`: Add dependencies
- `--build`: maven or gradle
- `--java-version`: 17, 21, 25, etc.
- `--packaging`: jar or war

**Example**:
```bash
spring init -dweb,data-jpa,h2,devtools -b3.5.7 --java-version=25 pizzastore
```

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
│   │   │               ├── service/
│   │   │               │   └── PizzaService.java         # Business logic
│   │   │               ├── repository/
│   │   │               │   └── PizzaRepository.java      # Data access (JPA)
│   │   │               └── model/
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
├── pom.xml
└── README.md
```

**💡 Example Project**: A complete working example is available in the `pizzastore-intro/` directory.

### Key Files

**pom.xml**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.7</version>
        <relativePath/>
    </parent>
    
    <groupId>be.vives</groupId>
    <artifactId>pizzastore-intro</artifactId>
    <version>1.0.0</version>
    <name>PizzaStore Intro</name>
    <description>PizzaStore REST API - Spring Boot Introduction</description>
    
    <properties>
        <java.version>25</java.version>
    </properties>
    
    <dependencies>
        <!-- Web starter: Spring MVC + embedded Tomcat -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- JPA starter: Spring Data JPA + Hibernate -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        
        <!-- H2 in-memory database for development -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- DevTools for automatic restart -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- Test starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

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

1. **File** → **Settings** (or **Preferences** on Mac) → **Build, Execution, Deployment** → **Compiler**
2. Check ✅ **"Build project automatically"**
3. **Help** → **Find Action** (Ctrl+Shift+A / Cmd+Shift+A)
4. Search for **"Registry"**
5. Enable ✅ **"compiler.automake.allow.when.app.running"**

Now your application will restart automatically when you make changes!

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

## 🍕 Building the PizzaStore - Complete Example with JPA

Now let's build the PizzaStore REST API using proper JPA and H2 database.

### Step 1: Create the Project

Follow the **Creating Spring Boot Projects** section above to create a project with:
- Spring Web
- Spring Data JPA
- H2 Database
- Spring Boot DevTools

### Step 2: Configure application.properties

**Create `src/main/resources/application.properties`**:

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
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect

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
- **h2.console.enabled=true**: Enables web console to view database

### Step 3: Create the Pizza Entity

**`src/main/java/be/vives/pizzastore/model/Pizza.java`**:

```java
package be.vives.pizzastore.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "pizzas")
public class Pizza {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(name = "image_url")
    private String imageUrl;
    
    @Column(nullable = false)
    private boolean available = true;
    
    // Constructors
    public Pizza() {
    }
    
    public Pizza(String name, String description, BigDecimal price, String imageUrl, boolean available) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.available = available;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public boolean isAvailable() {
        return available;
    }
    
    public void setAvailable(boolean available) {
        this.available = available;
    }
    
    @Override
    public String toString() {
        return "Pizza{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", available=" + available +
                '}';
    }
}
```

**Key JPA Annotations**:
- `@Entity`: Marks this as a JPA entity
- `@Table(name = "pizzas")`: Specifies the table name
- `@Id`: Marks the primary key
- `@GeneratedValue`: Auto-generates the ID
- `@Column`: Configures column properties (nullable, length, precision)

### Step 4: Create the Repository Interface

**`src/main/java/be/vives/pizzastore/repository/PizzaRepository.java`**:

```java
package be.vives.pizzastore.repository;

import be.vives.pizzastore.model.Pizza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PizzaRepository extends JpaRepository<Pizza, Long> {
    
    // Spring Data JPA provides implementations for:
    // - findAll()
    // - findById(Long id)
    // - save(Pizza pizza)
    // - deleteById(Long id)
    // - count()
    // - existsById(Long id)
    // And many more!
    
    // Custom query methods (Spring Data JPA creates implementation automatically)
    List<Pizza> findByAvailable(boolean available);
    
    List<Pizza> findByNameContainingIgnoreCase(String name);
}
```

**Magic of Spring Data JPA**:
- You only write the **interface**
- Spring Data JPA creates the **implementation** at runtime
- Method names follow conventions: `findBy`, `countBy`, `deleteBy`, etc.

### Step 5: Create the Service Layer

**`src/main/java/be/vives/pizzastore/service/PizzaService.java`**:

```java
package be.vives.pizzastore.service;

import be.vives.pizzastore.model.Pizza;
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
    
    public List<Pizza> getAvailablePizzas() {
        log.debug("Fetching available pizzas");
        return pizzaRepository.findByAvailable(true);
    }
    
    public Pizza createPizza(Pizza pizza) {
        log.info("Creating pizza: {}", pizza.getName());
        return pizzaRepository.save(pizza);
    }
    
    public Pizza updatePizza(Long id, Pizza pizza) {
        log.info("Updating pizza with id: {}", id);
        Pizza existing = getPizzaById(id);
        pizza.setId(id);
        return pizzaRepository.save(pizza);
    }
    
    public void deletePizza(Long id) {
        log.info("Deleting pizza with id: {}", id);
        pizzaRepository.deleteById(id);
    }
}
```

**Key Points**:
- `@Service`: Marks this as a Spring service component
- `@Transactional`: Ensures database operations are transactional
- Constructor injection for `PizzaRepository`
- SLF4J logger for debugging

### Step 6: Create the REST Controller

**`src/main/java/be/vives/pizzastore/controller/PizzaController.java`**:

```java
package be.vives.pizzastore.controller;

import be.vives.pizzastore.model.Pizza;
import be.vives.pizzastore.service.PizzaService;
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
    
    @GetMapping("/available")
    public List<Pizza> getAvailablePizzas() {
        return pizzaService.getAvailablePizzas();
    }
    
    @PostMapping
    public ResponseEntity<Pizza> createPizza(@RequestBody Pizza pizza) {
        Pizza created = pizzaService.createPizza(pizza);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    public Pizza updatePizza(@PathVariable Long id, @RequestBody Pizza pizza) {
        return pizzaService.updatePizza(id, pizza);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePizza(@PathVariable Long id) {
        pizzaService.deletePizza(id);
        return ResponseEntity.noContent().build();
    }
}
```

**Key Annotations**:
- `@RestController`: Combines `@Controller` and `@ResponseBody`
- `@RequestMapping("/api/pizzas")`: Base path for all endpoints
- `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`: HTTP methods
- `@PathVariable`: Extracts value from URL path
- `@RequestBody`: Converts JSON to Java object

### Step 7: Add Sample Data (Optional)

**Create `src/main/resources/data.sql`**:

```sql
INSERT INTO pizzas (name, description, price, image_url, available) 
VALUES ('Margherita', 'Classic tomato and mozzarella', 8.99, null, true);

INSERT INTO pizzas (name, description, price, image_url, available) 
VALUES ('Pepperoni', 'Pepperoni and cheese', 10.99, null, true);

INSERT INTO pizzas (name, description, price, image_url, available) 
VALUES ('Quattro Formaggi', 'Four cheese blend', 11.99, null, true);

INSERT INTO pizzas (name, description, price, image_url, available) 
VALUES ('Vegetariana', 'Fresh vegetables', 9.99, null, true);

INSERT INTO pizzas (name, description, price, image_url, available) 
VALUES ('Diavola', 'Spicy salami', 12.99, null, false);
```

**Note**: Spring Boot automatically executes `data.sql` on startup when using H2.

### Step 8: Run the Application

### From IntelliJ
1. Right-click `PizzaStoreApplication.java`
2. Select **Run 'PizzaStoreApplication'**

### From Command Line
```bash
# Using Maven
mvn spring-boot:run

# Or build and run the JAR
mvn clean package
java -jar target/pizzastore-intro-1.0.0.jar
```

### Expected Console Output
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::               (v3.5.7)

2024-01-15 10:00:00 INFO  PizzaStoreApplication: Starting PizzaStoreApplication
2024-01-15 10:00:01 INFO  PizzaService: PizzaService initialized
2024-01-15 10:00:02 INFO  PizzaStoreApplication: Started PizzaStoreApplication in 2.5 seconds
```

You'll also see SQL statements being executed if you have `show-sql=true`.

### Step 9: Test the API

### Using Browser
Open: http://localhost:8080/api/pizzas

### Using H2 Console
Open: http://localhost:8080/h2-console
- **JDBC URL**: `jdbc:h2:mem:pizzastoredb`
- **Username**: `sa`
- **Password**: (leave empty)

You can run SQL queries directly:
```sql
SELECT * FROM pizzas;
```

### Using curl

**Get all pizzas**:
```bash
curl http://localhost:8080/api/pizzas
```

**Get available pizzas**:
```bash
curl http://localhost:8080/api/pizzas/available
```

**Get specific pizza**:
```bash
curl http://localhost:8080/api/pizzas/1
```

**Create new pizza**:
```bash
curl -X POST http://localhost:8080/api/pizzas \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Hawaii",
    "description": "Pineapple and ham",
    "price": 11.99,
    "imageUrl": null,
    "available": true
  }'
```

**Update pizza**:
```bash
curl -X PUT http://localhost:8080/api/pizzas/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Margherita Deluxe",
    "description": "Extra cheese and basil",
    "price": 10.99,
    "imageUrl": null,
    "available": true
  }'
```

**Delete pizza**:
```bash
curl -X DELETE http://localhost:8080/api/pizzas/5
```

### Using Postman

1. **GET** `http://localhost:8080/api/pizzas` - Get all pizzas
2. **GET** `http://localhost:8080/api/pizzas/1` - Get pizza by ID
3. **GET** `http://localhost:8080/api/pizzas/available` - Get available pizzas
4. **POST** `http://localhost:8080/api/pizzas` - Create new pizza (set Body → raw → JSON)
5. **PUT** `http://localhost:8080/api/pizzas/1` - Update pizza
6. **DELETE** `http://localhost:8080/api/pizzas/1` - Delete pizza

### Expected Response

**GET /api/pizzas**:
```json
[
  {
    "id": 1,
    "name": "Margherita",
    "description": "Classic tomato and mozzarella",
    "price": 8.99,
    "imageUrl": null,
    "available": true
  },
  {
    "id": 2,
    "name": "Pepperoni",
    "description": "Pepperoni and cheese",
    "price": 10.99,
    "imageUrl": null,
    "available": true
  }
]
```

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
@Repository              // Data access component
@Transactional          // Transaction management
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
- Configures the correct `MANIFEST.MF`

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

**Note**: We'll explore Actuator in depth in Lesson 16.

---

## 💡 Tips & Tricks

### DevTools Hot Swap
- ✅ Java code changes → Automatic restart (fast!)
- ✅ Static resources (HTML, CSS, JS) → No restart, just LiveReload
- ✅ `application.properties` changes → Automatic restart
- ❌ `pom.xml` changes → Requires manual restart

### Debug Logging
```properties
# Spring Web
logging.level.org.springframework.web=DEBUG

# Hibernate SQL
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Your application
logging.level.be.vives.pizzastore=DEBUG
```

### H2 Console Tips
- Access at: http://localhost:8080/h2-console
- **JDBC URL**: Must match `spring.datasource.url` in properties
- **Username/Password**: Must match datasource configuration
- Use to verify data, run queries, debug schema

### IDE Tips for IntelliJ
- **Alt+Insert** (Win) / **Cmd+N** (Mac) → Generate (getters, setters, constructors)
- **Ctrl+Alt+O** (Win) / **Cmd+Alt+O** (Mac) → Optimize imports
- **Ctrl+Shift+F** (Win) / **Cmd+Shift+F** (Mac) → Format code
- **Shift+F10** (Win) / **Ctrl+R** (Mac) → Run
- **Shift+F9** (Win) / **Ctrl+D** (Mac) → Debug

### Common Issues

**Issue**: Application won't start - port 8080 already in use
**Solution**: Change port in `application.properties`:
```properties
server.port=8081
```

**Issue**: H2 console shows "Database not found"
**Solution**: Verify JDBC URL matches exactly:
```properties
spring.datasource.url=jdbc:h2:mem:pizzastoredb
```

**Issue**: DevTools not restarting automatically
**Solution**: Check IntelliJ settings (see DevTools section above)

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
   - **Embedded servers** - No external deployment needed (Tomcat, Jetty, Undertow)
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
   - Built complete REST API with CRUD operations
   - Used proper service layer with transactions

6. **DevTools**
   - Fast automatic restarts
   - LiveReload for static resources
   - Dramatically improves development speed

### Spring vs Spring Boot

| Aspect | Spring Framework | Spring Boot |
|--------|------------------|-------------|
| **Configuration** | Manual, XML or Java config classes | Auto-configuration based on classpath |
| **Dependency Management** | Manual version management | Starters with managed versions |
| **Server** | External (Tomcat, Glassfish, etc.) | Embedded (Tomcat, Jetty, Undertow) |
| **Deployment** | WAR file to application server | Executable JAR with `java -jar` |
| **Setup Time** | Hours to days | Minutes |
| **Production Features** | Manual setup required | Actuator built-in |
| **Learning Curve** | Steeper | Gentler - sensible defaults |
| **JPA Setup** | Manual EntityManagerFactory, TransactionManager | Auto-configured with starter |

### Benefits of Spring Boot

✅ **Faster development** - Less boilerplate and configuration  
✅ **Easier dependency management** - Starters with compatible versions  
✅ **Production-ready** - Actuator for monitoring and metrics  
✅ **Microservices-friendly** - Self-contained executable JARs  
✅ **Convention over Configuration** - Sensible defaults that just work  
✅ **Still flexible** - Can override any auto-configuration  
✅ **Better developer experience** - DevTools for rapid development  
✅ **Cloud-native ready** - Perfect for containerization and cloud deployment  
✅ **JPA made easy** - Spring Data JPA eliminates boilerplate

---

## 📖 Additional Resources

- [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Initializr](https://start.spring.io)
- [Spring Boot Starters List](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.build-systems.starters)
- [Spring Boot DevTools Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.devtools)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Spring Boot Auto-Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.auto-configuration)

---

**Congratulations!** 🎉 You've created your first complete Spring Boot application with JPA and H2, and started the PizzaStore journey! You now understand how Spring Boot simplifies Spring development and can create production-ready REST APIs with minimal configuration. In the next lessons, we'll build it into a feature-rich, production-ready API with proper REST principles, validation, exception handling, and more!
