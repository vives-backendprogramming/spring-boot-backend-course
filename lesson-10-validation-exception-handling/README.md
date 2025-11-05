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
7. [Creating Error Response Objects](#creating-error-response-objects)
8. [HTTP Status Codes for Errors](#http-status-codes-for-errors)
9. [Complete PizzaStore with Validation](#complete-pizzastore-with-validation)
10. [Summary](#summary)

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
    
    @NotBlank(message = "Phone is required")
    @Size(min = 10, max = 20, message = "Phone must be between 10 and 20 characters")
    String phone
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
    
    @Pattern(regexp = "^\\+32[0-9]{9}$", message = "Phone must be Belgian format (+32xxxxxxxxx)")
    String phone
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
) {}
```

---

## ✅ Validating Request Bodies

### Step 1: Add Validation Annotations to DTO

```java
package be.vives.pizzastore.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreatePizzaRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String name,
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be at least €0.01")
    @DecimalMax(value = "100.00", message = "Price cannot exceed €100.00")
    BigDecimal price,
    
    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 500, message = "Description must be between 10 and 500 characters")
    String description
) {
}
```

### Step 2: Add @Valid to Controller

```java
@RestController
@RequestMapping("/api/pizzas")
public class PizzaController {

    @PostMapping
    public ResponseEntity<PizzaResponse> createPizza(
            @Valid @RequestBody CreatePizzaRequest request) {
        
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

**Key Points:**
- `@Valid` triggers validation
- Validation happens **before** method execution
- If validation fails, method is **never called**
- Spring automatically returns 400 Bad Request

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

import java.time.LocalDateTime;
import java.util.List;

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

        log.warn("Validation failed: {}", ex.getMessage());

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
                request.getDescription(false).replace("uri=", ""),
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
                request.getDescription(false).replace("uri=", ""),
                null
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
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
                "An unexpected error occurred",
                request.getDescription(false).replace("uri=", ""),
                null
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
}
```

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

## 🍕 Complete PizzaStore with Validation

### Validated DTOs

#### CreatePizzaRequest

```java
public record CreatePizzaRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String name,
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be at least €0.01")
    @DecimalMax(value = "100.00", message = "Price cannot exceed €100.00")
    BigDecimal price,
    
    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 500, message = "Description must be between 10 and 500 characters")
    String description
) {}
```

#### CreateCustomerRequest

```java
public record CreateCustomerRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String name,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    
    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+32[0-9]{9}$", message = "Phone must be Belgian format (+32xxxxxxxxx)")
    String phone,
    
    @NotBlank(message = "Address is required")
    @Size(min = 10, max = 200, message = "Address must be between 10 and 200 characters")
    String address
) {}
```

#### CreateOrderRequest

```java
public record CreateOrderRequest(
    @NotNull(message = "Customer ID is required")
    @Positive(message = "Customer ID must be positive")
    Long customerId,
    
    @NotEmpty(message = "Order must contain at least one pizza")
    @Size(min = 1, max = 20, message = "Order can contain between 1 and 20 pizzas")
    @Valid
    List<OrderLineRequest> orderLines
) {
    public record OrderLineRequest(
        @NotNull(message = "Pizza ID is required")
        @Positive(message = "Pizza ID must be positive")
        Long pizzaId,
        
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 50, message = "Quantity cannot exceed 50")
        Integer quantity
    ) {}
}
```

### Exception Classes

```java
// Base exception
public class PizzaStoreException extends RuntimeException {
    public PizzaStoreException(String message) {
        super(message);
    }
}

// Resource not found
public class ResourceNotFoundException extends PizzaStoreException {
    public ResourceNotFoundException(String resourceName, Long id) {
        super(String.format("%s with id %d not found", resourceName, id));
    }
}

// Duplicate resource
public class DuplicateResourceException extends PizzaStoreException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}

// Business logic error
public class BusinessException extends PizzaStoreException {
    public BusinessException(String message) {
        super(message);
    }
}
```

### Service with Validation

```java
@Service
@Transactional
public class CustomerService {
    
    public CustomerResponse create(CreateCustomerRequest request) {
        // Check for duplicate email
        if (customerRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException(
                "Customer with email " + request.email() + " already exists"
            );
        }
        
        Customer customer = customerMapper.toEntity(request);
        Customer saved = customerRepository.save(customer);
        
        return customerMapper.toResponse(saved);
    }
    
    public CustomerResponse update(Long id, UpdateCustomerRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        
        // Check email uniqueness if changed
        if (!customer.getEmail().equals(request.email()) &&
            customerRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException(
                "Customer with email " + request.email() + " already exists"
            );
        }
        
        customerMapper.updateEntity(request, customer);
        Customer updated = customerRepository.save(customer);
        
        return customerMapper.toResponse(updated);
    }
}
```

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

The project includes:
- ✅ All DTOs validated with Bean Validation
- ✅ Custom validators (ValidPizzaName, etc.)
- ✅ Global exception handler
- ✅ Consistent error responses
- ✅ Custom exceptions (ResourceNotFoundException, etc.)
- ✅ Proper HTTP status codes
- ✅ Complete logging
- ✅ All CRUD operations from Lesson 8 + validation

See the project README for setup instructions and testing examples.

---

**Congratulations!** 🎉 You now know how to build robust, production-ready APIs with proper validation and error handling!
