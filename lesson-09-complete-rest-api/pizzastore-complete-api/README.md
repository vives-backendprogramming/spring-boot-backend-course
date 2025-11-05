# PizzaStore Complete REST API

This is the **complete, production-ready REST API** for PizzaStore, combining all concepts from Lessons 1-7:

- ✅ **Lesson 4**: Spring Boot with JPA & H2
- ✅ **Lesson 5**: REST principles
- ✅ **Lesson 6**: DTOs & MapStruct mappers  
- ✅ **Lesson 7**: JPA relationships (all types)
- ✅ **Lesson 8**: Complete CRUD operations

## 🏗️ Project Structure

```
src/main/java/be/vives/pizzastore/
├── domain/                      # JPA Entities (Lesson 7)
│   ├── Pizza.java
│   ├── NutritionalInfo.java     # @OneToOne with Pizza
│   ├── Customer.java
│   ├── Order.java               # @ManyToOne to Customer
│   ├── OrderLine.java           # Join entity
│   └── OrderStatus.java         # Enum
├── dto/                         # Data Transfer Objects (Lesson 6)
│   ├── request/
│   │   ├── CreatePizzaRequest.java
│   │   ├── UpdatePizzaRequest.java
│   │   ├── CreateCustomerRequest.java
│   │   ├── CreateOrderRequest.java
│   │   └── UpdateOrderStatusRequest.java
│   └── response/
│       ├── PizzaResponse.java
│       ├── CustomerResponse.java
│       ├── OrderResponse.java
│       └── OrderLineResponse.java
├── mapper/                      # MapStruct Mappers (Lesson 6)
│   ├── PizzaMapper.java
│   ├── CustomerMapper.java
│   └── OrderMapper.java
├── repository/                  # Spring Data JPA (Lesson 7)
│   ├── PizzaRepository.java
│   ├── CustomerRepository.java
│   └── OrderRepository.java
├── service/                     # Business Logic
│   ├── PizzaService.java
│   ├── CustomerService.java
│   └── OrderService.java
├── controller/                  # REST Controllers (Lesson 8)
│   ├── PizzaController.java
│   ├── CustomerController.java
│   └── OrderController.java
└── PizzaStoreApplication.java   # Main class

src/main/resources/
├── application.properties       # Configuration (Lesson 2)
└── data.sql                     # Sample data
```

## 📋 Features Demonstrated

### From Lesson 4: Spring Boot & JPA
- ✅ Spring Boot auto-configuration
- ✅ Spring Data JPA
- ✅ H2 in-memory database
- ✅ Entity relationships

### From Lesson 5: REST Principles
- ✅ Resource-based URLs (`/api/pizzas`, `/api/customers`, `/api/orders`)
- ✅ HTTP methods (GET, POST, PUT, PATCH, DELETE)
- ✅ Proper HTTP status codes (200, 201, 204, 404)
- ✅ Location headers for created resources

### From Lesson 6: DTOs & Mappers
- ✅ Request DTOs (CreatePizzaRequest, etc.)
- ✅ Response DTOs (PizzaResponse, etc.)
- ✅ MapStruct mappers
- ✅ Never exposing entities

### From Lesson 7: JPA Relationships
- ✅ @OneToOne: Pizza ↔ NutritionalInfo
- ✅ @OneToMany / @ManyToOne: Customer → Orders, Order → OrderLines
- ✅ @ManyToMany: Customer ↔ Pizza (favorites)
- ✅ JOIN FETCH queries
- ✅ Pagination support

### From Lesson 8: Complete CRUD Operations
- ✅ Full CRUD for all resources
- ✅ Query parameters and filtering
- ✅ Pagination and sorting
- ✅ Proper error handling (404 Not Found)

## 🚀 How to Run

### Prerequisites
- Java 25 or higher
- Maven 3.6 or higher

### Run the Application

```bash
# Navigate to project directory
cd pizzastore-complete-api

# Clean, compile, and run
mvn clean spring-boot:run
```

The application will start on `http://localhost:8080`

### Verify Startup

Check console output for:
```
Starting PizzaStore Complete REST API...
PizzaStore Complete REST API started successfully!
```

### Access H2 Console

