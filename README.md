# Java Backend Programming with Spring Boot

## 🎯 Overview

**Comprehensive Spring Boot 3 Course for Building REST API Backends**

This course teaches students to build production-ready Java backend applications for mobile applications using modern Spring Boot practices. Students will learn by building a complete **PizzaStore REST API** from scratch.

---

## 📚 Course Structure

**Foundation**
1. **Introduction to Java Servlets** - Why Spring Boot exists
2. **Spring DI and IoC** - Core Spring concepts and dependency injection
3. **Properties and Profiles** - Configuration management
4. **Spring Boot Introduction** - First PizzaStore project

**Web & Data**
5. **Spring MVC** - Web application architecture
6. **Working with JPA** - Data persistence and entity relationships
7. **DTOs & Mappers** - Request/Response objects with MapStruct

**REST API Development**
8. **REST Principles** - HTTP methods, status codes, resource design
9. **Building a Complete REST API** - Full CRUD, pagination, image upload
10. **Validation & Exception Handling** - Input validation and error responses
11. **Testing Spring Boot Applications** - Repository, Service, Controller, Integration tests
12. **JWT Authentication** - Token-based authentication with roles
(13. **Security with IdP** - OAuth2/OIDC with Dex Identity Provider)
14. **Swagger/OpenAPI** - API documentation and Swagger UI
15. **Spring Boot Actuator** - Production monitoring and health checks

---

## 🍕 The PizzaStore Project

Students build one evolving **PizzaStore REST API** throughout the course:

### Domain Model
- **Pizza** - Menu items with prices, descriptions, images, and nutritional info
- **Customer** - User accounts with favorites
- **Order** - Orders with multiple order lines
- **OrderLine** - Individual pizza items in an order
- **NutritionalInfo** - Calorie and nutrition data per pizza

---

## 🚀 Getting Started

### Prerequisites

- **Java 25** installed
- **Maven 3.6+**
- **IntelliJ IDEA Ultimate** (recommended)
- **Postman** or similar API testing tool
- **Docker** (for Dex IdP in Lesson 13)

### Quick Start

```bash
# Clone the repository
git clone <repository-url>
cd cursus

# Start with the latest complete project
cd lesson-11-testing/pizzastore-with-tests

# Build and run
mvn clean install
mvn spring-boot:run

# Test the API
curl http://localhost:8080/api/pizzas

# Access H2 Console
open http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:pizzastoredb
# Username: sa
# Password: (leave empty)
```

---

## 📁 Repository Structure

```
cursus/
├── lesson-01-servlets/              # Context for Spring Boot
├── lesson-02-spring-di-ioc/         # Dependency Injection
├── lesson-03-properties-profiles/   # Configuration
├── lesson-04-spring-boot-intro/     # 🍕 First PizzaStore
├── lesson-05-spring-mvc/            # MVC architecture
├── lesson-06-working-with-jpa/      # 🍕 Full domain model
├── lesson-07-dtos-mappers/          # 🍕 DTOs & Service layer
├── lesson-08-rest-principles/       # REST theory
├── lesson-09-complete-rest-api/     # 🍕 Complete CRUD API
├── lesson-10-validation-.../        # 🍕 With validation
├── lesson-11-testing/               # 🍕 With comprehensive tests
├── lesson-12-jwt-authentication/    # 🍕 With JWT authentication
└── uploads/                         # Pizza image storage
```

---

## 📖 Learning Approach

- ✅ **Incremental Learning** - Each lesson builds on the previous
- ✅ **Hands-On Practice** - Build a real-world application
- ✅ **Best Practices** - Production-ready patterns and code
- ✅ **Complete Examples** - Every lesson includes working code

---

## 🎯 Course Outcome

By completing this course, students will:

1. Understand Spring Boot fundamentals and architecture
2. Build complete REST APIs with proper design principles
3. Implement data persistence with JPA and complex relationships
4. Apply validation and comprehensive error handling
5. Write thorough tests (unit, integration, and E2E)
6. Secure applications with JWT and OAuth2/OIDC
7. Document APIs with OpenAPI/Swagger
8. Prepare applications for production with monitoring

**Final Goal**: Students can build and deploy a production-ready Spring Boot REST API backend for mobile applications, ready for the final exam.

---

## 🎓 Final Exam

Students will build a complete REST API backend using all learned concepts.

**Exam Repository**: [GitHub - Examenopdracht](https://github.com/vives-backendprogramming/Examenopdracht)

---

## 📝 License

This course material is created for educational purposes at VIVES University.
