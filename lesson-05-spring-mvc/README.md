# Lesson 5: Spring MVC & Building a REST API

## 📚 Table of Contents

- [📘 Overview](#-overview)
- [🎯 Learning Objectives](#-learning-objectives)
- [🌐 Introduction to Spring Web MVC](#-introduction-to-spring-web-mvc)
- [🗺️ Request Mapping](#️-request-mapping)
- [📥 Request Parameters](#-request-parameters)
- [📤 Response Handling](#-response-handling)
- [📦 Jackson - JSON Serialization](#-jackson---json-serialization)
- [⚠️ Exception Handling](#️-exception-handling)
- [📊 HTTP Status Codes](#-http-status-codes)
- [🎓 Summary](#-summary)
- [📖 Additional Resources](#-additional-resources)

---

## 📘 Overview

Now that we understand Spring Boot basics, it's time to build a proper **REST API** using **Spring MVC**. In this lesson, we'll explore the Spring MVC architecture, learn how to build REST endpoints, and implement proper request/response handling in our PizzaStore application.

## 🎯 Learning Objectives

By the end of this lesson, you will:
- Understand **Spring MVC architecture** and the **DispatcherServlet**
- Know the difference between **@Controller** and **@RestController**
- Build RESTful endpoints using **@RestController**
- Master request mapping annotations (`@GetMapping`, `@PostMapping`, etc.)
- Handle **path variables** and **request parameters**
- Work with **request and response bodies**
- Understand the role of **Jackson** in JSON serialization
- Implement basic **exception handling**
- Return appropriate **HTTP status codes**

---

## 🌐 Introduction to Spring Web MVC

### What is Spring Web MVC?

Spring Web MVC is Spring's **web framework** built on the Servlet API. It provides a powerful, flexible way to build web applications and REST APIs using the **Model-View-Controller** design pattern.

The name "Spring MVC" comes from the architectural pattern it implements:
- **Model**: The data and business logic
- **View**: The presentation layer (HTML, JSON, XML)
- **Controller**: The request handler that connects model and view

### Why Spring MVC?

Spring MVC has become the de facto standard for building web applications in the Java ecosystem. Here's why:

**🎯 Built on Proven Patterns**
- Based on the battle-tested MVC design pattern
- Clear separation of concerns (business logic, presentation, routing)
- Promotes maintainable and testable code

**🔌 Integration with Spring Ecosystem**
- Controllers are regular Spring beans → full IoC/DI support
- Seamless integration with Spring Boot, Spring Data, Spring Security
- Consistent programming model across your entire application

**⚡ Convention over Configuration**
- Annotation-based configuration (`@Controller`, `@GetMapping`, etc.)
- Sensible defaults that "just work"
- Override only what you need to customize

**🌍 Versatile**
- Traditional server-side rendered web apps (Thymeleaf, JSP)
- Modern REST APIs (JSON, XML)
- WebSockets for real-time communication

**🚀 Production Ready**
- Used by thousands of enterprises worldwide
- High performance and scalability
- Excellent tooling and IDE support

### The Model-View-Controller Pattern Explained

Before diving into Spring MVC specifics, let's understand the MVC pattern:

**Model (M)**
- Represents the **application data** and **business rules**
- In Spring: typically POJOs (Plain Old Java Objects) or DTOs (Data Transfer Objects)
- Examples: `Pizza`, `Order`, `Customer` classes
- The model is **independent** of the user interface

**View (V)**
- Responsible for **presenting data** to the user
- In traditional web apps: HTML templates (Thymeleaf, JSP, FreeMarker)
- In REST APIs: JSON or XML representations
- The view **displays** the model but doesn't modify it

**Controller (C)**
- Acts as the **intermediary** between Model and View
- Receives **HTTP requests** from clients
- Processes the request (possibly calling services)
- Prepares the model data
- Returns the appropriate view or response

### How Spring MVC Handles Requests

```
 Client (Browser/Mobile)
        ↓ 
   HTTP Request
        ↓
  DispatcherServlet (Front Controller - entry point for all requests)
        ↓
   Handler Mapping (Determines which controller handles this request)
        ↓
   Controller ──→ Service ──→ Repository ──→ Database
        ↓              ↓
      Model        Business Logic
        ↓
      View
        ↓
   HTTP Response
```

When a HTTP request arrives, here's what happens:

1. **DispatcherServlet** receives the request (front controller)
2. **HandlerMapping** finds the right controller method
3. **Controller** processes the request and returns a result
4. **ViewResolver** resolves views (traditional apps) OR
5. **Jackson** serializes objects to JSON (REST APIs)
6. Response sent back to client

Spring Boot **autoconfigures** all of this for you!

---

### 🔑 Key Components 

#### DispatcherServlet

- The `DispatcherServlet` is the front controller in Spring MVC: a single servlet that receives all incoming HTTP requests and coordinates handling.
- It delegates to other components: handler mappings to find the right controller, invokes the controller (handler), and uses view resolvers to render a view when necessary.
- Spring Boot autoconfigures the `DispatcherServlet` for you; in most apps you don't need to register it manually.

#### Handler Mapping

- Handler mappings determine which controller method should handle an incoming request.
- Mapping is commonly done via annotations (`@RequestMapping`, `@GetMapping`, `@PostMapping`, etc.).
- Multiple strategies exist (URL path patterns, HTTP method, headers, content types). Spring evaluates the best match.

#### View Resolver

- A view resolver maps a logical view name (for example, `"pizzaview"`) to a concrete template (for example, `pizzaview.html`).
- Used in traditional MVC applications that render HTML on the server side (Thymeleaf, JSP, FreeMarker).
- For a REST API (`@RestController`) the view resolver is typically not used because methods return data (JSON) rather than view names.

### What's in `spring-boot-starter-web`?

When you add this dependency, you get:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

**Included Components:**

| Component | Purpose |
|-----------|---------|
| **spring-webmvc** | Core Spring MVC framework |
| **spring-web** | Common web utilities |
| **Embedded Tomcat** | Built-in web server (no separate installation needed!) |
| **Jackson** | JSON serialization/deserialization |

**What This Means for You:**
- ✅ No need to configure a separate web server
- ✅ No XML configuration files
- ✅ No manual servlet registration
- ✅ JSON support out of the box
- ✅ Just write code and run!

###  Spring MVC: Two Flavors: Traditional Web App vs REST API

Understanding the difference is crucial:

**Traditional Web Application**
```
Browser → Request → Spring MVC → Thymeleaf → HTML → Browser
```

Use `@Controller` when you want to return HTML views (server-side rendering). Below is a compact example showing how to build a simple page that displays a list of pizzas.

```java
@Controller
@RequestMapping("/pizzas")
public class PizzaController {

    @GetMapping
    public String showList(Model model) {
        List<Pizza> pizzas = getAllPizzas();
        model.addAttribute("pizzas", pizzas);
        return "pizzaview"; // Resolves to pizzaview.html via a ViewResolver (e.g. Thymeleaf)
    }

    // ... helper methods ...
}
```
One small Thymeleaf example (use this exactly once in the course as a minimal example to render a table):

Thymeleaf file: `src/main/resources/templates/pizzaview.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Pizza Overview</title>
    <style>
        table { border-collapse: collapse; width: 80%; margin: 20px auto; }
        th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }
        th { background-color: #f2f2f2; }
        h1 { text-align: center; }
    </style>
</head>
<body>
    <h1>🍕 Pizza Overview</h1>
    <table>
        <thead>
            <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Price</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="pizza : ${pizzas}">
                <td th:text="${pizza.id}"></td>
                <td th:text="${pizza.name}"></td>
                <td th:text="${pizza.price}"></td>
            </tr>
        </tbody>
    </table>
</body>
</html>
```

(Include this one simple template in the course for demonstration purposes only.)

- Controller returns a **view name** (String)
- View resolver finds the template file (e.g. `pizzaview.html`)
- Template engine renders HTML
- Server generates complete HTML pages and sends them to the client
- Tight coupling between frontend and backend
- Examples: Admin panels, content websites

**REST API Application (Modern Approach)**
```
Client → Request → Spring MVC → JSON → Client (renders with JS)
```

Use `@RestController` when you want to return JSON data (no server-side rendering).

```java
@RestController
@RequestMapping("/api/pizzas")  // Base path for all methods
public class PizzaController {

    // GET /api/pizzas
    @GetMapping
    public List<Pizza> getAllPizzas() {
        return pizzaService.findAll();
    }

    // GET /api/pizzas/5
    @GetMapping("/{id}")
    public Pizza getPizzaById(@PathVariable Long id) {
        return pizzaService.findById(id);
    }
}
```

**@RestController = @Controller + @ResponseBody**

Every method in `@RestController` automatically serializes return values to JSON/XML.

- Controller returns **data objects**
- Jackson serializes to JSON automatically
- Client-side (React, Angular, Vue, mobile app) handles rendering
- Decoupled frontend and backend
- Server sends only data (JSON)
- Examples: Mobile apps, Single Page Apps (SPAs)

**In This Course:**
We focus primarily on **REST APIs** because:
- Modern development is trending towards REST
- Enables mobile app development
- Better separation of concerns

---

## 🗺️ Request Mapping

### HTTP Methods and Annotations

| HTTP Method | Annotation | Purpose | Idempotent |
|-------------|-----------|---------|------------|
| **GET** | `@GetMapping` | Retrieve resource(s) | ✅ Yes |
| **POST** | `@PostMapping` | Create new resource | ❌ No |
| **PUT** | `@PutMapping` | Update/Replace entire resource | ✅ Yes |
| **PATCH** | `@PatchMapping` | Partial update | ❌ No |
| **DELETE** | `@DeleteMapping` | Delete resource | ✅ Yes |

### Basic Request Mapping

```java
@RestController
@RequestMapping("/api/pizzas")  // Base path for all methods
public class PizzaController {
    
    // GET /api/pizzas
    @GetMapping
    public List<Pizza> getAllPizzas() {
        return pizzaService.findAll();
    }
    
    // GET /api/pizzas/5
    @GetMapping("/{id}")
    public Pizza getPizzaById(@PathVariable Long id) {
        return pizzaService.findById(id);
    }
    
    // POST /api/pizzas
    @PostMapping
    public Pizza createPizza(@RequestBody Pizza pizza) {
        return pizzaService.create(pizza);
    }
    
    // PUT /api/pizzas/5
    @PutMapping("/{id}")
    public Pizza updatePizza(@PathVariable Long id, @RequestBody Pizza pizza) {
        return pizzaService.update(id, pizza);
    }
    
    // DELETE /api/pizzas/5
    @DeleteMapping("/{id}")
    public void deletePizza(@PathVariable Long id) {
        pizzaService.delete(id);
    }
}
```

---

## 📥 Request Parameters

### 1. Path Variables (@PathVariable)

Extract values from the URL path:

```java
// GET /api/pizzas/5
@GetMapping("/{id}")
public Pizza getPizza(@PathVariable Long id) {
    return pizzaService.findById(id);
}

// GET /api/orders/123/items/456
@GetMapping("/{orderId}/items/{itemId}")
public OrderItem getOrderItem(
    @PathVariable Long orderId,
    @PathVariable Long itemId
) {
    return orderService.findOrderItem(orderId, itemId);
}

// Optional: Custom variable name
@GetMapping("/users/{userId}")
public User getUser(@PathVariable("userId") Long id) {
    return userService.findById(id);
}
```

### 2. Query Parameters (@RequestParam)

Extract values from query string:

```java
// GET /api/pizzas?name=Margherita
@GetMapping
public List<Pizza> searchPizzas(@RequestParam String name) {
    return pizzaService.findByName(name);
}

// GET /api/pizzas?name=Margherita&maxPrice=10
@GetMapping
public List<Pizza> searchPizzas(
    @RequestParam String name,
    @RequestParam BigDecimal maxPrice
) {
    return pizzaService.search(name, maxPrice);
}

// Optional parameters with default values
@GetMapping
public List<Pizza> getPizzas(
    @RequestParam(required = false, defaultValue = "0") int page,
    @RequestParam(required = false, defaultValue = "10") int size
) {
    return pizzaService.findAll(page, size);
}
```

### 3. Request Body (@RequestBody)

Extract JSON/XML from request body:

```java
// POST /api/pizzas
// Body: {"name": "Margherita", "price": 8.99}
@PostMapping
public Pizza createPizza(@RequestBody Pizza pizza) {
    return pizzaService.create(pizza);
}

// With validation
@PostMapping
public Pizza createPizza(@Valid @RequestBody Pizza pizza) {
    return pizzaService.create(pizza);
}
```

### 4. Request Headers (@RequestHeader)

```java
@GetMapping("/info")
public String getInfo(
    @RequestHeader("User-Agent") String userAgent,
    @RequestHeader(value = "Accept-Language", required = false) String language
) {
    return "User-Agent: " + userAgent + ", Language: " + language;
}
```

---

## 📤 Response Handling

### 1. Returning Objects (Automatic JSON Conversion)

```java
@GetMapping("/{id}")
public Pizza getPizza(@PathVariable Long id) {
    return pizzaService.findById(id);  // Automatically converted to JSON
}
```

**Response:**
```json
{
  "id": 1,
  "name": "Margherita",
  "description": "Classic tomato and mozzarella",
  "price": 8.99,
  "available": true
}
```

### 2. ResponseEntity (Full Control)

Use `ResponseEntity` for complete control over response:

```java
@GetMapping("/{id}")
public ResponseEntity<Pizza> getPizza(@PathVariable Long id) {
    Pizza pizza = pizzaService.findById(id);
    if (pizza == null) {
        return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(pizza);
}

@PostMapping
public ResponseEntity<Pizza> createPizza(@RequestBody Pizza pizza) {
    Pizza created = pizzaService.create(pizza);
    URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(created.getId())
        .toUri();
    
    return ResponseEntity.created(location).body(created);
}
```

### 3. HTTP Status Codes with @ResponseStatus

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)  // Returns 201 instead of 200
public Pizza createPizza(@RequestBody Pizza pizza) {
    return pizzaService.create(pizza);
}

@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)  // Returns 204
public void deletePizza(@PathVariable Long id) {
    pizzaService.delete(id);
}
```

---

## 📦 Jackson - JSON Serialization

- Jackson is the library Spring Boot uses (via `spring-boot-starter-web`) to convert Java objects to JSON and back.
- It performs **serialization** (Java → JSON) and **deserialization** (JSON → Java) automatically for controller methods that return objects or use `@RequestBody`.
- You can customize JSON mapping using Jackson annotations (`@JsonProperty`, `@JsonIgnore`, `@JsonFormat`, etc.) when needed.

Example:
```java
public class Pizza {
    @JsonProperty("pizza_id")
    private Long id;

    @JsonIgnore
    private String internalNotes;

    // getters / setters
}
```

---

## ⚠️ Exception Handling

### Custom Exception

```java
package com.example.pizzastore.exception;

public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

### Basic Exception Handler in Controller

```java
@RestController
@RequestMapping("/api/pizzas")
public class PizzaController {
    
    // ... controller methods ...
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ex.getMessage());
    }
}
```

**Note:** We'll cover advanced exception handling with `@ControllerAdvice` in Lesson 11.

---

## 📊 HTTP Status Codes

Use appropriate status codes:

| Code | Meaning | When to Use |
|------|---------|-------------|
| **200** | OK | Successful GET, PUT, PATCH |
| **201** | Created | Successful POST (resource created) |
| **204** | No Content | Successful DELETE |
| **400** | Bad Request | Invalid request data |
| **404** | Not Found | Resource doesn't exist |
| **409** | Conflict | Resource already exists |
| **500** | Internal Server Error | Unexpected server error |

---

## 🎓 Summary

### Key Takeaways

1. **Spring MVC Architecture**
   - DispatcherServlet is the front controller
   - Handler mapping routes requests to controllers
   - View Resolver resolves view names to templates (for traditional MVC)

2. **@Controller vs @RestController**
   - @Controller returns view names (HTML pages)
   - @RestController returns data (JSON/XML)

3. **Request Mapping**
   - Use specific annotations: @GetMapping, @PostMapping, etc.
   - @PathVariable for URL path segments
   - @RequestParam for query parameters
   - @RequestBody for request payload

4. **Response Handling**
   - Return objects directly for simple cases
   - Use ResponseEntity for full control
   - Set appropriate HTTP status codes

5. **Jackson**
   - Automatic JSON serialization/deserialization

---

## 📖 Additional Resources

- [Spring MVC Documentation](https://docs.spring.io/spring-framework/reference/web/webmvc.html)
- [Spring REST Documentation](https://spring.io/guides/tutorials/rest/)
- [Jackson Documentation](https://github.com/FasterXML/jackson)
- [HTTP Status Codes](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status)

---

**Great job!** 🎉 You now understand Spring MVC fundamentals and can build both traditional web applications and REST APIs. In the next lesson, we'll dive deeper into REST principles.