Open your browser: `http://localhost:8080/h2-console`

**Connection settings:**
- JDBC URL: `jdbc:h2:mem:pizzastore`
- Username: `sa`
- Password: (leave empty)

## 📡 API Endpoints

### Pizza API

| Method | Endpoint | Description | Success Status |
|--------|----------|-------------|----------------|
| GET | `/api/pizzas` | List all pizzas (paginated) | 200 OK |
| GET | `/api/pizzas/{id}` | Get single pizza | 200 OK |
| GET | `/api/pizzas?maxPrice=10` | Filter by price | 200 OK |
| POST | `/api/pizzas` | Create pizza | 201 Created |
| PUT | `/api/pizzas/{id}` | Update pizza (full) | 200 OK |
| PATCH | `/api/pizzas/{id}` | Update pizza (partial) | 200 OK |
| DELETE | `/api/pizzas/{id}` | Delete pizza | 204 No Content |

### Customer API

| Method | Endpoint | Description | Success Status |
|--------|----------|-------------|----------------|
| GET | `/api/customers` | List all customers | 200 OK |
| GET | `/api/customers/{id}` | Get single customer | 200 OK |
| GET | `/api/customers/{id}/orders` | Get customer orders | 200 OK |
| GET | `/api/customers/{id}/favorites` | Get favorite pizzas | 200 OK |
| POST | `/api/customers` | Create customer | 201 Created |
| PUT | `/api/customers/{id}` | Update customer | 200 OK |
| DELETE | `/api/customers/{id}` | Delete customer | 204 No Content |
| POST | `/api/customers/{id}/favorites/{pizzaId}` | Add favorite | 200 OK |
| DELETE | `/api/customers/{id}/favorites/{pizzaId}` | Remove favorite | 204 No Content |

### Order API

| Method | Endpoint | Description | Success Status |
|--------|----------|-------------|----------------|
| GET | `/api/orders` | List all orders | 200 OK |
| GET | `/api/orders?customerId=1` | Filter by customer | 200 OK |
| GET | `/api/orders?status=PENDING` | Filter by status | 200 OK |
| GET | `/api/orders/{id}` | Get order with details | 200 OK |
| POST | `/api/orders` | Create order | 201 Created |
| PATCH | `/api/orders/{id}/status` | Update status | 200 OK |
| DELETE | `/api/orders/{id}` | Cancel order | 204 No Content |

## 🧪 Test the API

### Create a Pizza

```bash
curl -X POST http://localhost:8080/api/pizzas \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Capricciosa",
    "price": 11.00,
    "description": "Tomato, mozzarella, ham, mushrooms, artichokes, and olives"
  }'
```

**Response** (201 Created):
```json
{
  "id": 9,
  "name": "Capricciosa",
  "price": 11.00,
  "description": "Tomato, mozzarella, ham, mushrooms, artichokes, and olives",
  "createdAt": "2024-11-05T10:30:00",
  "updatedAt": "2024-11-05T10:30:00"
}
```

### Get All Pizzas (Paginated)

```bash
curl "http://localhost:8080/api/pizzas?page=0&size=5&sortBy=price&direction=asc"
```

### Get Pizza with Nutritional Info

```bash
curl http://localhost:8080/api/pizzas/1
```

### Create an Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "orderLines": [
      { "pizzaId": 1, "quantity": 2 },
      { "pizzaId": 2, "quantity": 1 }
    ]
  }'
```

### Get Customer's Orders

```bash
curl http://localhost:8080/api/customers/1/orders
```

### Update Order Status

```bash
curl -X PATCH http://localhost:8080/api/orders/1/status \
  -H "Content-Type: application/json" \
  -d '{ "status": "DELIVERED" }'
```

### Add Pizza to Favorites

```bash
curl -X POST http://localhost:8080/api/customers/1/favorites/3
```

### Filter Pizzas by Price

```bash
# Pizzas under €10
curl "http://localhost:8080/api/pizzas?maxPrice=10.00"

