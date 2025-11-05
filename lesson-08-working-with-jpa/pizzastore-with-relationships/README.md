# PizzaStore with JPA Relationships

This project demonstrates **all JPA relationship types** in a complete PizzaStore application.

## 📋 What's Demonstrated

### JPA Relationships
- ✅ **@OneToOne**: Pizza ↔ NutritionalInfo
- ✅ **@ManyToOne** / **@OneToMany**: Customer → Orders, Order → OrderLines
- ✅ **@ManyToMany**: Customer ↔ Pizza (favorites)

### Additional Concepts
- ✅ **Fetch Types**: LAZY vs EAGER loading
- ✅ **Cascade Types**: ALL, PERSIST, REMOVE
- ✅ **orphanRemoval**: Automatic deletion of orphaned entities
- ✅ **Bidirectional relationships** with helper methods
- ✅ **Custom queries** with @Query and JOIN FETCH
- ✅ **Derived query methods** (findBy...)
- ✅ **Enum mapping** with @Enumerated(EnumType.STRING)

## 🏗️ Database Schema

```
pizzas                    nutritional_info (@OneToOne)
------                    ----------------
id (PK)              ┌──→ id (PK)
name                 │    pizza_id (FK) → pizzas.id
price                │    calories
description          │    protein
                     └─── carbohydrates
                          fat

customers                 orders (@ManyToOne)          order_lines
---------                 ------                       -----------
id (PK)              ┌──→ id (PK)                 ┌─→ id (PK)
name                 │    customer_id (FK)         │   order_id (FK) → orders.id
email                │    order_number             │   pizza_id (FK) → pizzas.id
phone                │    order_date               │   quantity
address              │    total_amount             │   unit_price
                     │    status                   │   subtotal
orders (OneToMany) ──┘                             │
                          order_lines (OneToMany) ─┘

customer_favorite_pizzas (@ManyToMany join table)
------------------------
customer_id (FK) → customers.id
pizza_id (FK) → pizzas.id
PRIMARY KEY (customer_id, pizza_id)
```

## 📦 Entity Relationships Explained

### 1. @OneToOne: Pizza ↔ NutritionalInfo
```java
// Pizza.java
@OneToOne(mappedBy = "pizza", cascade = CascadeType.ALL, orphanRemoval = true)
private NutritionalInfo nutritionalInfo;

// NutritionalInfo.java
@OneToOne
@JoinColumn(name = "pizza_id", nullable = false)
private Pizza pizza;
```
- Each Pizza has **exactly one** NutritionalInfo
- Foreign key is in `nutritional_info` table
- `orphanRemoval = true`: Delete nutritional info if pizza is deleted

### 2. @ManyToOne / @OneToMany: Customer → Orders
```java
// Customer.java
@OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Order> orders = new ArrayList<>();

// Order.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "customer_id", nullable = false)
private Customer customer;
```
- Each Customer can have **many** Orders
- Each Order belongs to **one** Customer
- Foreign key is in `orders` table

### 3. @ManyToMany: Customer ↔ Pizza (Favorites)
```java
// Customer.java
@ManyToMany
@JoinTable(
    name = "customer_favorite_pizzas",
    joinColumns = @JoinColumn(name = "customer_id"),
    inverseJoinColumns = @JoinColumn(name = "pizza_id")
)
private Set<Pizza> favoritePizzas = new HashSet<>();

// Pizza.java
@ManyToMany(mappedBy = "favoritePizzas")
private Set<Customer> favoritedByCustomers = new HashSet<>();
```
- Customers can favorite **many** Pizzas
- Pizzas can be favorited by **many** Customers
- Requires join table: `customer_favorite_pizzas`

## 🚀 How to Run

### Prerequisites
- Java 25 or higher
- Maven 3.6 or higher

### Run the Application

```bash
# Navigate to project directory
cd pizzastore-with-relationships

# Run with Maven
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Access H2 Console

Open your browser and navigate to: `http://localhost:8080/h2-console`

**Connection settings:**
- JDBC URL: `jdbc:h2:mem:pizzastore`
- Username: `sa`
- Password: (leave empty)

