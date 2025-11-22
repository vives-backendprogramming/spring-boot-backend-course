# PizzaStore REST API - Complete Specification

**Version**: 1.0  
**Base URL**: `http://localhost:8080/api`  
**Authentication**: JWT Bearer Token or OAuth2/OIDC (Dex IdP)

---

## Table of Contents

1. [Overview](#overview)
2. [Domain Model](#domain-model)
3. [Security & Roles](#security--roles)
4. [Common Patterns](#common-patterns)
5. [Error Responses](#error-responses)
6. [API Endpoints](#api-endpoints)
   - [Authentication](#authentication)
   - [Pizzas](#pizzas)
   - [Customers](#customers)
   - [Orders](#orders)
   - [Images](#images)
7. [Data Models](#data-models)

---

## Overview

The PizzaStore REST API is a backend service for a pizza ordering system. It supports:

- **Anonymous users**: Can browse pizzas (read-only access)
- **Authenticated customers**: Can place orders, manage their profile, and favorite pizzas
- **Administrators**: Can manage pizzas, view all orders, and configure the store

**Key Features**:
- JWT-based authentication or OAuth2/OIDC with Dex Identity Provider
- Role-based authorization (CUSTOMER, ADMIN)
- Image upload and storage for pizza images
- Audit fields (createdAt, createdBy, updatedAt, updatedBy) on key entities
- Full CRUD operations with proper REST principles
- Pagination, sorting, and filtering support
- Comprehensive validation and error handling
- OpenAPI/Swagger documentation
- Production monitoring via Spring Boot Actuator

---

## Domain Model

### Entities

```
┌─────────────┐       ┌─────────────────┐       ┌──────────────┐
│   Customer  │──────<│     Order       │>──────│  OrderLine   │
│             │ 1   * │                 │ 1   * │              │
│  - id       │       │  - id           │       │  - id        │
│  - name     │       │  - orderNumber  │       │  - quantity  │
│  - email    │       │  - orderDate    │       │  - unitPrice │
│  - phone    │       │  - totalAmount  │       │  - subtotal  │
│  - address  │       │  - status       │       └──────────────┘
│  - password │       │  - createdAt    │              │
│  - role     │       │  - createdBy    │              │ *
│  - createdAt│       │  - updatedAt    │              │
│  - updatedAt│       │  - updatedBy    │              ↓
└─────────────┘       └─────────────────┘       ┌──────────────┐
       │ *                                       │    Pizza     │
       │                                         │              │
       │             ┌─────────────────┐         │  - id        │
       └────────────<│ FavoritePizzas  │>────────│  - name      │
                 * * │ (Join Table)    │ *       │  - price     │
                     └─────────────────┘         │  - description│
                                                 │  - imageUrl  │
                                                 │  - available │
                           ┌──────────────────┐  │  - createdAt │
                           │ NutritionalInfo  │  │  - createdBy │
                           │                  │  │  - updatedAt │
                           │  - calories      │<─│  - updatedBy │
                           │  - protein       │1 └──────────────┘
                           │  - carbs         │
                           │  - fat           │
                           └──────────────────┘
```

### Entity Details

#### Pizza
- **id**: Long (auto-generated)
- **name**: String (required, max 100 chars)
- **price**: BigDecimal (required, precision 10, scale 2)
- **description**: String (max 1000 chars)
- **imageUrl**: String (nullable, URL to stored image)
- **available**: Boolean (default true)
- **createdAt**: LocalDateTime (auto-set on creation)
- **createdBy**: String (username/email of creator)
- **updatedAt**: LocalDateTime (auto-updated)
- **updatedBy**: String (username/email of updater)
- **nutritionalInfo**: NutritionalInfo (OneToOne, optional)

#### Customer
- **id**: Long (auto-generated)
- **name**: String (required, max 100 chars)
- **email**: String (required, unique, max 100 chars)
- **password**: String (required for JWT auth, null for OIDC, BCrypt hashed)
- **sub**: String (optional, OIDC subject identifier, unique, only used with OIDC)
- **phone**: String (optional, max 20 chars)
- **address**: String (optional, max 200 chars)
- **role**: Enum (CUSTOMER, ADMIN)
- **createdAt**: LocalDateTime (auto-set)
- **updatedAt**: LocalDateTime (auto-updated)
- **orders**: List<Order> (OneToMany)
- **favoritePizzas**: Set<Pizza> (ManyToMany)

**Note on authentication fields:**
- **JWT mode**: `password` is required and hashed, `sub` is null
- **OIDC mode**: `sub` is required (from IdP), `password` is null

#### Order
- **id**: Long (auto-generated)
- **orderNumber**: String (required, unique, format: "ORD-YYYYMMDD-XXXXX")
- **orderDate**: LocalDateTime (auto-set)
- **totalAmount**: BigDecimal (calculated from order lines)
- **status**: OrderStatus (PENDING, CONFIRMED, PREPARING, READY, DELIVERED, CANCELLED)
- **createdAt**: LocalDateTime (auto-set)
- **createdBy**: String (customer email)
- **updatedAt**: LocalDateTime (auto-updated)
- **updatedBy**: String (who updated the order)
- **customer**: Customer (ManyToOne, required)
- **orderLines**: List<OrderLine> (OneToMany)

#### OrderLine
- **id**: Long (auto-generated)
- **pizza**: Pizza (ManyToOne, required)
- **quantity**: Integer (required, min 1)
- **unitPrice**: BigDecimal (snapshot of pizza price at order time)
- **subtotal**: BigDecimal (calculated: unitPrice * quantity)
- **order**: Order (ManyToOne, required)

#### NutritionalInfo
- **id**: Long (auto-generated)
- **calories**: Integer
- **protein**: BigDecimal (grams)
- **carbohydrates**: BigDecimal (grams)
- **fat**: BigDecimal (grams)
- **pizza**: Pizza (OneToOne, required)

#### OrderStatus (Enum)
- `PENDING`: Order created, awaiting confirmation
- `CONFIRMED`: Order confirmed by customer or admin
- `PREPARING`: Pizza is being prepared
- `READY`: Order ready for pickup/delivery
- `DELIVERED`: Order completed
- `CANCELLED`: Order cancelled

---

## Security & Roles

### Authentication Methods

The PizzaStore API supports two authentication methods. **These are mutually exclusive** - choose one based on your requirements and learning objectives.

#### Method 1: JWT Authentication (Lessons 12-13)
**Self-managed authentication with email/password**

This is the **primary method** for the course as it teaches fundamental authentication concepts.

**How it works:**
1. Users register with email and password via `/customers/register`
2. Passwords are hashed using BCrypt and stored in the database
3. Users login via `/auth/login` with email/password
4. Application validates credentials against database
5. Upon successful login, a JWT token is generated and returned
6. Client includes token in `Authorization: Bearer <token>` header
7. Application validates token locally (no external dependencies)

**What you manage:**
- ✅ User registration and password storage (hashed)
- ✅ Login endpoint and credential validation
- ✅ JWT token generation and validation
- ✅ Password reset functionality (optional)

**Customer entity includes:**
```java
@Column(nullable = false)
private String password; // BCrypt hashed

@Enumerated(EnumType.STRING)
private Role role;
```

**Dependencies:**
- `spring-boot-starter-security`
- `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (for JWT)

**Use case:** Traditional web/mobile applications where you manage all customer data.

---

#### Method 2: OAuth2/OIDC with Dex Identity Provider (Lesson 14 - Advanced/Optional)
**Delegated authentication via external identity provider**

This is an **advanced, optional method** that demonstrates modern SSO (Single Sign-On) patterns.

**How it works:**
1. Users are redirected to Dex IdP for authentication
2. Dex supports multiple backends: Google, GitHub, LDAP, email/password, etc.
3. User authenticates at Dex (you don't see the password)
4. Dex returns an OIDC token to your application
5. Application validates token via OIDC discovery (calls Dex to verify)
6. On first login, customer profile is auto-created from OIDC claims
7. Client includes OIDC token in `Authorization: Bearer <token>` header

**What Dex manages:**
- ✅ Authentication process and login UI
- ✅ Password storage (if using Dex's email/password backend)
- ✅ Token generation
- ✅ User federation (Google, GitHub, LDAP, etc.)

**What you manage:**
- ✅ Token validation via OIDC
- ✅ User profile storage (name, email, role)
- ✅ Role assignment
- ❌ **NO password storage** in your database!

**Customer entity includes:**
```java
@Column(unique = true)
private String sub; // Subject from OIDC (unique ID from IdP)

// NO password field!

@Enumerated(EnumType.STRING)
private Role role; // Still managed locally
```

**First login auto-registration:**
```java
// Pseudo-code
OidcUser oidcUser = validateTokenWithDex(token);
Customer customer = customerRepo.findBySub(oidcUser.getSub())
    .orElseGet(() -> createCustomerFromOidcClaims(oidcUser));
```

**Dependencies:**
- `spring-boot-starter-security`
- `spring-boot-starter-oauth2-resource-server`
- Dex IdP (running in Docker)

**Use case:** Enterprise applications, SSO requirements, or when you want to support multiple login providers.

---

### Choosing Between JWT and OIDC

| Criteria | JWT (Self-managed) | OIDC (Dex IdP) |
|----------|-------------------|----------------|
| **Complexity** | Medium | Higher |
| **Password Management** | You manage (BCrypt hashing) | Dex manages (or delegates) |
| **User Registration** | Manual via API | Auto on first login |
| **External Dependencies** | None | Dex IdP required |
| **Learning Value** | Fundamentals of auth | Modern SSO patterns |
| **Production Suitable** | ✅ Yes | ✅ Yes (better for SSO) |
| **Mobile App Ready** | ✅ Yes | ✅ Yes |
| **Course Level** | Primary (Lessons 12-13) | Advanced (Lesson 14) |

**Recommendation for the course:**
1. **Start with JWT** to understand authentication fundamentals
2. **Optionally add OIDC** in Lesson 14 to learn modern patterns
3. Use Spring profiles to switch between them without code changes

---

### Switching Between Authentication Methods

You can use **Spring Profiles** to switch between JWT and OIDC without changing code:

**application-jwt.yml:**
```yaml
spring:
  profiles:
    active: jwt
  security:
    jwt:
      secret: ${JWT_SECRET:your-secret-key-min-256-bits}
      expiration: 3600000 # 1 hour in milliseconds
```

**application-oidc.yml:**
```yaml
spring:
  profiles:
    active: oidc
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:5556/dex
```

**Security Configuration with Profiles:**
```java
@Configuration
@Profile("jwt")
public class JwtSecurityConfig {
    // JWT configuration
}

@Configuration
@Profile("oidc")
public class OidcSecurityConfig {
    // OIDC configuration
}
```

**Run with JWT:**
```bash
mvn spring-boot:run -Dspring.profiles.active=jwt
```

**Run with OIDC:**
```bash
mvn spring-boot:run -Dspring.profiles.active=oidc
```

---

### Roles

| Role | Description |
|------|-------------|
| `ANONYMOUS` | Unauthenticated users - can only view pizzas |
| `CUSTOMER` | Authenticated customers - can place orders, manage profile |
| `ADMIN` | Store administrators - can manage pizzas, view all orders |

### Endpoint Authorization Matrix

| Endpoint | Anonymous | Customer | Admin |
|----------|-----------|----------|-------|
| **Pizzas** |
| GET /pizzas | ✅ | ✅ | ✅ |
| GET /pizzas/{id} | ✅ | ✅ | ✅ |
| POST /pizzas | ❌ | ❌ | ✅ |
| PUT /pizzas/{id} | ❌ | ❌ | ✅ |
| PATCH /pizzas/{id} | ❌ | ❌ | ✅ |
| DELETE /pizzas/{id} | ❌ | ❌ | ✅ |
| POST /pizzas/{id}/image | ❌ | ❌ | ✅ |
| **Customers** |
| POST /customers/register | ✅ | ❌ | ❌ |
| GET /customers/me | ❌ | ✅ (own) | ✅ |
| PUT /customers/me | ❌ | ✅ (own) | ✅ |
| GET /customers | ❌ | ❌ | ✅ |
| GET /customers/{id} | ❌ | ❌ | ✅ |
| **Orders** |
| POST /orders | ❌ | ✅ | ✅ |
| GET /orders/my-orders | ❌ | ✅ (own) | ✅ |
| GET /orders/{id} | ❌ | ✅ (own) | ✅ |
| GET /orders | ❌ | ❌ | ✅ |
| PATCH /orders/{id}/status | ❌ | ❌ | ✅ |
| DELETE /orders/{id} | ❌ | ❌ | ✅ |
| **Favorites** |
| POST /customers/me/favorites/{pizzaId} | ❌ | ✅ | ✅ |
| DELETE /customers/me/favorites/{pizzaId} | ❌ | ✅ | ✅ |
| GET /customers/me/favorites | ❌ | ✅ | ✅ |

---

## Common Patterns

### Pagination

All list endpoints support pagination:

**Request Parameters**:
- `page`: Page number (0-indexed, default: 0)
- `size`: Page size (default: 20, max: 100)
- `sort`: Sort field and direction (e.g., `name,asc` or `price,desc`)

**Response Format**:
```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalPages": 5,
  "totalElements": 100,
  "last": false,
  "first": true,
  "numberOfElements": 20
}
```

### Filtering

Supported filters vary by endpoint:

**Pizzas**:
- `available`: Boolean (filter by availability)
- `minPrice`: BigDecimal
- `maxPrice`: BigDecimal
- `name`: String (case-insensitive contains search)

**Orders**:
- `status`: OrderStatus
- `customerId`: Long
- `fromDate`: LocalDateTime
- `toDate`: LocalDateTime

### Timestamps

All timestamps are in ISO 8601 format with timezone:
```
2024-01-15T14:30:00+01:00
```

### CORS

CORS is enabled for mobile app integration:
- Allowed origins: Configurable (default: `*` for development)
- Allowed methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
- Allowed headers: Authorization, Content-Type
- Max age: 3600

---

## Error Responses

All error responses follow a consistent format:

```json
{
  "timestamp": "2024-01-15T14:30:00+01:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/pizzas",
  "errors": [
    {
      "field": "price",
      "rejectedValue": -5.00,
      "message": "Price must be greater than 0"
    }
  ]
}
```

### HTTP Status Codes

| Code | Description | When Used |
|------|-------------|-----------|
| 200 | OK | Successful GET, PUT, PATCH |
| 201 | Created | Successful POST (resource created) |
| 204 | No Content | Successful DELETE |
| 400 | Bad Request | Validation errors, invalid input |
| 401 | Unauthorized | Missing or invalid authentication |
| 403 | Forbidden | Authenticated but not authorized |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Duplicate resource (e.g., email already exists) |
| 500 | Internal Server Error | Unexpected server error |

---

## API Endpoints

### Authentication

#### 1. Login (JWT Authentication)

**Endpoint**: `POST /auth/login`  
**Access**: Anonymous  
**Authentication Method**: JWT only  
**Description**: Authenticate with email/password and receive JWT token

**Request**:
```json
{
  "email": "customer@example.com",
  "password": "password123"
}
```

**Response** (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "expiresIn": 3600,
  "customer": {
    "id": 1,
    "email": "customer@example.com",
    "name": "John Doe",
    "role": "CUSTOMER"
  }
}
```

**Errors**:
- 401: Invalid credentials
- 400: Validation error (missing email or password)

**Note**: This endpoint is only available when using JWT authentication. With OIDC, authentication happens via Dex IdP redirect flow.

---

#### 2. Refresh Token (JWT Authentication)

**Endpoint**: `POST /auth/refresh`  
**Access**: Authenticated  
**Authentication Method**: JWT only  
**Description**: Refresh an expiring JWT token

**Request**:
```json
{
  "refreshToken": "..."
}
```

**Response** (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "expiresIn": 3600
}
```

**Note**: Only applicable with JWT authentication. OIDC tokens are refreshed via the Dex IdP.

---

### Pizzas

#### 1. Get All Pizzas

**Endpoint**: `GET /pizzas`  
**Access**: Anonymous  
**Description**: Retrieve all pizzas with optional filtering and pagination

**Query Parameters**:
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)
- `sort`: Sort criteria (e.g., `name,asc`, `price,desc`)
- `available`: Filter by availability (true/false)
- `minPrice`: Minimum price filter
- `maxPrice`: Maximum price filter
- `name`: Search by name (case-insensitive)

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": 1,
      "name": "Margherita",
      "price": 8.99,
      "description": "Classic tomato and mozzarella",
      "imageUrl": "https://storage.pizzastore.com/pizzas/margherita.jpg",
      "available": true,
      "nutritionalInfo": {
        "calories": 250,
        "protein": 12.5,
        "carbohydrates": 30.0,
        "fat": 8.5
      }
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalPages": 3,
  "totalElements": 45,
  "last": false,
  "first": true
}
```

---

#### 2. Get Pizza by ID

**Endpoint**: `GET /pizzas/{id}`  
**Access**: Anonymous  
**Description**: Retrieve a specific pizza by ID

**Path Parameters**:
- `id`: Pizza ID (Long)

**Response** (200 OK):
```json
{
  "id": 1,
  "name": "Margherita",
  "price": 8.99,
  "description": "Classic tomato and mozzarella",
  "imageUrl": "https://storage.pizzastore.com/pizzas/margherita.jpg",
  "available": true,
  "nutritionalInfo": {
    "calories": 250,
    "protein": 12.5,
    "carbohydrates": 30.0,
    "fat": 8.5
  }
}
```

**Errors**:
- 404: Pizza not found

---

#### 3. Create Pizza

**Endpoint**: `POST /pizzas`  
**Access**: Admin only  
**Description**: Create a new pizza

**Request**:
```json
{
  "name": "Quattro Formaggi",
  "price": 11.99,
  "description": "Four cheese blend with gorgonzola, mozzarella, parmesan, and fontina",
  "available": true,
  "nutritionalInfo": {
    "calories": 320,
    "protein": 18.0,
    "carbohydrates": 28.0,
    "fat": 15.0
  }
}
```

**Response** (201 Created):
```json
{
  "id": 15,
  "name": "Quattro Formaggi",
  "price": 11.99,
  "description": "Four cheese blend with gorgonzola, mozzarella, parmesan, and fontina",
  "imageUrl": null,
  "available": true,
  "nutritionalInfo": {
    "calories": 320,
    "protein": 18.0,
    "carbohydrates": 28.0,
    "fat": 15.0
  }
}
```

**Headers**:
- `Location: /api/pizzas/15`

**Validation Rules**:
- `name`: Required, 1-100 characters
- `price`: Required, > 0, max 2 decimal places
- `description`: Optional, max 1000 characters
- `available`: Default true
- `nutritionalInfo`: Optional

**Errors**:
- 400: Validation error
- 401: Not authenticated
- 403: Not authorized (not admin)

---

#### 4. Update Pizza (Full)

**Endpoint**: `PUT /pizzas/{id}`  
**Access**: Admin only  
**Description**: Full update of a pizza (replaces all fields)

**Path Parameters**:
- `id`: Pizza ID (Long)

**Request**:
```json
{
  "name": "Margherita Deluxe",
  "price": 10.99,
  "description": "Premium tomato sauce with buffalo mozzarella and fresh basil",
  "available": true,
  "nutritionalInfo": {
    "calories": 280,
    "protein": 14.0,
    "carbohydrates": 32.0,
    "fat": 9.5
  }
}
```

**Response** (200 OK):
```json
{
  "id": 1,
  "name": "Margherita Deluxe",
  "price": 10.99,
  "description": "Premium tomato sauce with buffalo mozzarella and fresh basil",
  "imageUrl": "https://storage.pizzastore.com/pizzas/margherita.jpg",
  "available": true,
  "nutritionalInfo": {
    "calories": 280,
    "protein": 14.0,
    "carbohydrates": 32.0,
    "fat": 9.5
  }
}
```

**Errors**:
- 400: Validation error
- 401: Not authenticated
- 403: Not authorized
- 404: Pizza not found

---

#### 5. Update Pizza (Partial)

**Endpoint**: `PATCH /pizzas/{id}`  
**Access**: Admin only  
**Description**: Partial update of a pizza (updates only provided fields)

**Path Parameters**:
- `id`: Pizza ID (Long)

**Request** (example - only updating price and availability):
```json
{
  "price": 9.99,
  "available": false
}
```

**Response** (200 OK):
```json
{
  "id": 1,
  "name": "Margherita",
  "price": 9.99,
  "description": "Classic tomato and mozzarella",
  "imageUrl": "https://storage.pizzastore.com/pizzas/margherita.jpg",
  "available": false,
  "nutritionalInfo": {
    "calories": 250,
    "protein": 12.5,
    "carbohydrates": 30.0,
    "fat": 8.5
  }
}
```

**Errors**:
- 400: Validation error
- 401: Not authenticated
- 403: Not authorized
- 404: Pizza not found

---

#### 6. Delete Pizza

**Endpoint**: `DELETE /pizzas/{id}`  
**Access**: Admin only  
**Description**: Delete a pizza (soft delete - mark as unavailable, or hard delete if no orders reference it)

**Path Parameters**:
- `id`: Pizza ID (Long)

**Response** (204 No Content)

**Errors**:
- 401: Not authenticated
- 403: Not authorized
- 404: Pizza not found
- 409: Cannot delete - pizza is referenced in existing orders

---

#### 7. Upload Pizza Image

**Endpoint**: `POST /pizzas/{id}/image`  
**Access**: Admin only  
**Description**: Upload an image for a pizza

**Path Parameters**:
- `id`: Pizza ID (Long)

**Request**:
- Content-Type: `multipart/form-data`
- Field: `image` (file)

**Allowed Formats**:
- JPEG (.jpg, .jpeg)
- PNG (.png)
- Maximum size: 5MB
- Recommended dimensions: 800x800px

**Response** (200 OK):
```json
{
  "id": 1,
  "name": "Margherita",
  "imageUrl": "https://storage.pizzastore.com/pizzas/margherita-20240115151500.jpg"
}
```

**Errors**:
- 400: Invalid file format or size
- 401: Not authenticated
- 403: Not authorized
- 404: Pizza not found

---

### Customers

#### 1. Register Customer

**Endpoint**: `POST /customers/register`  
**Access**: Anonymous  
**Authentication Method**: JWT only  
**Description**: Register a new customer account with email and password

**Request**:
```json
{
  "name": "Jane Smith",
  "email": "jane.smith@example.com",
  "password": "SecurePass123!",
  "phone": "+32 123 45 67 89",
  "address": "123 Pizza Street, Brussels, Belgium"
}
```

**Response** (201 Created):
```json
{
  "id": 5,
  "name": "Jane Smith",
  "email": "jane.smith@example.com",
  "phone": "+32 123 45 67 89",
  "address": "123 Pizza Street, Brussels, Belgium",
  "role": "CUSTOMER"
}
```

**Headers**:
- `Location: /api/customers/5`

**Validation Rules**:
- `name`: Required, 2-100 characters
- `email`: Required, valid email format, unique
- `password`: Required, min 8 characters, at least 1 uppercase, 1 lowercase, 1 number
- `phone`: Optional, max 20 characters
- `address`: Optional, max 200 characters

**Errors**:
- 400: Validation error
- 409: Email already exists

**Note**: This endpoint is only available with JWT authentication. With OIDC, users are auto-registered on first login from the IdP.

---

#### 2. Get Current Customer Profile

**Endpoint**: `GET /customers/me`  
**Access**: Customer (own profile) or Admin  
**Description**: Get the authenticated customer's profile

**Response** (200 OK):
```json
{
  "id": 5,
  "name": "Jane Smith",
  "email": "jane.smith@example.com",
  "phone": "+32 123 45 67 89",
  "address": "123 Pizza Street, Brussels, Belgium",
  "role": "CUSTOMER",
  "orderCount": 12,
  "totalSpent": 156.78
}
```

**Errors**:
- 401: Not authenticated

---

#### 3. Update Current Customer Profile

**Endpoint**: `PUT /customers/me`  
**Access**: Customer (own profile) or Admin  
**Description**: Update the authenticated customer's profile

**Request**:
```json
{
  "name": "Jane Smith-Jones",
  "phone": "+32 123 99 99 99",
  "address": "456 New Street, Antwerp, Belgium"
}
```

**Response** (200 OK):
```json
{
  "id": 5,
  "name": "Jane Smith-Jones",
  "email": "jane.smith@example.com",
  "phone": "+32 123 99 99 99",
  "address": "456 New Street, Antwerp, Belgium",
  "role": "CUSTOMER"
}
```

**Note**: Email and password cannot be changed via this endpoint (use separate endpoints)

**Errors**:
- 400: Validation error
- 401: Not authenticated

---

#### 4. Get All Customers

**Endpoint**: `GET /customers`  
**Access**: Admin only  
**Description**: Get all customers with pagination

**Query Parameters**:
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)
- `sort`: Sort criteria (e.g., `name,asc`)
- `email`: Filter by email (contains)
- `name`: Filter by name (contains)

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": 5,
      "name": "Jane Smith",
      "email": "jane.smith@example.com",
      "phone": "+32 123 45 67 89",
      "address": "123 Pizza Street, Brussels, Belgium",
      "role": "CUSTOMER",
      "orderCount": 12
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalPages": 8,
  "totalElements": 156
}
```

**Errors**:
- 401: Not authenticated
- 403: Not authorized (not admin)

---

#### 5. Get Customer by ID

**Endpoint**: `GET /customers/{id}`  
**Access**: Admin only  
**Description**: Get a specific customer by ID

**Path Parameters**:
- `id`: Customer ID (Long)

**Response** (200 OK):
```json
{
  "id": 5,
  "name": "Jane Smith",
  "email": "jane.smith@example.com",
  "phone": "+32 123 45 67 89",
  "address": "123 Pizza Street, Brussels, Belgium",
  "role": "CUSTOMER",
  "orderCount": 12,
  "totalSpent": 156.78
}
```

**Errors**:
- 401: Not authenticated
- 403: Not authorized
- 404: Customer not found

---

### Orders

#### 1. Create Order

**Endpoint**: `POST /orders`  
**Access**: Customer or Admin  
**Description**: Create a new order for the authenticated customer

**Request**:
```json
{
  "orderLines": [
    {
      "pizzaId": 1,
      "quantity": 2
    },
    {
      "pizzaId": 5,
      "quantity": 1
    }
  ],
  "deliveryAddress": "123 Pizza Street, Brussels, Belgium"
}
```

**Response** (201 Created):
```json
{
  "id": 42,
  "orderNumber": "ORD-20240115-00042",
  "orderDate": "2024-01-15T17:00:00+01:00",
  "status": "PENDING",
  "totalAmount": 28.97,
  "deliveryAddress": "123 Pizza Street, Brussels, Belgium",
  "customer": {
    "id": 5,
    "name": "Jane Smith",
    "email": "jane.smith@example.com"
  },
  "orderLines": [
    {
      "id": 84,
      "pizza": {
        "id": 1,
        "name": "Margherita",
        "imageUrl": "https://storage.pizzastore.com/pizzas/margherita.jpg"
      },
      "quantity": 2,
      "unitPrice": 8.99,
      "subtotal": 17.98
    },
    {
      "id": 85,
      "pizza": {
        "id": 5,
        "name": "Pepperoni",
        "imageUrl": "https://storage.pizzastore.com/pizzas/pepperoni.jpg"
      },
      "quantity": 1,
      "unitPrice": 10.99,
      "subtotal": 10.99
    }
  ]
}
```

**Headers**:
- `Location: /api/orders/42`

**Validation Rules**:
- `orderLines`: Required, min 1 item
- `orderLines[].pizzaId`: Required, must exist
- `orderLines[].quantity`: Required, min 1
- Pizza must be available
- Total amount calculated automatically

**Errors**:
- 400: Validation error (empty order, invalid pizza, unavailable pizza)
- 401: Not authenticated

---

#### 2. Get My Orders

**Endpoint**: `GET /orders/my-orders`  
**Access**: Customer (own orders) or Admin  
**Description**: Get all orders for the authenticated customer

**Query Parameters**:
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)
- `sort`: Sort criteria (default: `orderDate,desc`)
- `status`: Filter by status

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": 42,
      "orderNumber": "ORD-20240115-00042",
      "orderDate": "2024-01-15T17:00:00+01:00",
      "status": "DELIVERED",
      "totalAmount": 28.97,
      "itemCount": 3
    },
    {
      "id": 38,
      "orderNumber": "ORD-20240110-00038",
      "orderDate": "2024-01-10T19:30:00+01:00",
      "status": "DELIVERED",
      "totalAmount": 22.98,
      "itemCount": 2
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalPages": 2,
  "totalElements": 12
}
```

**Errors**:
- 401: Not authenticated

---

#### 3. Get Order by ID

**Endpoint**: `GET /orders/{id}`  
**Access**: Customer (own order) or Admin  
**Description**: Get full details of a specific order

**Path Parameters**:
- `id`: Order ID (Long)

**Response** (200 OK):
```json
{
  "id": 42,
  "orderNumber": "ORD-20240115-00042",
  "orderDate": "2024-01-15T17:00:00+01:00",
  "status": "DELIVERED",
  "totalAmount": 28.97,
  "deliveryAddress": "123 Pizza Street, Brussels, Belgium",
  "customer": {
    "id": 5,
    "name": "Jane Smith",
    "email": "jane.smith@example.com",
    "phone": "+32 123 45 67 89"
  },
  "orderLines": [
    {
      "id": 84,
      "pizza": {
        "id": 1,
        "name": "Margherita",
        "price": 8.99,
        "imageUrl": "https://storage.pizzastore.com/pizzas/margherita.jpg"
      },
      "quantity": 2,
      "unitPrice": 8.99,
      "subtotal": 17.98
    },
    {
      "id": 85,
      "pizza": {
        "id": 5,
        "name": "Pepperoni",
        "price": 10.99,
        "imageUrl": "https://storage.pizzastore.com/pizzas/pepperoni.jpg"
      },
      "quantity": 1,
      "unitPrice": 10.99,
      "subtotal": 10.99
    }
  ]
}
```

**Errors**:
- 401: Not authenticated
- 403: Not authorized (not own order and not admin)
- 404: Order not found

---

#### 4. Get All Orders (Admin)

**Endpoint**: `GET /orders`  
**Access**: Admin only  
**Description**: Get all orders in the system

**Query Parameters**:
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)
- `sort`: Sort criteria (default: `orderDate,desc`)
- `status`: Filter by status
- `customerId`: Filter by customer ID
- `fromDate`: Filter orders from date (ISO 8601)
- `toDate`: Filter orders to date (ISO 8601)
- `orderNumber`: Search by order number

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": 42,
      "orderNumber": "ORD-20240115-00042",
      "orderDate": "2024-01-15T17:00:00+01:00",
      "status": "DELIVERED",
      "totalAmount": 28.97,
      "customer": {
        "id": 5,
        "name": "Jane Smith",
        "email": "jane.smith@example.com"
      },
      "itemCount": 3
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalPages": 45,
  "totalElements": 892
}
```

**Errors**:
- 401: Not authenticated
- 403: Not authorized

---

#### 5. Update Order Status

**Endpoint**: `PATCH /orders/{id}/status`  
**Access**: Admin only  
**Description**: Update the status of an order

**Path Parameters**:
- `id`: Order ID (Long)

**Request**:
```json
{
  "status": "PREPARING"
}
```

**Response** (200 OK):
```json
{
  "id": 42,
  "orderNumber": "ORD-20240115-00042",
  "status": "PREPARING"
}
```

**Valid Status Transitions**:
- PENDING → CONFIRMED, CANCELLED
- CONFIRMED → PREPARING, CANCELLED
- PREPARING → READY, CANCELLED
- READY → DELIVERED, CANCELLED
- DELIVERED → (no transitions)
- CANCELLED → (no transitions)

**Errors**:
- 400: Invalid status transition
- 401: Not authenticated
- 403: Not authorized
- 404: Order not found

---

#### 6. Cancel Order

**Endpoint**: `DELETE /orders/{id}`  
**Access**: Customer (own order, within 5 min) or Admin  
**Description**: Cancel an order

**Path Parameters**:
- `id`: Order ID (Long)

**Response** (204 No Content)

**Business Rules**:
- Customers can only cancel their own orders
- Orders can only be cancelled if status is PENDING or CONFIRMED
- Customers can only cancel within 5 minutes of order creation
- Admins can cancel at any time (before DELIVERED)

**Errors**:
- 401: Not authenticated
- 403: Not authorized or cancellation period expired
- 404: Order not found
- 409: Order cannot be cancelled (status is READY, DELIVERED, or already CANCELLED)

---

### Favorites

#### 1. Add Pizza to Favorites

**Endpoint**: `POST /customers/me/favorites/{pizzaId}`  
**Access**: Customer or Admin  
**Description**: Add a pizza to the authenticated customer's favorites

**Path Parameters**:
- `pizzaId`: Pizza ID (Long)

**Response** (200 OK):
```json
{
  "message": "Pizza added to favorites",
  "pizza": {
    "id": 1,
    "name": "Margherita",
    "price": 8.99,
    "imageUrl": "https://storage.pizzastore.com/pizzas/margherita.jpg"
  }
}
```

**Errors**:
- 401: Not authenticated
- 404: Pizza not found
- 409: Pizza already in favorites

---

#### 2. Remove Pizza from Favorites

**Endpoint**: `DELETE /customers/me/favorites/{pizzaId}`  
**Access**: Customer or Admin  
**Description**: Remove a pizza from the authenticated customer's favorites

**Path Parameters**:
- `pizzaId`: Pizza ID (Long)

**Response** (204 No Content)

**Errors**:
- 401: Not authenticated
- 404: Pizza not found or not in favorites

---

#### 3. Get My Favorite Pizzas

**Endpoint**: `GET /customers/me/favorites`  
**Access**: Customer or Admin  
**Description**: Get all favorite pizzas for the authenticated customer

**Response** (200 OK):
```json
{
  "favorites": [
    {
      "id": 1,
      "name": "Margherita",
      "price": 8.99,
      "description": "Classic tomato and mozzarella",
      "imageUrl": "https://storage.pizzastore.com/pizzas/margherita.jpg",
      "available": true
    },
    {
      "id": 5,
      "name": "Pepperoni",
      "price": 10.99,
      "description": "Pepperoni and cheese",
      "imageUrl": "https://storage.pizzastore.com/pizzas/pepperoni.jpg",
      "available": true
    }
  ],
  "count": 2
}
```

**Errors**:
- 401: Not authenticated

---

## Data Models

### Request DTOs

#### CreatePizzaRequest
```json
{
  "name": "string (required, 1-100 chars)",
  "price": "decimal (required, > 0, max 2 decimals)",
  "description": "string (optional, max 1000 chars)",
  "available": "boolean (optional, default true)",
  "nutritionalInfo": {
    "calories": "integer (optional)",
    "protein": "decimal (optional)",
    "carbohydrates": "decimal (optional)",
    "fat": "decimal (optional)"
  }
}
```

#### UpdatePizzaRequest
```json
{
  "name": "string (optional, 1-100 chars)",
  "price": "decimal (optional, > 0, max 2 decimals)",
  "description": "string (optional, max 1000 chars)",
  "available": "boolean (optional)",
  "nutritionalInfo": {
    "calories": "integer (optional)",
    "protein": "decimal (optional)",
    "carbohydrates": "decimal (optional)",
    "fat": "decimal (optional)"
  }
}
```

#### CreateCustomerRequest (JWT only)
```json
{
  "name": "string (required, 2-100 chars)",
  "email": "string (required, valid email)",
  "password": "string (required for JWT, min 8 chars, 1 upper, 1 lower, 1 number)",
  "phone": "string (optional, max 20 chars)",
  "address": "string (optional, max 200 chars)"
}
```

**Note**: Password field is only required when using JWT authentication. With OIDC, registration happens automatically on first login.

#### UpdateCustomerRequest
```json
{
  "name": "string (optional, 2-100 chars)",
  "phone": "string (optional, max 20 chars)",
  "address": "string (optional, max 200 chars)"
}
```

#### CreateOrderRequest
```json
{
  "orderLines": [
    {
      "pizzaId": "long (required)",
      "quantity": "integer (required, min 1)"
    }
  ],
  "deliveryAddress": "string (optional, max 200 chars)"
}
```

#### UpdateOrderStatusRequest
```json
{
  "status": "string (required, one of: PENDING, CONFIRMED, PREPARING, READY, DELIVERED, CANCELLED)"
}
```

#### LoginRequest (JWT only)
```json
{
  "email": "string (required)",
  "password": "string (required)"
}
```

**Note**: Only used with JWT authentication.

---

### Response DTOs

#### PizzaResponse
```json
{
  "id": "long",
  "name": "string",
  "price": "decimal",
  "description": "string",
  "imageUrl": "string (nullable)",
  "available": "boolean",
  "nutritionalInfo": {
    "calories": "integer",
    "protein": "decimal",
    "carbohydrates": "decimal",
    "fat": "decimal"
  }
}
```

#### CustomerResponse
```json
{
  "id": "long",
  "name": "string",
  "email": "string",
  "phone": "string",
  "address": "string",
  "role": "string",
  "orderCount": "integer (optional)",
  "totalSpent": "decimal (optional)"
}
```

#### OrderResponse
```json
{
  "id": "long",
  "orderNumber": "string",
  "orderDate": "datetime",
  "status": "string",
  "totalAmount": "decimal",
  "deliveryAddress": "string",
  "customer": {
    "id": "long",
    "name": "string",
    "email": "string",
    "phone": "string"
  },
  "orderLines": [
    {
      "id": "long",
      "pizza": {
        "id": "long",
        "name": "string",
        "price": "decimal",
        "imageUrl": "string"
      },
      "quantity": "integer",
      "unitPrice": "decimal",
      "subtotal": "decimal"
    }
  ]
}
```

#### OrderSummaryResponse (for lists)
```json
{
  "id": "long",
  "orderNumber": "string",
  "orderDate": "datetime",
  "status": "string",
  "totalAmount": "decimal",
  "itemCount": "integer",
  "customer": {
    "id": "long",
    "name": "string",
    "email": "string"
  }
}
```

#### ErrorResponse
```json
{
  "timestamp": "datetime",
  "status": "integer",
  "error": "string",
  "message": "string",
  "path": "string",
  "errors": [
    {
      "field": "string",
      "rejectedValue": "any",
      "message": "string"
    }
  ]
}
```

---

## Implementation Notes

### Auditing

Use Spring Data JPA auditing with:
- `@CreatedDate` for createdAt
- `@CreatedBy` for createdBy  
- `@LastModifiedDate` for updatedAt
- `@LastModifiedBy` for updatedBy

Implement `AuditorAware<String>` to automatically populate createdBy/updatedBy from security context.

### Image Storage

Options:
1. **Local file system** (development): Store in `uploads/pizzas/` directory
2. **Cloud storage** (production): AWS S3, Google Cloud Storage, or Azure Blob Storage
3. Store only the URL/path in database, not the actual file

Image naming convention: `{pizzaId}-{timestamp}.{extension}`

### Security Implementation

#### JWT Authentication (Lessons 12-13)

**Dependencies:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

**Configuration:**
```yaml
jwt:
  secret: ${JWT_SECRET:your-secret-key-must-be-at-least-256-bits-long}
  expiration: 3600000 # 1 hour
  refresh-expiration: 604800000 # 7 days
```

**Implementation highlights:**
- Password hashing with BCryptPasswordEncoder (strength 12)
- Token expiration: 1 hour (access token), 7 days (refresh token)
- Token contains: customer ID, email, role
- Custom `JwtAuthenticationFilter` to validate tokens
- Store tokens client-side (localStorage or secure HTTP-only cookies)

**Customer entity (JWT mode):**
```java
@Column(nullable = false)
private String password; // BCrypt hashed

// sub field is null or not present
```

---

#### OAuth2/OIDC with Dex (Lesson 14 - Optional)

**Dependencies:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

**Configuration:**
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:5556/dex
          # Spring Boot auto-discovers OIDC configuration
```

**Dex Setup (Docker Compose):**
```yaml
version: '3'
services:
  dex:
    image: ghcr.io/dexidp/dex:latest
    ports:
      - "5556:5556"
    volumes:
      - ./dex-config.yaml:/etc/dex/config.yaml
```

**Implementation highlights:**
- No password storage in your database
- Token validation via OIDC discovery endpoint
- Auto-registration on first login
- Support for multiple identity providers
- Role assignment can still be managed locally

**Customer entity (OIDC mode):**
```java
@Column(unique = true)
private String sub; // OIDC subject (unique ID from IdP)

// password field is null
```

**First login flow:**
```java
// Extract claims from OIDC token
String sub = jwt.getClaim("sub");
String email = jwt.getClaim("email");
String name = jwt.getClaim("name");

// Find or create customer
Customer customer = customerRepository.findBySub(sub)
    .orElseGet(() -> {
        Customer newCustomer = new Customer();
        newCustomer.setSub(sub);
        newCustomer.setEmail(email);
        newCustomer.setName(name);
        newCustomer.setRole(Role.CUSTOMER);
        return customerRepository.save(newCustomer);
    });
```

### Database Considerations

- Use database indexes on frequently queried fields (email, orderNumber, orderDate)
- Consider archiving old orders to separate table
- Use database transactions for order creation (atomic operation)
- Implement soft delete for pizzas that are referenced in orders

### Testing Strategy

- Unit tests: Service layer business logic
- Integration tests: Repository layer
- API tests: Controller endpoints with MockMvc
- Security tests: Authorization rules
- E2E tests: Full customer flows

### Performance Optimization

- Use pagination for all list endpoints
- Implement caching for pizza list (rarely changes)
- Use lazy loading for relationships
- Add database indexes
- Consider CDN for pizza images

---

## OpenAPI/Swagger Documentation

The API will be fully documented using Swagger/OpenAPI 3.0:

**Access**: `http://localhost:8080/swagger-ui.html`

**Features**:
- Try out endpoints directly
- Authentication support (JWT/OAuth2)
- Request/response examples
- Schema definitions
- Error codes documentation

---

## Actuator Endpoints

**Access**: `http://localhost:8080/actuator`

**Available Endpoints**:
- `/actuator/health` - Application health status
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics
- `/actuator/env` - Environment properties
- `/actuator/loggers` - Logging configuration

**Security**: Actuator endpoints should be secured and only accessible to admins.

---

## Summary

This specification defines a complete REST API for the PizzaStore application with:

✅ **Clear security model** with roles and permissions  
✅ **Complete CRUD operations** for all entities  
✅ **Proper REST principles** with correct HTTP methods and status codes  
✅ **Image upload and storage** for pizzas  
✅ **Audit fields** (createdAt, createdBy, updatedAt, updatedBy)  
✅ **Pagination, filtering, and sorting**  
✅ **Validation and error handling**  
✅ **Business rules** (order cancellation, status transitions)  
✅ **Ready for implementation** with Spring Boot 3.5.7

The API is designed to be consumed by mobile applications while providing a secure, scalable, and maintainable backend service.
