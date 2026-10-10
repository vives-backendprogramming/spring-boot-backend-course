# Lesson 12: Testing Spring Boot Applications

**Proving That PizzaStore Works: Unit Tests, Test Slices, `RestTestClient` and Full-Stack Integration Tests**

---

## 📋 Learning Objectives

By the end of this lesson, you will be able to:
- Explain the testing pyramid and choose the cheapest kind of test that can prove a given behavior
- Set up the **modular Spring Boot 4 test starters** and find the (moved) Spring Boot 4 test annotations
- Write fast **unit tests** with JUnit 6, Mockito and AssertJ, without a Spring context
- Test one layer at a time with **test slices**: `@DataJpaTest`, `@WebMvcTest`, `@JsonTest` and `@RestClientTest`
- Call your API in a test with the unified **`RestTestClient`** (and know how it compares to MockMvc and `MockMvcTester`)
- Write full-stack **`@SpringBootTest`** integration tests, with a mock servlet environment or a real random port
- Replace beans in a test with **`@MockitoBean`** and **`@MockitoSpyBean`**
- Override configuration properties per test, and explain how the **context cache** decides whether a test is fast or slow
- Test validation rules and the `ProblemDetail` error contract of Lesson 10
- Organize tests with `@Nested` and `@ParameterizedTest`
- Know when a real database in a container (Testcontainers) is worth it
- Split unit tests from integration tests in Maven with Surefire and Failsafe

---

## 📚 Table of Contents

