# Lesson 1: Introduction to Java Servlets

## 📚 Table of Contents

- [📘 Overview](#-overview)
- [🎯 Learning Objectives](#-learning-objectives)
- [🌐 Jakarta EE Platform](#-jakarta-ee-platform)
- [📚 What are Java Servlets?](#-what-are-java-servlets)
- [🔄 The Servlet Container](#-the-servlet-container)
- [📖 HttpServlet in Detail](#-httpservlet-in-detail)
- [📥 HttpServletRequest](#-httpservletrequest)
- [📤 HttpServletResponse](#-httpservletresponse)
- [💻 Complete Servlet Examples](#-complete-servlet-examples)
- [❌ Limitations of Servlets](#-limitations-of-servlets)
- [🚀 The Path to Spring Boot](#-the-path-to-spring-boot)
- [📊 Comparison: Servlet vs Spring Boot](#-comparison-servlet-vs-spring-boot)
- [🔍 Real-World Example: REST Endpoint](#-real-world-example-rest-endpoint)
- [🎓 Key Takeaways](#-key-takeaways)
- [📖 Additional Resources](#-additional-resources)

---

## 📘 Overview

This lesson provides a **concise introduction to Java Servlets** to understand the context and motivation behind Spring Boot. We'll explore why servlets exist, what problems they solve, and their limitations that led to the creation of frameworks like Spring Boot.

> **Note**: This lesson is intentionally brief. The goal is to provide context for why Spring Boot exists, not to become servlet experts.

## 🎯 Learning Objectives

By the end of this lesson, you will:
- Understand what Java Servlets are and their role in web development
- Recognize the problems that servlets solve
- Identify the limitations and complexities of servlet-based development
- Appreciate why frameworks like Spring Boot were created

## 🌐 Jakarta EE Platform

Before diving into servlets, let's understand the platform they're part of.

### What is Jakarta EE?

**Jakarta EE** (formerly Java Enterprise Edition or Java EE) is a powerful and widely-used software platform for implementing enterprise Java applications. This open-source platform provides developers with a robust and flexible environment to design and implement complex business applications.

### Jakarta EE Key Characteristics

**Jakarta EE is known for:**

- **Platform Independence**: Jakarta EE applications can run on various Java-compatible application servers. Developers can focus on application logic without worrying about the underlying infrastructure.

- **Distribution and Scalability**: Jakarta EE is designed to support distributed systems. Applications can easily be scaled to meet the demands of growing user numbers and workloads.

- **Security**: Security is a crucial aspect of enterprise applications. Jakarta EE offers extensive security features, including authentication, authorization, and data protection, to ensure sensitive data remains protected.

- **Community and Support**: Jakarta EE is maintained by an active and engaged community of developers and companies. This community works together to improve specifications and keep the technology relevant for modern business needs.

- **Open Source**: Jakarta EE is fully open source and supported by the Eclipse Foundation. The platform is freely available at no cost.

> **In short**: Jakarta EE is an essential player in the world of enterprise software development. It provides developers with the tools and infrastructure they need to build robust, scalable, and secure applications that can meet the demands of modern businesses.

### Jakarta EE Specifications

**Jakarta EE is a set of specifications and APIs** that form the foundation for building complex and distributed software applications, such as web applications, middleware, and services for large enterprises. These specifications form the backbone of Jakarta EE and provide developers with a consistent and standardized way to implement various aspects of enterprise applications.

📚 **Resources:**
- [Jakarta EE Specifications](https://jakarta.ee/specifications/)
- [Jakarta EE Compatible Products](https://jakarta.ee/compatibility/)

---

## 📚 What are Java Servlets?

### Definition

A **Java Servlet** is a Java class that extends the capabilities of servers hosting applications accessed via a request-response programming model. Servlets are the foundation of Java web applications and are part of the **Jakarta EE Servlet specification**.

**We focus on**: [Jakarta Servlet 6.0 API](https://jakarta.ee/specifications/servlet/6.0/jakarta-servlet-spec-6.0)

> **Servlets will generate an HTTP response based on an HTTP request to send back to the client.**

### Key Characteristics

- **Server-side component**: Runs on a Jakarta EE application server
- **Request-response model**: Handles HTTP requests and generates HTTP responses
- **Platform-independent**: Write once, run anywhere (Java's promise)
- **Lifecycle managed by container**: The servlet container manages creation, initialization, and destruction
- **Part of Jakarta EE specification**: Standardized API with multiple implementations

---

## 🔄 The Servlet Container

### What is a Servlet Container?

**Servlets are managed by another Java application called a Servlet Container**. When an application on a web server receives a request, the server passes this request to the servlet container, which in turn passes the request to the target servlet.

### Request Processing Flow

```
┌─────────────────────────────────────────────────────────────┐
│                         CLIENT                               │
│                    (Browser/Mobile App)                      │
└────────────────────┬─────────────────────────────────────────┘
                     │ 1. HTTP Request
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                      WEB SERVER                              │
│                   (Apache, Nginx, etc.)                      │
└────────────────────┬─────────────────────────────────────────┘
                     │ 2. Forward Request
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                   SERVLET CONTAINER                          │
│                  (Tomcat, Jetty, etc.)                       │
│                                                              │
│  3. URL Mapping → Determine target servlet                  │
└────────────────────┬─────────────────────────────────────────┘
                     │ 4. Dispatch to Servlet
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                        SERVLET                               │
│              (Your Java Application Code)                    │
│                                                              │
│  5. Process Request & Generate Response                     │
└────────────────────┬─────────────────────────────────────────┘
                     │ 6. Return Response
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                   SERVLET CONTAINER                          │
└────────────────────┬─────────────────────────────────────────┘
                     │ 7. Send Response
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                      WEB SERVER                              │
└────────────────────┬─────────────────────────────────────────┘
                     │ 8. HTTP Response
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                         CLIENT                               │
│                   (Display in Browser)                       │
└─────────────────────────────────────────────────────────────┘
```

### The 8 Steps in Detail

Every request to a servlet goes through these steps:

1. **Client sends request** → User clicks link or submits form
2. **Web server receives request** → Nginx, Apache, or embedded server
3. **Request forwarded to servlet container** → Container takes over
4. **Container determines target servlet** → Based on URL mapping
5. **Servlet processes request** → Business logic executes
6. **Servlet generates response** → HTML, JSON, XML, etc.
7. **Response sent back to web server** → Container returns result
8. **Web server sends response to client** → User sees result in browser

---

## 📖 HttpServlet in Detail

### The HttpServlet Class

The `HttpServlet` class is the foundation for creating HTTP servlets. 

📚 **JavaDoc**: [HttpServlet Documentation](https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/http/httpservlet)

**From the official documentation:**

> Provides an abstract class to be subclassed to create an HTTP servlet suitable for a Web site. A subclass of HttpServlet must override at least one method, usually one of these:

### Service Methods

Each service method handles a specific HTTP method:

| Method | HTTP Verb | Purpose | Example Use Case |
|--------|-----------|---------|------------------|
| **`doGet()`** | GET | Retrieve data | Display page, fetch list |
| **`doPost()`** | POST | Submit data | Submit form, create resource |
| **`doPut()`** | PUT | Update data | Update existing resource |
| **`doDelete()`** | DELETE | Delete data | Remove resource |
| **`init()`** | - | Initialize servlet | Setup connections, load config |
| **`destroy()`** | - | Cleanup | Close connections, release resources |
| **`getServletInfo()`** | - | Provide info | Return servlet description |

### Method Signatures

All service methods have the same signature:

```java
protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
    throws ServletException, IOException {
    // Handle GET request
}

protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
    throws ServletException, IOException {
    // Handle POST request
}
```

**Two parameters:**
1. **`HttpServletRequest req`** - The incoming request
2. **`HttpServletResponse resp`** - The response to send back

---

## 📥 HttpServletRequest

### What is HttpServletRequest?

The **servlet container creates an `HttpServletRequest` object** and passes it as a parameter to the servlet's service methods (`doGet()`, `doPost()`, etc.).

The `req` parameter represents the original HTTP request and contains all request information, such as:
- The URL used for the request
- Query parameters
- Request headers
- Form data
- Cookies
- Session information

### Common Methods

```java
@Override
protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
        throws ServletException, IOException {
    
    // Get request parameters (from URL query string or form data)
    String name = req.getParameter("name");
    String email = req.getParameter("email");
    
    // Get request headers
    String userAgent = req.getHeader("User-Agent");
    String contentType = req.getHeader("Content-Type");
    
    // Get request URL information
    String requestURI = req.getRequestURI();
    String queryString = req.getQueryString();
    
    // Get session
    HttpSession session = req.getSession();
    
    // Get request attributes
    Object attribute = req.getAttribute("someAttribute");
}
```

### Getting Parameters

**An HTTP parameter (POST or GET query parameter) is retrieved using the `getParameter()` method:**

```java
// GET request: /register?name=John&email=john@example.com
String name = req.getParameter("name");      // "John"
String email = req.getParameter("email");    // "john@example.com"

// POST request with form data
String username = req.getParameter("username");
String password = req.getParameter("password");
```

---

## 📤 HttpServletResponse

### What is HttpServletResponse?

The **`HttpServletResponse` represents the HTTP response** that the server will send back to the client. The `HttpServletResponse` consists of:
- Response body (HTML, JSON, XML, etc.)
- Cookies
- Response headers
- Status code
- Content type

**When a request comes in, the ServletContainer creates an "empty" `HttpServletResponse` and passes it to the service method.**

### Setting Response Properties

```java
@Override
protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
        throws ServletException, IOException {
    
    // Set response headers
    resp.setHeader("color", "red");
    resp.setHeader("Cache-Control", "no-cache");
    
    // Set status code
    resp.setStatus(200);  // OK
    // resp.setStatus(404);  // Not Found
    // resp.setStatus(500);  // Internal Server Error
    
    // Set content type
    resp.setContentType("text/html");
    // resp.setContentType("application/json");
    
    // Write response body
    resp.getWriter().println("My first servlet");
}
```

### Writing Response Content

```java
@Override
protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
        throws ServletException, IOException {
    
    resp.setContentType("text/html");
    resp.setCharacterEncoding("UTF-8");
    
    PrintWriter out = resp.getWriter();
    out.println("<!DOCTYPE html>");
    out.println("<html>");
    out.println("<head><title>Hello Servlet</title></head>");
    out.println("<body>");
    out.println("<h1>Hello from Servlet!</h1>");
    out.println("<p>Current time: " + new java.util.Date() + "</p>");
    out.println("</body>");
    out.println("</html>");
}
```

### Common Response Methods

| Method | Purpose | Example |
|--------|---------|---------|
| `setStatus(int)` | Set HTTP status code | `resp.setStatus(200)` |
| `setContentType(String)` | Set content MIME type | `resp.setContentType("text/html")` |
| `setHeader(String, String)` | Set response header | `resp.setHeader("Cache-Control", "no-cache")` |
| `getWriter()` | Get writer for text | `resp.getWriter().println("Hello")` |
| `getOutputStream()` | Get stream for binary | `resp.getOutputStream().write(bytes)` |
| `sendRedirect(String)` | Redirect to URL | `resp.sendRedirect("/home")` |
| `sendError(int)` | Send error page | `resp.sendError(404, "Not Found")` |

---

## 💻 Complete Servlet Examples

### Simple GET Servlet: HelloServlet.java

```java
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.WebServlet;
import java.io.*;

@WebServlet("/hello")
public class HelloServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, 
                         HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head><title>Hello Servlet</title></head>");
        out.println("<body>");
        out.println("<h1>Hello from Servlet!</h1>");
        out.println("<p>This is a simple GET request example.</p>");
        out.println("</body>");
        out.println("</html>");
    }
}
```

### Servlet Configuration (web.xml)

Before Servlet 3.0, you had to configure servlets in `web.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
         https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
         version="6.0">
    
    <servlet>
        <servlet-name>hello</servlet-name>
        <servlet-class>HelloServlet</servlet-class>
    </servlet>
    
    <servlet-mapping>
        <servlet-name>hello</servlet-name>
        <url-pattern>/hello</url-pattern>
    </servlet-mapping>
    
</web-app>
```

### Modern Annotation-Based Configuration (recommended)

Since Servlet 3.0, you can use annotations:

```java
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;

@WebServlet("/hello")
public class HelloServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, 
                         HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.println("<html><body>");
        out.println("<h1>Hello from Servlet!</h1>");
        out.println("</body></html>");
    }
}
```

## ❌ Limitations of Servlets

Despite their advantages, servlets have significant drawbacks:

### 1. **Boilerplate Code**
```java
// Too much code for simple tasks
response.setContentType("text/html");
PrintWriter out = response.getWriter();
out.println("<html>");
out.println("<body>");
// ... and so on
```

### 2. **HTML Mixed with Java**
- Hard to maintain
- Difficult for designers to work with
- No clear separation of concerns

```java
out.println("<table>");
out.println("<tr><td>" + user.getName() + "</td></tr>");
out.println("</table>");
```

### 3. **Manual Request Parameter Handling**
```java
String name = request.getParameter("name");
String ageStr = request.getParameter("age");
int age = Integer.parseInt(ageStr); // Manual conversion, no validation
```

### 4. **No Built-in Dependency Injection**
- Manual object creation
- Tight coupling between components
- Hard to test

### 5. **Configuration Complexity**
- Verbose XML configuration (pre-Servlet 3.0)
- Manual URL mapping
- No convention-over-configuration

### 6. **Limited Abstraction**
- Low-level HTTP handling
- No MVC pattern built-in
- No automatic JSON/XML conversion

### 7. **Testing Challenges**
- Requires servlet container to test
- Hard to mock HttpServletRequest/Response
- Integration tests are slow

## 🚀 The Path to Spring Boot

The limitations of servlets led to the creation of:

### 1. **JSP (JavaServer Pages)**
- Separated HTML from Java (somewhat)
- Still messy for complex logic

### 2. **MVC Frameworks**
- Struts, JSF, Spring MVC
- Better separation of concerns
- More structure and conventions

### 3. **Spring Framework**
- Dependency Injection
- Aspect-Oriented Programming
- Transaction management
- But still complex to configure

### 4. **Spring Boot** 🎉
- **Convention over configuration**
- **Auto-configuration** (no XML!)
- **Embedded servers** (no deployment needed)
- **Production-ready features** (metrics, health checks)
- **Opinionated defaults** (but customizable)

## 📊 Comparison: Servlet vs Spring Boot

| Aspect | Servlet | Spring Boot |
|--------|---------|-------------|
| **Configuration** | Verbose XML or annotations | Minimal, auto-configured |
| **Boilerplate** | Lots of repetitive code | Minimal boilerplate |
| **Dependency Injection** | Manual object creation | Built-in DI container |
| **JSON Handling** | Manual parsing/serialization | Automatic with Jackson |
| **Database Access** | Manual JDBC | Spring Data JPA |
| **Testing** | Requires container | Easy unit/integration tests |
| **Deployment** | WAR to external server | Embedded server, JAR |
| **URL Mapping** | Manual configuration | Annotation-based |
| **Learning Curve** | Moderate | Easier (but more to learn) |

## 🔍 Real-World Example: REST Endpoint

### With Pure Servlet

```java
@WebServlet("/api/users")
public class UserServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        // Manual JSON handling
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        // Manual database connection
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM users");
            
            // Manual JSON building
            StringBuilder json = new StringBuilder("[");
            while (rs.next()) {
                json.append("{");
                json.append("\"id\":").append(rs.getInt("id")).append(",");
                json.append("\"name\":\"").append(rs.getString("name")).append("\"");
                json.append("},");
            }
            json.append("]");
            
            PrintWriter out = resp.getWriter();
            out.print(json.toString());
            
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print("{\"error\":\"" + e.getMessage() + "\"}");
        } finally {
            if (conn != null) conn.close();
        }
    }
}
```

### With Spring Boot

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
```

**Notice the difference?**
- ✅ No manual JSON handling
- ✅ No manual database connection
- ✅ No boilerplate code
- ✅ Clean, readable, maintainable

## 🎓 Key Takeaways

1. **Servlets are the foundation** of Java web applications
2. **Servlets solved important problems** (performance, portability, scalability)
3. **But servlets have limitations** (boilerplate, complexity, testing)
4. **Frameworks evolved** to address these limitations
5. **Spring Boot represents the culmination** of decades of Java web development evolution
6. **Understanding servlets helps appreciate** what Spring Boot does for you

## 📖 Additional Resources

- [Jakarta Servlet Specification](https://jakarta.ee/specifications/servlet/)
- [Oracle Servlet Tutorial](https://docs.oracle.com/javaee/7/tutorial/servlets.htm)

---

**Remember**: You don't need to be a servlet expert. The goal is to understand the "why" behind Spring Boot, not the "how" of servlets. Let's move forward to Spring! 🚀
