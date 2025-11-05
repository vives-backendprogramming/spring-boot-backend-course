# PizzaStore with Validation & Exception Handling

Extends Lesson 8 project with **Bean Validation** and **Global Exception Handling**.

## What's New?

- ✅ Bean Validation on all DTOs (@NotBlank, @Size, @Email, @Pattern, etc.)
- ✅ Global Exception Handler (@RestControllerAdvice)
- ✅ Custom Exceptions (ResourceNotFoundException, DuplicateResourceException, BusinessException)
- ✅ Consistent ErrorResponse DTO
- ✅ Proper HTTP Status Codes (400, 404, 409, 422, 500)
- ✅ Email uniqueness validation

## Run the Project

```bash
cd pizzastore-with-validation
mvn clean spring-boot:run
```

## Test Validation

### Invalid Pizza (400 Bad Request)
```bash
curl -X POST http://localhost:8080/api/pizzas \
  -H "Content-Type: application/json" \
  -d '{"name":"A","price":-5,"description":"x"}'
```

### Resource Not Found (404)
```bash
curl http://localhost:8080/api/pizzas/999
```

### Duplicate Email (409 Conflict)
```bash
curl -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"john@example.com","phone":"+32471234567","address":"Street 1, City"}'
```

## Error Response Format

```json
{
  "timestamp": "2024-11-05T20:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/pizzas",
  "validationErrors": [
    {"field": "name", "message": "Name must be between 2 and 100 characters"},
    {"field": "price", "message": "Price must be at least €0.01"}
  ]
}
```

See [Lesson 10 README](../README.md) for complete documentation.
