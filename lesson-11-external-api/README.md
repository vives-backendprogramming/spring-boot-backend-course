# Lesson 11: Calling an External API

**From Answering Requests to Making Them: a Declarative `@HttpExchange` Client for Open Food Facts, with Timeouts and Clean Error Handling**

---

## 📋 Learning Objectives

By the end of this lesson, you will be able to:
- Explain why a back-end calls other APIs, and what changes when your API depends on somebody else's
- Compare Spring's HTTP clients (`RestTemplate`, `RestClient`, `WebClient`, HTTP service interfaces) and choose one
- Map the JSON of an external API onto a small set of Java records, ignoring what you don't need
- Write a declarative HTTP client: an `@HttpExchange`/`@GetExchange` interface without an implementation
- Register it with `@ImportHttpServices` and configure base URL, headers and timeouts in `spring.http.serviceclient.<group>.*`
- Validate input *before* spending a request of a rate-limited API
- Translate the failures of the other side into your own exceptions: `422` for unusable data, `502 Bad Gateway` for an outage
- Explain why there must be no database transaction around a remote call
- Design an endpoint for an operation that is not plain CRUD (`POST /api/pizzas/{id}/nutritional-info/import`)

---

## 📚 Table of Contents

1. [Recap: Where Lesson 10 Left Us](#-recap-where-lesson-10-left-us)
2. [What This Lesson Adds to PizzaStore](#-what-this-lesson-adds-to-pizzastore)
3. [HTTP Clients in Spring](#-http-clients-in-spring)
4. [The Feature: Importing Nutrition Data](#-the-feature-importing-nutrition-data)
5. [Building the Client, Step by Step](#%EF%B8%8F-building-the-client-step-by-step)
6. [Everything That Can Go Wrong](#-everything-that-can-go-wrong)
7. [Trying It Out](#-trying-it-out)
8. [Best Practices](#-best-practices)
9. [Summary](#-summary)
10. [Additional Resources](#-additional-resources)
11. [Runnable Project](#-runnable-project)

---

## 🔄 Recap: Where Lesson 10 Left Us

After [Lesson 10](../lesson-10-validation-exception-handling/README.md) PizzaStore rejects invalid input with a `400`, answers every other error with the right status code (`404`, `409`, `422`, `500`) and always uses the same RFC 7807 `ProblemDetail` body. Every failure so far had one of two causes: **the client sent something wrong**, or **our own code has a bug**.

Until now PizzaStore only *answered* requests. Real back-ends also *make* them: a payment provider, an address lookup, a nutrition database. This lesson adds PizzaStore's first outgoing call, to the free [Open Food Facts](https://world.openfoodfacts.org) database. It also brings a third cause of failure: **the other side can be down, slow or return unusable data, and that must not break your API**. Lesson 10's exception handling is exactly what we need to deal with it.

---

## 🧱 What This Lesson Adds to PizzaStore

```
                 ┌──────────────────────────────┐
  HTTP request → │ @Valid ImportNutritionRequest│ ← NEW: invalid barcode → 400, no outgoing call
                 ├──────────────────────────────┤
                 │   PizzaController            │ ← NEW endpoint: POST /api/pizzas/{id}/nutritional-info/import
                 ├──────────────────────────────┤
                 │   NutritionImportService     │ ← NEW: no @Transactional, translates the failures
                 │        │            │        │
                 │        ▼            ▼        │
                 │   PizzaService  OpenFoodFacts│ ← NEW: declarative @HttpExchange client
                 │        │          Client ────┼──────► https://world.openfoodfacts.org
                 ├────────┼─────────────────────┤
                 │   Repository / Domain        │
                 └──────────────────────────────┘
                        │ any exception
                        ▼
                 ┌──────────────────────────────┐
                 │ GlobalExceptionHandler       │ ← NEW: ExternalServiceException → 502 Bad Gateway
                 └──────────────────────────────┘
```

The project in this lesson, [`pizzastore-with-external-api`](pizzastore-with-external-api), is **Lesson 10's [`pizzastore-with-validation`](../lesson-10-validation-exception-handling/pizzastore-with-validation) plus exactly these changes**:

| Added / changed | What it does |
|-----------------|--------------|
| [`pom.xml`](pizzastore-with-external-api/pom.xml) | Adds `spring-boot-starter-restclient` |
| [`client/`](pizzastore-with-external-api/src/main/java/be/vives/pizzastore/client) (new package) | `OpenFoodFactsClient` (the declarative client) and `OpenFoodFactsResponse` (records for its JSON) |
| [`config/HttpClientConfig.java`](pizzastore-with-external-api/src/main/java/be/vives/pizzastore/config/HttpClientConfig.java) (new) + [`application.properties`](pizzastore-with-external-api/src/main/resources/application.properties) | Registers the client; base URL, `User-Agent` and timeouts in `spring.http.serviceclient.openfoodfacts.*` |
| [`dto/request/ImportNutritionRequest.java`](pizzastore-with-external-api/src/main/java/be/vives/pizzastore/dto/request/ImportNutritionRequest.java) (new) | The request body, with a barcode constraint |
| [`exception/ExternalServiceException.java`](pizzastore-with-external-api/src/main/java/be/vives/pizzastore/exception/ExternalServiceException.java) (new) + [`GlobalExceptionHandler`](pizzastore-with-external-api/src/main/java/be/vives/pizzastore/exception/GlobalExceptionHandler.java) | "The other side failed" → `502 Bad Gateway` |
| [`service/PizzaService.java`](pizzastore-with-external-api/src/main/java/be/vives/pizzastore/service/PizzaService.java) | New method `updateNutritionalInfo` |
| [`service/NutritionImportService.java`](pizzastore-with-external-api/src/main/java/be/vives/pizzastore/service/NutritionImportService.java) (new) | Calls the client, checks the answer, translates the failures |
| [`controller/PizzaController.java`](pizzastore-with-external-api/src/main/java/be/vives/pizzastore/controller/PizzaController.java) | New endpoint `POST /api/pizzas/{id}/nutritional-info/import` |

Everything else (`domain`, `repository`, `mapper`, the other DTOs, services and controllers, `data.sql`) is **unchanged** from Lesson 10.

---

## 🔌 HTTP Clients in Spring

A controller handles **incoming** requests; an HTTP client sends **outgoing** ones. Spring has four ways to do the latter:

| Client | Since | Style | Use it? |
|--------|-------|-------|---------|
| `RestTemplate` | Spring 3 (2009) | Synchronous, one method per HTTP verb (`getForObject`, `postForEntity`, ...) | Not for new code: in maintenance mode, and the Spring team has announced its deprecation. You will still see it in older projects and tutorials. |
| `WebClient` | Spring 5 (2017) | Reactive, fluent, non-blocking (`Mono`/`Flux`) | For reactive (WebFlux) applications. Needs `spring-boot-starter-webflux`. |
| `RestClient` | Spring 6.1 (2023) | Synchronous, fluent, the API of `WebClient` without the reactive types | The default for a Spring MVC application like PizzaStore. |
| **HTTP service interface** (`@HttpExchange`) | Spring 6, registry with `@ImportHttpServices` in Spring 7 | **Declarative**: you write an interface, Spring generates the implementation on top of a `RestClient` (or `WebClient`) | **What this lesson uses.** |

To see what the declarative style saves you, this is the call of this lesson written with a plain `RestClient` (an illustration, not code from the project):

```java
OpenFoodFactsResponse response = restClient.get()
        .uri("/api/v2/product/{barcode}.json?fields=code,product_name,nutriments", barcode)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()                                  // a 4xx/5xx answer becomes an exception
        .body(OpenFoodFactsResponse.class);          // JSON → record with Jackson
```

That's fine for one call. With an HTTP service interface the same request is **described** once, with annotations you already know from your controllers, and every caller just calls a Java method. The interface is also easy to replace by a mock in a unit test (Lesson 12).

---

## 🍕 The Feature: Importing Nutrition Data

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
| 1 | [`pom.xml`](pizzastore-with-external-api/pom.xml) | adds `spring-boot-starter-restclient` |
| 2 | [`client/OpenFoodFactsResponse.java`](pizzastore-with-external-api/src/main/java/be/vives/pizzastore/client/OpenFoodFactsResponse.java) | record for the part of the JSON we use |
| 3 | [`client/OpenFoodFactsClient.java`](pizzastore-with-external-api/src/main/java/be/vives/pizzastore/client/OpenFoodFactsClient.java) | the declarative client: an interface, no implementation |
| 4 | [`config/HttpClientConfig.java`](pizzastore-with-external-api/src/main/java/be/vives/pizzastore/config/HttpClientConfig.java) + [`application.properties`](pizzastore-with-external-api/src/main/resources/application.properties) | lets Spring generate the client, with base URL, `User-Agent` and timeouts |
| 5 | [`dto/request/ImportNutritionRequest.java`](pizzastore-with-external-api/src/main/java/be/vives/pizzastore/dto/request/ImportNutritionRequest.java) | the request body, with a barcode constraint |
| 6 | [`exception/ExternalServiceException.java`](pizzastore-with-external-api/src/main/java/be/vives/pizzastore/exception/ExternalServiceException.java) + [`GlobalExceptionHandler`](pizzastore-with-external-api/src/main/java/be/vives/pizzastore/exception/GlobalExceptionHandler.java) | "the other side failed" → `502 Bad Gateway` |
| 7 | [`service/PizzaService.java`](pizzastore-with-external-api/src/main/java/be/vives/pizzastore/service/PizzaService.java) | new method `updateNutritionalInfo` |
| 8 | [`service/NutritionImportService.java`](pizzastore-with-external-api/src/main/java/be/vives/pizzastore/service/NutritionImportService.java) | calls the client, checks the answer, translates the failures |
| 9 | [`controller/PizzaController.java`](pizzastore-with-external-api/src/main/java/be/vives/pizzastore/controller/PizzaController.java) | the endpoint |

---

## 🛠️ Building the Client, Step by Step

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

We model only what we need, as nested records ([`OpenFoodFactsResponse`](pizzastore-with-external-api/src/main/java/be/vives/pizzastore/client/OpenFoodFactsResponse.java)):

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

*Pro Spring Boot 4* (Chapter 3, *Declarative HTTP Service Clients*) shows the zero-implementation way to call another API: write an **interface**, annotate it like a controller, and let Spring generate the implementation ([`OpenFoodFactsClient`](pizzastore-with-external-api/src/main/java/be/vives/pizzastore/client/OpenFoodFactsClient.java)):

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

An interface alone is not a bean. [`HttpClientConfig`](pizzastore-with-external-api/src/main/java/be/vives/pizzastore/config/HttpClientConfig.java) tells Spring Boot to generate one:

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

The barcode comes from our own client, so it is validated like any other request body ([`ImportNutritionRequest`](pizzastore-with-external-api/src/main/java/be/vives/pizzastore/dto/request/ImportNutritionRequest.java)):

```java
public record ImportNutritionRequest(

        @NotBlank(message = "Barcode is required")
        @Pattern(regexp = "\\d{8}|\\d{12,14}", message = "Barcode must be an EAN-8, UPC-A, EAN-13 or GTIN-14 number (digits only)")
        String barcode
) { }
```

Rejecting `"abc"` with a `400` costs nothing; sending it to Open Food Facts would waste a request of a **rate limit of 15 product lookups per minute per IP address**. Validating before calling out is the same idea as in Lesson 10, with an extra reason.

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

It joins [Lesson 10's exception hierarchy](../lesson-10-validation-exception-handling/README.md#pizzastores-exception-hierarchy), and the `GlobalExceptionHandler` gets one more handler:

```
RuntimeException
└── PizzaStoreException            → 400 Bad Request (fallback)
    ├── ResourceNotFoundException  → 404 Not Found
    ├── DuplicateResourceException → 409 Conflict
    ├── BusinessException          → 422 Unprocessable Content
    ├── ExternalServiceException   → 502 Bad Gateway          ← NEW
    └── InvalidFileException       → 400 Bad Request
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

[`PizzaService`](pizzastore-with-external-api/src/main/java/be/vives/pizzastore/service/PizzaService.java) gets one method that creates or overwrites a pizza's `NutritionalInfo`:

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

[`NutritionImportService`](pizzastore-with-external-api/src/main/java/be/vives/pizzastore/service/NutritionImportService.java) puts it together:

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

[`PizzaController`](pizzastore-with-external-api/src/main/java/be/vives/pizzastore/controller/PizzaController.java) gets `NutritionImportService` as a second constructor argument, and one method:

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

---

## 🚨 Everything That Can Go Wrong

| Situation | Example barcode | Detected by | PizzaStore answers |
|-----------|-----------------|-------------|--------------------|
| Not a barcode | `abc` | `@Pattern` (step 5) | `400` Validation failed, the service is never called |
| Unknown pizza | pizza `999` | `pizzaService.findById` (step 8) | `404`, Open Food Facts is never called |
| Unknown barcode, HTTP 404 | `5412345678901` | `catch NotFound` (step 8) | `422` "Open Food Facts does not know a product with barcode ..." |
| Unknown barcode, **HTTP 200 with `"status": 0`** | `0000000000017` | status check (step 8) | `422`, same message |
| Product exists, but has no nutrition data | `1234567890128` | completeness check (step 8) | `422` "Open Food Facts has no complete nutritional data ..." |
| Open Food Facts down (`5xx`), rate limit (`429`), timeout, no network | — | `catch RestClientException` (step 8) | `502 Bad Gateway` "Open Food Facts is currently unavailable, please try again later" |

The rows with the **unknown barcode** show why you must read the documentation *and* try the real API: Open Food Facts answers some unknown barcodes with a `404` and others with `200` plus `"status": 0` in the body. A client that only checks the HTTP status would store `null` values.

Why `422` and not `404` for an unknown barcode? The URL `/api/pizzas/1/nutritional-info/import` exists and the request is valid; it just can't be carried out with this data. That's the rule from [400, 404 or 422?](../lesson-10-validation-exception-handling/README.md#400-404-or-422) of Lesson 10.

With this lesson the status code overview of [Choosing the Right Status Code](../lesson-10-validation-exception-handling/README.md#-choosing-the-right-status-code) in Lesson 10 is complete: **502 Bad Gateway** is the code for *an API we depend on failed*.

---

## 🧪 Trying It Out

Start the application and import the data of barcode `3017620422003` into pizza 1 (needs internet access):

```bash
curl -X POST http://localhost:8080/api/pizzas/1/nutritional-info/import \
  -H "Content-Type: application/json" -d '{"barcode":"3017620422003"}'
```

Then try the barcodes of the table above. To see the `502`, start the application with a base URL where nothing listens:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.http.serviceclient.openfoodfacts.base-url=http://localhost:1
```

How to test this client **without** calling the real server is part of [Lesson 12](../lesson-12-testing/README.md) (`@RestClientTest` and `MockRestServiceServer`).

> **Terms of use.** Open Food Facts data is available under the Open Database License (ODbL) and is crowd-sourced without guarantees of accuracy. For anything bigger than a course project, read [their API documentation](https://openfoodfacts.github.io/openfoodfacts-server/api/): use the staging server `world.openfoodfacts.net` while developing, and download their data dumps instead of calling the API for bulk use.

---

## 💡 Best Practices

1. **Always set timeouts.** A connect and a read timeout for every external API; without them one slow server can block all your request threads.
2. **Externalize the connection.** Base URL, headers and timeouts belong in `application.properties`, so tests and other environments can point the client elsewhere.
3. **Identify yourself.** Send a `User-Agent` (or API key) the provider asks for; anonymous clients get blocked first.
4. **Validate before you call out.** A `400` costs nothing; a wasted request of a rate-limited API does.
5. **Model only what you use.** Small records for the part of the JSON you need; let Jackson ignore the rest.
6. **Keep someone else's DTOs out of your API.** `OpenFoodFactsResponse` never leaves the service layer; your clients keep seeing your own response DTOs.
7. **Never trust external data.** Check for missing and incomplete values, and convert them to your own types and precision.
8. **Translate their failures into your exceptions.** Catch the specific case first (`HttpClientErrorException.NotFound`), then `RestClientException`; answer `422` for unusable data and `502` for an outage.
9. **No transaction around a remote call.** Do the database work in short transactions before and after the call.
10. **Log the cause, hide it from the client.** The client gets a generic "currently unavailable"; the stack trace with host names and timeouts goes to your logs.

---

## 🎓 Summary

### What We Learned

1. **HTTP clients in Spring**
   - `RestTemplate` (legacy), `WebClient` (reactive), `RestClient` (synchronous, fluent) and declarative HTTP service interfaces
   - `spring-boot-starter-restclient` for outgoing calls; `spring-boot-starter-webmvc` only covers the incoming side

2. **A declarative client**
   - An `@HttpExchange`/`@GetExchange` interface, no implementation
   - `@ImportHttpServices(group = ..., types = ...)` generates and registers the proxy bean
   - Base URL, `User-Agent` and timeouts in `spring.http.serviceclient.<group>.*`
   - Records with `@JsonProperty` for the JSON of the other API

3. **When the other side fails**
   - `4xx`/`5xx` and network problems arrive as subclasses of `RestClientException`
   - `ExternalServiceException` → `502 Bad Gateway`; unknown or incomplete data → `422`
   - An HTTP `200` does not mean usable data: check the body too
   - No `@Transactional` around a remote call

4. **API design**
   - An operation that is not plain CRUD: `POST` to a sub-resource of the pizza

### Key Takeaways

- 🔌 **A declarative client is an interface; Spring writes the implementation**
- ⏱️ **No external call without a timeout**
- 🌐 **An external API will fail: decide what *your* client sees (`422` or `502`)**
- 🛡️ **Never trust, never forward, never leak the other side's data or errors**
- 🗄️ **Never keep a database transaction open while you wait for another server**

### What's Next?

PizzaStore now answers valid and invalid requests correctly and even fetches data from another service — but we've only checked all of that by hand with `curl`, and this lesson's checks even need an internet connection. [Lesson 12](../lesson-12-testing/README.md) turns them into automated tests, including a `@RestClientTest` that tests `OpenFoodFactsClient` against a mock server instead of the real Open Food Facts.

---

## 📖 Additional Resources

- [Spring Framework Reference: REST Clients](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html) (`RestClient`, `WebClient`, `RestTemplate`, HTTP service interfaces)
- [Spring Boot Reference: Calling REST Services](https://docs.spring.io/spring-boot/reference/io/rest-client.html) (including the `spring.http.serviceclient.*` properties)
- [Open Food Facts API documentation](https://openfoodfacts.github.io/openfoodfacts-server/api/)
- [RFC 9110, section 15.6.3: 502 Bad Gateway](https://www.rfc-editor.org/rfc/rfc9110#section-15.6.3)

---

**Note on the book**: *Pro Spring Boot 4* covers this lesson in Chapter 3, section *Declarative HTTP Service Clients* (p. 74-77): a `CurrencyClient` interface with `@HttpExchange` and `@GetExchange` for an exchange-rate API (Listing 3-7), `@ImportHttpServices` on a configuration class (Listing 3-8), and a `@SpringBootTest` that calls the real API (Listing 3-9). This lesson applies the same building blocks to Open Food Facts. Where it goes further: the book hard-codes the URL in `@HttpExchange` and advises to externalize it, and only mentions a `RestClientCustomizer` for timeouts; PizzaStore puts base URL, `User-Agent` and timeouts in `spring.http.serviceclient.<group>.*` with a named group, and shows how the failures of the other side surface as `422`/`502` — the book does not handle errors of the remote API at all. Chapter 18 (*Modern, Declarative HTTP Clients* and *Modern Synchronous HTTP Clients: RestClient*, p. 492-494) puts both in the list of Spring Boot 4 highlights, with a fluent `RestClient` example (Listing 18-5). Note that, despite the book's wording that Spring Boot 4 "introduces" `RestClient`, it has existed since Spring Framework 6.1 / Spring Boot 3.2; what is new in Spring Framework 7 / Spring Boot 4 is the HTTP service registry (`@ImportHttpServices`, client groups and their properties) and the separate `spring-boot-starter-restclient`. More advanced uses appear later in the book and are outside this course: a client with mutual TLS through an SSL bundle (Chapter 11, Listings 11-24 and 11-25), and declarative and load-balanced clients between microservices (Chapters 15 and 16). Testing the client without the real server is covered in Chapter 10 (`@RestClientTest`) and in [Lesson 12](../lesson-12-testing/README.md).

---

## 🚀 Runnable Project

**[`pizzastore-with-external-api/`](pizzastore-with-external-api)** is Lesson 10's [`pizzastore-with-validation`](../lesson-10-validation-exception-handling/pizzastore-with-validation) plus the changes listed in [What This Lesson Adds to PizzaStore](#-what-this-lesson-adds-to-pizzastore). Its client, configuration, services and controllers are the same as in the final PizzaStore, minus the security handler (Lesson 13) and the OpenAPI annotations (Lesson 14).

The project includes:
- ✅ **Spring Boot 4.0** on **Java 25** (`spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-restclient`, H2, MapStruct 1.6.3)
- ✅ Everything from Lessons 6a, 7, 9 and 10: domain model, repositories, DTOs, mappers, services, complete REST API, validation and `ProblemDetail` errors
- ✅ A declarative `@HttpExchange` client for Open Food Facts, with timeouts and `422`/`502` error handling (needs internet access to try it out)
- ❌ No automated tests yet — Lesson 12
- ❌ No security — every endpoint is open until Lesson 13

### Running It

```bash
cd pizzastore-with-external-api
mvn spring-boot:run
```

Then import some nutrition data:

```bash
# 200 - fill the nutritional info of pizza 1 with the data of Open Food Facts product 3017620422003 (needs internet)
curl -X POST http://localhost:8080/api/pizzas/1/nutritional-info/import \
  -H "Content-Type: application/json" -d '{"barcode":"3017620422003"}'

# 400 - not a barcode: Open Food Facts is never called
curl -X POST http://localhost:8080/api/pizzas/1/nutritional-info/import \
  -H "Content-Type: application/json" -d '{"barcode":"abc"}'

# 404 - unknown pizza: Open Food Facts is never called
curl -X POST http://localhost:8080/api/pizzas/999/nutritional-info/import \
  -H "Content-Type: application/json" -d '{"barcode":"3017620422003"}'

# 422 - Open Food Facts does not know this barcode
curl -X POST http://localhost:8080/api/pizzas/1/nutritional-info/import \
  -H "Content-Type: application/json" -d '{"barcode":"5412345678901"}'
```

The H2 console is available at http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:pizzastore_external_api`, user `sa`, no password).

---

**Congratulations!** 🎉 PizzaStore no longer lives on its own: it fetches data from another service, and stays well-behaved when that service lets it down. Continue to [Lesson 12: Testing](../lesson-12-testing/README.md) to prove it all with automated tests.
