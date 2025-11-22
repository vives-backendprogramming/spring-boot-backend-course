# Lesson 10: Validation & Exception Handling

**Building Robust APIs with Input Validation and Consistent Error Responses**

---

## 📋 Learning Objectives

By the end of this lesson, you will be able to:
- Validate incoming data using Bean Validation (JSR-380)
- Use built-in validation annotations (@NotNull, @NotBlank, @Size, @Email, etc.)
- Create custom validators for complex validation rules
- Handle exceptions globally with @ControllerAdvice
- Return consistent error responses across your API
- Use appropriate HTTP status codes for different error types
- Validate path variables and request parameters
- Handle validation errors with detailed error messages

---

## 📚 Table of Contents

1. [Why Validation Matters](#why-validation-matters)
2. [Bean Validation (JSR-380)](#bean-validation-jsr-380)
3. [Common Validation Annotations](#common-validation-annotations)
4. [Validating Request Bodies](#validating-request-bodies)
5. [Custom Validators](#custom-validators)
6. [Exception Handling with @ControllerAdvice](#exception-handling-with-controlleradvice)
7. [HTTP Status Codes for Errors](#http-status-codes-for-errors)
8. [Business Rules and Custom Exceptions](#business-rules-and-custom-exceptions)
9. [Complete PizzaStore with Validation](#complete-pizzastore-with-validation)
10. [Best Practices](#best-practices)
11. [Summary](#summary)
12. [Runnable Project](#runnable-project)

---

## ⚠️ Why Validation Matters

### Security & Data Integrity

Without validation:
```java
@PostMapping
public ResponseEntity<PizzaResponse> createPizza(@RequestBody CreatePizzaRequest request) {
    // What if name is null?
    // What if price is negative?
    // What if email is invalid?
    return ResponseEntity.ok(pizzaService.create(request));
}
```

**Problems:**
- ❌ Null values in database
- ❌ Invalid data (negative prices, malformed emails)
- ❌ SQL injection risks
- ❌ Inconsistent error responses
- ❌ Crashes when processing invalid data

### With Validation

```java
@PostMapping
public ResponseEntity<PizzaResponse> createPizza(@Valid @RequestBody CreatePizzaRequest request) {
    // Spring automatically validates before this method executes
    // If validation fails, returns 400 Bad Request
    return ResponseEntity.ok(pizzaService.create(request));
}
```

**Benefits:**
- ✅ Guaranteed valid data
- ✅ Consistent error responses
- ✅ Early failure (at API boundary)
- ✅ Self-documenting API
- ✅ Better security

---

## 🎯 Bean Validation (JSR-380)

Bean Validation is a Java standard (JSR-380) implemented by Hibernate Validator.

### Add Dependency

Already included in `spring-boot-starter-web`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### How It Works

1. Add validation annotations to DTOs
2. Use `@Valid` or `@Validated` in controllers
3. Spring validates before method execution
4. If validation fails, throws `MethodArgumentNotValidException`
5. Return 400 Bad Request with error details

---

## 📝 Common Validation Annotations

### Null/Empty Checks

```java
public record CreatePizzaRequest(
    @NotNull(message = "Name cannot be null")
    @NotBlank(message = "Name cannot be blank")
    String name,
    
    @NotNull(message = "Price is required")
    BigDecimal price,
    
    @NotEmpty(message = "Description cannot be empty")
    String description
) {}
```

**Annotations:**
- `@NotNull` - Value must not be null
- `@NotBlank` - String must not be null, empty, or whitespace only
- `@NotEmpty` - Collection/String/Array must not be null or empty

### Size & Length

```java
public record CreateCustomerRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String name,
    
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    String phone,
    
    @Size(max = 200, message = "Address must not exceed 200 characters")
    String address
) {}
```

### Numeric Constraints

```java
public record CreatePizzaRequest(
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
    @DecimalMax(value = "100.00", message = "Price cannot exceed 100.00")
    @Digits(integer = 3, fraction = 2, message = "Price must have max 3 digits and 2 decimals")
    BigDecimal price
) {}
```

**Annotations:**
- `@Min` / `@Max` - For integers
- `@DecimalMin` / `@DecimalMax` - For decimals
- `@Positive` / `@PositiveOrZero` - Must be > 0 or >= 0
- `@Negative` / `@NegativeOrZero` - Must be < 0 or <= 0
- `@Digits` - Specify integer and fraction digits

### Pattern & Format

```java
public record CreateCustomerRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password
) {}
```

### Collections

```java
public record CreateOrderRequest(
    @NotNull(message = "Customer ID is required")
    Long customerId,
    
    @NotEmpty(message = "Order must contain at least one pizza")
    @Size(min = 1, max = 20, message = "Order can contain between 1 and 20 pizzas")
    List<OrderLineRequest> orderLines
) {
    public record OrderLineRequest(
            Long pizzaId,
            Integer quantity
    ) {
    }
}
```

**Note**: In this example, validation annotations are not applied. To add validation:
- Add `@NotNull(message = "Customer ID is required")` to `customerId`
- Add `@NotEmpty(message = "Order must contain at least one item")` to `orderLines`
- Add `@Valid` to `orderLines` to validate nested objects
- Add `@NotNull` and `@Min(1)` to quantity field

---

## ✅ Validating Request Bodies

### Step 1: Add Validation Annotations to DTO

```java
package be.vives.pizzastore.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreatePizzaRequest(

        @NotBlank(message = "Pizza name is required")
        String name,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be positive")
        BigDecimal price,

        String description,

        Boolean available,

        NutritionalInfoRequest nutritionalInfo
) {
}
```

**NutritionalInfoRequest** (nested DTO, no validation):
```java
public record NutritionalInfoRequest(
        Integer calories,
        BigDecimal protein,
        BigDecimal carbohydrates,
        BigDecimal fat
) {
}
```

### Step 2: Use in Controller (without @Valid)

```java
@RestController
@RequestMapping("/api/pizzas")
public class PizzaController {

    @PostMapping
    public ResponseEntity<PizzaResponse> createPizza(@RequestBody CreatePizzaRequest request) {
        log.debug("POST /api/pizzas - {}", request);

        PizzaResponse created = pizzaService.create(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }
}
```

**⚠️ Important Note:**
- This example shows validation annotations on the DTO
- To **activate** validation, add `@Valid` before `@RequestBody`: `@Valid @RequestBody CreatePizzaRequest request`
- Without `@Valid`, validation annotations are ignored
- With `@Valid`, validation happens **before** method execution
- If validation fails, method is **never called** and Spring returns 400 Bad Request

### What Happens on Validation Failure?

**Without Exception Handler:**
```json
{
  "timestamp": "2024-11-05T19:30:00",
  "status": 400,
  "error": "Bad Request",
  "path": "/api/pizzas"
}
```

**Not helpful!** We need detailed error messages.

---

## 🎨 Custom Validators

For complex validation logic, create custom validators.

### Example: Validate Pizza Name Format

**Step 1: Create Annotation**

```java
package be.vives.pizzastore.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidPizzaNameValidator.class)
@Documented
public @interface ValidPizzaName {
    
    String message() default "Pizza name must start with capital letter and contain only letters and spaces";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
```

**Step 2: Create Validator**

```java
package be.vives.pizzastore.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidPizzaNameValidator implements ConstraintValidator<ValidPizzaName, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // Use @NotBlank for null checks
        }
        
        // Must start with capital letter and contain only letters and spaces
        return value.matches("^[A-Z][a-zA-Z ]+$");
    }
}
```

**Step 3: Use in DTO**

```java
public record CreatePizzaRequest(
    @NotBlank(message = "Name is required")
    @ValidPizzaName
    String name,
    
    // ... other fields
) {}
```

### Another Example: Validate Order Quantity

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidQuantityValidator.class)
public @interface ValidQuantity {
    String message() default "Quantity must be between 1 and 50";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class ValidQuantityValidator implements ConstraintValidator<ValidQuantity, Integer> {
    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return value >= 1 && value <= 50;
    }
}
```

---

## 🛡️ Exception Handling with @ControllerAdvice

Create a global exception handler to return consistent error responses.

### Create Error Response DTO

```java
package be.vives.pizzastore.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<ValidationError> validationErrors
) {
    public record ValidationError(
            String field,
            String message
    ) {}
}
```

**Key Points:**
- `@JsonInclude(JsonInclude.Include.NON_NULL)` excludes null fields from JSON response
- `validationErrors` will be null for non-validation errors

### Create Global Exception Handler

```java
package be.vives.pizzastore.exception;

import be.vives.pizzastore.dto.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        log.warn("Validation failed for request: {}", request.getDescription(false));

        List<ErrorResponse.ValidationError> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ErrorResponse.ValidationError(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Validation failed",
                extractPath(request),
                validationErrors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            WebRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                extractPath(request),
                null
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex,
            WebRequest request) {

        log.warn("Duplicate resource: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                extractPath(request),
                null
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(errorResponse);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            WebRequest request) {

        log.warn("Business logic error: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Unprocessable Entity",
                ex.getMessage(),
                extractPath(request),
                null
        );

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            WebRequest request) {

        log.error("Unexpected error occurred", ex);

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred. Please contact support if the problem persists.",
                extractPath(request),
                null
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
```

**Key Points:**
- `@RestControllerAdvice` makes this a global exception handler
- Each `@ExceptionHandler` handles a specific exception type
- Returns appropriate HTTP status codes (400, 404, 409, 422, 500)
- Logs errors at appropriate levels (warn vs error)
- Helper method `extractPath()` cleans up the request path

### Create Custom Exception

```java
package be.vives.pizzastore.exception;

public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String resourceName, Long id) {
        super(String.format("%s with id %d not found", resourceName, id));
    }
}
```

### Use in Service

```java
@Service
@Transactional
public class PizzaService {
    
    public PizzaResponse findById(Long id) {
        return pizzaRepository.findById(id)
                .map(pizzaMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Pizza", id));
    }
    
    public PizzaResponse update(Long id, UpdatePizzaRequest request) {
        Pizza pizza = pizzaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pizza", id));
        
        pizzaMapper.updateEntity(request, pizza);
        Pizza updated = pizzaRepository.save(pizza);
        
        return pizzaMapper.toResponse(updated);
    }
}
```

### Exception Hierarchy

The project uses a custom exception hierarchy:

```java
PizzaStoreException (base)
├── ResourceNotFoundException (404)
├── DuplicateResourceException (409)
└── BusinessException (422)
```

All custom exceptions extend from `PizzaStoreException`:

```java
package be.vives.pizzastore.exception;

public class PizzaStoreException extends RuntimeException {

    public PizzaStoreException(String message) {
        super(message);
    }

    public PizzaStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

---

## 📊 HTTP Status Codes for Errors

Use appropriate status codes for different error types:

| Status Code | Name | Use Case | Example |
|-------------|------|----------|---------|
| **400** | Bad Request | Invalid input data | Validation failed |
| **401** | Unauthorized | Authentication required | No JWT token |
| **403** | Forbidden | No permission | User not admin |
| **404** | Not Found | Resource doesn't exist | Pizza with id 999 |
| **409** | Conflict | Duplicate resource | Email already exists |
| **422** | Unprocessable Entity | Business logic error | Cannot cancel delivered order |
| **500** | Internal Server Error | Unexpected error | Database connection failed |

### Example Error Responses

**400 Bad Request (Validation)**
```json
{
  "timestamp": "2024-11-05T19:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/pizzas",
  "validationErrors": [
    {
      "field": "name",
      "message": "Name is required"
    },
    {
      "field": "price",
      "message": "Price must be at least €0.01"
    }
  ]
}
```

**404 Not Found**
```json
{
  "timestamp": "2024-11-05T19:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Pizza with id 999 not found",
  "path": "/api/pizzas/999",
  "validationErrors": null
}
```

**409 Conflict**
```json
{
  "timestamp": "2024-11-05T19:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "Customer with email john@example.com already exists",
  "path": "/api/customers",
  "validationErrors": null
}
```

---

## 🎯 Business Rules and Custom Exceptions

Beyond basic validation, your application needs to enforce business rules. These are domain-specific constraints that go beyond simple data validation.

### Types of Business Rules

1. **Resource existence** - Verify referenced entities exist
2. **State validation** - Check if an operation is allowed in current state
3. **Duplicate prevention** - Ensure uniqueness constraints
4. **Domain constraints** - Business-specific rules

### Business Exception Hierarchy

```java
package be.vives.pizzastore.exception;

public class BusinessException extends PizzaStoreException {

    public BusinessException(String message) {
        super(message);
    }
}
```

This exception returns **422 Unprocessable Entity** - the request is valid but cannot be processed due to business logic.

### Example 1: Cannot Order Unavailable Pizzas

In `OrderService.create()`:

```java
public OrderResponse create(CreateOrderRequest request) {
    log.debug("Creating new order for customer: {}", request.customerId());

    Customer customer = customerRepository.findById(request.customerId()).orElse(null);
    if (customer == null) {
        throw new BusinessException("Customer with id " + request.customerId() + " not found");
    }

    String orderNumber = generateOrderNumber();
    Order order = new Order(orderNumber, customer, OrderStatus.PENDING);

    for (CreateOrderRequest.OrderLineRequest lineRequest : request.orderLines()) {
        Pizza pizza = pizzaRepository.findById(lineRequest.pizzaId()).orElse(null);
        if (pizza == null) {
            throw new BusinessException("Pizza with id " + lineRequest.pizzaId() + " not found");
        }
        
        // Business rule: Cannot order unavailable pizzas
        if (!pizza.getAvailable()) {
            throw new BusinessException("Pizza '" + pizza.getName() + "' is currently not available");
        }

        OrderLine orderLine = new OrderLine(pizza, lineRequest.quantity());
        order.addOrderLine(orderLine);
    }

    Order savedOrder = orderRepository.save(order);
    log.info("Created order with id: {} and number: {}", savedOrder.getId(), savedOrder.getOrderNumber());

    return orderMapper.toResponse(savedOrder);
}
```

**Response when trying to order unavailable pizza:**
```json
{
  "timestamp": "2024-11-05T19:30:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Pizza 'Hawaiian Supreme' is currently not available",
  "path": "/api/orders"
}
```

### Example 2: Cannot Cancel Delivered Orders

In `OrderService.cancel()`:

```java
public void cancel(Long id) {
    log.debug("Cancelling order with id: {}", id);
    Order order = orderRepository.findById(id).orElse(null);
    if (order == null) {
        throw new BusinessException("Order with id " + id + " not found");
    }
    
    // Business rule: Cannot cancel delivered orders
    if (order.getStatus() == OrderStatus.DELIVERED) {
        throw new BusinessException("Cannot cancel order with status " + order.getStatus());
    }
    
    // Business rule: Cannot cancel already cancelled orders
    if (order.getStatus() == OrderStatus.CANCELLED) {
        throw new BusinessException("Order is already cancelled");
    }
    
    order.setStatus(OrderStatus.CANCELLED);
    orderRepository.save(order);
    log.info("Cancelled order with id: {}", id);
}
```

**Response when trying to cancel a delivered order:**
```json
{
  "timestamp": "2024-11-05T19:30:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Cannot cancel order with status DELIVERED",
  "path": "/api/orders/5"
}
```

### When to Use Which Exception?

| Exception Type | HTTP Status | Use Case | Example |
|---------------|-------------|----------|---------|
| `BusinessException` | 422 | Business logic prevents operation | Cannot order unavailable pizza |
| `ResourceNotFoundException` | 404 | Entity not found | Pizza with id 999 not found |
| `DuplicateResourceException` | 409 | Unique constraint violated | Email already exists |
| Validation Exception | 400 | Invalid input data | Price must be positive |

### Mixing Exceptions and ResponseEntity

You can use both approaches in your controllers:

**Approach 1: Throw exception (recommended for business logic)**
```java
@PostMapping
public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
    // Service throws BusinessException if pizza unavailable
    OrderResponse created = orderService.create(request);
    
    URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id())
            .toUri();
    
    return ResponseEntity.created(location).body(created);
}
```

**Approach 2: Return ResponseEntity.notFound() (simple cases)**
```java
@GetMapping("/{id}")
public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
    OrderResponse order = orderService.findById(id);
    if (order == null) {
        return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(order);
}
```

**Both approaches are valid:**
- Use exceptions for **business logic violations** that need descriptive error messages
- Use `ResponseEntity.notFound()` for **simple resource lookups** where 404 is self-explanatory

---

## 💡 Best Practices

### 1. Validate at API Boundary
- ✅ Validate in DTOs, not in entities
- ✅ Use `@Valid` in controllers
- ❌ Don't validate in service layer (already validated)

### 2. Clear Error Messages
```java
@NotBlank(message = "Name is required")  // ✅ Clear
@NotBlank  // ❌ Generic message
```

### 3. Consistent Error Responses
- ✅ Use `@RestControllerAdvice`
- ✅ Return same structure for all errors
- ✅ Include field-level errors for validation

### 4. Appropriate Status Codes
- ✅ 400 for validation errors
- ✅ 404 for not found
- ✅ 409 for conflicts
- ✅ 500 for unexpected errors

### 5. Log Errors
```java
log.warn("Validation failed: {}", ex.getMessage());  // 400
log.error("Unexpected error", ex);  // 500
```

### 6. Don't Expose Internal Details
```java
// ❌ Bad
"message": "SQLException: Duplicate entry 'john@example.com'"

// ✅ Good
"message": "Customer with email john@example.com already exists"
```

---

## 🎓 Summary

### What We Learned

1. **Bean Validation**
   - Standard annotations (@NotNull, @NotBlank, @Size, @Email, etc.)
   - Custom validators for complex rules

2. **Exception Handling**
   - @RestControllerAdvice for global handling
   - Custom exceptions (ResourceNotFoundException, etc.)
   - Consistent error response structure

3. **HTTP Status Codes**
   - 400 Bad Request (validation)
   - 404 Not Found (resource missing)
   - 409 Conflict (duplicate)
   - 422 Unprocessable Entity (business logic)
   - 500 Internal Server Error (unexpected)

4. **Error Response Structure**
   - Timestamp
   - Status code
   - Error type
   - Message
   - Path
   - Validation errors (field-level)

### Key Takeaways

⚠️ **Always validate at API boundary**  
✅ **Use @Valid with request bodies**  
📝 **Provide clear error messages**  
🎯 **Return consistent error responses**  
🔢 **Use appropriate HTTP status codes**  
🛡️ **Never expose internal details**  

---

## 📖 Additional Resources

- [Jakarta Bean Validation Specification](https://jakarta.ee/specifications/bean-validation/)
- [Hibernate Validator Documentation](https://hibernate.org/validator/)
- [Spring Validation Guide](https://spring.io/guides/gs/validating-form-input/)
- [RFC 7807 Problem Details](https://www.rfc-editor.org/rfc/rfc7807) - Standard for HTTP API errors
- [Spring @ControllerAdvice](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html)

---

## 🚀 Runnable Project

A complete, production-ready Spring Boot project with **validation and exception handling** is available in:

**`pizzastore-with-validation/`**

### Project Structure

The project includes:

#### ✅ Validation
- Request DTOs with Bean Validation annotations (`@NotBlank`, `@NotNull`, `@Email`, `@Size`, `@DecimalMin`)
- `CreatePizzaRequest`, `UpdatePizzaRequest` - Pizza creation/update validation
- `CreateCustomerRequest`, `UpdateCustomerRequest` - Customer validation with email, password rules
- `CreateOrderRequest` - Order creation structure (validation can be added)

#### ✅ Exception Handling
- **Global Exception Handler** (`@RestControllerAdvice`) for consistent error responses
- **Custom Exceptions**:
  - `PizzaStoreException` - Base exception
  - `ResourceNotFoundException` - 404 errors
  - `DuplicateResourceException` - 409 Conflict errors
  - `BusinessException` - 422 Unprocessable Entity errors
- **ErrorResponse DTO** - Consistent error structure with field-level validation errors

#### ✅ Complete REST API
- Full CRUD operations for Pizzas, Customers, Orders
- Pagination support with `Pageable`
- Query parameters for filtering (price range, name search)
- File upload for pizza images
- Relationships: Orders → OrderLines → Pizza, Customer favorites

### How to Run

```bash
cd pizzastore-with-validation
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Test the API

**Get all pizzas:**
```bash
curl http://localhost:8080/api/pizzas
```

**Create a pizza (will validate):**
```bash
curl -X POST http://localhost:8080/api/pizzas \
  -H "Content-Type: application/json" \
  -d '{"name":"Margherita","price":8.50,"description":"Classic pizza"}'
```

**Trigger validation error:**
```bash
curl -X POST http://localhost:8080/api/pizzas \
  -H "Content-Type: application/json" \
  -d '{"name":"","price":-5}'
```

See the project README for more testing examples.

---

**Congratulations!** 🎉 You now know how to build robust, production-ready APIs with proper validation and error handling!