# Pizzas between €8 and €12
curl "http://localhost:8080/api/pizzas?minPrice=8.00&maxPrice=12.00"
```

## 🗄️ Database Schema

The application creates the following tables:

```
pizzas                    nutritional_info
customers                 orders
order_lines               customer_favorite_pizzas (join table)
```

### Entity Relationships

```
Pizza (1) ←→ (1) NutritionalInfo     @OneToOne
Customer (1) ←→ (*) Order             @OneToMany / @ManyToOne
Order (1) ←→ (*) OrderLine            @OneToMany / @ManyToOne
Customer (*) ←→ (*) Pizza             @ManyToMany (favorites)
```

### Sample Data

On startup, the application loads:
- 8 Pizzas (Margherita, Pepperoni, Hawaiian, etc.)
- 8 NutritionalInfo records
- 5 Customers
- 6 Orders with 10 OrderLines
- 14 Customer-Pizza favorites

## 🔍 Explore with H2 Console

### View All Pizzas with Nutritional Info

```sql
SELECT p.name, p.price, n.calories, n.protein
FROM pizzas p
LEFT JOIN nutritional_info n ON n.pizza_id = p.id;
```

### View Customer Orders

```sql
SELECT c.name as customer,
       o.order_number,
       o.order_date,
       o.total_amount,
       o.status
FROM customers c
JOIN orders o ON o.customer_id = c.id
ORDER BY o.order_date DESC;
```

### View Customer Favorite Pizzas

```sql
SELECT c.name as customer, p.name as favorite_pizza
FROM customers c
JOIN customer_favorite_pizzas cfp ON cfp.customer_id = c.id
JOIN pizzas p ON p.id = cfp.pizza_id
ORDER BY c.name;
```

### View Order Details

```sql
SELECT o.order_number,
       p.name as pizza,
       ol.quantity,
       ol.unit_price,
       ol.subtotal
FROM orders o
JOIN order_lines ol ON ol.order_id = o.id
JOIN pizzas p ON p.id = ol.pizza_id
WHERE o.id = 1;
```

## 📊 Logging

The application uses SLF4J with Logback for logging.

**Log Levels** (configured in application.properties):
- Root: INFO
- be.vives.pizzastore: DEBUG
- org.hibernate.SQL: DEBUG (shows SQL queries)

**Console Output**:
```
2024-11-05 10:30:00 - Starting PizzaStore Complete REST API...
2024-11-05 10:30:01 - Initialized JPA EntityManagerFactory
2024-11-05 10:30:02 - Started PizzaStoreApplication in 3.456 seconds
```

## 🎯 Learning Points

This project demonstrates:

1. **Spring Boot** application structure
2. **REST API** best practices
3. **JPA** entity relationships
4. **DTOs** for proper API design
5. **MapStruct** for entity ↔ DTO mapping
6. **Pagination** and **sorting**
7. **Query parameters** for filtering
8. **Proper HTTP methods** and **status codes**
9. **Configuration** with application.properties
10. **Logging** configuration

## 🔄 What's Different from Lesson 6 & 7?

### Enhanced from Lesson 6:
- ✅ Full CRUD operations (not just Pizza)
- ✅ Pagination support
- ✅ Query parameters
- ✅ Multiple resources (Pizza, Customer, Order)

### Enhanced from Lesson 7:
- ✅ Complete REST API (not just entities)
- ✅ DTOs for all responses
- ✅ Pagination in repositories
- ✅ Real-world endpoints

### New in Lesson 8:
- ✅ Complete API for all resources
- ✅ Filtering and search
- ✅ Status updates (Order status)
- ✅ Relationship management (favorites)
- ✅ Production-ready structure

## 📖 Related Lessons

This project is the culmination of:
- [Lesson 4: Spring Boot Introduction](../lesson-04-spring-boot-intro/)
- [Lesson 5: REST Principles](../lesson-05-rest-principles/)
- [Lesson 6: DTOs & Mappers](../lesson-06-dtos-mappers/)
- [Lesson 7: Working with JPA](../lesson-07-working-with-jpa/)
- [Lesson 8: Building a Complete REST API](../README.md)

## 🔜 Next Steps

In **Lesson 9: Testing**, you will learn to:
- Write unit tests for services
- Test controllers with MockMvc
- Test repositories with @DataJpaTest
- Integration testing

---

**This is a complete, production-ready REST API demonstrating all core Spring Boot concepts!** 🎉
