# Lesson 5: Spring MVC & Building a REST API

**What happens between an HTTP request and your controller method — and back**

---

## 📋 Learning Objectives

By the end of this lesson, you will be able to:

- Explain the **Spring MVC architecture** and the role of the **`DispatcherServlet`**
- Explain the difference between **`@Controller`** and **`@RestController`**
- Map requests with `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping`
- Read **path variables**, **query parameters**, **headers** and **request bodies**
- Control the response with **`ResponseEntity`** and **`@ResponseStatus`**
- Explain the role of **Jackson** in JSON serialization

---

## 📚 Table of Contents

1. [Recap: Where Lesson 4 Left Us](#-recap-where-lesson-4-left-us)
2. [Introduction to Spring Web MVC](#-introduction-to-spring-web-mvc)
3. [Request Mapping](#️-request-mapping)
4. [Request Parameters](#-request-parameters)
5. [Response Handling](#-response-handling)
6. [Jackson - JSON Serialization](#-jackson---json-serialization)
7. [Best Practices](#-best-practices)
8. [Summary](#-summary)
9. [Additional Resources](#-additional-resources)

---

## 🔄 Recap: Where Lesson 4 Left Us

Lesson 4's `pizzastore-intro` already answers HTTP requests: a `@RestController` with `@GetMapping`, `@PostMapping`, `@PathVariable` and `@RequestBody`, returning objects that somehow became JSON. Spring Boot set all of that up for us — but *what* did it set up?

This lesson opens the web layer: the **Spring MVC** architecture behind those annotations, the full set of mapping and parameter annotations, and the ways to control the response. It is a theory lesson without a project of its own; the PizzaStore controllers that use all of this are built in Lesson 9, on top of the data layer (Lesson 6a) and services (Lesson 7).

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
- The view **displays** the model

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
3. **HandlerAdapter** invokes that method, resolving arguments like `@PathVariable`/`@RequestBody` along the way
4. **Controller** processes the request and returns a result
5. **ViewResolver** resolves views (traditional apps) OR
6. **Jackson** serializes objects to JSON (REST APIs)
7. Response sent back to client

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

#### Handler Adapter

- Once the `HandlerMapping` has found the right controller, the `DispatcherServlet` uses a `HandlerAdapter` to actually invoke that controller's method.
- The adapter is what resolves method arguments such as `@PathVariable` and `@RequestBody` before calling your code (see [Request Parameters](#-request-parameters) below).

#### View Resolver

- A view resolver maps a logical view name (for example, `"pizzaview"`) to a concrete template (for example, `pizzaview.html`).
- Used in traditional MVC applications that render HTML on the server side (Thymeleaf, JSP, FreeMarker).
- For a REST API (`@RestController`) the view resolver is typically not used because methods return data (JSON) rather than view names.

### What's in `spring-boot-starter-webmvc`?

When you add this dependency, you get:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
```

**Note**: as of Spring Boot 4, this starter is called `spring-boot-starter-webmvc` (it used to be `spring-boot-starter-web` in Spring Boot 3 and earlier) 

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

### Spring MVC in Two Flavors: Traditional Web App vs REST API

Understanding the difference is crucial:

**Traditional Web Application**

```
Browser → Request → Spring MVC → Thymeleaf → HTML → Browser
```

Use `@Controller` when you want to return HTML views (server-side rendering). Below is a compact example showing how to build a simple page that displays a list of pizzas. It needs a template engine on the classpath, e.g. `spring-boot-starter-thymeleaf`.

```java
@Controller
@RequestMapping("/pizzas")
public class PizzaViewController {

    private final PizzaService pizzaService;

    public PizzaViewController(PizzaService pizzaService) {
        this.pizzaService = pizzaService;
    }

    @GetMapping
    public String showList(Model model) {
        model.addAttribute("pizzas", pizzaService.getAllPizzas());
        return "pizzaview"; // Resolves to templates/pizzaview.html via Thymeleaf's ViewResolver
    }
}
```

The matching template — the only Thymeleaf example in the entire course, a minimal page that renders a table:

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

Every method in `@RestController` automatically serializes its return value to the response body — as JSON by default (XML only if you add `jackson-dataformat-xml`).

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

The examples in this lesson send and return the `Pizza` entity to keep them short. The real PizzaStore uses request and response DTOs instead (Lesson 7) — never expose entities in an API.

| HTTP Method | Annotation | Purpose | Idempotent |
|-------------|-----------|---------|------------|
| **GET** | `@GetMapping` | Retrieve resource(s) | ✅ Yes |
| **POST** | `@PostMapping` | Create new resource | ❌ No |
| **PUT** | `@PutMapping` | Update/Replace entire resource | ✅ Yes |
| **PATCH** | `@PatchMapping` | Partial update | ❌ Not guaranteed |
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

Extract values from query string. The three methods below are **alternatives** for the same URL — put two of them in one controller and Spring fails at startup with an *ambiguous mapping* error.

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
    @RequestParam(defaultValue = "0") int page,     // a defaultValue makes the parameter optional
    @RequestParam(defaultValue = "10") int size
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
  "name": "Classic tomato and mozzarella",
  "price": 8.99
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

- Jackson is the library Spring Boot uses (via `spring-boot-starter-webmvc`) to convert Java objects to JSON and back.
- It performs **serialization** (Java → JSON) and **deserialization** (JSON → Java) automatically for controller methods that return objects or use `@RequestBody`.
- You can customize JSON mapping using Jackson annotations (`@JsonProperty`, `@JsonIgnore`, `@JsonFormat`, etc.) when needed.
- **Spring Boot 4** ships **Jackson 3** (package `tools.jackson` instead of Jackson 2's `com.fasterxml.jackson`; the annotations keep their `com.fasterxml.jackson.annotation` package). Java records serialize and deserialize without extra configuration (as they already did since Jackson 2.12), and `java.time` values are written as ISO-8601 strings by default. We'll lean on the record support once we introduce DTOs as `record`s in Lesson 7.

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

## 💡 Best Practices

1. **Use `@RestController` for APIs** and keep `@Controller` for server-rendered HTML.
2. **Use the specific mapping annotations** (`@GetMapping`, ...) instead of `@RequestMapping(method = ...)` on methods; put the shared base path on the class.
3. **Keep controllers thin** — read the request, call a service, build the response.
4. **Return `ResponseEntity`** when the status or headers depend on the outcome (`201` + `Location`, `404`); return the object directly otherwise.
5. **Give optional query parameters a `defaultValue`** instead of handling `null`.
6. **Don't expose entities** — accept and return DTOs (Lesson 7).

---

## 🎓 Summary

### What We Learned

1. **Spring MVC Architecture**
   - `DispatcherServlet` is the front controller
   - `HandlerMapping` finds the controller method, `HandlerAdapter` calls it and resolves its arguments
   - A `ViewResolver` turns view names into templates (traditional MVC); Jackson writes the body (REST)

2. **@Controller vs @RestController**
   - `@Controller` returns view names (HTML pages)
   - `@RestController` returns data (JSON)

3. **Request Mapping**
   - Use specific annotations: `@GetMapping`, `@PostMapping`, etc.
   - `@PathVariable` for URL path segments
   - `@RequestParam` for query parameters
   - `@RequestHeader` for headers
   - `@RequestBody` for request payload

4. **Response Handling**
   - Return objects directly for simple cases
   - Use `ResponseEntity` for full control
   - Use `@ResponseStatus` for a fixed status code

5. **Jackson**
   - Automatic JSON serialization/deserialization, Jackson 3 in Spring Boot 4

### Key Takeaways

- Every Spring MVC request passes **one servlet**, the `DispatcherServlet` — the servlet from Lesson 1, now managed for you
- Annotations describe **what** a method handles; Spring MVC does the parsing, converting and serializing
- A REST controller returns **data**, and the HTTP status code is part of that answer

### What's Next?

Controllers need data to return. Lesson 6 introduces **Spring Data**, the abstraction PizzaStore uses to store and query its domain, before Lesson 6a builds the PizzaStore domain model with Spring Data JPA.

---

## 📖 Additional Resources

- [Spring MVC Documentation](https://docs.spring.io/spring-framework/reference/web/webmvc.html)
- [Spring Boot — Servlet Web Applications](https://docs.spring.io/spring-boot/reference/web/servlet.html)
- [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
- [Jackson Documentation](https://github.com/FasterXML/jackson)
- [HTTP Status Codes](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status)

**Note on the book**: This lesson corresponds to *Pro Spring Boot 4*, Chapter 3: *Web Development with Spring Boot* — the *Spring MVC Architecture Deep Dive* section (p. 61: the request lifecycle through `DispatcherServlet`, `HandlerMapping`, `HandlerAdapter` and `HttpMessageConverter`) and *Annotations Uncovered* (p. 62: `@RestController`, `@RequestMapping`, `@RequestBody`, `@PathVariable`), plus the Jackson 3 part of *Infrastructure and Serialization Improvements* (p. 60). The book has no server-side rendered (`@Controller` + Thymeleaf) example; this lesson adds one so the difference with `@RestController` is visible.

Chapter 3 goes on to cover several topics this course spreads across later, dedicated lessons instead of repeating them here: core REST principles and native API versioning (pp. 63–64, [Lesson 8](../lesson-08-rest-principles/README.md)), full CRUD with `ResponseEntity` and `Location` headers (*Implementing Full CRUD*, p. 64, [Lesson 9](../lesson-09-complete-rest-api/README.md)), Jakarta Bean Validation and `ProblemDetail`/`@RestControllerAdvice` exception handling (pp. 64–72, [Lesson 10](../lesson-10-validation-exception-handling/README.md)), declarative HTTP service clients for calling external APIs (`@HttpExchange`, pp. 74–76, [Lesson 11](../lesson-11-external-api/README.md)), and the unified `RestTestClient` (pp. 77–78, [Lesson 12](../lesson-12-testing/README.md)). *Content Negotiation* (p. 72) is not covered: PizzaStore only speaks JSON.

---

**Congratulations!** 🎉 You now understand Spring MVC fundamentals and can tell a traditional web application from a REST API. Continue to [Lesson 6: Spring Data with Spring Boot](../lesson-06-spring-data/README.md) to give the controllers something to return.