## 🧪 Explore the Data

The application automatically loads sample data on startup:
- 5 Pizzas (Margherita, Pepperoni, Hawaiian, Quattro Formaggi, Vegetariana)
- 5 NutritionalInfo records (one per pizza)
- 3 Customers (John, Jane, Bob)
- 3 Orders with 5 OrderLines
- 6 Customer-Pizza favorites

### Sample Queries to Try

```sql
-- View all pizzas with nutritional info
SELECT p.name, n.calories, n.protein 
FROM pizzas p 
LEFT JOIN nutritional_info n ON n.pizza_id = p.id;

-- View customer orders
SELECT c.name, o.order_number, o.total_amount, o.status
FROM customers c
JOIN orders o ON o.customer_id = c.id;

-- View customer favorite pizzas (ManyToMany)
SELECT c.name as customer, p.name as favorite_pizza
FROM customers c
JOIN customer_favorite_pizzas cfp ON cfp.customer_id = c.id
JOIN pizzas p ON p.id = cfp.pizza_id;

-- View order details with pizzas
SELECT o.order_number, p.name, ol.quantity, ol.subtotal
FROM orders o
JOIN order_lines ol ON ol.order_id = o.id
JOIN pizzas p ON p.id = ol.pizza_id;
```

## 🎯 Key Concepts to Understand

### Fetch Types
```java
@ManyToOne(fetch = FetchType.LAZY)  // Load when accessed
private Customer customer;

@ManyToOne(fetch = FetchType.EAGER) // Load immediately
private Pizza pizza;
```

### Cascade Types
```java
@OneToMany(cascade = CascadeType.ALL)  // All operations cascade
private List<Order> orders;

@OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
private List<OrderLine> orderLines;  // Only save/update cascades
```

### Bidirectional Helper Methods
Always maintain both sides of the relationship:
```java
public void addOrder(Order order) {
    orders.add(order);
    order.setCustomer(this);  // Maintain both sides!
}
```

### Avoiding N+1 Problem
Use JOIN FETCH in custom queries:
```java
@Query("SELECT o FROM Order o JOIN FETCH o.orderLines ol JOIN FETCH ol.pizza WHERE o.id = :id")
Optional<Order> findByIdWithOrderLines(@Param("id") Long id);
```

## 📚 Repository Features

### Derived Query Methods
```java
// In PizzaRepository
List<Pizza> findByPriceLessThan(BigDecimal maxPrice);
List<Pizza> findByNameContainingIgnoreCase(String keyword);
List<Pizza> findByPriceBetween(BigDecimal min, BigDecimal max);

// In OrderRepository
List<Order> findByCustomerId(Long customerId);
List<Order> findByStatus(OrderStatus status);
```

### Custom JPQL Queries
```java
@Query("SELECT p FROM Pizza p WHERE p.name LIKE %:keyword% OR p.description LIKE %:keyword%")
List<Pizza> searchByKeyword(@Param("keyword") String keyword);
```

### JOIN FETCH Queries
```java
@Query("SELECT c FROM Customer c JOIN FETCH c.orders WHERE c.id = :id")
Optional<Customer> findByIdWithOrders(@Param("id") Long id);
```

## 🔍 Testing Relationships

Run the application and check the console output. You'll see:
- Schema creation with all tables and foreign keys
- Sample data insertion
- Relationship constraints enforced

Try these scenarios:
1. Delete a Pizza → NutritionalInfo is automatically deleted (orphanRemoval)
2. Delete a Customer → All their Orders are deleted (cascade)
3. Remove a Pizza from favorites → Only join table entry is deleted (not the Pizza)

## 📖 Related Lesson

This project accompanies **Lesson 7: Working with JPA** from the Backend Programming Course.

See `../README.md` for the complete lesson with detailed explanations.

## 🎓 Learning Points

After studying this project, you should understand:
- ✅ When to use each relationship type
- ✅ How to configure fetch types
- ✅ How cascade operations work
- ✅ How to maintain bidirectional relationships
- ✅ How to avoid N+1 queries
- ✅ How to write custom queries with JOIN FETCH
- ✅ How to structure a database schema with proper relationships
