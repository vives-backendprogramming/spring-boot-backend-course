# Lesson 2: Spring Framework - Dependency Injection & IoC

## 📚 Table of Contents

- [📘 Overview](#-overview)
- [🎯 Learning Objectives](#-learning-objectives)
- [🌱 What is Spring Framework?](#-what-is-spring-framework)
- [🧩 Spring Framework Modules](#-spring-framework-modules)
- [🫘 What is a Bean?](#-what-is-a-bean)
- [🔄 IoC Container](#-ioc-container)
- [💉 Dependency Injection (DI)](#-dependency-injection-di)
- [📦 Spring Stereotype Annotations](#-spring-stereotype-annotations)
- [⚙️ @Configuration and @Bean](#️-configuration-and-bean)
- [🔍 Component Scanning](#-component-scanning)
- [🚀 Getting Beans from Application Context](#-getting-beans-from-application-context)
- [📊 Spring IoC Container - The Big Picture](#-spring-ioc-container---the-big-picture)
- [📚 Best Practices Summary](#-best-practices-summary)
- [🎓 Summary](#-summary)
- [📖 Additional Resources](#-additional-resources)

---

## 📘 Overview

The **Spring Framework** is one of the most widely used Java frameworks for building enterprise applications. At its core, Spring provides an **IoC (Inversion of Control) container** that manages your application's components and their dependencies through **Dependency Injection**.

In this lesson, we'll explore the fundamental concepts that make Spring powerful: how it manages objects (beans), how it wires them together, and why this approach leads to more maintainable and testable code.

## 🎯 Learning Objectives

By the end of this lesson, you will:
- Understand what the **Spring Framework** is and why it's used
- Grasp the **IoC (Inversion of Control)** principle
- Master **Dependency Injection** (DI) concepts
- Use Spring annotations: `@Component`, `@Service`, `@Repository`, `@Controller`
- Understand `@Autowired` and how Spring resolves dependencies
- Create beans with `@Configuration` and `@Bean`
- Choose between **constructor injection** and **setter injection**

---

## 🌱 What is Spring Framework?

### Introduction

**Spring Framework** is an open-source Java application framework used as an alternative or extension to technologies from Jakarta EE (formerly Java EE).

**Website**: [https://spring.io](https://spring.io)

### Why Spring?

> **"The Spring Framework provides a comprehensive programming and configuration model for modern Java-based enterprise applications."**

> **"A key element of Spring is infrastructural support at the application level: Spring focuses on the 'plumbing' of enterprise applications so that teams can focus on application-level business logic."**

### Spring Projects

Spring is not just one framework—it's an ecosystem of projects:
- **Spring Framework** - Core DI/IoC, MVC, Data Access
- **Spring Boot** - Rapid application development (Lesson 4!)
- **Spring Data** - Simplified data access (JPA, MongoDB, etc.)
- **Spring Security** - Authentication & Authorization
- **Spring Cloud** - Microservices & distributed systems
- And many more...

**Explore**: [https://spring.io/projects](https://spring.io/projects)

---

## 🧩 Spring Framework Modules

The Spring Framework is divided into modules. Applications can choose which modules they need.

### Core Container (The Heart of Spring)

At the heart are the modules of the **core container**, including:
- **Spring Core** - IoC container
- **Spring Beans** - Bean factory & dependency injection
- **Spring Context** - Application context
- **Spring Expression Language (SpEL)**

### Other Important Modules

- **Spring MVC** - Web applications
- **Spring Data Access/Integration** - JDBC, ORM, Transactions
- **Spring AOP** - Aspect-Oriented Programming
- **Spring Test** - Testing support

**For now, we focus on**: **Core Container** (IoC & DI)

---

## 🫘 What is a Bean?

### Definition

> **Bean is a key concept of the Spring Framework.**

> **In Spring, the objects that form the backbone of your application and that are managed by the Spring IoC container are called beans.**

> **A bean is an object that is instantiated, assembled, and otherwise managed by a Spring IoC container.**

### Simple Explanation

A **bean** is simply a Java object that Spring creates and manages for you.

**Without Spring:**
```java
// You create objects yourself
PizzaRepository repository = new PizzaRepository();
PizzaService service = new PizzaService(repository);
PizzaController controller = new PizzaController(service);
```

**With Spring:**
```java
// Spring creates and manages these objects for you
@Component
public class PizzaRepository { ... }

@Service
public class PizzaService { ... }

@Controller
public class PizzaController { ... }

// Spring wires them together automatically!
```

---

## 🔄 IoC Container

### 🔑 What is Inversion of Control (IoC)?

**Inversion of Control** is a design principle where the control of object creation and management is **inverted** from the application code to a framework or container.

#### Traditional Approach (No IoC)
```java
public class OrderService {
    private PaymentService paymentService;
    private EmailService emailService;
    
    public OrderService() {
        // We create and manage dependencies manually
        this.paymentService = new PaymentService();
        this.emailService = new EmailService();
    }
    
    public void processOrder(Order order) {
        paymentService.processPayment(order);
        emailService.sendConfirmation(order);
    }
}
```

**Problems:**
- Tight coupling
- Hard to test (can't mock dependencies)
- Difficult to change implementations
- We control everything ourselves

#### With IoC (Spring)
```java
@Service
public class OrderService {
    private final PaymentService paymentService;
    private final EmailService emailService;
    
    // Spring creates and injects dependencies
    public OrderService(PaymentService paymentService, EmailService emailService) {
        this.paymentService = paymentService;
        this.emailService = emailService;
    }
    
    public void processOrder(Order order) {
        paymentService.processPayment(order);
        emailService.sendConfirmation(order);
    }
}
```

**Benefits:**
- Loose coupling
- Easy to test (inject mocks)
- Easy to swap implementations
- The framework controls object creation

### The IoC Principle

> **IoC = Dependency Injection (DI)**

> **Objects define their dependencies (= other objects they work with). The IoC container injects those dependencies when it creates the bean.**

> **This process is fundamentally the inverse (hence the name, Inversion of Control) of the bean itself controlling the instantiation of its dependencies by using direct construction of classes.**

**You don't create objects - Spring does!**

---

### 💉 Dependency Injection (DI)

**Dependency Injection** is the mechanism by which IoC is achieved. Dependencies are "injected" into objects rather than objects creating their own dependencies.

#### Three Types of Dependency Injection

##### 1. Constructor Injection (Recommended ✅)
```java
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    // Constructor injection - recommended for required dependencies
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
}
```

**Advantages:**
- Immutable dependencies (final fields)
- Required dependencies are explicit
- Easy to test
- No need for `@Autowired` annotation (since Spring 4.3)

##### 2. Setter Injection
```java
@Service
public class EmailService {
    private EmailConfig emailConfig;
    
    // Setter injection - for optional dependencies
    @Autowired
    public void setEmailConfig(EmailConfig emailConfig) {
        this.emailConfig = emailConfig;
    }
}
```

**Use cases:**
- Optional dependencies
- Dependencies that can change during object lifecycle

##### 3. Field Injection (❌ Avoid in production code)
```java
@Service
public class NotificationService {
    @Autowired
    private EmailService emailService; // Avoid this!
    
    @Autowired
    private SmsService smsService; // Harder to test
}
```

**Why avoid?**
- Cannot make fields final
- Harder to test (need Spring context)
- Hidden dependencies
- Violates immutability principles

### 🤔 When to Use What?

**Rule of Thumb**:
- ✅ **Constructor injection for mandatory dependencies**
- ⚠️ **Setter injection for optional dependencies**

**Best Practice**: Use **constructor injection** by default!

---

## 📦 Spring Stereotype Annotations

Spring uses **stereotype annotations** to identify beans automatically through **component scanning**.

### The Annotation Hierarchy

```
            @Component
                 │
    ┌────────────┼────────────┐
    │            │            │
@Service    @Repository   @Controller
```

All stereotype annotations are **specializations** of `@Component`.

### @Component

The **base** stereotype annotation for any Spring-managed component.

```java
@Component
public class EmailValidator {
    
    public boolean isValid(String email) {
        return email != null && email.contains("@");
    }
}
```

**Use when**: The class doesn't fit into Service, Repository, or Controller categories.

### @Service

Used for **business logic** layer (service classes).

```java
@Service
public class PizzaService {
    
    private final PizzaRepository pizzaRepository;
    
    @Autowired
    public PizzaService(PizzaRepository pizzaRepository) {
        this.pizzaRepository = pizzaRepository;
    }
    
    public List<Pizza> getAllPizzas() {
        return pizzaRepository.findAll();
    }
    
    public Pizza getPizzaById(Long id) {
        return pizzaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Pizza not found"));
    }
}
```

**Purpose**: 
- Contains business logic
- Transaction management
- Orchestrates multiple repositories

### @Repository

Used for **data access** layer (DAO - Data Access Object pattern).

```java
@Repository
public class PizzaRepository {
    
    private List<Pizza> pizzas = new ArrayList<>();
    
    public List<Pizza> findAll() {
        return new ArrayList<>(pizzas);
    }
    
    public Optional<Pizza> findById(Long id) {
        return pizzas.stream()
            .filter(p -> p.getId().equals(id))
            .findFirst();
    }
    
    public void save(Pizza pizza) {
        pizzas.add(pizza);
    }
}
```

**Purpose**:
- Database operations (CRUD)
- Data retrieval and persistence
- Exception translation (SQLException → DataAccessException)

### @Controller

Used for **presentation layer** (web controllers).

```java
@Controller
public class PizzaController {
    
    private final PizzaService pizzaService;
    
    @Autowired
    public PizzaController(PizzaService pizzaService) {
        this.pizzaService = pizzaService;
    }
    
    // Handle web requests
}
```

**Purpose**:
- Handle HTTP requests
- Return views or data
- User interaction

### Why Use Specific Stereotypes?

1. **Clarity** - Clear role in the application
2. **Functionality** - Some add extra behavior (e.g., `@Repository` exception translation)
3. **AOP** - Aspect-Oriented Programming can target specific stereotypes
4. **Future-proofing** - Spring may add functionality to specific stereotypes

---

## ⚙️ @Configuration and @Bean

For **Java-based configuration**, use `@Configuration` and `@Bean` annotations.

### @Configuration

Annotating a class with `@Configuration` indicates that its primary purpose is as a **source of bean definitions**.

```java
@Configuration
public class DataSourceConfig {
    
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl("jdbc:mysql://localhost:3306/mydb");
        ds.setUsername("user");
        ds.setPassword("pass");
        return ds;
    }
}
```

### @Bean

The `@Bean` annotation is used to indicate that a method **instantiates, configures, and initializes** a new object to be managed by the Spring IoC container.

```java
@Configuration
public class AppConfig {
    
    @Bean
    public PizzaService pizzaService() {
        return new PizzaService(pizzaRepository());
    }
    
    @Bean
    public PizzaRepository pizzaRepository() {
        return new PizzaRepository();
    }
}
```

### Bean Dependencies

Spring automatically injects dependencies into `@Bean` methods:

```java
@Configuration
public class AppConfig {
    
    @Bean
    public PizzaRepository pizzaRepository() {
        return new PizzaRepository();
    }
    
    @Bean
    public PizzaService pizzaService(PizzaRepository pizzaRepository) {
        // Spring automatically provides pizzaRepository
        return new PizzaService(pizzaRepository);
    }
}
```

### When to Use @Bean?

Use `@Bean` when:
- Configuring **third-party classes** (you can't add `@Component`)
- **Complex initialization** logic needed
- **Multiple instances** of same type with different configs
- **Conditional bean creation**

---

## 🔍 Component Scanning

**Component scanning** is how Spring finds your beans.

### @ComponentScan

```java
@Configuration
@ComponentScan("com.example.pizzastore")
public class AppConfig {
}
```

**What happens:**
1. Spring scans the specified package (`com.example.pizzastore`)
2. Spring scans **all sub-packages** recursively
3. All classes with `@Component`, `@Service`, `@Repository`, `@Controller` are registered as beans
4. Spring resolves dependencies and wires beans together

### Multiple Packages

```java
@Configuration
@ComponentScan(basePackages = {"com.example.pizzastore", "com.example.shared"})
public class AppConfig {
}
```

### Type-Safe Package Scanning

```java
@Configuration
@ComponentScan(basePackageClasses = {PizzaService.class, OrderService.class})
public class AppConfig {
}
```

Spring scans the packages of the specified classes.

---

## 🚀 Getting Beans from Application Context

### Creating the Application Context

```java
ApplicationContext context = 
    new AnnotationConfigApplicationContext(AppConfig.class);
```

### Retrieving Beans

**Option 1: By Type (Preferred ✅)**

```java
PizzaService service = context.getBean(PizzaService.class);
```

**Option 2: By Name**

```java
PizzaService service = (PizzaService) context.getBean("pizzaService");
```

**Note**: Bean names default to the class name with lowercase first letter:
- `PizzaService` → bean name = `"pizzaService"`
- `PizzaRepository` → bean name = `"pizzaRepository"`

### Custom Bean Name

```java
@Service("myPizzaService")
public class PizzaService {
    // ...
}

// Retrieve by custom name
PizzaService service = (PizzaService) context.getBean("myPizzaService");
```

---

---

## 📊 Spring IoC Container - The Big Picture

```
┌─────────────────────────────────────────────────────────────┐
│                  SPRING IOC CONTAINER                        │
│              (Application Context)                           │
│                                                              │
│  1. SCAN packages for @Component, @Service, @Repository,    │
│     @Controller                                              │
│                                                              │
│  2. CREATE bean instances                                    │
│                                                              │
│  3. WIRE dependencies using @Autowired                       │
│                                                              │
│  4. MANAGE lifecycle (initialization, destruction)           │
│                                                              │
│                                                              │
│  ┌──────────────┐    ┌──────────────┐                      │
│  │ @Controller  │───▶│  @Service    │                      │
│  │              │    │              │                      │
│  └──────────────┘    └──────┬───────┘                      │
│                             │                               │
│                             ▼                               │
│                      ┌──────────────┐                      │
│                      │ @Repository  │                      │
│                      │              │                      │
│                      └──────────────┘                      │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Key Principles

1. **Spring manages everything** - You don't use `new`
2. **Beans are singletons by default** - One instance per container
3. **Dependencies are injected** - Constructor > Setter > Field
4. **Beans are created in correct order** - Based on dependencies
5. **Fully initialized** - All dependencies resolved before use

---

## 📚 Best Practices Summary

### ✅ DO

1. **Use constructor injection** for required dependencies
2. **Make injected fields `final`** for immutability
3. **Use appropriate stereotypes** (@Service, @Repository, @Controller)
4. **Prefer type-based bean retrieval** over name-based
5. **Package structure by layer** (controller, service, repository)
6. **One responsibility per class** (Single Responsibility Principle)

### ❌ DON'T

1. **Don't use field injection** - harder to test
2. **Don't create beans manually** with `new` when Spring can manage them
3. **Don't use @Component** when a specific stereotype fits better
4. **Don't forget @ComponentScan** - beans won't be found!
5. **Don't create circular dependencies** (A depends on B, B depends on A)

---

## 🎓 Summary

### What We Learned

1. **Spring Framework** - Infrastructure for enterprise Java applications
2. **IoC (Inversion of Control)** - Spring controls object lifecycle
3. **Dependency Injection** - Objects receive dependencies, don't create them
4. **Bean** - Spring-managed object
5. **Stereotype Annotations** - `@Component`, `@Service`, `@Repository`, `@Controller`
6. **@Autowired** - Automatic dependency injection
7. **@Configuration & @Bean** - Java-based configuration
8. **@ComponentScan** - Auto-discovery of beans
9. **Constructor Injection** - Best practice for DI

### Why This Matters

- **Loose Coupling** - Easy to change implementations
- **Testability** - Easy to mock dependencies
- **Maintainability** - Clear separation of concerns
- **Reusability** - Components can be reused
- **Spring Ecosystem** - Foundation for Spring Boot, Spring Data, Spring Security

---

## 📖 Additional Resources

- [Spring Framework Documentation](https://docs.spring.io/spring-framework/reference/)
- [Core Technologies (IoC Container)](https://docs.spring.io/spring-framework/reference/core.html)
- [Bean Definition](https://docs.spring.io/spring-framework/reference/core/beans/definition.html)
- [Dependency Injection](https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html)
- [Component Scanning](https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html)

---

**Congratulations!** 🎉 You've mastered the fundamentals of Spring's IoC Container and Dependency Injection. These concepts are the foundation for everything we'll build with Spring and Spring Boot!