1. [Recap: Where Lesson 11 Left Us](#-recap-where-lesson-11-left-us)
2. [What This Lesson Adds to PizzaStore](#-what-this-lesson-adds-to-pizzastore)
3. [Why Test?](#%EF%B8%8F-why-test)
4. [The Testing Pyramid](#-the-testing-pyramid)
5. [Spring Boot 4 Test Setup](#-spring-boot-4-test-setup)
6. [Choosing the Right Kind of Test](#-choosing-the-right-kind-of-test)
7. [Unit Tests Without Spring](#-unit-tests-without-spring)
8. [Test Slices](#-test-slices)
9. [Testing the Web Layer: MockMvc, RestTestClient or MockMvcTester](#-testing-the-web-layer-mockmvc-resttestclient-or-mockmvctester)
10. [Full-Stack Tests with @SpringBootTest](#-full-stack-tests-with-springboottest)
11. [Replacing Beans: @MockitoBean and @MockitoSpyBean](#-replacing-beans-mockitobean-and-mockitospybean)
12. [Overriding Properties in Tests](#-overriding-properties-in-tests)
13. [The Context Cache: Why Test Suites Get Slow](#-the-context-cache-why-test-suites-get-slow)
14. [Testing Validation and Error Responses](#-testing-validation-and-error-responses)
15. [Assertions: AssertJ, JsonPath and Hamcrest](#-assertions-assertj-jsonpath-and-hamcrest)
16. [When H2 Is Not Enough: Testcontainers](#-when-h2-is-not-enough-testcontainers)
17. [Running the Tests with Maven: Surefire and Failsafe](#%EF%B8%8F-running-the-tests-with-maven-surefire-and-failsafe)
18. [Best Practices](#-best-practices)
19. [The PizzaStore Test Suite](#-the-pizzastore-test-suite)
20. [Summary](#-summary)
21. [Additional Resources](#-additional-resources)
22. [Runnable Project](#-runnable-project)

---

## 🔄 Recap: Where Lesson 11 Left Us

[Lesson 10](../lesson-10-validation-exception-handling/README.md) ended with an API that rejects bad input (`400`), explains missing resources (`404`), refuses broken business rules (`422`) and reports conflicts (`409`), always as an RFC 7807 `ProblemDetail`. [Lesson 11](../lesson-11-external-api/README.md) added PizzaStore's first call to somebody else's API, the Open Food Facts import, with a `502` when that service fails. Both lessons ended with tables of requests and answers, and every row was verified by hand with `curl`.

Doing that by hand has three problems:
- It is **slow**: you repeat it after every change.
- It is **incomplete**: nobody re-checks all rows before every commit.
- It **proves nothing tomorrow**: the next refactoring can silently break last week's behavior.

An automated test is a `curl` command that checks its own answer and runs in milliseconds. This lesson adds a complete test suite to PizzaStore.

---

## 🧱 What This Lesson Adds to PizzaStore

[`pizzastore-with-tests`](pizzastore-with-tests) is Lesson 11's [`pizzastore-with-external-api`](../lesson-11-external-api/pizzastore-with-external-api) plus tests. The production code is **unchanged**. The tests include a `@RestClientTest` for Lesson 11's Open Food Facts client, [`OpenFoodFactsClient`](pizzastore-with-tests/src/main/java/be/vives/pizzastore/client/OpenFoodFactsClient.java).

| What | Where |
|------|-------|
| Test dependencies (`spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`, `spring-boot-starter-restclient-test`) | [`pom.xml`](pizzastore-with-tests/pom.xml) |
| Test configuration (own H2 database, no `data.sql`, quieter logging) | [`src/test/resources/application.properties`](pizzastore-with-tests/src/test/resources/application.properties) |
| **226 tests** in 21 test classes (plus 7 `@Nested` classes) | [`src/test/java`](pizzastore-with-tests/src/test/java/be/vives/pizzastore) |
| Tests for the Open Food Facts import of Lesson 11: [`OpenFoodFactsClientTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/client/OpenFoodFactsClientTest.java) (`@RestClientTest`), [`NutritionImportServiceTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/service/NutritionImportServiceTest.java) (Mockito) and extra cases in the controller, service and validation tests | [`src/test/java`](pizzastore-with-tests/src/test/java/be/vives/pizzastore) |

The H2 database of the running application is called `pizzastore_tests` in this project (`jdbc:h2:mem:pizzastore_tests`). The tests use databases of their own, so running `mvn test` never touches data of a running application.

---

## ⚠️ Why Test?

Look at this service method:

```java
public PizzaResponse findById(Long id) {
    Pizza pizza = pizzaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Pizza", id));
    return pizzaMapper.toResponse(pizza);
}
```

How do you know that it throws the right exception for an unknown id, that the controller turns that exception into a `404`, and that it still does so after the next refactoring? With tests, the answer is a green build:

```java
@Test
void findById_NonExistingPizza_ThrowsResourceNotFoundException() {
    when(pizzaRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> pizzaService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Pizza with id 999 not found");
}
```

Tests give you confidence, early bug detection, safe refactoring and living documentation. As a bonus, code that is easy to test (small classes, dependencies passed through the constructor) is usually well-designed code.

---

## 🔺 The Testing Pyramid

```
              ┌───────────────┐
              │   End-to-End  │   few, slow, expensive
              └───────────────┘
          ┌───────────────────────┐
          │   Integration tests   │   some, moderate speed
          │ (slices, @SpringBoot  │
          │  Test, Testcontainers)│
          └───────────────────────┘
      ┌───────────────────────────────┐
      │          Unit tests           │   many, milliseconds
      │   (no Spring, mocked deps)    │
      └───────────────────────────────┘
```

| Level | Proves | Speed | Example in PizzaStore |
|-------|--------|-------|-----------------------|
| **Unit test** | One class, in isolation | milliseconds | [`PizzaServiceTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/service/PizzaServiceTest.java) |
| **Slice test** | One layer with the real Spring infrastructure around it | ~0.1 - 1 s | [`PizzaRepositoryTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/repository/PizzaRepositoryTest.java), [`PizzaControllerTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/controller/PizzaControllerTest.java) |
| **Integration test** | Several layers working together | ~1 - 10 s | [`PizzaStoreApiIntegrationTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/integration/PizzaStoreApiIntegrationTest.java) |
| **Smoke test** | The application starts at all | ~1 s | [`ApplicationSmokeTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/integration/ApplicationSmokeTest.java) |

Aim for many unit tests, fewer slice tests and a small number of full-stack tests. Spring Boot 4 pushes in the same direction: the book (Chapter 10) describes a shift "from monolithic integration tests to highly optimized test slices", plus *Automatic Context Pausing* in large test suites, where Spring pauses cached application contexts that are not in use so that their background threads and scheduled tasks stop consuming resources.

---

## 🧰 Spring Boot 4 Test Setup

### Modular test starters

Before Spring Boot 4 you added one big `spring-boot-starter-test`. In Spring Boot 4 every technology has its **own test starter**, mirroring the production starters (`spring-boot-starter-webmvc` → `spring-boot-starter-webmvc-test`). You add the starters for the layers you test and keep the test classpath small.

```xml
<!-- Test Dependencies -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-restclient-test</artifactId>
    <scope>test</scope>
</dependency>
```

| Starter | Brings you |
|---------|------------|
| `spring-boot-starter-webmvc-test` | `spring-boot-starter-test` (JUnit, AssertJ, Mockito, JsonPath, Awaitility, Hamcrest), `@WebMvcTest`, `MockMvc`, `MockMvcTester`, `RestTestClient`, `@JsonTest`/`JacksonTester` |
| `spring-boot-starter-data-jpa-test` | `@DataJpaTest`, `TestEntityManager` |
| `spring-boot-starter-restclient-test` | `@RestClientTest`, `MockRestServiceServer` |

You can see all of these in the project with `mvn dependency:list -DincludeScope=test`. Other modules follow the same pattern (`spring-boot-starter-security-test`, `spring-boot-starter-webflux-test`, ...). Lesson 13 will add the security one.

### The versions you get

| Library | Version in Spring Boot 4.0.8 |
|---------|------------------------------|
| JUnit | **6.0** (JUnit 4 support is gone) |
| Mockito | 5.20 |
| AssertJ | 3.27 |
| Spring Framework (`spring-test`) | 7.0 |
| Jackson (the `JsonMapper` you inject in tests) | 3 (`tools.jackson.*`) |

JUnit 6 keeps the JUnit 5 programming model (`@Test`, `@BeforeEach`, `@Nested`, `@ParameterizedTest`, ...), so everything you know from JUnit 5 still applies. The only things that changed are the version and that the old JUnit 4 `@RunWith` is no longer supported.

### Where did the annotations go?

Because Spring Boot 4 split its test support into modules, the **packages of the well-known annotations changed**. This is the most common reason an old tutorial does not compile:

| Annotation | Spring Boot 3 package | Spring Boot 4 package |
|------------|-----------------------|-----------------------|
| `@WebMvcTest` | `org.springframework.boot.test.autoconfigure.web.servlet` | `org.springframework.boot.webmvc.test.autoconfigure` |
| `@AutoConfigureMockMvc` | `org.springframework.boot.test.autoconfigure.web.servlet` | `org.springframework.boot.webmvc.test.autoconfigure` |
| `@DataJpaTest` | `org.springframework.boot.test.autoconfigure.orm.jpa` | `org.springframework.boot.data.jpa.test.autoconfigure` |
| `TestEntityManager` | `org.springframework.boot.test.autoconfigure.orm.jpa` | `org.springframework.boot.jpa.test.autoconfigure` |
| `@RestClientTest` | `org.springframework.boot.test.autoconfigure.web.client` | `org.springframework.boot.restclient.test.autoconfigure` |
| `@AutoConfigureRestTestClient` | *(did not exist)* | `org.springframework.boot.resttestclient.autoconfigure` |
| `@JsonTest` | `org.springframework.boot.test.autoconfigure.json` | unchanged |
| `@SpringBootTest` | `org.springframework.boot.test.context` | unchanged |
| `@MockBean` / `@SpyBean` | `org.springframework.boot.test.mock.mockito` | **removed**, use `@MockitoBean` / `@MockitoSpyBean` from `org.springframework.test.context.bean.override.mockito` |

---

## 🎯 Choosing the Right Kind of Test

| Annotation | Starts | Database | Server | Use it to test |
|------------|--------|----------|--------|----------------|
| *(none)*, JUnit + Mockito | nothing | - | - | services, mappers, validation rules, anything with plain Java collaborators |
| `@SpringJUnitConfig({A.class, B.class})` | only the listed beans | - | - | a few beans wired together |
| `@DataJpaTest` | JPA, repositories, an embedded DB | ✅ H2 | - | custom queries, entity mapping |
| `@WebMvcTest` | controllers, `@RestControllerAdvice`, Jackson, validation | - | mock | request mapping, status codes, JSON, error responses |
| `@JsonTest` | Jackson only | - | - | how a record looks as JSON |
| `@RestClientTest` | the HTTP-client infrastructure + the client you name or `@Import` | - | mock | classes that *call* another API |
| `@SpringBootTest` (MOCK) | the whole application | ✅ | mock | flows over several layers |
| `@SpringBootTest` (RANDOM_PORT) | the whole application | ✅ | ✅ real | the real HTTP contract |

The rule of thumb: **use the cheapest test that can fail for the reason you care about.** A price rule is tested in a unit test, a custom query in `@DataJpaTest`, a status code in `@WebMvcTest`, and "can a customer place an order" in `@SpringBootTest`.

---

## ⚡ Unit Tests Without Spring

A unit test needs no Spring at all. Mockito replaces the collaborators of the class under test.

[`PizzaServiceTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/service/PizzaServiceTest.java) tests `PizzaService` with a mocked repository, mapper and file storage:

```java
@ExtendWith(MockitoExtension.class)   // activates @Mock and @InjectMocks
class PizzaServiceTest {

    @Mock
    private PizzaRepository pizzaRepository;

    @Mock
    private PizzaMapper pizzaMapper;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private PizzaService pizzaService;      // built with the mocks above

    @Test
    void findById_ExistingPizza_ReturnsPizzaResponse() {
        // Given
        when(pizzaRepository.findById(1L)).thenReturn(Optional.of(testPizza));
        when(pizzaMapper.toResponse(testPizza)).thenReturn(testResponse);

        // When
        PizzaResponse result = pizzaService.findById(1L);

        // Then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Margherita");
        verify(pizzaRepository).findById(1L);
    }
}
```

The same pattern covers the business rules of `OrderService` ([`OrderServiceTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/service/OrderServiceTest.java)): an order for an unavailable pizza, cancelling a delivered order, cancelling an order twice. With Lesson 10's exceptions these are easy to assert:

```java
assertThatThrownBy(() -> orderService.cancel(1L))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Order is already cancelled");
```

### More tests that need no Spring context

| Test | What it shows |
|------|---------------|
| [`RequestValidationTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/dto/request/RequestValidationTest.java) | Builds a Jakarta `Validator` by hand and checks every constraint of the request records, see [Testing Validation](#-testing-validation-and-error-responses) |
| [`PizzaControllerStandaloneTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/controller/PizzaControllerStandaloneTest.java) | A controller test with `RestTestClient.bindToController(...)`, see [the web layer](#-testing-the-web-layer-mockmvc-resttestclient-or-mockmvctester) |
| [`PizzaMapperTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/mapper/PizzaMapperTest.java) | Tests the generated MapStruct mapper in a context with just two beans |

`PizzaMapperTest` is the smallest possible Spring test: `@SpringJUnitConfig({PizzaMapperImpl.class, NutritionalInfoMapperImpl.class})` creates a context with only the two MapStruct implementations that are needed, instead of the whole application.

### `@Mock` or `@MockitoBean`?

| | `@Mock` | `@MockitoBean` |
|---|---------|----------------|
| Needs a Spring context | ❌ No | ✅ Yes (`@WebMvcTest`, `@SpringBootTest`, ...) |
| Speed | milliseconds | slower: the context must start |
| What it does | creates a mock object | **replaces a bean in the Spring context** by a mock |
| Package | `org.mockito` | `org.springframework.test.context.bean.override.mockito` |
| Typical use | service tests | controller slice tests |

---

## 🍰 Test Slices

A *slice* loads only the part of the application that one layer needs, so the test is fast and a failure points at that layer. Behind the scenes a type-exclude filter keeps component scanning from picking up the rest of your code.

### `@DataJpaTest`: the repository layer

`@DataJpaTest` starts JPA, Spring Data and an **embedded H2 database**, and nothing else: no controllers, no services. Every test runs in a transaction that is **rolled back** afterwards, so tests cannot influence each other.

[`PizzaRepositoryTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/repository/PizzaRepositoryTest.java):

```java
@DataJpaTest
@Import(JpaConfig.class)   // @DataJpaTest does not scan @Configuration classes: import the one that enables JPA auditing
class PizzaRepositoryTest {

    @Autowired
    private PizzaRepository pizzaRepository;

    @Autowired
    private TestEntityManager entityManager;   // test helper to arrange data

    @Test
    void findByPriceLessThan_MultipleResults_ReturnsFilteredList() {
        // Given: persist and flush, so the query hits real rows
        entityManager.persist(new Pizza("Cheap Pizza", new BigDecimal("5.00"), "Budget option"));
        entityManager.persist(new Pizza("Mid Pizza", new BigDecimal("10.00"), "Medium price"));
        entityManager.persist(new Pizza("Expensive Pizza", new BigDecimal("15.00"), "Premium"));
        entityManager.flush();

        // When
        List<Pizza> result = pizzaRepository.findByPriceLessThan(new BigDecimal("12.00"));

        // Then
        assertThat(result)
                .extracting(Pizza::getName)
                .containsExactlyInAnyOrder("Cheap Pizza", "Mid Pizza");
    }
}
```

What to test here: **your own queries** (derived queries, `@Query`, the DTO projection `findPizzaSalesStatistics()` from Lesson 6a) and the mapping that matters (cascades, `@OneToMany`). Do not test `save()` or `findById()` of Spring Data itself: that is the framework's job. See also [`CustomerRepositoryTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/repository/CustomerRepositoryTest.java) and [`OrderRepositoryTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/repository/OrderRepositoryTest.java).

> **Why `@Import(JpaConfig.class)`?** Lesson 6a's `JpaConfig` carries `@EnableJpaAuditing`, which fills `createdAt`/`updatedAt` (columns that are `NOT NULL`). A slice does not component-scan `@Configuration` classes, so without the import every `persist` would fail on the missing timestamp. This is a typical slice lesson: *a slice only knows what you tell it*.

### `@JsonTest`: how does a record look as JSON?

[`PizzaJsonTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/dto/PizzaJsonTest.java) loads nothing but the JSON infrastructure (the Jackson 3 `JsonMapper` with the application's `spring.jackson.*` settings) and gives you a `JacksonTester`:

```java
@JsonTest
class PizzaJsonTest {

    @Autowired
    private JacksonTester<PizzaResponse> responseJson;

    @Autowired
    private JacksonTester<CreatePizzaRequest> requestJson;

    @Test
    void serialize_PizzaResponse_ContainsAllFields() throws Exception {
        PizzaResponse pizza = new PizzaResponse(1L, "Margherita", new BigDecimal("8.50"), "Classic", null, true, null);

        assertThat(responseJson.write(pizza)).extractingJsonPathStringValue("$.name").isEqualTo("Margherita");
        assertThat(responseJson.write(pizza)).extractingJsonPathNumberValue("$.price").isEqualTo(8.5);
    }

    @Test
    void deserialize_CreatePizzaRequest_MapsJsonToRecord() throws Exception {
        CreatePizzaRequest request = requestJson.parseObject("""
                { "name": "Diavola", "price": 12.99, "available": true }
                """);

        assertThat(request.name()).isEqualTo("Diavola");
    }
}
```

`write(...)` turns an object into JSON and lets you assert on it with JsonPath, `parseObject(...)` does the reverse. Use it to protect the **JSON contract** of your DTOs: field names, date formats, `null` handling, `@JsonProperty`/`@JsonIgnore`. It is much faster than starting a web layer for that.

### `@RestClientTest`: classes that call another API

All tests so far test code that is *called* by clients. `@RestClientTest` tests code that **calls** someone else's API. The book introduces this in Chapter 10 (*Client-Side Testing with @RestClientTest*) for declarative HTTP clients.

The thing under test is Lesson 11's declarative client [`OpenFoodFactsClient`](pizzastore-with-tests/src/main/java/be/vives/pizzastore/client/OpenFoodFactsClient.java): an interface that Spring turns into a proxy that calls the free Open Food Facts API.

You do not want a test that depends on the real server: it would be slow, need internet, give different answers when someone edits the product, and burn the **15 lookups per minute** that Open Food Facts allows per IP address. `@RestClientTest` solves this by replacing the HTTP transport with a **`MockRestServiceServer`**. In [`OpenFoodFactsClientTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/client/OpenFoodFactsClientTest.java) you first say which request you expect and which response must come back, then you call the client:

```java
@RestClientTest(properties = {
        "spring.http.serviceclient.openfoodfacts.base-url=https://off.test",
        "spring.http.serviceclient.openfoodfacts.default-header.User-Agent=PizzaStoreTest/1.0 (test@example.com)"
})
@Import(HttpClientConfig.class)
@ImportAutoConfiguration({HttpServiceClientAutoConfiguration.class, HttpServiceClientPropertiesAutoConfiguration.class})
class OpenFoodFactsClientTest {

    @Autowired
    private OpenFoodFactsClient client;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void getProduct_KnownBarcode_MapsTheNutrimentsPer100g() {
        server.expect(requestTo("https://off.test/api/v2/product/3017620422003.json?fields=code,product_name,nutriments"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("User-Agent", "PizzaStoreTest/1.0 (test@example.com)"))
                .andRespond(withSuccess("""
                        {"code": "3017620422003", "status": 1,
                         "product": {"product_name": "Nutella",
                                     "nutriments": {"energy-kcal_100g": 539, "proteins_100g": 6.3, ...}}}
                        """, MediaType.APPLICATION_JSON));

        OpenFoodFactsResponse response = client.getProduct("3017620422003");

        assertThat(response.product().nutriments().energyKcal100g()).isEqualByComparingTo("539");
        server.verify();   // the expected request really happened
    }
    ...
}
```

Three things are different from testing a hand-written `RestClient` class:

- **An interface has no class to name.** Instead of `@RestClientTest(SomeClient.class)` you `@Import(HttpClientConfig.class)`: the same configuration class as in the application, so the test checks the real `@ImportHttpServices` wiring too.
- **The slice does not know about declarative clients.** The `@ImportAutoConfiguration` line adds the two auto-configurations that apply `spring.http.serviceclient.*` and the mock server to the generated `RestClient`. Leave it out and the test fails with *"Unable to use auto-configured MockRestServiceServer since a mock server customizer has not been bound to a RestTemplate or RestClient"*. The book's own example (Listing 3-9) avoids the problem by using `@SpringBootTest` and the real API.
- **The same properties as in production.** The test sets `spring.http.serviceclient.openfoodfacts.base-url` to a fake host, which is how a test redirects a client without touching code. The test `application.properties` also points it to `http://openfoodfacts.invalid`, so a test that forgets to mock can never reach the real server.

The error cases are exactly what you can hardly provoke against a real server: the quirky `200` with `"status": 0` for an unknown barcode, a `404`, a `429` when you are rate-limited and a `503` when the other side is down. With a mock server they are a few lines each. The slice loads no controllers and no database.

The *consequences* of those failures (`422` or `502`) are not tested here but one level up, without any HTTP at all: [`NutritionImportServiceTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/service/NutritionImportServiceTest.java) lets a Mockito mock of the client throw `HttpServerErrorException`, `ResourceAccessException` (a timeout) and so on, and asserts which PizzaStore exception comes out; `PizzaControllerTest` then checks that an `ExternalServiceException` becomes a `502` ProblemDetail that does not leak the cause. Each layer tests its own responsibility.

### Other slices

Spring Boot 4 has a slice for nearly every technology, each in its own module: `@DataJdbcTest`, `@JdbcTest`, `@DataMongoTest` (Lesson 6b), `@DataRedisTest`, `@GraphQlTest`, `@WebFluxTest`, `@DataR2dbcTest` and more. They all work the same way: start one layer, configure only what that layer needs, roll back or discard state afterwards. The book lists them in Tables 10-1 and 10-2.

---

## 🌐 Testing the Web Layer: MockMvc, RestTestClient or MockMvcTester

`@WebMvcTest(PizzaController.class)` starts the web layer only: the controller, `@RestControllerAdvice` classes (`GlobalExceptionHandler` must be imported explicitly in our tests), Jackson, validation and Spring Data's `Pageable` support. **Services and repositories are not loaded**, so you supply the service as a `@MockitoBean`. Because no database is involved, these tests run in milliseconds.

The project contains the **same web-layer test written three ways**, so you can compare:

### 1. MockMvc: the classic

[`PizzaControllerTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/controller/PizzaControllerTest.java), `CustomerControllerTest` and `OrderControllerTest` use MockMvc with Hamcrest matchers:

```java
@WebMvcTest(controllers = PizzaController.class)
@Import(GlobalExceptionHandler.class)
class PizzaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PizzaService pizzaService;

    @Test
    void getPizza_NonExistingId_Returns404() throws Exception {
        when(pizzaService.findById(999L)).thenThrow(new ResourceNotFoundException("Pizza", 999L));

        mockMvc.perform(get("/api/pizzas/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", is("Pizza with id 999 not found")));
    }
}
```

MockMvc is not deprecated and it is everywhere in existing code, so you must be able to read it. Notice the `throws Exception`, the static imports of `status()`, `jsonPath()`, `content()` and the Hamcrest `is(...)`.

### 2. RestTestClient: the Spring Boot 4 unified client

[`PizzaControllerRestTestClientTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/controller/PizzaControllerRestTestClientTest.java):

```java
@WebMvcTest(PizzaController.class)
@AutoConfigureRestTestClient
@Import(GlobalExceptionHandler.class)
class PizzaControllerRestTestClientTest {

    @Autowired
    private RestTestClient client;

    @MockitoBean
    private PizzaService pizzaService;

    @Test
    void getPizza_NonExistingId_ReturnsProblemDetail() {
        when(pizzaService.findById(999L)).thenThrow(new ResourceNotFoundException("Pizza", 999L));

        client.get().uri("/api/pizzas/{id}", 999)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.detail").isEqualTo("Pizza with id 999 not found")
                .jsonPath("$.instance").isEqualTo("/api/pizzas/999");
    }
}
```

`RestTestClient` is Spring Framework 7's **fluent client for testing REST APIs**. It reads like the `RestClient` you use in production: `get().uri(...).exchange()` followed by `expectStatus()`, `expectHeader()`, `expectBody()`. The big idea is that **the same client works in every environment**:

| Binding | How you get it | Used in |
|---------|----------------|---------|
| Mock MVC environment of the slice | `@AutoConfigureRestTestClient` on a `@WebMvcTest` | web-layer slice tests |
| One controller, no Spring context | `RestTestClient.bindToController(new PizzaController(service))` | unit tests |
| A running server | `@AutoConfigureRestTestClient` on `@SpringBootTest(webEnvironment = RANDOM_PORT)` | full-stack tests |

So the test code you write for a slice can look exactly like the test code for the real server. The assertions can be done in two styles. `expectBody(PizzaResponse.class).value(pizza -> assertThat(...))` deserializes the body into your record and lets you use AssertJ; `expectBody().jsonPath("$.detail").isEqualTo(...)` checks single JSON fields without a class.

### 3. Without a Spring context: `bindToController`

[`PizzaControllerStandaloneTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/controller/PizzaControllerStandaloneTest.java) uses no Spring context at all: not even `@WebMvcTest`.

```java
class PizzaControllerStandaloneTest {

    private final PizzaService pizzaService = mock(PizzaService.class);

    private final RestTestClient client = RestTestClient
            .bindToController(new PizzaController(pizzaService))
            .configureServer(server -> server.setControllerAdvice(new GlobalExceptionHandler()))
            .build();
    ...
}
```

You create the controller yourself with a mocked service and the client talks to it through a minimal MVC setup. It starts in milliseconds, but nothing is auto-configured (no `application.properties`, no Spring Data `Pageable` JSON, no auto-registered advice), so keep it for simple request/response mappings.

### 4. MockMvcTester: MockMvc with AssertJ

[`PizzaControllerMockMvcTesterTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/controller/PizzaControllerMockMvcTesterTest.java) uses `MockMvcTester`, which `@WebMvcTest` also auto-configures:

```java
@Autowired
private MockMvcTester mvc;

@Test
void getPizza_NonExistingId_ReturnsProblemDetail() {
    when(pizzaService.findById(999L)).thenThrow(new ResourceNotFoundException("Pizza", 999L));

    assertThat(mvc.get().uri("/api/pizzas/{id}", 999))
            .hasStatus(HttpStatus.NOT_FOUND)
            .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
            .bodyJson()
            .extractingPath("$.detail").isEqualTo("Pizza with id 999 not found");
}
```

No `throws Exception`, no static `status()`/`jsonPath()` imports, no Hamcrest: it is plain AssertJ, with autocompletion that actually helps.

### Which one should I use?

| | MockMvc | `MockMvcTester` | `RestTestClient` |
|---|---------|-----------------|------------------|
| Style | `perform(...).andExpect(...)` | AssertJ `assertThat(...)` | fluent `exchange().expectX()` |
| Assertions | Hamcrest matchers | AssertJ | its own fluent API + AssertJ in `value(...)` |
| Same test against a real server | ❌ | ❌ | ✅ |
| Where you meet it | all existing Spring code | Spring Boot 3.4+ code | new Spring Boot 4 code |

**There is no official default.** The Spring Boot reference lists `MockMvc`, `MockMvcTester`, `RestTestClient` and `WebTestClient` side by side as supported ways to test Spring MVC, without a preference. The book calls `RestTestClient` and AssertJ "the modern defaults" but still shows MockMvc with Hamcrest in its own examples, and community comparisons conclude that the choice between `MockMvcTester` and `RestTestClient` is largely a matter of preference. Our choice for PizzaStore is `RestTestClient`: the same fluent style serves slice tests and full-stack tests, and it resembles the production `RestClient`. `MockMvcTester` is the better fit when you want pure AssertJ, and MockMvc remains the right tool for **multipart requests in a `@WebMvcTest` slice**: a `RestTestClient` that is bound to MockMvc does not turn a multipart body into request parts (it does against a real server). Keep reading and maintaining MockMvc where it exists.

---

## 🚀 Full-Stack Tests with @SpringBootTest

`@SpringBootTest` starts the **complete application**: all beans, the real H2 database, the real exception handler. It finds your `@SpringBootApplication` class and loads the same context as `mvn spring-boot:run`. It is the slowest and most realistic kind of test, so use it for flows that cross layers and for a few critical paths.

### The `webEnvironment` choices

| `webEnvironment` | What happens | Combine with |
|------------------|--------------|--------------|
| `MOCK` (default) | Mock servlet environment, no real server | `MockMvc` |
| `RANDOM_PORT` | Starts Tomcat on a free port | `RestTestClient` (bound to the server) |
| `NONE` | No web environment at all | services, batch jobs |

### Smoke test

[`ApplicationSmokeTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/integration/ApplicationSmokeTest.java) only checks that the context starts and the main beans exist, including the `OpenFoodFactsClient` proxy that `@ImportHttpServices` generates. It catches a wrong bean wiring, a missing property or a broken JPA mapping in one second. If you have to write one `@SpringBootTest`, write this one.

### With MockMvc: `@Transactional` rolls back

[`PizzaIntegrationTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/integration/PizzaIntegrationTest.java) runs the full application, but sends requests through MockMvc on the test thread. Because the test and the "server" share one thread, a class-level `@Transactional` rolls back everything that every test wrote:

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PizzaIntegrationTest { ... }
```

### With a real server: `RestTestClient` and `RANDOM_PORT`

[`PizzaStoreApiIntegrationTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/integration/PizzaStoreApiIntegrationTest.java) starts a real Tomcat on a random port and talks HTTP to it:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:api-integration-test")
@AutoConfigureRestTestClient
class PizzaStoreApiIntegrationTest {

    @Autowired
    private RestTestClient client;     // already bound to http://localhost:<random port>
    ...
}
```

Two things to know:

1. **`@Transactional` does not roll back anything here.** The server handles each request on its own thread, so the test method has no transaction to roll back. Instead, every test creates its own, uniquely named data (`"Pizza " + UUID.randomUUID()`) and never assumes the database is empty. The `properties = ...` attribute gives this context its own H2 database, so other test contexts cannot interfere.
2. **Create test data through the public API**, like a real client would. The helper methods `createPizza(...)` and `createCustomer(...)` do that, which makes the tests independent of `data.sql` (disabled for tests).

The most valuable test of the class is the **order lifecycle**, which crosses controllers, validation, services, mappers and the database:

```java
@Test
void orderLifecycle_CreateUpdateStatusAndCancel() {
    PizzaResponse margherita = createPizza("8.50");
    PizzaResponse diavola = createPizza("12.00");
    CustomerResponse customer = createCustomer(uniqueEmail());

    // 1. place an order: 2 x 8.50 + 1 x 12.00 = 29.00
    OrderResponse order = client.post().uri("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .body(new CreateOrderRequest(customer.id(), List.of(
                    new CreateOrderRequest.OrderLineRequest(margherita.id(), 2),
                    new CreateOrderRequest.OrderLineRequest(diavola.id(), 1))))
            .exchange()
            .expectStatus().isCreated()
            .expectBody(OrderResponse.class)
            .returnResult().getResponseBody();

    assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
    assertThat(order.totalAmount()).isEqualByComparingTo("29.00");

    // 2. move it through the workflow, 3. cancel it (204), 4. cancel again (422) ...
}
```

`returnResult().getResponseBody()` gives you the deserialized body to continue with, e.g. to reuse an `id` in the next request.

The nested classes `Pizzas`, `Errors` and `Orders` group the 13 tests of this class by topic (`@Nested`, see [Best Practices](#-best-practices)).

> **Context caching.** Every different `properties`, `@MockitoBean` or `@MockitoSpyBean` setup makes Spring start a **new** application context. That is why this project gives each integration test its own H2 database name. How the cache works, and how to keep your suite fast, is explained in [The Context Cache](#-the-context-cache-why-test-suites-get-slow).

---

## 🎭 Replacing Beans: @MockitoBean and @MockitoSpyBean

Sometimes you want the real application, except for one bean: the one that writes to the disk, calls a payment provider or is slow. Spring Boot 4 uses Spring Framework 7's unified **bean override** mechanism for this. `@MockBean` and `@SpyBean` are gone; their replacements live in `org.springframework.test.context.bean.override.mockito`:

| Annotation | What happens to the bean |
|------------|--------------------------|
| `@MockitoBean` | replaced by a Mockito mock (also if the bean does not exist yet) |
| `@MockitoSpyBean` | the real bean is wrapped in a Mockito spy: it keeps working, but calls can be stubbed and verified (the bean **must already exist**) |

[`BeanOverrideIntegrationTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/integration/BeanOverrideIntegrationTest.java) shows both in a full-stack test:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:bean-override-test")
@AutoConfigureRestTestClient
class BeanOverrideIntegrationTest {

    @MockitoBean
    private FileStorageService fileStorageService;   // no files are written to disk

    @MockitoSpyBean
    private PizzaService pizzaService;               // the real service, but observable

    @Test
    void createPizza_ThroughTheApi_CallsTheRealServiceExactlyOnce() {
        client.post().uri("/api/pizzas") /* ... */ .expectStatus().isCreated();

        verify(pizzaService).create(any(CreatePizzaRequest.class));
    }
}
```

The image-upload test replaces only `FileStorageService`: the real controller, service, mapper and database still run, the "storage" is a mock that returns a fixed URL, and the test then reads the pizza back to prove the URL was persisted.

In `@WebMvcTest` classes you have already used `@MockitoBean` for the service that is not part of the slice. It is the same annotation.

---

## 🔧 Overriding Properties in Tests

Tests often need other configuration than production: another database, a temporary upload folder, a fake URL for an external API. Spring Boot gives you six places to put it, from "for every test" to "for this one test":

| Where | Scope | Use it for |
|-------|-------|------------|
| `src/test/resources/application.properties` | every test | the shared test configuration |
| `application-test.properties` + `@ActiveProfiles("test")` | the tests that activate the profile | a *named* configuration, for example `it` or `postgres` |
| `@SpringBootTest(properties = "...")` or `@WebMvcTest(properties = "...")` | one test class | one or two values that are specific to this class |
| `@TestPropertySource(properties = "...")` or `@TestPropertySource("/custom.properties")` | one test class | the same, or a whole file that does not follow Spring Boot's naming |
| `@DynamicPropertySource` | one test class | values that are only known **at runtime**: the random port of a WireMock server, the URL of a container |
| constructor argument (no Spring) | one unit test | `new OrderService(Set.of("BE"))` instead of `ReflectionTestUtils.setField(...)` |

### The shared test file *replaces* the main file

The first row hides a trap. `src/test/resources/application.properties` has the same name as `src/main/resources/application.properties`, and the test classpath comes first. Spring Boot therefore loads **only the test file**: the two are *not* merged. That is why [`application.properties` in the test resources](pizzastore-with-tests/src/test/resources/application.properties) of this project repeats the datasource settings, switches off `data.sql` and moves the upload folder:

```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
spring.sql.init.mode=never
file.upload-dir=target/test-uploads
```

If you want to *add* to the main configuration instead of replacing it, use a profile file (`application-test.properties` is loaded **on top of** `application.properties`) or one of the per-class options.

### Per class: `properties`

The two real-server tests of this project give each class its own database, so that their contexts never share the same in-memory H2 (see the next section for why that matters):

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:api-integration-test")
class PizzaStoreApiIntegrationTest { ... }
```

`@TestPropertySource(properties = "...")` does the same and has a slightly higher precedence.

### Runtime values: `@DynamicPropertySource`

Some values do not exist until the test starts: a container gets a random port, a fake HTTP server too. A static method with `@DynamicPropertySource` registers them as *suppliers*, so Spring reads them when it builds the context:

```java
// NOT part of the project: it needs a fake server on a random port
@SpringBootTest
class OpenFoodFactsIntegrationTest {

    static final WireMockServer server = new WireMockServer(options().dynamicPort());

    @BeforeAll
    static void start() { server.start(); }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.http.serviceclient.openfoodfacts.base-url", server::baseUrl);
    }
}
```

With Testcontainers the same method is replaced by `@ServiceConnection` (see [When H2 Is Not Enough](#-when-h2-is-not-enough-testcontainers)).

### Which one wins?

When the same key is set in several places, the strongest source wins: `@DynamicPropertySource`, then `@TestPropertySource`, then the `properties` attribute, then the profile and `application.properties` files. Whatever you choose, remember the price: **every different combination of these settings is a different context** (next section).

> 💡 **Rule of thumb**: put what is true for *all* tests in the test `application.properties`, and what is true for *one* test next to that test. Prefer constructor injection over `ReflectionTestUtils` in unit tests: a renamed field then breaks the compiler instead of the test.

---

## 🧠 The Context Cache: Why Test Suites Get Slow

Starting a Spring context takes seconds: component scan, bean creation, JPA, Tomcat. If every test class started its own, a suite of 20 classes would spend most of its time starting applications. Spring therefore **caches** every context it has started, in a map that lives for the whole test run, and hands the same instance to every test class that needs the same one.

### What makes two tests "the same"?

Spring builds a **cache key** from the configuration of the test class. Two classes share a context only if all of this is identical:

| Part of the key | Changed by |
|-----------------|-----------|
| the configuration classes and initializers | `@ContextConfiguration`, `@Import`, the slice annotation (`@WebMvcTest(PizzaController.class)` vs. `@WebMvcTest(CustomerController.class)`) |
| the active profiles | `@ActiveProfiles` |
| the property sources | `properties = ...`, `@TestPropertySource` |
| the context customizers | `@MockitoBean`, `@MockitoSpyBean`, `@DynamicPropertySource`, `@AutoConfigureMockMvc`, `@AutoConfigureRestTestClient` |
| the web environment | `webEnvironment = MOCK` vs. `RANDOM_PORT` |

A test that is annotated with `@DirtiesContext` additionally throws its context away after the class or method.

### What does it look like in PizzaStore?

Switch on the debug log of the cache package and run the suite:

```bash
mvn test -Dlogging.level.org.springframework.test.context.cache=DEBUG
```

Spring then prints a line like this after every lookup:

```
Spring test ApplicationContext cache statistics: [DefaultContextCache@... size = 12, maxSize = 32, contextUsageCount = 1, parentContextCount = 0, hitCount = 1523, missCount = 12, failureCount = 0]
```

Every **miss** is a context that had to be started. The 226 tests of this project use **12 different contexts**: one for `@JsonTest`, one for the three `@DataJpaTest` classes together (same key, so one start), one for every `@WebMvcTest` variant (a different controller, `RestTestClient` or `MockMvcTester` makes a different key), one each for the smoke test, `PizzaIntegrationTest`, `PizzaStoreApiIntegrationTest`, `BeanOverrideIntegrationTest` (it has two bean overrides), the `PizzaMapperTest` mini-context and the `@RestClientTest`. The six pure unit-test classes need none. This is a *good* number for a lesson that demonstrates every kind of test; a real project with the same coverage would try to have fewer.

### The usual suspects

| Cause | Why it costs a context | Do instead |
|-------|-----------------------|-----------|
| `@MockitoBean` / `@MockitoSpyBean` in a `@SpringBootTest` | every different set of overrides is a new key | use a slice (`@WebMvcTest`) and mock there, or share the same set of overrides in a base class |
| `properties = ...` per class | every different value is a new key | only when you need it (see the H2 trap below), otherwise the test `application.properties` |
| `@DirtiesContext` as a quick fix for "my tests influence each other" | the context is discarded and restarted | clean up the data in the test (`@Transactional`, unique data, `@AfterEach`) |
| many profiles | one context per profile combination | keep `test` and at most one or two extra |
| `@SpringBootTest` where a slice suffices | a full context is ten times heavier than a slice | the cheapest test that can fail (see [Choosing the Right Kind of Test](#-choosing-the-right-kind-of-test)) |

If several classes really need the same full-stack setup, put it once in a shared base class (or a composed annotation) so that they have the same key:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
abstract class AbstractApiTest { }

class PizzaApiTest extends AbstractApiTest { ... }
class CustomerApiTest extends AbstractApiTest { ... }   // same context as PizzaApiTest
```

### The H2 trap: a cached context keeps its database

A cached context is **never closed during the test run**, and `jdbc:h2:mem:testdb` is a *named* in-memory database that every context in the same JVM can reach. Two full contexts with the same name and `ddl-auto=create-drop` therefore work on the same tables, and one context can wipe or fill what the other expects. That is why the two classes that start a real server (`PizzaStoreApiIntegrationTest` and `BeanOverrideIntegrationTest`) each set their own `spring.datasource.url`. It is also the price of that choice: those classes can never share a context. When you build your own suite, either clean up after each test and share one context, or isolate completely and accept the startup time. Do not mix the two by accident.

### Contexts are paused when idle

Spring Framework 7 adds **automatic context pausing**: a cached context that no test is using is *paused* (its `Lifecycle` and `SmartLifecycle` beans are stopped) and restarted when a later test class needs it. This stops the background threads and scheduled tasks of the contexts that are only waiting in the cache. A component that must keep running can opt out by returning `false` from `SmartLifecycle#isPauseable()`. The cache itself holds at most 32 contexts by default and evicts the least recently used one; the limit can be changed with the JVM property `spring.test.context.cache.maxSize`.

---

## ✅ Testing Validation and Error Responses

Lesson 10 added two things worth protecting: **validation rules** and the **`ProblemDetail` error contract**. Each can be tested at three levels:

| Level | Question it answers | Example |
|-------|---------------------|---------|
| Plain `Validator` | *Is the constraint correct?* (`@DecimalMin("0.01")`) | [`RequestValidationTest`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/dto/request/RequestValidationTest.java) |
| `@WebMvcTest` | *Is `@Valid` wired up, and does the response have the right shape?* | `createPizza_InvalidRequest_Returns400WithFieldErrors` in `PizzaControllerRestTestClientTest` |
| `@SpringBootTest` | *Does it work end to end, with the real exception handler?* | `Errors` in `PizzaStoreApiIntegrationTest` |

### Constraints without Spring

A `Validator` can be built by hand, so you can test a constraint in microseconds. The test names each violation as `"property path: message"`, which also shows *which element* of a list was wrong:

```java
@Nested
class CreateOrderRequestTests {

    @Test
    void quantityZero_IsRejectedInsideTheOrderLine() {
        CreateOrderRequest request = new CreateOrderRequest(1L,
                List.of(new CreateOrderRequest.OrderLineRequest(1L, 0)));

        assertThat(violationsOf(request)).containsExactly("orderLines[0].quantity: Quantity must be at least 1");
    }
}
```

`@ParameterizedTest` runs one test body for many inputs, perfect for boundary values:

```java
@ParameterizedTest
@CsvSource({
        "0.00,  false",
        "0.01,  true",
        "-5.00, false",
        "99.99, true"
})
void price_MustBeAtLeastOneCent(String price, boolean valid) {
    assertThat(violationsOf(pizza("Margherita", price, true)).isEmpty()).isEqualTo(valid);
}
```

### Wiring and response shape in the web slice

With the service mocked, `@Valid` must reject the request *before* the service is called, and the answer must be Lesson 10's problem detail with a list of field errors:

```java
@Test
void createPizza_InvalidRequest_Returns400WithFieldErrors() {
    CreatePizzaRequest request = new CreatePizzaRequest("", new BigDecimal("-1"), "Broken", true, null);

    client.post().uri("/api/pizzas")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus().isBadRequest()
            .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.detail").isEqualTo("Validation failed")
            .jsonPath("$.errors[?(@.field == 'name')].message").isEqualTo("Pizza name is required")
            .jsonPath("$.errors[?(@.field == 'price')].message").isEqualTo("Price must be positive");

    verify(pizzaService, never()).create(any());   // never reached the service
}
```

### The whole error table, end to end

[`PizzaStoreApiIntegrationTest.Errors`](pizzastore-with-tests/src/test/java/be/vives/pizzastore/integration/PizzaStoreApiIntegrationTest.java) automates the *Before and After* table of Lesson 10 against the running application:

| Scenario | Expected |
|----------|----------|
| `GET /api/pizzas/987654` | `404` + `application/problem+json`, `detail` = "Pizza with id 987654 not found", `instance` = the path |
| `POST /api/pizzas` with a blank name and a negative price | `400` + both field errors in `errors` |
| `POST /api/pizzas` with `{ this is not json` | `400`, "Malformed JSON request" |
| `GET /api/does-not-exist` | `404` as `application/problem+json`: an error thrown by Spring MVC itself |
| `PATCH /api/pizzas/1` (no such mapping) | `405` with an `Allow` header |
| Second customer with the same e-mail | `409` |
| Order for an unknown pizza | `422`, and **no order is created** |
| Cancel an order twice | `204`, then `422` "Order is already cancelled" |

The "unknown URL" and "wrong method" rows are the payoff of `ResponseEntityExceptionHandler` in Lesson 10: even errors you never wrote a handler for have the same format.

---

## 💪 Assertions: AssertJ, JsonPath and Hamcrest

### AssertJ: the default

AssertJ assertions are fluent, readable and have excellent IDE completion:

```java
assertThat(result).isNotNull();
assertThat(pizza.getPrice()).isEqualByComparingTo("8.50");     // BigDecimal: ignores scale (8.5 == 8.50)
assertThat(pizzas).extracting(Pizza::getName).containsExactlyInAnyOrder("Cheap Pizza", "Mid Pizza");
assertThatThrownBy(() -> service.cancel(1L)).isInstanceOf(BusinessException.class).hasMessage("Order is already cancelled");
```

Two details that bite beginners: compare `BigDecimal` with `isEqualByComparingTo` (`new BigDecimal("8.5").equals(new BigDecimal("8.50"))` is `false`), and prefer `containsExactlyInAnyOrder` over `containsExactly` when the order is not part of the requirement.

### JsonPath: query JSON instead of comparing strings

Comparing a complete JSON string is brittle: it breaks when you add a field. JsonPath asserts only what you care about:

| JsonPath | Selects |
|----------|---------|
| `$.name` | the field `name` |
| `$.content[0].name` | the `name` of the first element of `content` |
| `$.content.length()` | the number of elements |
| `$.errors[?(@.field == 'price')].message` | the message of the error whose `field` is `price` |
| `$[?(@.price > 2.00)]` | all elements with a price above 2 (assert `.isEmpty()` for "none") |

JsonPath is available in `MockMvc` (`jsonPath(...)`), in `MockMvcTester` (`extractingPath(...)`), in `RestTestClient` (`expectBody().jsonPath(...)`) and in `JacksonTester`.

### Hamcrest: still around

MockMvc tests use Hamcrest matchers (`is`, `hasSize`, `containsString`). The book notes that many developers keep them *inside MockMvc*, and that `RestTestClient` and AssertJ are "the modern defaults". In new code, prefer AssertJ.

### Awaitility: asserting asynchronous work

If code does its work on another thread (events, messaging, `@Async`), assert with Awaitility instead of `Thread.sleep(...)`: `await().atMost(5, SECONDS).untilAsserted(() -> ...)`. It is part of the test starter. PizzaStore has no asynchronous code, so there is no example in the project.

---

## 🐳 When H2 Is Not Enough: Testcontainers

All database tests in this lesson run against **H2**. That is fast and needs no installation, but H2 is not PostgreSQL: SQL dialect, locking, JSON columns and native queries can behave differently. A test that passes on H2 can fail in production. The book (Chapter 10) therefore recommends **Testcontainers 2.0** for tests that need the *real* database: it starts a Docker container for the duration of the tests, and Spring Boot's **`@ServiceConnection`** wires the datasource to it with zero configuration:

```java
// NOT part of the project: the PostgreSQL container needs Docker
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)   // do not swap in H2
@Testcontainers
class PizzaRepositoryPostgresTest {

    @Container
    @ServiceConnection                                  // sets spring.datasource.url/username/password
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    // ... the same tests as PizzaRepositoryTest, now against a real PostgreSQL
}
```

You would add `spring-boot-testcontainers`, `org.testcontainers:testcontainers-junit-jupiter` and `org.testcontainers:testcontainers-postgresql` as test dependencies (Spring Boot 4 manages the version). The final PizzaStore's `prod` profile runs on PostgreSQL, which is exactly the situation where this pays off.

> **This is deliberately not in the project.** It needs Docker, which a student's laptop or the build server may not have, and the example above has not been executed as part of this course. The book also warns that Docker Engine 29 has networking problems with Testcontainers 2.0. Start with H2, and add Testcontainers when the differences with production start to hurt.

The same chapter shows `SpringApplication.from(...).with(TestcontainersConfiguration.class)`, a "test main" that starts your application with containerized infrastructure, so developers can run it without installing a database at all.

---

## 🏗️ Running the Tests with Maven: Surefire and Failsafe

Maven runs tests with two plugins that differ in **when** they run and **which classes** they pick up:

| | Surefire | Failsafe |
|---|----------|----------|
| Phase | `test` (before packaging) | `integration-test` + `verify` (after packaging) |
| Picks up | `*Test`, `Test*`, `*Tests`, `*TestCase` | `*IT`, `IT*`, `*ITCase` |
| Typical content | unit tests and slices | full-stack tests |
| Command | `mvn test` | `mvn verify` (runs both) |

Spring Boot's parent POM already configures and versions Surefire (3.5.6 in this project), which is why `mvn test` works with a bare `pom.xml`. **This project keeps all 226 tests in Surefire** to stay simple: one command, one result.

In a real project you split them, so that developers get the fast feedback of `mvn test` while the slow full-stack tests run on `mvn verify` (and on the build server). That takes two steps: rename the full-stack classes to `*IT` (`ApplicationSmokeTest` becomes `ApplicationSmokeIT`) and add Failsafe to the `<build>`. The parent already manages its version:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>integration-test</goal>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

> ✅ **Verified on a copy of this project**: after renaming the four classes in `integration/` to `*IT`, `mvn verify` ran **206** tests in Surefire and then **20** in Failsafe. `mvn test` ran only the 206. The `verify` goal is not optional: without it a failing integration test does not fail the build.

| Command | Does |
|---------|------|
| `mvn test` | the unit and slice tests only |
| `mvn verify` | everything, including the `*IT` classes |
| `mvn verify -DskipITs` | everything except the integration tests (still runs Surefire) |
| `mvn test -Dtest=PizzaServiceTest#findById_ExistingPizza_ReturnsPizzaResponse` | one test method |
| `mvn verify -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=ApplicationSmokeIT` | one integration test class |

Both plugins fail the build on a red test, so a pipeline only has to run `mvn verify`.

---

## 💡 Best Practices

### 1. Name tests after behavior, structure them as Given / When / Then

`method_Condition_ExpectedResult` tells you what broke without opening the test:

```java
@Test
void cancel_whenAlreadyDelivered_shouldThrowException() {
    // Given: an order that was delivered
    // When:  we cancel it
    // Then:  the business rule rejects it
}
```

### 2. Test behavior, not implementation

Assert on **results** (return values, status codes, stored data), and use `verify(...)` only for interactions that *are* the behavior (`verify(pizzaService, never()).create(any())`: "validation stops the request"). A test that mirrors every internal call breaks on every refactoring.

### 3. Do not test the framework

Do not write tests proving that `save()` saves or that `@NotBlank` rejects blanks *in general*. Test **your** query, **your** constraint on **your** field, **your** mapping.

### 4. Keep tests independent

No test may rely on another having run first. Use `@BeforeEach` for fresh state, roll back in `@DataJpaTest`, unique data in server tests. Test order is not guaranteed.

### 5. Group with `@Nested`, vary with `@ParameterizedTest`

JUnit's `@Nested` classes turn a long test class into readable sections (`RequestValidationTest.CreatePizzaRequestTests`, `PizzaStoreApiIntegrationTest.Errors`). The book calls this "living documentation": the test report reads like a specification. Use `@ParameterizedTest` for boundary values instead of copy-pasting a test.

### 6. Test edge cases and failures

The happy path rarely breaks. Test the empty list, the boundary (`0.00` vs `0.01`), the unknown id, the duplicate, the downstream server that returns `503`.

### 7. Keep it fast

A suite you run on every save must stay fast. Prefer the cheapest test (see [Choosing the Right Kind of Test](#-choosing-the-right-kind-of-test)), limit the number of distinct `@SpringBootTest` configurations (see [The Context Cache](#-the-context-cache-why-test-suites-get-slow)), and never `Thread.sleep(...)`.

### 8. A test must be able to fail

A test that has never failed has not been proven to work. When you write one, break the production code on purpose (or change the expected value) and check that the test turns red.

---

## 🍕 The PizzaStore Test Suite

The project contains **226 tests**. `mvn test` runs them all in about half a minute.

| Kind | Test class | Tests | Covers |
|------|------------|------:|--------|
| **Unit** | `PizzaServiceTest`, `CustomerServiceTest`, `OrderServiceTest` | 51 | services with mocked repositories/mappers |
| | `NutritionImportServiceTest` | 9 | Open Food Facts failures → PizzaStore exceptions, Mockito |
| | `RequestValidationTest` (4 nested classes) | 32 | Jakarta constraints, parameterized |
| | `PizzaControllerStandaloneTest` | 3 | `RestTestClient.bindToController` |
| | `PizzaMapperTest` | 3 | MapStruct mapper in a 2-bean context |
| **Slice** | `PizzaRepositoryTest`, `CustomerRepositoryTest`, `OrderRepositoryTest` | 40 | `@DataJpaTest`, custom queries |
| | `PizzaControllerTest`, `CustomerControllerTest`, `OrderControllerTest` | 49 | `@WebMvcTest` + MockMvc |
| | `PizzaControllerRestTestClientTest` | 6 | `@WebMvcTest` + `RestTestClient` |
| | `PizzaControllerMockMvcTesterTest` | 3 | `@WebMvcTest` + `MockMvcTester` |
| | `PizzaJsonTest` | 5 | `@JsonTest` |
| | `OpenFoodFactsClientTest` | 5 | `@RestClientTest` + declarative client |
| **Full stack** | `ApplicationSmokeTest` | 1 | context starts |
| | `PizzaIntegrationTest` | 4 | `@SpringBootTest` + MockMvc + `@Transactional` |
| | `PizzaStoreApiIntegrationTest` (3 nested classes) | 13 | `RANDOM_PORT` + `RestTestClient` |
| | `BeanOverrideIntegrationTest` | 2 | `@MockitoBean`, `@MockitoSpyBean` |

Most service, repository and controller tests have a counterpart in the final PizzaStore's suite. Here they run without Spring Security, which only arrives in Lesson 13, so the tests that check `401`/`403` per role are left out; in the final project the controller tests are also written with `RestTestClient` instead of MockMvc. The comparison classes (`PizzaControllerRestTestClientTest`, `PizzaControllerMockMvcTesterTest`, `PizzaControllerStandaloneTest`), `PizzaJsonTest`, `PizzaMapperTest`, the full-stack tests and the Open Food Facts tests demonstrate the Spring Boot 4 additions of this lesson.

The new test classes start with a Javadoc comment that says what they demonstrate, so reading the test sources is a good way to study this lesson.

---

## 🎓 Summary

### What We Learned

1. **Test pyramid**: many fast unit tests, fewer slice tests, a handful of full-stack tests.
2. **Spring Boot 4 test setup**: modular test starters (`spring-boot-starter-webmvc-test`, `-data-jpa-test`, `-restclient-test`), JUnit 6, and moved annotation packages.
3. **Unit tests**: JUnit + Mockito + AssertJ without Spring; Jakarta `Validator` and MapStruct mappers can be tested the same way.
4. **Slices**: `@DataJpaTest` (repositories + H2), `@WebMvcTest` (controllers + advice), `@JsonTest` (JSON contract), `@RestClientTest` (outgoing HTTP with `MockRestServiceServer`).
5. **`RestTestClient`**: one fluent API for mock MVC, a single controller, or a real server, next to MockMvc (Hamcrest) and `MockMvcTester` (AssertJ).
6. **`@SpringBootTest`**: `MOCK` + `@Transactional` rollback or `RANDOM_PORT` + unique data; smoke test; context caching.
7. **Bean overrides**: `@MockitoBean` and `@MockitoSpyBean` replace `@MockBean` and `@SpyBean`.
   **Properties**: test `application.properties` (replaces the main file), profiles, `properties`, `@TestPropertySource`, `@DynamicPropertySource`.
   **Context cache**: every different configuration is a new context; slices, shared base classes and no `@DirtiesContext` keep the suite fast.
8. **Testing Lesson 10's contract**: constraints, `@Valid` wiring and every `ProblemDetail` response, at three levels.
9. **Testcontainers** for tests that need the real database.
10. **Maven**: Surefire runs `*Test` in `mvn test`, Failsafe runs `*IT` in `mvn verify`.

### Key Takeaways

✅ Use the cheapest test that can fail for the reason you care about
✅ A slice only knows what you tell it: import configuration, mock the layers it does not load
✅ `RestTestClient` is the one client for slice and full-stack tests; MockMvc stays supported
✅ `@Transactional` rolls back MockMvc tests, not tests against a real server
✅ Create test data through the API with unique values, and never rely on an empty database
✅ Every different `@MockitoBean`, profile or property is a new context: count your contexts
✅ Test behavior, not implementation, and make sure a test can fail

---

## 📖 Additional Resources

- [Spring Boot Reference: Testing](https://docs.spring.io/spring-boot/reference/testing/index.html)
- [Spring Boot Reference: Test Slices (auto-configured tests)](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html)
- [Spring Framework Reference: Testing](https://docs.spring.io/spring-framework/reference/testing.html)
- [Spring Framework Reference: `RestTestClient`](https://docs.spring.io/spring-framework/reference/testing/resttestclient.html)
- [Spring Framework Reference: Bean Overriding in Tests](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/bean-overriding.html)
- [JUnit 6 User Guide](https://docs.junit.org/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [Testcontainers for Java](https://java.testcontainers.org/)

**Philip Riecks (rieckpil.de)**, the most-read blog on Spring Boot testing. Many articles were written before Spring Boot 4: the ideas hold, but package names and `@MockBean` have changed, so use the tables in this lesson when a listing does not compile.

- [What's New for Testing in Spring Boot 4.0 and Spring Framework 7](https://rieckpil.de/whats-new-for-testing-in-spring-boot-4-0-and-spring-framework-7/)
- [Spring Boot Test Slices: Overview and Usage](https://rieckpil.de/spring-boot-test-slices-overview-and-usage/)
- [Improve Build Times with Context Caching](https://rieckpil.de/improve-build-times-with-context-caching-from-spring-test/)
- [Override Spring Boot Configuration Properties for Tests](https://rieckpil.de/override-spring-boot-configuration-properties-for-tests/)
- [Maven Setup for Testing Java Applications](https://rieckpil.de/maven-setup-for-testing-java-applications/)
- [Guide to Testing Spring Boot Applications with MockMvc](https://rieckpil.de/guide-to-testing-spring-boot-applications-with-mockmvc/)
- [Testing JSON Serialization with `@JsonTest`](https://rieckpil.de/testing-your-json-serialization-with-jsontest/)
- [Write Spring Boot Integration Tests with Testcontainers](https://rieckpil.de/howto-write-spring-boot-integration-tests-with-a-real-database/)
- [The Missing Spring Boot Testing Manual](https://rieckpil.de/free-spring-boot-testing-book/) (free book)
- [Start here: all testing articles](https://rieckpil.de/start-here/)

---

**Note on the book**: *Pro Spring Boot 4* devotes Chapter 10, *Advanced Testing with Spring Boot* (p. 259-278), to this lesson: *The Testing Pyramid and @SpringBootTest*, *Modularized Test Starters*, *Embracing the JUnit 6 Baseline* (with `@Nested`), *Mastering Test Slices* (`@WebMvcTest`, `@JsonTest`, `@RestClientTest`, plus the WebFlux and R2DBC slices), *Verification Strategies: Mockito and Hamcrest*, *RestTestClient*, *Advanced Assertions: Awaitility and JsonPath*, *Advanced Bean Overriding* (`@MockitoBean`, `@MockitoSpyBean`), `@RecordApplicationEvents`, and *Testcontainers 2.0* with `@ServiceConnection`. The unified `RestTestClient` was already introduced in Chapter 3 (*Unified Testing with RestTestClient*, p. 77-78: `bindToController` for unit tests and `bindToServer`/`@AutoConfigureRestTestClient` for integration tests), and Chapter 1 (Listing 1-7) uses it in the very first test of the quick start; Chapter 4 (*Integration Testing the Repository*, `@SpringBootTest` + `@Transactional`), Chapter 6 (`@DataJpaTest` and repository tests) and Chapter 7 (`@DataMongoTest` with Testcontainers) test the persistence layer. Where PizzaStore goes beyond the book: tests for the Lesson 10 error contract, `MockMvcTester`, parameterized Jakarta-validation tests, and an explicit comparison of MockMvc, `MockMvcTester` and `RestTestClient` for the same controller. Two things to watch when you read the book: its Listing 3-11 imports `AutoConfigureRestTestClient` from `org.springframework.boot.test.autoconfigure.web.servlet`, while in Spring Boot 4.0.8 the annotation lives in `org.springframework.boot.resttestclient.autoconfigure` (as in Listings 1-7 and 4-10, and as used in this project); and although Chapter 3 presents `RestTestClient` as replacing MockMvc and `WebTestClient`, MockMvc remains fully supported and is still used by the book itself in Listing 10-4. The book's `@RecordApplicationEvents` example has no counterpart here because PizzaStore publishes no application events, and its Testcontainers example is shown in this README but not executed in the project (no Docker requirement for students).

---

## 🚀 Runnable Project

**[`pizzastore-with-tests/`](pizzastore-with-tests)** is Lesson 11's [`pizzastore-with-external-api`](../lesson-11-external-api/pizzastore-with-external-api) plus the test suite described above. **The production code is identical**: the project differs from the final PizzaStore only by what Lessons 13 (security) and 14 (OpenAPI) still have to add.

The project includes:
- ✅ **Spring Boot 4.0** on **Java 25** (`spring-boot-starter-webmvc`, `-data-jpa`, `-validation`, H2, MapStruct 1.6.3), JUnit 6, Mockito 5, AssertJ
- ✅ Everything from Lessons 6a, 7, 9 and 10
- ✅ 226 tests: unit tests, `@DataJpaTest`, `@WebMvcTest`, `@JsonTest`, `@RestClientTest`, `@SpringBootTest`
- ✅ The same web-layer test with MockMvc, `RestTestClient` and `MockMvcTester`
- ✅ `RestTestClient` against a real server on a random port
- ❌ No security yet: Lesson 13
- ❌ No API documentation yet: Lesson 14

### Running the Tests

```bash
cd pizzastore-with-tests
mvn test
```

Run one test class, or one nested class:

```bash
mvn test -Dtest=PizzaControllerRestTestClientTest
mvn test -Dtest='PizzaStoreApiIntegrationTest*'
```

The tests need a JDK 25 (`java -version`). If your default `mvn` picks another JDK, point `JAVA_HOME` to JDK 25 first.

### Running the Application

```bash
mvn spring-boot:run
```

The H2 console is available at http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:pizzastore_tests`, user `sa`, no password).

---

**Congratulations!** 🎉 PizzaStore is now protected by an automated safety net. Continue to [Lesson 13: JWT Authentication](../lesson-13-jwt-authentication/README.md) to secure the API, and watch how the tests tell you what the new security rules break.
