# Lesson 10: Validation & Exception Handling

**Rejecting Bad Input Early and Answering Every Error with a Consistent RFC 7807 Problem Detail**

---

## 📋 Learning Objectives

By the end of this lesson, you will be able to:
- Validate incoming request bodies with Jakarta Bean Validation (`@NotBlank`, `@NotNull`, `@Size`, `@Email`, `@DecimalMin`, ...)
- Trigger validation with `@Valid`, including nested objects and lists
- Choose the right constraints for a create (`POST`) and a full-replacement update (`PUT`) DTO
- Design a small exception hierarchy and let the service layer throw exceptions instead of returning `Optional`/`boolean`/`null`
- Handle all exceptions in one place with `@RestControllerAdvice` and `@ExceptionHandler`
- Return RFC 7807 `ProblemDetail` responses, also for the exceptions Spring MVC throws itself (`ResponseEntityExceptionHandler`)
- Call an external REST API (Open Food Facts) with a declarative `@HttpExchange` client and turn its failures into a clean `502 Bad Gateway`
- Pick the right status code: 400, 404, 409, 422, 500 or 502
- Avoid leaking internal details (SQL errors, stack traces) to API clients

---

## 📚 Table of Contents

1. [Recap: Where Lesson 9 Left Us](#-recap-where-lesson-9-left-us)
2. [What This Lesson Adds to PizzaStore](#-what-this-lesson-adds-to-pizzastore)
3. [Part 1: Jakarta Bean Validation](#-part-1-jakarta-bean-validation)
4. [Built-in Constraints](#-built-in-constraints)
5. [Validating Request Bodies with @Valid](#-validating-request-bodies-with-valid)
6. [Validation Rules in PizzaStore's DTOs](#-validation-rules-in-pizzastores-dtos)
7. [Validating Path Variables, Parameters & Custom Constraints](#-validating-path-variables-parameters--custom-constraints)
8. [Part 2: Exceptions Instead of Optional](#-part-2-exceptions-instead-of-optional)
9. [Global Exception Handling with @RestControllerAdvice](#%EF%B8%8F-global-exception-handling-with-restcontrolleradvice)
10. [Problem Details (RFC 7807)](#-problem-details-rfc-7807)
11. [Part 3: Calling an External API](#-part-3-calling-an-external-api)
12. [Choosing the Right Status Code](#-choosing-the-right-status-code)
13. [Before and After](#-before-and-after)
14. [Best Practices](#-best-practices)
15. [Summary](#-summary)
16. [Additional Resources](#-additional-resources)
17. [Runnable Project](#-runnable-project)

---

## 🔄 Recap: Where Lesson 9 Left Us

Lesson 9 turned PizzaStore into a working REST API: controllers on top of the Lesson 7 services, full CRUD, pagination, filtering and image upload. Its last section, [What Happens When Things Go Wrong?](../lesson-09-complete-rest-api/README.md#%EF%B8%8F-what-happens-when-things-go-wrong), showed where that API is still weak:

| Request | Lesson 9 returns | What we want |
|---------|------------------|--------------|
| `GET /api/pizzas/999` | `404`, empty body | `404` that says *what* wasn't found |
| `PUT /api/pizzas/1` with `{ "price": 12 }` | `500` (`DataIntegrityViolationException`) | `400`: "name is required" |
| `POST /api/orders` with an unknown `customerId` | `500` (`RuntimeException`) | `422`: the order can't be processed |
| `DELETE /api/pizzas/1` (pizza is someone's favorite) | `500` (foreign-key violation) | `409 Conflict` |

Two problems: **client mistakes show up as server errors**, and **the body never explains what went wrong**. This lesson fixes both, in two parts:

1. **Validation** rejects invalid input at the API boundary, before it reaches a service or the database.
2. **Exception handling** turns everything that still goes wrong into a precise status code and a standard, machine-readable error body.

---

## 🧱 What This Lesson Adds to PizzaStore

```
                 ┌──────────────────────────────┐
  HTTP request → │ @Valid on the request DTO    │ ← NEW: invalid input → 400 before the controller runs
                 ├──────────────────────────────┤
                 │   Controller                 │ ← simpler: no more Optional/boolean → 404 code
                 ├──────────────────────────────┤
                 │   Service                    │ ← NEW: throws ResourceNotFoundException, BusinessException, ...
                 ├──────────────────────────────┤
                 │   Repository / Domain        │
                 └──────────────────────────────┘
                        │ any exception
                        ▼
                 ┌──────────────────────────────┐
                 │ GlobalExceptionHandler       │ ← NEW: exception → status code + ProblemDetail body
                 └──────────────────────────────┘
```

The project in this lesson, [`pizzastore-with-validation`](pizzastore-with-validation), is **Lesson 9's [`pizzastore-complete-api`](../lesson-09-complete-rest-api/pizzastore-complete-api) plus exactly these changes**:

| Added / changed | What it does |
|-----------------|--------------|
| [`pom.xml`](pizzastore-with-validation/pom.xml) | Adds `spring-boot-starter-validation` |
| [`dto/request/*.java`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/dto/request) | Validation constraints on every request DTO |
| [`exception/`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/exception) (new package) | `PizzaStoreException` and its subclasses, plus the `GlobalExceptionHandler` |
| [`service/PizzaService.java`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/service/PizzaService.java), [`CustomerService.java`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/service/CustomerService.java), [`OrderService.java`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/service/OrderService.java) | Throw exceptions instead of returning `Optional`/`boolean`; new business rules (unavailable pizza, cancelling a delivered order, duplicate email) |
| [`service/FileStorageService.java`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/service/FileStorageService.java) | Throws `InvalidFileException` instead of `IllegalArgumentException` |
| [`controller/*.java`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/controller) | `@Valid` on every `@RequestBody`; "not found" handling and the `try/catch` around the upload removed |
| [`client/`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/client), [`config/HttpClientConfig.java`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/config/HttpClientConfig.java), [`service/NutritionImportService.java`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/service/NutritionImportService.java) (new), `POST /api/pizzas/{id}/nutritional-info/import` | [Part 3](#-part-3-calling-an-external-api): PizzaStore's first call to somebody else's API (Open Food Facts), with `spring-boot-starter-restclient` in the `pom.xml` and `spring.http.serviceclient.openfoodfacts.*` in `application.properties` |

The `domain`, `repository`, `mapper`, `dto/response` packages and `data.sql` are **unchanged**.

---

## ✅ Part 1: Jakarta Bean Validation

### Why Validate at the API Boundary?

An HTTP API receives data from clients you don't control. Without validation, whatever the client sends travels through the controller, the mapper and the service until something breaks, usually the database:

```
PUT /api/pizzas/1  { "price": 12 }
  → PizzaMapper sets name = null, available = null
  → UPDATE pizzas SET name = NULL ...   ✗ NOT NULL constraint
  → 500 Internal Server Error
```

The book summarises the idea as **fail fast**: validation "prevents invalid data from ever reaching your service layer or database". The client gets a `400 Bad Request` that says which fields are wrong, and our code only ever sees valid data.

### Jakarta Bean Validation

**Jakarta Bean Validation** (formerly JSR-380 / "Bean Validation 2.0"; Spring Boot 4 uses version 3.1) is the Java standard for declaring rules as annotations on fields. **Hibernate Validator** is the implementation that actually checks them. You annotate, the framework validates:

```java
public record CreateCustomerRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,
        ...
) {}
```

### Setup

Validation is **not** part of `spring-boot-starter-webmvc`. Add its own starter:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

It brings in Hibernate Validator and auto-configures a `Validator` bean that Spring MVC uses. Without it, `@Valid` and all constraint annotations compile (they're in `jakarta.validation-api`) but are **silently ignored**.

---

## 📝 Built-in Constraints

All constraints live in `jakarta.validation.constraints`. The most useful ones:

| Constraint | Checks | Typical use |
|------------|--------|-------------|
| `@NotNull` | Not `null` | Required numbers, enums, booleans, IDs |
| `@NotEmpty` | Not `null` and not empty (length/size > 0) | Required lists |
| `@NotBlank` | Not `null` and contains at least one non-whitespace character | Required strings |
| `@Size(min, max)` | Length of a string / size of a collection | Names, phone numbers, max number of order lines |
| `@Min` / `@Max` | Integer value bounds | Quantity ≥ 1 |
| `@DecimalMin` / `@DecimalMax` | Decimal bounds (as a string, e.g. `"0.01"`) | Prices |
| `@Positive` / `@PositiveOrZero` | > 0 / ≥ 0 | Amounts, nutritional values |
| `@Digits(integer, fraction)` | Number of digits before/after the decimal point | Money with 2 decimals |
| `@Email` | Well-formed email address | Email fields |
| `@Pattern(regexp)` | Matches a regular expression | Phone numbers, codes |
| `@Past` / `@Future` / `@PastOrPresent` | Date/time relative to now | Birth dates, delivery dates |

### `null` Is Valid for Almost Every Constraint

Except for the `@Not...` family, **every constraint treats `null` as valid**. `@Size(max = 20)` on `phone` means "*if* a phone number is given, it's at most 20 characters". That's deliberate: it lets you combine "optional" and "if present, then ...". When a field is required, add `@NotNull`/`@NotBlank` explicitly:

```java
@NotNull(message = "Price is required")          // required ...
@DecimalMin(value = "0.01", message = "Price must be positive")   // ... and positive
BigDecimal price
```

`@NotNull` vs `@NotEmpty` vs `@NotBlank` for a string:

| Value | `@NotNull` | `@NotEmpty` | `@NotBlank` |
|-------|:----------:|:-----------:|:-----------:|
| `null` | ❌ | ❌ | ❌ |
| `""` | ✅ | ❌ | ❌ |
| `"   "` | ✅ | ✅ | ❌ |
| `"Margherita"` | ✅ | ✅ | ✅ |

### Always Write Your Own Message

Every constraint has a default message ("must not be blank"), but it's generic and doesn't mention the field. Some good advice: "Always provide custom message attributes". *"Email must be valid"* helps an API consumer more than *"must be a well-formed email address"*.

---

## 🎯 Validating Request Bodies with @Valid

Constraints on a DTO do nothing until something asks to validate it. In a controller, that's `@Valid` in front of `@RequestBody`:

```java
@PostMapping
public ResponseEntity<PizzaResponse> createPizza(@Valid @RequestBody CreatePizzaRequest request) {
    PizzaResponse created = pizzaService.create(request);   // only reached with a valid request
    ...
}
```

What happens on a request:

1. Jackson converts the JSON body into a `CreatePizzaRequest`. (Invalid JSON already fails here with an `HttpMessageNotReadableException`.)
2. Because of `@Valid`, Spring MVC validates the object.
3. If there are violations, Spring throws a **`MethodArgumentNotValidException`** that holds all field errors. **The controller method is never called.**
4. An exception handler turns it into a `400 Bad Request`. Without one, the client gets Spring Boot's default error body — the same `{ timestamp, status, error, path }` you saw in Lesson 9, now with status 400 but still without saying *which* field is wrong.

All violations are collected, not just the first one. `POST /api/pizzas` with `{ "name": "", "price": -5 }` reports three problems at once:

```json
"errors" : [
  { "field" : "price",     "message" : "Price must be positive" },
  { "field" : "available", "message" : "Availability is required" },
  { "field" : "name",      "message" : "Pizza name is required" }
]
```

> The order of the errors is not guaranteed (Hibernate Validator collects them in a `Set`). Clients should look at the `field` names, not at the position in the list.

### `@Valid` vs `@Validated`

Both trigger validation of a `@RequestBody`. `@Valid` is the Jakarta standard annotation; `@Validated` is Spring's variant that additionally supports **validation groups** (e.g. different rules for create and update in one DTO). PizzaStore uses separate Create/Update DTOs instead of groups, so plain `@Valid` is enough.

### Cascading into Nested Objects and Lists

Validation does **not** automatically descend into nested objects. Put `@Valid` on the field to cascade:

```java
public record CreateOrderRequest(
        @NotNull(message = "Customer ID is required")
        Long customerId,

        @NotEmpty(message = "Order must contain at least one pizza")
        @Size(max = 20, message = "Order can contain at most 20 order lines")
        @Valid                                       // ← validate every OrderLineRequest too
        List<OrderLineRequest> orderLines
) {
    public record OrderLineRequest(
            @NotNull(message = "Pizza ID is required")
            Long pizzaId,

            @NotNull(message = "Quantity is required")
            @Min(value = 1, message = "Quantity must be at least 1")
            Integer quantity
    ) {}
}
```

The field path in the error tells the client exactly where the problem is:

```json
"errors" : [ { "field" : "orderLines[0].quantity", "message" : "Quantity must be at least 1" } ]
```

`CreatePizzaRequest` and `UpdatePizzaRequest` do the same for `nutritionalInfo` (error field `nutritionalInfo.calories`).

---

## 🍕 Validation Rules in PizzaStore's DTOs

The constraints go on the **request DTOs** from Lesson 7, not on the JPA entities. That's the book's third best practice too: "prefer applying validation on Data Transfer Objects (DTOs) specifically designed for incoming requests, keeping your internal domain entities clean." (The book's own examples put them directly on its `Customer` record, which is its domain model *and* request body at the same time.) Different endpoints can then have different rules for the same entity, which is exactly what PizzaStore needs:

| DTO | Rules |
|-----|-------|
| [`CreatePizzaRequest`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/dto/request/CreatePizzaRequest.java) | `name` `@NotBlank`; `price` `@NotNull` + `@DecimalMin("0.01")`; `available` `@NotNull`; `nutritionalInfo` `@Valid` |
| [`UpdatePizzaRequest`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/dto/request/UpdatePizzaRequest.java) | Same as create (PUT replaces the whole pizza) |
| [`NutritionalInfoRequest`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/dto/request/NutritionalInfoRequest.java) | All values optional, but `@PositiveOrZero` |
| [`CreateCustomerRequest`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/dto/request/CreateCustomerRequest.java) | `name` `@NotBlank` + `@Size(2..100)`; `email` `@NotBlank` + `@Email`; `password` `@NotBlank` + `@Size(min = 8)`; `phone` ≤ 20, `address` ≤ 200 characters |
| [`UpdateCustomerRequest`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/dto/request/UpdateCustomerRequest.java) | `name` required as for create; `email` only `@Email` (see below); `phone`/`address` size limits |
| [`CreateOrderRequest`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/dto/request/CreateOrderRequest.java) | `customerId` `@NotNull`; 1–20 order lines, each with a `pizzaId` and a `quantity` ≥ 1 |
| [`UpdateOrderStatusRequest`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/dto/request/UpdateOrderStatusRequest.java) | `status` `@NotNull` |

A few choices worth explaining:

**PUT means "here is the complete new state".** Lesson 9 showed that `PUT /api/pizzas/1` with only `{ "price": 12 }` makes MapStruct set `name` and `available` to `null` (the default `SET_TO_NULL` strategy from Lesson 7) and the database rejects it with a 500. The fix is not to make PUT silently ignore missing fields — that would turn it into a PATCH — but to **require the same fields as on create**. Now the request is rejected before it reaches the mapper:

```json
{
  "detail" : "Validation failed",
  "instance" : "/api/pizzas/1",
  "status" : 400,
  "title" : "Bad Request",
  "type" : "https://api.pizzastore.example.com/errors/validation",
  "errors" : [ {
    "field" : "name",
    "message" : "Pizza name is required"
  }, {
    "field" : "available",
    "message" : "Availability is required"
  } ]
}
```

Optional fields (`description`, `nutritionalInfo`) keep their PUT meaning: leaving them out clears them.

**Why is `available` required on create?** The `Pizza` entity initialises `available = true`, but the generated `PizzaMapper.toEntity` calls `pizza.setAvailable(request.available())` unconditionally, so a missing value overwrites that default with `null` and the insert fails on the `NOT NULL` column. Requiring the field makes the contract explicit. (A mapper default, `@Mapping(target = "available", defaultValue = "true")`, would be the alternative if the API should accept it being left out.)

**The email of a customer can't be changed.** `CustomerMapper.updateEntity` ignores `email` (it becomes the login in Lesson 12), so `UpdateCustomerRequest` only checks the format if one is sent.

> 💡 Validation checks the **shape** of a single request: required fields, lengths, ranges, formats. Rules that need the database or the current state — "does customer 7 exist?", "is this pizza available?", "is this email already taken?" — are **business rules**. They belong in the service layer and are signalled with exceptions, which is Part 2.

---

## 🔧 Validating Path Variables, Parameters & Custom Constraints

PizzaStore only validates request bodies. Two more tools are worth knowing.

### Constraints on `@PathVariable` and `@RequestParam`

Since Spring Framework 6.1, Spring MVC applies **method validation** automatically when a controller method parameter carries a constraint annotation, no extra configuration needed:

```java
@GetMapping("/{id}/orders")
public ResponseEntity<Page<OrderResponse>> getCustomerOrders(
        @PathVariable Long id,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) { ... }
```

`size=500` is then rejected with a `HandlerMethodValidationException`. `GlobalExceptionHandler` below already turns that into a `400` problem detail, because it extends `ResponseEntityExceptionHandler` — but only with the generic detail `"Validation failure"`. To list the offending parameters like we do for request bodies, you would also override its `handleHandlerMethodValidationException` method. Type errors (`GET /api/pizzas/abc`) don't need a constraint at all: they fail during conversion and also end up as a `400` problem detail.

### Custom Constraints

When no built-in constraint fits, write your own: an annotation plus a `ConstraintValidator`.

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PizzaNameValidator.class)
public @interface ValidPizzaName {
    String message() default "Pizza name must start with a capital letter and contain only letters and spaces";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class PizzaNameValidator implements ConstraintValidator<ValidPizzaName, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;                       // null is @NotBlank's job, like the built-in constraints
        }
        return value.matches("^[A-Z][a-zA-Z ]*$");
    }
}
```

Use it like any other constraint: `@NotBlank @ValidPizzaName String name`. Keep validators free of database access; "is this name already taken?" is a business rule for the service, not a constraint.

---

## 🚨 Part 2: Exceptions Instead of Optional

### The Service Decides, the Handler Translates

In previous lessons the services reported "not found" with `Optional.empty()` or `false`, and every controller method turned that into a 404:

```java
// Lesson 9 — PizzaController
return pizzaService.findById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
```

That works for one kind of error, but not for "this pizza is unavailable" or "this email is already taken": an `Optional` can only say *empty*, not *why*. From this lesson on, services **throw an exception that describes the problem**, and one central handler translates exceptions into HTTP responses:

```java
// Lesson 10 — PizzaService
public PizzaResponse findById(Long id) {
    Pizza pizza = pizzaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Pizza", id));
    return pizzaMapper.toResponse(pizza);
}

// Lesson 10 — PizzaController
public ResponseEntity<PizzaResponse> getPizza(@PathVariable Long id) {
    PizzaResponse pizza = pizzaService.findById(id);
    return ResponseEntity.ok(pizza);
}
```

The controller only describes the happy path. The same change runs through all services: `update`, `delete`, `uploadImage`, `updateStatus`, `cancel`, `addFavoritePizza`, ... return a result or `void` and throw when something is wrong. Note that the services still know nothing about HTTP: they throw *domain* exceptions ("resource not found", "business rule violated"), and only the exception handler maps those to status codes.

### PizzaStore's Exception Hierarchy

```
RuntimeException
└── PizzaStoreException            → 400 Bad Request (fallback)
    ├── ResourceNotFoundException  → 404 Not Found
    ├── DuplicateResourceException → 409 Conflict
    ├── BusinessException          → 422 Unprocessable Content
    ├── ExternalServiceException   → 502 Bad Gateway (Part 3)
    └── InvalidFileException       → 400 Bad Request (via the PizzaStoreException handler)
```

All files are in [`exception/`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/exception). They're tiny; `ResourceNotFoundException` adds a convenience constructor for a consistent message:

```java
public class ResourceNotFoundException extends PizzaStoreException {

    public ResourceNotFoundException(String resourceName, Long id) {
        super(String.format("%s with id %d not found", resourceName, id));   // "Pizza with id 999 not found"
    }
    ...
}
```

They extend `RuntimeException` (unchecked) on purpose: callers don't have to declare or catch them, and — as you saw in Lesson 7's [rollback rules](../lesson-07-dtos-mappers/README.md) — an unchecked exception thrown from a `@Transactional` service method rolls the transaction back automatically.

### Where Each Exception Is Thrown

| Situation | Where | Exception |
|-----------|-------|-----------|
| Pizza/customer/order in the **URL** doesn't exist | `PizzaService`, `CustomerService`, `OrderService` | `ResourceNotFoundException` |
| Customer or pizza referenced in the **body** of a new order doesn't exist | `OrderService.create` | `BusinessException` |
| Ordering a pizza that is not available | `OrderService.create` | `BusinessException` |
| Cancelling an order that is delivered or already cancelled | `OrderService.cancel` | `BusinessException` |
| Creating a customer with an email that already exists | `CustomerService.create` | `DuplicateResourceException` |
| Uploading an empty file or a file that isn't JPG/PNG | `FileStorageService` | `InvalidFileException` |
| Open Food Facts doesn't know the barcode, or has no complete nutrition data for it | `NutritionImportService` | `BusinessException` |
| Open Food Facts is down, too slow or rate-limiting us | `NutritionImportService` | `ExternalServiceException` |

An example business rule from [`OrderService`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/service/OrderService.java):

```java
public void cancel(Long id) {
    Order order = orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order", id));

    // Business rule: Cannot cancel delivered orders
    if (order.getStatus() == OrderStatus.DELIVERED) {
        throw new BusinessException("Cannot cancel order with status " + order.getStatus());
    }
    ...
}
```

Why is an unknown `customerId` in `POST /api/orders` a **422** and not a 404? A 404 says "the resource at *this URL* doesn't exist" — but `/api/orders` exists fine. The request is syntactically valid, yet its content can't be processed: that's exactly what 422 means. See [400, 404 or 422?](#400-404-or-422) for the full reasoning.

---

## 🛡️ Global Exception Handling with @RestControllerAdvice


Instead of a `try/catch` in every controller method (like the upload endpoint in Lesson 9), Spring lets you handle exceptions **globally**:

- **`@RestControllerAdvice`** marks a class whose `@ExceptionHandler` methods apply to all controllers. It is a `@Component`, so component scanning picks it up; "Rest" means the return values are written as the response body (like `@RestController`).
- **`@ExceptionHandler(SomeException.class)`** declares which exception type a method handles. The method can take the exception and the `WebRequest` as parameters and returns the response body.

The book's Figure 3-2 ("Problem Detail Flow") shows the flow: exception thrown in the controller (or in a service it calls) → the advice → the matching `@ExceptionHandler` → a `ProblemDetail` → a message converter → JSON.

PizzaStore's handler is [`GlobalExceptionHandler`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/exception/GlobalExceptionHandler.java). An excerpt:

```java
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage(), "not-found", request);
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex, WebRequest request) {
        log.warn("Business logic error: {}", ex.getMessage());
        return buildProblemDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), "business-rule", request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception ex, WebRequest request) {
        log.error("Unexpected error occurred", ex);
        return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support if the problem persists.", "internal-error", request);
    }
    ...
}
```

Its handlers:

| Handler for | Status | `detail` |
|-------------|--------|----------|
| `MethodArgumentNotValidException` (override) | 400 | "Validation failed" + an `errors` list |
| `HttpMessageNotReadableException` (override) | 400 | "Malformed JSON request", or the allowed values for an invalid `OrderStatus` |
| `ResourceNotFoundException` | 404 | The exception's message |
| `DuplicateResourceException` | 409 | The exception's message |
| `BusinessException` | 422 | The exception's message |
| `PizzaStoreException` (all other subclasses, e.g. `InvalidFileException`) | 400 | The exception's message |
| `DataIntegrityViolationException` | 409 | A generic message; the SQL error is only logged |
| `Exception` (everything else) | 500 | A generic message; the stack trace is only logged |

### Which Handler Wins?

When an exception is thrown, Spring picks the handler for the **closest matching type** in the exception's class hierarchy. A `ResourceNotFoundException` goes to its own handler, not to the `PizzaStoreException` or `Exception` handler, even though it "is" both. That's what makes the catch-all `Exception` handler safe: it only gets what nothing more specific handles.

### Why Extend `ResponseEntityExceptionHandler`?

Our own exceptions are only half of what can go wrong. Spring MVC itself throws exceptions before a controller method even runs: the HTTP method isn't supported (405), the `Content-Type` isn't JSON (415), `abc` can't be converted to a `Long` (400), a multipart part is missing (400), an upload is too large (413), no handler exists for the path (404), ... Without extra work, those still produce Spring Boot's default `{ timestamp, status, error, path }` body, so the API would speak two error formats.

Spring's **`ResponseEntityExceptionHandler`** is a base class with an `@ExceptionHandler` for all of those exceptions that answers with a `ProblemDetail`. Extending it gives PizzaStore one format for everything (all verified against the running project):

```json
// PUT /api/pizzas — the collection only supports GET and POST
{
  "detail" : "Method 'PUT' is not supported.",
  "instance" : "/api/pizzas",
  "status" : 405,
  "title" : "Method Not Allowed"
}
```

Because the base class already declares an `@ExceptionHandler` for `MethodArgumentNotValidException` and `HttpMessageNotReadableException`, you **must not** declare another one for them: two handlers for the same exception is an "ambiguous `@ExceptionHandler`" error at startup. Customise them by **overriding** the protected methods instead:

```java
@Override
protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                              HttpHeaders headers, HttpStatusCode status,
                                                              WebRequest request) {
    List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> {
                Map<String, String> entry = new LinkedHashMap<>();
                entry.put("field", error.getField());
                entry.put("message", error.getDefaultMessage());
                return entry;
            })
            .toList();

    ProblemDetail problemDetail = buildProblemDetail(HttpStatus.BAD_REQUEST, "Validation failed", "validation", request);
    problemDetail.setProperty("errors", errors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(problemDetail);
}
```

> 💡 If you don't need your own handler at all, Spring Boot can do this for you: `spring.mvc.problemdetails.enabled=true` registers a `ResponseEntityExceptionHandler` of its own. It is **off by default**, also in Spring Boot 4, and it steps aside as soon as your application defines its own `ResponseEntityExceptionHandler` — like PizzaStore does.

### Don't Leak Internals: `DataIntegrityViolationException`

Deleting pizza 1 fails because it's still in a customer's favorites. The database error is full of internal details:

```
Referential integrity constraint violation: "FK2CQLHU18N4MQ3GJ5VIG3YCVBH:
PUBLIC.CUSTOMER_FAVORITE_PIZZAS FOREIGN KEY(PIZZA_ID) REFERENCES PUBLIC.PIZZAS(ID) (CAST(1 AS BIGINT))" ...
```

That's useful in the log, but a client should never see table names, constraint names or SQL. The handler logs the real cause and returns a safe message:

```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
    log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
    return buildProblemDetail(HttpStatus.CONFLICT,
            "The request conflicts with existing data (e.g. the resource is still referenced by other data).",
            "conflict", request);
}
```

Where does this exception come from? `PizzaService.delete` doesn't throw it: the `DELETE` statement is only sent when the transaction is **committed**, after the service method returns (Lesson 7, dirty checking and flushing). The `JpaTransactionManager` translates the Hibernate error into Spring's `DataIntegrityViolationException` (the exception translation from Lesson 6), the transactional proxy rethrows it to the controller, and from there it reaches the advice like any other exception.

The same "log everything, show little" rule applies to the catch-all handler: the stack trace goes to the log at `ERROR` level, the client gets a generic 500 message.

### Log Levels

The handler logs **client errors** (4xx) at `WARN` — they're expected and not a bug in our code — and **unexpected errors** (5xx) at `ERROR` with the full stack trace, because those need a developer's attention.

---

## 📄 Problem Details (RFC 7807)

Every error response now has the same, standardised shape: a **Problem Detail**, defined by RFC 7807 "Problem Details for HTTP APIs" (since 2023 updated as RFC 9457, same format). The book calls it "the recommended standard for error responses" in Spring Boot 4. Spring's `org.springframework.http.ProblemDetail` class represents it:

```json
{
  "type" : "https://api.pizzastore.example.com/errors/not-found",
  "title" : "Not Found",
  "status" : 404,
  "detail" : "Pizza with id 999 not found",
  "instance" : "/api/pizzas/999"
}
```

| Field | Meaning | In PizzaStore |
|-------|---------|---------------|
| `type` | A URI that identifies the *kind* of problem; clients can switch on it | `https://api.pizzastore.example.com/errors/<slug>`: `validation`, `not-found`, `conflict`, `business-rule`, ... |
| `title` | Short, human-readable summary of the kind of problem | Filled in automatically from the status ("Not Found") |
| `status` | The HTTP status code, repeated in the body | Set by `forStatusAndDetail` |
| `detail` | Explanation of *this* occurrence | The exception message |
| `instance` | URI of *this* occurrence | The request path |
| *extensions* | Any extra properties | `errors` for validation failures |

The response is sent with `Content-Type: application/problem+json`, so clients can recognise an error body without parsing it first.

[`GlobalExceptionHandler`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/exception/GlobalExceptionHandler.java) builds all of its problem details with one helper:

```java
private ProblemDetail buildProblemDetail(HttpStatus status, String detail, String typeSlug, WebRequest request) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
    problemDetail.setType(URI.create(ERROR_TYPE_BASE + typeSlug));
    problemDetail.setInstance(URI.create(extractPath(request)));
    return problemDetail;
}
```

Returning a `ProblemDetail` from an `@ExceptionHandler` is enough: Spring uses its `status` as the HTTP status of the response. The problem details produced by the `ResponseEntityExceptionHandler` base class (405, 415, ...) have no custom `type`; RFC 7807 then defaults it to `about:blank`, which is why it's left out of their JSON.

### Compared with the Book

The book's `GlobalExceptionHandler` (Listing 3-4) uses the same building blocks — `@RestControllerAdvice`, `@ExceptionHandler`, `ProblemDetail.forStatusAndDetail`, `setType`, an `errors` extension — with two differences:

- It joins all field errors into **one string** (`"name: Name cannot be empty, email: Invalid email format"`). PizzaStore returns a **list of `{ field, message }` objects**, which a client can map onto its form fields without parsing text.
- It handles `NoResourceFoundException` with its own `@ExceptionHandler`; PizzaStore gets that (and the other Spring MVC exceptions) from `ResponseEntityExceptionHandler`.

---

## 🌐 Part 3: Calling an External API

Until now PizzaStore only *answered* requests. Real back-ends also *make* them: a payment provider, an address lookup, a nutrition database. This part adds PizzaStore's first outgoing call, to the free [Open Food Facts](https://world.openfoodfacts.org) database. It also shows the new problem that comes with it: **the other side can fail, and that must not break your API**.

### The Feature: Importing Nutrition Data

Every pizza has an optional `NutritionalInfo` (calories, protein, carbohydrates, fat) since Lesson 6a. Until now somebody had to type those numbers in a `POST` or `PUT`. With the new endpoint the pizzeria gives a **barcode** instead, and PizzaStore fetches the values from Open Food Facts:

```
POST /api/pizzas/1/nutritional-info/import
Content-Type: application/json

{ "barcode": "3017620422003" }
```

```json
HTTP/1.1 200 OK

{
  "id" : 1,
  "name" : "Margherita",
  "price" : 8.99,
  ...
  "nutritionalInfo" : {
    "calories" : 539,
    "protein" : 6.30,
    "carbohydrates" : 57.50,
    "fat" : 30.90
  }
}
```

A few things to know about the data:

- **Open Food Facts** is a free, open, crowd-sourced database of millions of *packaged* food products, identified by the barcode on the package (EAN-13, EAN-8, UPC-A). Anyone can read it without an account or API key.
- A pizza from our own oven has no barcode, so the pizzeria uses the barcode of a **comparable packaged product** (a frozen Margherita, for instance). The example barcode `3017620422003` is a jar of Nutella: not a pizza, but a product whose data is complete and stable, which makes it handy for trying out the endpoint.
- The values are **per 100 g**, exactly as Open Food Facts publishes them. An existing `NutritionalInfo` is overwritten.

### How It Works

```
 Client                PizzaStore                                            Open Food Facts
   │                                                                               │
   │ POST /api/pizzas/1/nutritional-info/import  {"barcode":"3017620422003"}       │
   ├──────────► PizzaController   @Valid ImportNutritionRequest  (400 if invalid)  │
   │                 │                                                             │
   │                 ▼                                                             │
   │           NutritionImportService                                              │
   │                 │ 1. pizzaService.findById(1)               (404 if unknown)  │
   │                 │ 2. openFoodFactsClient.getProduct(barcode) ────────────────►│
   │                 │      GET /api/v2/product/3017620422003.json?fields=...      │
   │                 │◄──────────────────────────────── JSON with "nutriments" ────┤
   │                 │ 3. check and convert the values           (422 / 502)       │
   │                 │ 4. pizzaService.updateNutritionalInfo(1, values)            │
   │                 ▼                                                             │
   │◄──────── 200 PizzaResponse with the new nutritionalInfo                       │
```

The new and changed files, in the order you would write them:

| Step | File | What it does |
|------|------|--------------|
| 1 | [`pom.xml`](pizzastore-with-validation/pom.xml) | adds `spring-boot-starter-restclient` |
| 2 | [`client/OpenFoodFactsResponse.java`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/client/OpenFoodFactsResponse.java) | record for the part of the JSON we use |
| 3 | [`client/OpenFoodFactsClient.java`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/client/OpenFoodFactsClient.java) | the declarative client: an interface, no implementation |
| 4 | [`config/HttpClientConfig.java`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/config/HttpClientConfig.java) + [`application.properties`](pizzastore-with-validation/src/main/resources/application.properties) | lets Spring generate the client, with base URL, `User-Agent` and timeouts |
| 5 | [`dto/request/ImportNutritionRequest.java`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/dto/request/ImportNutritionRequest.java) | the request body, with a barcode constraint |
| 6 | [`exception/ExternalServiceException.java`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/exception/ExternalServiceException.java) + [`GlobalExceptionHandler`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/exception/GlobalExceptionHandler.java) | "the other side failed" → `502 Bad Gateway` |
| 7 | [`service/PizzaService.java`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/service/PizzaService.java) | new method `updateNutritionalInfo` |
| 8 | [`service/NutritionImportService.java`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/service/NutritionImportService.java) | calls the client, checks the answer, translates the failures |
| 9 | [`controller/PizzaController.java`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/controller/PizzaController.java) | the endpoint |

### Step 1: The Dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-restclient</artifactId>
</dependency>
```

Spring Boot 4 has its own starter for **outgoing** HTTP calls: `RestClient` (Spring's synchronous HTTP client, the successor of `RestTemplate`) plus the auto-configuration for declarative clients. `spring-boot-starter-webmvc` only covers the incoming side.

### Step 2: A Record for the Response

What Open Food Facts returns for `GET /api/v2/product/3017620422003.json?fields=code,product_name,nutriments` looks like this (shortened, the real `nutriments` object has more than 50 fields):

```json
{
  "code": "3017620422003",
  "status": 1,
  "status_verbose": "product found",
  "product": {
    "product_name": "Nutella",
    "nutriments": {
      "energy-kcal_100g": 539,
      "proteins_100g": 6.3,
      "carbohydrates_100g": 57.5,
      "fat_100g": 30.9,
      "salt_100g": 0.107,
      ...
    }
  }
}
```

We model only what we need, as nested records ([`OpenFoodFactsResponse`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/client/OpenFoodFactsResponse.java)):

```java
public record OpenFoodFactsResponse(String code, Integer status, Product product) {

    public record Product(@JsonProperty("product_name") String productName, Nutriments nutriments) { }

    public record Nutriments(
            @JsonProperty("energy-kcal_100g") BigDecimal energyKcal100g,
            @JsonProperty("proteins_100g") BigDecimal proteins100g,
            @JsonProperty("carbohydrates_100g") BigDecimal carbohydrates100g,
            @JsonProperty("fat_100g") BigDecimal fat100g) { }
}
```

- Jackson **ignores the JSON fields that have no record component** (`status_verbose`, `salt_100g`, ...), so the record stays small and does not break when Open Food Facts adds fields.
- `@JsonProperty` maps names that are not valid Java identifiers (`energy-kcal_100g`) or that don't follow Java naming (`product_name`).
- This record is a DTO of *someone else's* API. It lives in the `client` package and never leaves the service layer: our own clients keep seeing `PizzaResponse`.

### Step 3: The Declarative Client

*Pro Spring Boot 4* (Chapter 3, *Declarative HTTP Service Clients*) shows the zero-implementation way to call another API: write an **interface**, annotate it like a controller, and let Spring generate the implementation ([`OpenFoodFactsClient`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/client/OpenFoodFactsClient.java)):

```java
@HttpExchange(url = "/api/v2", accept = "application/json")
public interface OpenFoodFactsClient {

    @GetExchange("/product/{barcode}.json?fields=code,product_name,nutriments")
    OpenFoodFactsResponse getProduct(@PathVariable String barcode);
}
```

| Annotation | On | Means |
|------------|----|-------|
| `@HttpExchange(url = "/api/v2", accept = ...)` | the interface | common path and `Accept` header for every method |
| `@GetExchange("/product/{barcode}.json?...")` | a method | send a `GET` to this path (also `@PostExchange`, `@PutExchange`, `@DeleteExchange`, ...) |
| `@PathVariable` | a parameter | fill `{barcode}` with the argument, URL-encoded |
| return type | the method | the JSON body is converted to `OpenFoodFactsResponse` with Jackson |

It reads like a `@RestController` turned inside out: the same annotations describe an **outgoing** request instead of an incoming one. Calling `getProduct("3017620422003")` sends `GET https://world.openfoodfacts.org/api/v2/product/3017620422003.json?fields=code,product_name,nutriments`. The `fields` parameter asks Open Food Facts to send only those three fields, instead of a product document of tens of kilobytes.

What you do **not** write: building the URL, sending the request, checking the status code, parsing JSON. A `4xx`/`5xx` answer arrives as an exception (`HttpClientErrorException`, `HttpServerErrorException`), a network problem as `ResourceAccessException`; all of them extend `RestClientException`. Step 8 uses that.

### Step 4: Registering the Client and Configuring the Connection

An interface alone is not a bean. [`HttpClientConfig`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/config/HttpClientConfig.java) tells Spring Boot to generate one:

```java
@Configuration
@ImportHttpServices(group = "openfoodfacts", types = OpenFoodFactsClient.class)
public class HttpClientConfig { }
```

`@ImportHttpServices` creates a proxy that implements `OpenFoodFactsClient` on top of a `RestClient`, and registers it as a bean you can inject anywhere. The **group** name `openfoodfacts` links the client to its configuration in `application.properties`:

```properties
spring.http.serviceclient.openfoodfacts.base-url=https://world.openfoodfacts.org
spring.http.serviceclient.openfoodfacts.default-header.User-Agent=PizzaStore/1.0 (pizzastore-course@example.com)
spring.http.serviceclient.openfoodfacts.connect-timeout=2s
spring.http.serviceclient.openfoodfacts.read-timeout=5s
```

| Property | Why |
|----------|-----|
| `base-url` | Put in front of the `@HttpExchange` paths. Outside the code, so a test or another environment can point the client elsewhere (the book hard-codes the URL in `@HttpExchange` and advises to externalize it, which is what this does). |
| `default-header.User-Agent` | **Required by Open Food Facts**, in the form `AppName/Version (contact)`. Anonymous clients risk being blocked. Use your own contact address in a real project. |
| `connect-timeout`, `read-timeout` | **Not optional.** Without timeouts a hanging server keeps the request thread waiting indefinitely, and a handful of slow calls can use up all of Tomcat's threads. 5 seconds is generous for a lookup. |

### Step 5: Validating the Barcode

The barcode comes from our own client, so it is validated like any other request body ([`ImportNutritionRequest`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/dto/request/ImportNutritionRequest.java)):

```java
public record ImportNutritionRequest(

        @NotBlank(message = "Barcode is required")
        @Pattern(regexp = "\\d{8}|\\d{12,14}", message = "Barcode must be an EAN-8, UPC-A, EAN-13 or GTIN-14 number (digits only)")
        String barcode
) { }
```

Rejecting `"abc"` with a `400` costs nothing; sending it to Open Food Facts would waste a request of a **rate limit of 15 product lookups per minute per IP address**. Validating before calling out is the same idea as Part 1, with an extra reason.

### Step 6: A New Exception for "The Other Side Failed"

Every failure so far was either the client's fault (`400`, `404`, `409`, `422`) or a bug of ours (`500`). A broken external service is neither, so it gets its own exception and status code:

```java
/** An API that PizzaStore depends on failed or could not be reached: not the client's fault, so 502 Bad Gateway. */
public class ExternalServiceException extends PizzaStoreException {

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

```java
@ExceptionHandler(ExternalServiceException.class)
public ProblemDetail handleExternalServiceException(ExternalServiceException ex, WebRequest request) {
    log.warn("External service failure: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.BAD_GATEWAY, ex.getMessage(), "external-service", request);
}
```

**`502 Bad Gateway`** means "the server, while acting as a gateway or proxy, received an invalid response from an upstream server". Why not `500`? A `500` says *our* code is broken and should alarm the developers; a `502` says the problem is upstream, so a client can simply try again later. The cause (timeout, `503`, ...) is kept in the exception for our logs, but the client only sees the generic message: never forward someone else's error page or host names.

### Step 7: Storing the Values

[`PizzaService`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/service/PizzaService.java) gets one method that creates or overwrites a pizza's `NutritionalInfo`:

```java
public PizzaResponse updateNutritionalInfo(Long id, NutritionalInfoRequest request) {
    Pizza pizza = pizzaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Pizza", id));

    NutritionalInfo info = pizza.getNutritionalInfo();
    if (info == null) {                      // first import: create the row and link both sides
        info = new NutritionalInfo();
        info.setPizza(pizza);
        pizza.setNutritionalInfo(info);
    }
    info.setCalories(request.calories());    // later imports: update the existing row in place
    info.setProtein(request.protein());
    info.setCarbohydrates(request.carbohydrates());
    info.setFat(request.fat());

    return pizzaMapper.toResponse(pizzaRepository.save(pizza));
}
```

It takes the existing `NutritionalInfoRequest`, so it knows nothing about Open Food Facts: the service layer stays independent of where the numbers come from. Updating the existing row instead of replacing the object avoids deleting and re-inserting it (`orphanRemoval` on `Pizza.nutritionalInfo`).

### Step 8: The Import Service

[`NutritionImportService`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/service/NutritionImportService.java) puts it together:

```java
@Service
public class NutritionImportService {

    private final OpenFoodFactsClient openFoodFactsClient;   // the generated proxy, injected like any bean
    private final PizzaService pizzaService;
    ...

    public PizzaResponse importFromBarcode(Long pizzaId, String barcode) {
        pizzaService.findById(pizzaId);   // 404 for an unknown pizza BEFORE we spend a request of our limited quota

        OpenFoodFactsResponse response = fetchProduct(barcode);
        NutritionalInfoRequest nutrition = toNutritionalInfo(barcode, response);

        return pizzaService.updateNutritionalInfo(pizzaId, nutrition);
    }
}
```

**Calling the client** and turning its exceptions into ours:

```java
private OpenFoodFactsResponse fetchProduct(String barcode) {
    try {
        return openFoodFactsClient.getProduct(barcode);
    } catch (HttpClientErrorException.NotFound e) {
        throw unknownBarcode(barcode);                                   // 422
    } catch (RestClientException e) {
        // 5xx, 429 (rate limit), timeouts, connection refused, unreadable body, ...
        log.error("Open Food Facts call failed for barcode {}", barcode, e);
        throw new ExternalServiceException("Open Food Facts is currently unavailable, please try again later", e);   // 502
    }
}
```

The order of the `catch` blocks matters: `HttpClientErrorException.NotFound` is a subclass of `RestClientException`, so the specific case comes first.

**Checking the answer**, because a successful HTTP call does not mean usable data:

```java
private NutritionalInfoRequest toNutritionalInfo(String barcode, OpenFoodFactsResponse response) {
    // Open Food Facts can answer HTTP 200 with status 0 for an unknown barcode
    if (response == null || response.product() == null || Integer.valueOf(0).equals(response.status())) {
        throw unknownBarcode(barcode);
    }
    OpenFoodFactsResponse.Nutriments n = response.product().nutriments();
    if (n == null || n.energyKcal100g() == null || n.proteins100g() == null
            || n.carbohydrates100g() == null || n.fat100g() == null) {
        throw new BusinessException("Open Food Facts has no complete nutritional data (kcal, protein, carbohydrates, fat) for barcode " + barcode);
    }
    return new NutritionalInfoRequest(
            n.energyKcal100g().setScale(0, RoundingMode.HALF_UP).intValueExact(),   // the calories column is an Integer
            scale(n.proteins100g()), scale(n.carbohydrates100g()), scale(n.fat100g()));
}
```

Two design decisions to remember:

1. **`NutritionImportService` is not `@Transactional`** (unlike `PizzaService`, which is `@Transactional` on the class). A transaction holds a database connection for as long as it is open. Keeping one open while we wait up to 5 seconds for another server would let a slow Open Food Facts drain the connection pool and block requests that have nothing to do with nutrition. The database work happens in two short transactions, `findById` before and `updateNutritionalInfo` after the call.
2. **Never trust external data.** It can be missing, incomplete or in another format than your columns: values are rounded (`538.6` kcal becomes `539`), and an incomplete product is refused instead of stored half-filled.

### Step 9: The Endpoint

[`PizzaController`](pizzastore-with-validation/src/main/java/be/vives/pizzastore/controller/PizzaController.java) gets `NutritionImportService` as a second constructor argument, and one method:

```java
@PostMapping("/{id}/nutritional-info/import")
public ResponseEntity<PizzaResponse> importNutritionalInfo(
        @PathVariable Long id,
        @Valid @RequestBody ImportNutritionRequest request) {

    PizzaResponse updated = nutritionImportService.importFromBarcode(id, request.barcode());
    return ResponseEntity.ok(updated);
}
```

Why `POST` and not `PUT`? The client does not send the new state of the resource (that would be a `PUT /api/pizzas/1` with the numbers); it asks the server to **perform an operation** that fetches and stores data, and doing it twice may give a different result if Open Food Facts changed the product in between. Lesson 8 asks for nouns in URIs; an operation that is not a plain create/read/update/delete is the accepted exception, modelled as a sub-resource of the pizza and sent with `POST`, just like the image upload `POST /api/pizzas/{id}/image` of Lesson 9.

### Everything That Can Go Wrong

| Situation | Example barcode | Detected by | PizzaStore answers |
|-----------|-----------------|-------------|--------------------|
| Not a barcode | `abc` | `@Pattern` (step 5) | `400` Validation failed, the service is never called |
| Unknown pizza | pizza `999` | `pizzaService.findById` (step 8) | `404`, Open Food Facts is never called |
| Unknown barcode, HTTP 404 | `5412345678901` | `catch NotFound` (step 8) | `422` "Open Food Facts does not know a product with barcode ..." |
| Unknown barcode, **HTTP 200 with `"status": 0`** | `0000000000017` | status check (step 8) | `422`, same message |
| Product exists, but has no nutrition data | `1234567890128` | completeness check (step 8) | `422` "Open Food Facts has no complete nutritional data ..." |
| Open Food Facts down (`5xx`), rate limit (`429`), timeout, no network | — | `catch RestClientException` (step 8) | `502 Bad Gateway` "Open Food Facts is currently unavailable, please try again later" |

The rows with the **unknown barcode** show why you must read the documentation *and* try the real API: Open Food Facts answers some unknown barcodes with a `404` and others with `200` plus `"status": 0` in the body. A client that only checks the HTTP status would store `null` values.

Why `422` and not `404` for an unknown barcode? The URL `/api/pizzas/1/nutritional-info/import` exists and the request is valid; it just can't be carried out with this data. That's the rule from [400, 404 or 422?](#400-404-or-422).

### Trying It Out

Start the application and import the data of barcode `3017620422003` into pizza 1 (needs internet access):

```bash
curl -X POST http://localhost:8080/api/pizzas/1/nutritional-info/import \
  -H "Content-Type: application/json" -d '{"barcode":"3017620422003"}'
```

Then try the barcodes of the table above. To see the `502`, start the application with a base URL where nothing listens:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.http.serviceclient.openfoodfacts.base-url=http://localhost:1
```

How to test this client **without** calling the real server is part of [Lesson 11](../lesson-11-testing/README.md) (`@RestClientTest` and `MockRestServiceServer`).

> **Terms of use.** Open Food Facts data is available under the Open Database License (ODbL) and is crowd-sourced without guarantees of accuracy. For anything bigger than a course project, read [their API documentation](https://openfoodfacts.github.io/openfoodfacts-server/api/): use the staging server `world.openfoodfacts.net` while developing, and download their data dumps instead of calling the API for bulk use.

---

## 🎯 Choosing the Right Status Code

| Status | Meaning | PizzaStore example |
|--------|---------|--------------------|
| **400** Bad Request | The request itself is malformed or invalid | Validation failed, malformed JSON, `abc` as ID, file isn't an image |
| **401** Unauthorized | Not authenticated | Lesson 12 |
| **403** Forbidden | Authenticated, but not allowed | Lesson 12 |
| **404** Not Found | The resource in the URL doesn't exist | `GET /api/pizzas/999` |
| **405** Method Not Allowed | The URL exists, the method doesn't | `PUT /api/pizzas` |
| **409** Conflict | Conflicts with the current state of the data | Email already exists, pizza still referenced |
| **413** Content Too Large | Request body too large | Uploading a 6 MB image |
| **415** Unsupported Media Type | Body format not supported | `Content-Type: text/plain` |
| **422** Unprocessable Content | Well-formed and valid, but breaks a business rule | Unavailable pizza, cancelling a delivered order, unknown customer in a new order |
| **500** Internal Server Error | A bug or an outage on our side | Anything unexpected |
| **502** Bad Gateway | An API we depend on failed | Open Food Facts is down or too slow ([Part 3](#-part-3-calling-an-external-api)) |

### 400, 404 or 422?

These three are the ones most often mixed up. The HTTP specification (RFC 9110) defines them as follows:

| Status | RFC 9110 in short | The question it answers |
|--------|-------------------|-------------------------|
| **400** Bad Request | The server can't or won't process the request because of something it perceives as a client error, e.g. malformed syntax | Is the request wrong **on its own**? |
| **404** Not Found | The server did not find a current representation for the **target resource** — the resource identified by the URL | Does the thing **in the URL** exist? |
| **422** Unprocessable Content | The server understands the content type and the syntax is correct, but it was unable to process the contained instructions | Is the request correct, but **impossible to carry out given the current data**? |

Ask the questions in this order:

1. **Does the resource in the URL exist?** No → **404**. `GET /api/pizzas/999`, `DELETE /api/orders/999`, `POST /api/customers/999/favorites/1`.
2. **Is the request wrong on its own** — can you tell without looking at the database? Yes → **400**. Malformed JSON, `abc` as an ID, an empty name, a negative price, `quantity: 0`. Bean Validation catches exactly this category.
3. **Is the request valid, but can it not be carried out given the current data?** → **422**. An unavailable pizza, cancelling a delivered order, an order for a customer that doesn't exist.

The third example is the one people most often get wrong. `POST /api/orders` with `"customerId": 999` is **not a 404**: the target resource is `/api/orders`, and that exists. Answering 404 would tell the client "this endpoint doesn't exist" — misleading, and a 404 may even be cached by clients and proxies. The ID that doesn't exist is *part of the content*, so the content can't be processed: 422. Rule of thumb: **404 only for IDs in the path; an invalid reference in the body is a 422** (or a 400 — some APIs treat an unknown reference as invalid input; that's defensible too, as long as you're consistent).

The detail of the problem (`"Customer with id 999 not found"`) tells the client *which* reference was wrong; the status tells it *what kind* of problem it is.

> 422 comes from WebDAV (RFC 4918) and became part of core HTTP in RFC 9110 (2022), which renamed it from "Unprocessable Entity" to "Unprocessable Content". Spring Framework 7 follows that with `HttpStatus.UNPROCESSABLE_CONTENT`; the old constant `UNPROCESSABLE_ENTITY` is deprecated.

**409 or 422?** Both are about the current state of the data. PizzaStore uses **409 Conflict** when the request collides with *another existing resource* (an email that's already taken, a pizza that other data still refers to) and **422** when a *business rule* forbids it (the order is already delivered).

**4xx or 5xx?** 4xx means "the client should change the request", 5xx means "the client did nothing wrong". Lesson 9's biggest problem was client mistakes showing up as 500s; a well-behaved API only returns a 500 for real server problems.

---

## 🔁 Before and After

The same requests as at the start of this lesson, now against `pizzastore-with-validation` (all verified with `curl` against the running project):

| Request | Lesson 9 | Lesson 10 |
|---------|----------|-----------|
| `GET /api/pizzas/999` | `404`, empty body | `404` "Pizza with id 999 not found" |
| `GET /api/pizzas/abc` | `400`, default body | `400` "Failed to convert 'id' with value: 'abc'" |
| `POST /api/pizzas` with `{ "name": "", "price": -5 }` | `500` | `400` + three field errors |
| `POST /api/pizzas` with malformed JSON | `400`, default body | `400` "Malformed JSON request" |
| `PUT /api/pizzas/1` with `{ "price": 12 }` | `500` | `400` + field errors for `name` and `available` |
| `PATCH /api/orders/1/status` with `{ "status": "BAKING" }` | `400`, default body | `400` + the allowed status values |
| `POST /api/orders` with `customerId` 999 | `500` | `422` "Customer with id 999 not found" |
| `POST /api/orders` with an unavailable pizza | `201` (order accepted!) | `422` "Pizza 'Margherita' is currently not available" |
| `POST /api/orders` with `quantity: 0` | `201` | `400` field `orderLines[0].quantity` |
| `DELETE /api/orders/1` (delivered) | `204` | `422` "Cannot cancel order with status DELIVERED" |
| `POST /api/customers` with an existing email | `500` (unique constraint) | `409` "Customer with email ... already exists" |
| `DELETE /api/pizzas/1` (a customer's favorite) | `500` | `409`, SQL error hidden |
| Upload a `.txt` file | `400`, empty body | `400` "Only JPG, JPEG, and PNG files are allowed" |
| Upload a 6 MB image | `413`, default body | `413` "Maximum upload size exceeded" |

Every error response is now an `application/problem+json` body.

---

## 💡 Best Practices

1. **Fail fast.** Validate request DTOs with `@Valid` so invalid input never reaches a service or the database.
2. **Validate DTOs, not entities.** Each endpoint's DTO carries the rules for that endpoint (create vs update).
3. **Always write a message.** `"Price must be positive"` beats `"must be greater than or equal to 0.01"`.
4. **Combine `@NotNull`/`@NotBlank` with the other constraints** for required fields; everything else accepts `null`.
5. **Constraints for the shape, exceptions for the rules.** Anything that needs the database or the current state is a business rule in the service.
6. **Throw specific exceptions.** The book's advice: catch specific exceptions "rather than a generic `Exception` or `RuntimeException`" — and throw them. That's what lets the handler choose between 404, 409 and 422.
7. **Handle exceptions in one place** with `@RestControllerAdvice`, not with `try/catch` in controllers.
8. **One error format for everything.** Use `ProblemDetail`, and extend `ResponseEntityExceptionHandler` so Spring's own exceptions use it too.
9. **Don't leak internals.** No stack traces, SQL, table or constraint names in `detail`; log them instead.
10. **Log 4xx at `WARN`, 5xx at `ERROR`.**

---

## 🎓 Summary

### What We Learned

1. **Jakarta Bean Validation**
   - `spring-boot-starter-validation` + constraint annotations on request DTOs
   - `@Valid` on `@RequestBody`, and on fields to cascade into nested objects and lists
   - `null` is valid for all constraints except `@NotNull`/`@NotEmpty`/`@NotBlank`
   - A PUT DTO requires the same fields as the create DTO
   - Method validation for `@PathVariable`/`@RequestParam`, and custom constraints

2. **Exceptions in the service layer**
   - `PizzaStoreException` with `ResourceNotFoundException`, `DuplicateResourceException`, `BusinessException` and `InvalidFileException`
   - Services throw, controllers only describe the happy path
   - Business rules: unavailable pizzas, cancelling delivered orders, duplicate emails

3. **Global exception handling**
   - `@RestControllerAdvice` + `@ExceptionHandler`; the most specific handler wins
   - `ResponseEntityExceptionHandler` for Spring MVC's own exceptions; override, don't redeclare
   - `DataIntegrityViolationException` → 409 without leaking SQL; a catch-all 500

4. **Problem Details (RFC 7807)**
   - `type`, `title`, `status`, `detail`, `instance` and extensions like `errors`
   - `application/problem+json`

5. **Calling an external API**
   - A declarative client: `@HttpExchange`/`@GetExchange` interface + `@ImportHttpServices`
   - Base URL, `User-Agent` and timeouts in `spring.http.serviceclient.<group>.*`
   - Failures of the other side: `422` for unusable data, `502` for outages, never a hanging request
   - No database transaction around a remote call

### Key Takeaways

- ✅ **Invalid input → 400 before your code runs**
- 🎯 **Every error has the right status code: 400, 404, 409, 422, 500 or 502**
- 🌐 **An external API will fail: set timeouts and decide what the client sees**
- 📄 **Every error body is a `ProblemDetail`**
- 🛡️ **Never expose internal details**

### What's Next?

PizzaStore now behaves correctly for valid *and* invalid requests — but we've only checked that by hand with `curl`. [Lesson 11](../lesson-11-testing/README.md) turns these checks into automated tests.

---

## 📖 Additional Resources

- [Jakarta Validation 3.1 Specification](https://jakarta.ee/specifications/bean-validation/3.1/)
- [Hibernate Validator Reference Guide](https://docs.jboss.org/hibernate/stable/validator/reference/en-US/html_single/)
- [Spring Boot Reference: Validation](https://docs.spring.io/spring-boot/reference/io/validation.html)
- [Spring Framework Reference: Validation in Spring MVC](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html)
- [Spring Framework Reference: Exceptions (`@ExceptionHandler`)](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html)
- [Spring Framework Reference: Error Responses (`ProblemDetail`)](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
- [RFC 9457: Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457) (replaces RFC 7807)

---

**Note on the book**: *Pro Spring Boot 4* covers this lesson in Chapter 3. Part 3 follows its *Declarative HTTP Service Clients* section (`@HttpExchange`, `@GetExchange`, `@ImportHttpServices`), applied to Open Food Facts instead of an exchange-rate API; the book hard-codes the URL and only mentions timeouts in passing, while this course puts the connection settings in `spring.http.serviceclient.<group>.*` and shows how the failures of the other side surface as `422`/`502`. *Validating the Domain Model* (inside *Implementing Full CRUD*) adds `spring-boot-starter-validation`, puts `@NotBlank`, `@Email` and `@Pattern` on the `Customer` record, and gives the three best practices quoted above: fail fast, specific messages, and validation on DTOs rather than entities. *The Complete CustomerController* triggers it with `@Valid @RequestBody` and explains the resulting `MethodArgumentNotValidException`. *Global Exception Handling with @ControllerAdvice* (inside *Exception Handling and Content Negotiation*) is the counterpart of `GlobalExceptionHandler`: `@RestControllerAdvice`, `@ExceptionHandler`, `ProblemDetail` with a custom `type` and an `errors` extension, the Problem Detail flow of Figure 3-2, and the best practices "Standardize Errors", "Don't Leak Internals" and "Use Specific Exceptions". Chapter 3's versioning example returns to the same handler for its strict v2 API, Chapter 6 (Listing 6-4) uses `ResponseStatusException` for a 404, Chapter 9 shows the WebFlux variant (`WebExchangeBindException`), and Chapter 18 lists *First-Class Problem Details Support* among the Spring Boot 4 highlights, with the `ErrorResponse.builder` example. Where PizzaStore goes further than the book: a custom exception hierarchy thrown from the service layer, separate create/update DTOs with nested (`@Valid`) validation, and `ResponseEntityExceptionHandler` for Spring's own exceptions. The book's error status codes are limited to **400** (validation), **404** (a resource that doesn't exist, always identified by the URL) and **500** (unexpected errors); **409 Conflict and 422 Unprocessable Content don't appear anywhere in the book**, and its examples never reference another resource from a request body. The 409/422 distinction and the "404 only for IDs in the path" rule in [400, 404 or 422?](#400-404-or-422) are this course's addition, based on the HTTP specification (RFC 9110) rather than on the book. Note that, despite the book's wording, Spring Boot 4 does **not** return problem details by default: without your own handler (or `spring.mvc.problemdetails.enabled=true`) errors still get the `{ timestamp, status, error, path }` body.

---

## 🚀 Runnable Project

**[`pizzastore-with-validation/`](pizzastore-with-validation)** is Lesson 9's [`pizzastore-complete-api`](../lesson-09-complete-rest-api/pizzastore-complete-api) plus the changes listed in [What This Lesson Adds to PizzaStore](#-what-this-lesson-adds-to-pizzastore). Its request DTOs, `exception` package, services and controllers are the same as in the final PizzaStore, minus the security handler (Lesson 12) and the OpenAPI annotations (Lesson 13).

The project includes:
- ✅ **Spring Boot 4.0** on **Java 25** (`spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, H2, MapStruct 1.6.3)
- ✅ Everything from Lessons 6a, 7 and 9: domain model, repositories, DTOs, mappers, services, complete REST API
- ✅ Jakarta Bean Validation on all request DTOs, including nested objects and lists
- ✅ A custom exception hierarchy and business rules in the services
- ✅ A global `@RestControllerAdvice` returning RFC 7807 `ProblemDetail` for every error
- ✅ A declarative `@HttpExchange` client for Open Food Facts (needs internet access to try it out)
- ❌ No automated tests yet — Lesson 11
- ❌ No security — every endpoint is open until Lesson 12

### Running It

```bash
cd pizzastore-with-validation
mvn spring-boot:run
```

Then provoke some errors:

```bash
# 400 - validation errors
curl -X POST http://localhost:8080/api/pizzas \
  -H "Content-Type: application/json" -d '{"name":"","price":-5}'

# 404 - unknown pizza
curl http://localhost:8080/api/pizzas/999

# 422 - business rule: order 1 is already delivered
curl -X DELETE http://localhost:8080/api/orders/1

# 200 - fill the nutritional info of pizza 1 with the data of Open Food Facts product 3017620422003 (needs internet)
curl -X POST http://localhost:8080/api/pizzas/1/nutritional-info/import \
  -H "Content-Type: application/json" -d '{"barcode":"3017620422003"}'

# 400 - not a barcode (the 404, 422 and 502 cases of this endpoint are listed in Part 3)
curl -X POST http://localhost:8080/api/pizzas/1/nutritional-info/import \
  -H "Content-Type: application/json" -d '{"barcode":"abc"}'

# 409 - email already in use
curl -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{"name":"Emma","email":"emma.johnson@example.com","password":"password123"}'
```

The H2 console is available at http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:pizzastore_validation`, user `sa`, no password).

---

**Congratulations!** 🎉 PizzaStore now rejects invalid input and explains every error in a standard format. Continue to [Lesson 11: Testing](../lesson-11-testing/README.md) to prove it with automated tests.
